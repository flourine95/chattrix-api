# User Status Architecture - Clean & Clear

## 🎯 Vấn đề hiện tại

**Lung tung:**
- UserStatusService (wrapper không cần thiết)
- OnlineStatusCache (Caffeine cache)
- UserStatusBatchService (batch updates)
- UserStatusBroadcastService (broadcast)
- HeartbeatMonitorService (heartbeat)
- UserStatusCleanupService (cleanup)

**Quá nhiều services, logic không rõ ràng!**

---

## ✅ Architecture mới - Rõ ràng & Đơn giản

### 1. OnlineStatusCache (Caffeine)
**Responsibility:** Track online status in-memory

```java
@ApplicationScoped
public class OnlineStatusCache {
    // Caffeine cache: userId -> lastSeen timestamp
    // Auto-expiration: 5 minutes
    // Max size: 100,000 users
    
    void markOnline(Long userId)
    void markOffline(Long userId)
    boolean isOnline(Long userId)
    Set<Long> getOnlineUserIds()
}
```

**Usage:**
- WebSocket onOpen → `markOnline()`
- WebSocket onClose → `markOffline()`
- WebSocket onMessage → `markOnline()` (update timestamp)
- Check status → `isOnline()`

---

### 2. UserStatusBatchService (Singleton)
**Responsibility:** Batch update lastSeen to database

```java
@Singleton
public class UserStatusBatchService {
    // Queue: userId -> lastSeen timestamp
    // Flush every 30 seconds
    
    void queueLastSeenUpdate(Long userId)
    
    @Schedule(second = "*/30")
    void flushPendingUpdates()  // Batch UPDATE to DB
}
```

**Usage:**
- WebSocket onOpen → `queueLastSeenUpdate()`
- WebSocket onMessage → `queueLastSeenUpdate()`
- WebSocket onClose → `queueLastSeenUpdate()`

---

### 3. UserStatusBroadcastService (ApplicationScoped)
**Responsibility:** Broadcast status changes to other users

```java
@ApplicationScoped
public class UserStatusBroadcastService {
    void broadcastUserStatusChange(Long userId, boolean isOnline)
}
```

**Usage:**
- WebSocket onOpen → `broadcast(userId, true)`
- WebSocket onClose → `broadcast(userId, false)`
- Heartbeat timeout → `broadcast(userId, false)`

---

### 4. HeartbeatMonitorService (Singleton)
**Responsibility:** Monitor heartbeats, detect timeouts

```java
@Singleton
public class HeartbeatMonitorService {
    // Track: userId -> last heartbeat timestamp
    
    void recordHeartbeat(Long userId)
    void removeHeartbeat(Long userId)
    
    @Schedule(second = "*/15")
    void checkStaleHeartbeats()  // Mark offline if timeout
}
```

**Usage:**
- WebSocket onOpen → `recordHeartbeat()`
- WebSocket heartbeat message → `recordHeartbeat()`
- WebSocket onClose → `removeHeartbeat()`

---

## 📊 Data Flow - Rõ ràng

### User Connect (WebSocket onOpen)
```
1. OnlineStatusCache.markOnline(userId)           ← In-memory
2. UserStatusBatchService.queueLastSeenUpdate()   ← Queue for DB
3. HeartbeatMonitorService.recordHeartbeat()      ← Track heartbeat
4. UserStatusBroadcastService.broadcast(true)     ← Notify others
```

### User Send Message (WebSocket onMessage)
```
1. OnlineStatusCache.markOnline(userId)           ← Update timestamp
2. UserStatusBatchService.queueLastSeenUpdate()   ← Queue for DB
```

### User Disconnect (WebSocket onClose)
```
1. OnlineStatusCache.markOffline(userId)          ← Remove from cache
2. UserStatusBatchService.queueLastSeenUpdate()   ← Queue for DB
3. HeartbeatMonitorService.removeHeartbeat()      ← Stop tracking
4. UserStatusBroadcastService.broadcast(false)    ← Notify others
```

### Heartbeat Timeout (Scheduled)
```
1. HeartbeatMonitorService detects timeout
2. OnlineStatusCache.markOffline(userId)          ← Remove from cache
3. UserStatusBatchService.queueLastSeenUpdate()   ← Queue for DB
4. UserStatusBroadcastService.broadcast(false)    ← Notify others
```

### Batch Update (Every 30s)
```
1. UserStatusBatchService.flushPendingUpdates()
2. Single SQL UPDATE for all queued users
3. Database updated
```

---

## 🗂️ Package Structure

```
services/
├── cache/
│   ├── OnlineStatusCache.java          ← Caffeine cache (online status)
│   ├── UserProfileCache.java           ← Caffeine cache (user profiles)
│   ├── MessageCache.java               ← Caffeine cache (messages)
│   └── ConversationCache.java          ← Caffeine cache (conversations)
│
└── user/
    ├── UserStatusBatchService.java     ← Batch DB updates
    ├── UserStatusBroadcastService.java ← Broadcast status changes
    └── HeartbeatMonitorService.java    ← Monitor heartbeats
```

**Xóa:**
- ❌ `UserStatusService.java` (wrapper không cần thiết)
- ❌ `UserStatusCleanupService.java` (Caffeine auto-cleanup)

---

## 🔧 Refactoring Plan

