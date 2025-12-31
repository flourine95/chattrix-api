# WebSocket & User Status Issues Analysis

## 🔴 Critical Issues Found

### 1. UserStatusService - Mỗi message đều UPDATE database

**Location:** `UserStatusService.updateLastSeen()`

**Problem:**
```java
@OnMessage
public void onMessage(Session session, WebSocketMessage<?> message) {
    Long userId = (Long) session.getUserProperties().get("userId");
    userStatusService.updateLastSeen(userId);  // ❌ UPDATE DB mỗi message!
    // ...
}

@Transactional
public void updateLastSeen(Long userId) {
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
        user.setLastSeen(Instant.now());
        userRepository.save(user);  // ❌ DB write mỗi message!
    }
}
```

**Impact:**
- User gửi 100 messages → 100 DB updates
- High DB load
- Slow performance
- Unnecessary writes

**Solution:** Batch update với cache

---

### 2. UserStatusService - Đã có in-memory tracking nhưng vẫn UPDATE DB

**Current Implementation:**
```java
// ✅ GOOD: In-memory tracking
private final ConcurrentMap<Long, Integer> activeSessionsCount = new ConcurrentHashMap<>();

// ❌ BAD: Vẫn update DB
@Transactional
public void setUserOnline(Long userId) {
    activeSessionsCount.merge(userId, 1, Integer::sum);  // ✅ In-memory
    
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
        user.setLastSeen(Instant.now());
        userRepository.save(user);  // ❌ Không cần thiết!
    }
}
```

**Why it's wrong:**
- `activeSessionsCount` đã track online status in-memory
- `isUserOnline()` chỉ check map, không query DB
- Nhưng vẫn UPDATE DB mỗi lần connect/disconnect
- `lastSeen` chỉ cần update khi user thực sự offline

**Impact:**
- Mỗi WebSocket connection → DB update (không cần)
- User có nhiều tabs → nhiều DB updates (không cần)
- Online status đã có in-memory, không cần persist ngay

---

### 3. HeartbeatMonitorService - Không có batch update

**Problem:**
```java
@Schedule(second = "*/15", minute = "*", hour = "*", persistent = false)
public void checkStaleHeartbeats() {
    // Chỉ check timeout, không batch update lastSeen
    lastHeartbeat.entrySet().removeIf(entry -> {
        if (lastBeat.isBefore(threshold)) {
            userStatusService.setUserOffline(userId);  // ❌ Individual update
        }
    });
}
```

**Missing:**
- Không có scheduled batch update cho lastSeen
- Không có cache cho pending updates

---

### 4. ChatServerEndpoint - Duplicate user lookups

**Problem:**
```java
// Lookup 1: Get user for status broadcast
UserResponse userResponse = userProfileCache.get(userId);
if (userResponse == null) {
    User user = userRepository.findById(userId).orElse(null);  // ❌ DB query
}

// Lookup 2: Get user for typing indicator
List<User> missingUsers = userRepository.findByIds(new ArrayList<>(missingIds));  // ❌ DB query

// Lookup 3: Get user for mentions
List<User> missingUsers = userRepository.findByIds(missingIds);  // ❌ DB query
```

**Impact:**
- Multiple DB queries cho cùng users
- Cache không được dùng hiệu quả

---

### 5. broadcastUserStatusChange - Duplicate logic

**Problem:**
Có 2 nơi implement cùng logic:
1. `ChatServerEndpoint.broadcastUserStatusChange()`
2. `HeartbeatMonitorService.broadcastUserStatusChange()`

**Issues:**
- Code duplication
- Inconsistent behavior
- Hard to maintain

---

## 🎯 Architecture Overview

### Current (Wrong) Architecture:
```
WebSocket Message
    ↓
UserStatusService.updateLastSeen()
    ↓
@Transactional + userRepository.save()  ❌ DB write mỗi message!
    ↓
Database UPDATE
```

### Correct Architecture:
```
WebSocket Message
    ↓
UserStatusService.updateLastSeen()
    ↓
Queue in ConcurrentHashMap (in-memory)  ✅ Fast, no DB
    ↓
[Wait 30 seconds...]
    ↓
UserStatusBatchService.flushPendingUpdates()
    ↓
Single batch UPDATE query  ✅ 1 query cho nhiều users
    ↓
Database UPDATE
```

### Online Status Flow:
```
User Connect
    ↓
activeSessionsCount.merge(userId, 1)  ✅ In-memory only
    ↓
isUserOnline(userId) → check map  ✅ No DB query
    ↓
User Disconnect (last session)
    ↓
activeSessionsCount.remove(userId)  ✅ In-memory
    ↓
Queue lastSeen update  ✅ Batch later
```

---

### Solution 1: Batch Update Service

Tạo service để batch update lastSeen:

```java
@Singleton
public class UserStatusBatchService {
    
    private final ConcurrentMap<Long, Instant> pendingUpdates = new ConcurrentHashMap<>();
    
    @Inject
    private UserRepository userRepository;
    
    /**
     * Queue update (in-memory only)
     */
    public void queueLastSeenUpdate(Long userId) {
        pendingUpdates.put(userId, Instant.now());
    }
    
    /**
     * Batch update every 30 seconds
     */
    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    @Transactional
    public void flushPendingUpdates() {
        if (pendingUpdates.isEmpty()) return;
        
        // Get all pending updates
        Map<Long, Instant> updates = new HashMap<>(pendingUpdates);
        pendingUpdates.clear();
        
        // Batch update in single query
        userRepository.batchUpdateLastSeen(updates);
        
        log.info("Batch updated lastSeen for {} users", updates.size());
    }
}
```

### Solution 2: Fix UserStatusService - Dùng OnlineStatusCache (Caffeine)

**Principle:** 
- Online status = Caffeine cache (fast, auto-expiration, no manual cleanup)
- lastSeen = chỉ update DB khi batch update

**Why Caffeine instead of ConcurrentHashMap:**
- ✅ Auto-expiration (không cần manual cleanup)
- ✅ LRU eviction (tự động remove old entries)
- ✅ Thread-safe
- ✅ Statistics & monitoring
- ✅ Consistent với các cache khác trong project

```java
@ApplicationScoped
public class UserStatusService {
    
    @Inject
    private OnlineStatusCache onlineStatusCache;  // ✅ Caffeine cache
    
    @Inject
    private UserStatusBatchService batchService;
    
    /**
     * Set user online - Use Caffeine cache
     */
    public void setUserOnline(Long userId) {
        onlineStatusCache.markOnline(userId);  // ✅ Caffeine cache
        batchService.queueLastSeenUpdate(userId);
    }
    
    /**
     * Set user offline - Use Caffeine cache
     */
    public void setUserOffline(Long userId) {
        onlineStatusCache.markOffline(userId);  // ✅ Caffeine cache
        batchService.queueLastSeenUpdate(userId);
    }
    
    /**
     * Update last seen - Update cache and queue DB update
     */
    public void updateLastSeen(Long userId) {
        onlineStatusCache.markOnline(userId);  // ✅ Update cache timestamp
        batchService.queueLastSeenUpdate(userId);
    }
    
    /**
     * Check online status - Caffeine cache only, NO DB query
     */
    public boolean isUserOnline(Long userId) {
        return onlineStatusCache.isOnline(userId);  // ✅ Fast, no DB
    }
    
    /**
     * Get online users - Caffeine cache
     */
    public Set<Long> getOnlineUserIds() {
        return onlineStatusCache.getOnlineUserIds();  // ✅ Fast, no DB
    }
}
```

**OnlineStatusCache (Already exists in project):**
```java
@ApplicationScoped
public class OnlineStatusCache {
    
    private final Cache<Long, Instant> onlineUsers = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)  // Auto-expiration
        .maximumSize(100_000)  // LRU eviction
        .build();
    
    public void markOnline(Long userId) {
        onlineUsers.put(userId, Instant.now());
    }
    
    public boolean isOnline(Long userId) {
        Instant lastSeen = onlineUsers.getIfPresent(userId);
        return lastSeen != null && 
            Duration.between(lastSeen, Instant.now()).toMinutes() < 2;
    }
}
```