### Step 1: Update ChatServerEndpoint
```java
@Inject
private OnlineStatusCache onlineStatusCache;

@Inject
private UserStatusBatchService batchService;

@Inject
private UserStatusBroadcastService broadcastService;

@Inject
private HeartbeatMonitorService heartbeatMonitor;

@OnOpen
public void onOpen(Session session) {
    // ...
    onlineStatusCache.markOnline(userId);
    batchService.queueLastSeenUpdate(userId);
    heartbeatMonitor.recordHeartbeat(userId);
    broadcastService.broadcastUserStatusChange(userId, true);
}

@OnMessage
public void onMessage(Session session, WebSocketMessage<?> message) {
    onlineStatusCache.markOnline(userId);
    batchService.queueLastSeenUpdate(userId);
    // ...
}

@OnClose
public void onClose(Session session) {
    // ...
    onlineStatusCache.markOffline(userId);
    batchService.queueLastSeenUpdate(userId);
    heartbeatMonitor.removeHeartbeat(userId);
    broadcastService.broadcastUserStatusChange(userId, false);
}
```

### Step 2: Update HeartbeatMonitorService
```java
@Inject
private OnlineStatusCache onlineStatusCache;

@Inject
private UserStatusBatchService batchService;

@Inject
private UserStatusBroadcastService broadcastService;

@Schedule(second = "*/15")
public void checkStaleHeartbeats() {
    // ...
    if (timeout) {
        onlineStatusCache.markOffline(userId);
        batchService.queueLastSeenUpdate(userId);
        broadcastService.broadcastUserStatusChange(userId, false);
    }
}
```

### Step 3: Update UserStatusResource
```java
@Inject
private OnlineStatusCache onlineStatusCache;

@Inject
private UserRepository userRepository;

@GET
@Path("/online")
public Response getOnlineUsers() {
    Set<Long> onlineUserIds = onlineStatusCache.getOnlineUserIds();
    List<User> users = userRepository.findByIds(List.copyOf(onlineUserIds));
    // ...
}

@GET
@Path("/{userId}")
public Response getUserStatus(@PathParam("userId") Long userId) {
    boolean isOnline = onlineStatusCache.isOnline(userId);
    // ...
}
```

### Step 4: Update FriendRequestService
```java
@Inject
private OnlineStatusCache onlineStatusCache;

public FriendRequestResponse toResponse(Contact contact, User otherUser) {
    // ...
    response.setOnline(onlineStatusCache.isOnline(otherUser.getId()));
    // ...
}
```

### Step 5: Delete unnecessary files
```bash
# Delete
rm src/main/java/com/chattrix/api/services/user/UserStatusService.java
rm src/main/java/com/chattrix/api/services/user/UserStatusCleanupService.java
```

---

## 📝 Usage Examples

### Check if user is online
```java
@Inject
private OnlineStatusCache onlineStatusCache;

boolean isOnline = onlineStatusCache.isOnline(userId);
```

### Get all online users
```java
@Inject
private OnlineStatusCache onlineStatusCache;

@Inject
private UserRepository userRepository;

Set<Long> onlineIds = onlineStatusCache.getOnlineUserIds();
List<User> onlineUsers = userRepository.findByIds(List.copyOf(onlineIds));
```

### Update user activity
```java
@Inject
private OnlineStatusCache onlineStatusCache;

@Inject
private UserStatusBatchService batchService;

// Mark online and queue DB update
onlineStatusCache.markOnline(userId);
batchService.queueLastSeenUpdate(userId);
```

### Broadcast status change
```java
@Inject
private UserStatusBroadcastService broadcastService;

broadcastService.broadcastUserStatusChange(userId, true);
```

---

## ✨ Benefits

### Before (Lung tung)
```
UserStatusService (wrapper)
    ↓
OnlineStatusCache + UserStatusBatchService
```
- ❌ Extra layer không cần thiết
- ❌ Logic không rõ ràng
- ❌ Khó hiểu flow

### After (Rõ ràng)
```
Direct injection:
- OnlineStatusCache (online status)
- UserStatusBatchService (DB updates)
- UserStatusBroadcastService (broadcast)
- HeartbeatMonitorService (heartbeat)
```
- ✅ Mỗi service có responsibility rõ ràng
- ✅ Không có wrapper không cần thiết
- ✅ Dễ hiểu flow
- ✅ Dễ test

---

## 🎯 Summary

### Services & Responsibilities

| Service | Responsibility | Storage |
|---------|---------------|---------|
| **OnlineStatusCache** | Track online status | Caffeine (in-memory) |
| **UserStatusBatchService** | Batch update lastSeen | Queue → DB |
| **UserStatusBroadcastService** | Broadcast status changes | WebSocket |
| **HeartbeatMonitorService** | Monitor heartbeats | In-memory map |

### Data Storage

| Data | Storage | Why |
|------|---------|-----|
| **Online status** | Caffeine cache | Fast, auto-expiration |
| **lastSeen** | Database (batch) | Persistent, batch update |
| **Heartbeat** | In-memory map | Temporary, timeout detection |

### Key Principles

1. **Single Responsibility** - Mỗi service làm 1 việc
2. **No Wrappers** - Không có layer không cần thiết
3. **Clear Flow** - Data flow rõ ràng
4. **Direct Injection** - Inject trực tiếp services cần dùng