**Key Changes:**
1. ❌ Remove `ConcurrentHashMap activeSessionsCount`
2. ✅ Use `OnlineStatusCache` (Caffeine)
3. ✅ Auto-expiration (no manual cleanup needed)
4. ✅ Consistent với UserProfileCache, MessageCache, etc.
5. ✅ Better monitoring & statistics

### Solution 3: Add Batch Update to Repository

```java
@ApplicationScoped
public class UserRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Batch update lastSeen for multiple users
     */
    @Transactional
    public void batchUpdateLastSeen(Map<Long, Instant> updates) {
        if (updates.isEmpty()) return;
        
        // Use CASE WHEN for efficient batch update
        StringBuilder sql = new StringBuilder(
            "UPDATE users SET last_seen = CASE id "
        );
        
        for (Map.Entry<Long, Instant> entry : updates.entrySet()) {
            sql.append("WHEN ").append(entry.getKey())
               .append(" THEN '").append(entry.getValue()).append("' ");
        }
        
        sql.append("END WHERE id IN (");
        sql.append(updates.keySet().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        sql.append(")");
        
        entityManager.createNativeQuery(sql.toString()).executeUpdate();
    }
}
```

### Solution 4: Centralize Status Broadcast

```java
@ApplicationScoped
public class UserStatusBroadcastService {
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private ChatSessionService chatSessionService;
    
    @Inject
    private UserProfileCache userProfileCache;
    
    @Inject
    private UserMapper userMapper;
    
    public void broadcastUserStatusChange(Long userId, boolean isOnline) {
        try {
            // Use cache
            UserResponse userResponse = userProfileCache.get(userId);
            if (userResponse == null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) return;
                userResponse = userMapper.toResponse(user);
                userProfileCache.put(userId, userResponse);
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("username", userResponse.getUsername());
            payload.put("fullName", userResponse.getFullName());
            payload.put("online", isOnline);
            payload.put("lastSeen", userResponse.getLastSeen() != null ? 
                userResponse.getLastSeen().toString() : null);
            
            WebSocketMessage<Map<String, Object>> statusMessage = 
                new WebSocketMessage<>("user.status", payload);
            
            List<Long> recipientUserIds = 
                userRepository.findUserIdsWhoShouldReceiveStatusUpdates(userId);
            
            for (Long recipientId : recipientUserIds) {
                try {
                    chatSessionService.sendDirectMessage(recipientId, statusMessage);
                } catch (Exception e) {
                    log.warn("Failed to broadcast status to {}: {}", recipientId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting status for user {}: {}", userId, e.getMessage(), e);
        }
    }
}
```

### Solution 5: Update ChatServerEndpoint

```java
@OnMessage
public void onMessage(Session session, WebSocketMessage<?> message) {
    Long userId = (Long) session.getUserProperties().get("userId");
    if (userId == null) return;
    
    userStatusService.updateLastSeen(userId);  // ✅ Now queues instead of immediate update
    
    // ... rest of code
}
```

---

## 📊 Performance Comparison

### Before (Current Implementation)

**Scenario:** 100 users, mỗi user gửi 10 messages/minute

```
DB Updates per minute:
- updateLastSeen: 100 users × 10 messages = 1,000 updates
- setUserOnline: 100 users × 1 = 100 updates
- Total: 1,100 DB updates/minute
```

### After (With Batch Updates)

```
DB Updates per minute:
- Batch update (every 30s): 100 users × 2 batches = 2 batch queries
- Each batch query updates 100 users in single query
- Total: 2 DB queries/minute (550x reduction!)
```

---

## 🎯 Implementation Priority

### Phase 1: Critical (Immediate)
1. ✅ Create `UserStatusBatchService`
2. ✅ Add `batchUpdateLastSeen()` to `UserRepository`
3. ✅ Update `UserStatusService` to use batch service

### Phase 2: Important (This week)
4. ✅ Create `UserStatusBroadcastService`
5. ✅ Remove duplicate broadcast logic
6. ✅ Update `ChatServerEndpoint` and `HeartbeatMonitorService`

### Phase 3: Optimization (Next week)
7. ✅ Add metrics/monitoring for batch updates
8. ✅ Tune batch interval (30s vs 60s)
9. ✅ Add cache warming strategy

---

## 📝 Additional Improvements

### 1. Cache Invalidation Strategy

```java
@ApplicationScoped
public class UserProfileCache {
    
    /**
     * Invalidate cache when user updates profile
     */
    @Observes
    public void onUserProfileUpdated(UserProfileUpdatedEvent event) {
        invalidate(event.getUserId());
    }
}
```

### 2. Monitoring & Metrics

```java
@Singleton
public class UserStatusBatchService {
    
    private final AtomicLong totalBatchedUpdates = new AtomicLong(0);
    private final AtomicLong totalBatches = new AtomicLong(0);
    
    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    @Transactional
    public void flushPendingUpdates() {
        if (pendingUpdates.isEmpty()) return;
        
        Map<Long, Instant> updates = new HashMap<>(pendingUpdates);
        pendingUpdates.clear();
        
        userRepository.batchUpdateLastSeen(updates);
        
        totalBatchedUpdates.addAndGet(updates.size());
        totalBatches.incrementAndGet();
        
        log.info("Batch #{}: Updated {} users. Total: {} updates in {} batches",
            totalBatches.get(), updates.size(), 
            totalBatchedUpdates.get(), totalBatches.get());
    }
    
    public Map<String, Object> getMetrics() {
        return Map.of(
            "totalBatchedUpdates", totalBatchedUpdates.get(),
            "totalBatches", totalBatches.get(),
            "avgBatchSize", totalBatches.get() > 0 ? 
                totalBatchedUpdates.get() / totalBatches.get() : 0,
            "pendingUpdates", pendingUpdates.size()
        );
    }
}
```

### 3. Graceful Shutdown

```java
@Singleton
public class UserStatusBatchService {
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down UserStatusBatchService, flushing pending updates...");
        flushPendingUpdates();
        log.info("UserStatusBatchService shutdown complete");
    }
}
```

---

## 🔍 Testing Strategy

### Unit Tests
```java
@Test
public void testBatchUpdate() {
    Map<Long, Instant> updates = Map.of(
        1L, Instant.now(),
        2L, Instant.now(),
        3L, Instant.now()
    );
    
    batchService.queueLastSeenUpdate(1L);
    batchService.queueLastSeenUpdate(2L);
    batchService.queueLastSeenUpdate(3L);
    
    batchService.flushPendingUpdates();
    
    // Verify single DB query was executed
    verify(userRepository, times(1)).batchUpdateLastSeen(any());
}
```

### Load Tests
```java
@Test
public void testHighLoad() {
    // Simulate 1000 users sending messages
    for (int i = 0; i < 1000; i++) {
        batchService.queueLastSeenUpdate((long) i);
    }
    
    // Verify no immediate DB updates
    verify(userRepository, never()).save(any());
    
    // Trigger batch
    batchService.flushPendingUpdates();
    
    // Verify single batch query
    verify(userRepository, times(1)).batchUpdateLastSeen(any());
}
```

---

## 📚 Summary

### Current Issues:
- ❌ 1,100+ DB updates/minute for 100 active users
- ❌ No batch processing
- ❌ Duplicate code
- ❌ Inefficient cache usage

### After Implementation:
- ✅ 2 DB queries/minute (550x reduction)
- ✅ Batch processing every 30s
- ✅ Centralized broadcast logic
- ✅ Efficient cache usage
- ✅ Better monitoring

### Expected Impact:
- 🚀 99.8% reduction in DB writes
- 🚀 Better scalability (support 10,000+ concurrent users)
- 🚀 Lower DB load
- 🚀 Faster response times
