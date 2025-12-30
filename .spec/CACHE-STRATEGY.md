# Cache Strategy cho Chattrix

## 📊 Tổng Quan

Hệ thống sử dụng **Caffeine Cache** (in-memory) để tối ưu hiệu năng và giảm tải database.

## 🎯 Các Cache Services

### 1. OnlineStatusCache
**Mục đích:** Quản lý trạng thái online của users

**Cấu hình:**
- TTL: 5 phút
- Max Size: 100,000 users
- Eviction: Time-based

**Data Structure:**
```java
userId (Long) -> lastHeartbeat (Instant)
```

**Use Cases:**
- Check user online status
- Display online indicator
- Heartbeat updates (every 30s-1min)

**Invalidation:**
- Auto-expire sau 5 phút
- Manual: `markOffline(userId)` khi logout

**Trade-offs:**
- ✅ Giảm 99% queries cho online status
- ✅ Acceptable staleness (1-2 phút)
- ⚠️ Memory: ~1.6MB cho 100k users

---

### 2. UserProfileCache
**Mục đích:** Cache thông tin user cơ bản (avatar, username, fullName)

**Cấu hình:**
- TTL: 1 giờ
- Max Size: 50,000 users
- Eviction: LRU

**Data Structure:**
```java
userId (Long) -> UserResponse {
    id, username, fullName, avatarUrl, 
    email, gender, profileVisibility, lastSeen
}
```

**Use Cases:**
- Display sender info in messages
- Display participants in conversations
- User search, mentions, contacts
- Profile previews

**Invalidation:**
- Auto-expire sau 1 giờ
- Manual: `invalidate(userId)` khi user updates profile
- Bulk: `invalidateAll(userIds)`

**Trade-offs:**
- ✅ Giảm JOIN với users table
- ✅ Message list load nhanh hơn 3-5x
- ⚠️ Stale data: Avatar/username cũ hiển thị đến khi expire
- ⚠️ Memory: ~50MB cho 50k users

---

### 3. ConversationCache
**Mục đích:** Cache conversation metadata (lastMessage, unreadCount, participants)

**Cấu hình:**
- TTL: 10 phút
- Max Size: 100,000 entries
- Eviction: Time-based + LRU

**Data Structure:**
```java
"userId_conversationId" (String) -> ConversationResponse {
    id, name, avatarUrl, type,
    lastMessage: { id, content, sentAt, senderName },
    unreadCount,
    participants: [UserResponse],
    createdAt, updatedAt
}
```

**Use Cases:**
- Conversation list loading
- Pull to refresh
- Unread count display

**Invalidation:**
- Auto-expire sau 10 phút
- Manual: `invalidate(userId, conversationId)` khi conversation updated
- Bulk: `invalidateForAllParticipants(conversationId, participantIds)` khi new message

**Trade-offs:**
- ✅ Conversation list load nhanh hơn 5-10x
- ✅ Giảm complex JOINs (conversations + messages + users + participants)
- ⚠️ Invalidation complexity: Phải invalidate cho tất cả participants
- ⚠️ Memory: ~100MB cho 100k entries

---

### 4. MessageCache
**Mục đích:** Cache 50 messages gần nhất của mỗi conversation

**Cấu hình:**
- TTL: 5 phút
- Max Size: 5,000 conversations
- Max Messages: 50 per conversation
- Eviction: Time-based + LRU

**Data Structure:**
```java
conversationId (Long) -> List<MessageResponse> (50 messages)
```

**Use Cases:**
- Load message history
- Scroll up to view old messages
- Real-time message updates

**Operations:**
- `get(conversationId)` - Get cached messages
- `addMessage(conversationId, message)` - Prepend new message
- `updateMessage(conversationId, message)` - Update edited message
- `removeMessage(conversationId, messageId)` - Remove deleted message

**Invalidation:**
- Auto-expire sau 5 phút
- Manual: `invalidate(conversationId)` khi major changes

**Trade-offs:**
- ✅ Giảm queries khi scroll
- ✅ Real-time updates nhanh
- ⚠️ Memory: ~250MB cho 5k conversations
- ⚠️ Complexity: Phải maintain list order

---

## 🔧 CacheManager

Central manager cho tất cả caches:

```java
@Inject
private CacheManager cacheManager;

// Clear all caches
cacheManager.clearAll();

// Get statistics
String stats = cacheManager.getAllStats();

// Invalidate user caches
cacheManager.invalidateUserCaches(userId);

// Invalidate conversation caches
cacheManager.invalidateConversationCaches(conversationId, participantIds);

// Get health status
CacheHealthStatus health = cacheManager.getHealthStatus();
```

---

## 🚀 CacheWarmer

Warm up caches on startup hoặc on-demand:

```java
@Inject
private CacheWarmer cacheWarmer;

// Warm up user profiles (1000 hot users)
cacheWarmer.warmUpUserProfiles();

// Warm up all caches
cacheWarmer.warmUpAll();
```

---

## 📝 Usage Examples

### Example 1: Load Conversation List
```java
@Inject
private ConversationCache conversationCache;

@Inject
private ConversationService conversationService;

public List<ConversationResponse> getConversations(Long userId) {
    // Try cache first
    List<ConversationResponse> cached = conversationCache.get(userId, conversationId);
    if (cached != null) {
        return cached;
    }
    
    // Cache miss - load from DB
    List<ConversationResponse> conversations = conversationService.loadFromDB(userId);
    
    // Cache for next time
    conversations.forEach(conv -> 
        conversationCache.put(userId, conv.getId(), conv)
    );
    
    return conversations;
}
```

### Example 2: Send Message
```java
@Inject
private MessageCache messageCache;

@Inject
private ConversationCache conversationCache;

@Transactional
public void sendMessage(Long conversationId, MessageRequest request) {
    // Save to DB
    Message message = messageRepository.save(...);
    MessageResponse response = messageMapper.toResponse(message);
    
    // Update message cache
    messageCache.addMessage(conversationId, response);
    
    // Invalidate conversation cache for all participants
    Set<Long> participantIds = getParticipantIds(conversationId);
    conversationCache.invalidateForAllParticipants(conversationId, participantIds);
}
```

### Example 3: Update User Profile
```java
@Inject
private UserProfileCache userProfileCache;

@Inject
private CacheManager cacheManager;

@Transactional
public void updateProfile(Long userId, UpdateProfileRequest request) {
    // Update DB
    User user = userRepository.findById(userId).orElseThrow();
    user.setFullName(request.getFullName());
    user.setAvatarUrl(request.getAvatarUrl());
    userRepository.save(user);
    
    // Invalidate all user-related caches
    cacheManager.invalidateUserCaches(userId);
}
```

### Example 4: Display Message with Sender Info
```java
@Inject
private UserProfileCache userProfileCache;

@Inject
private UserService userService;

public MessageResponse getMessageWithSender(Long messageId) {
    Message message = messageRepository.findById(messageId).orElseThrow();
    MessageResponse response = messageMapper.toResponse(message);
    
    // Get sender info from cache
    UserResponse sender = userProfileCache.get(message.getSender().getId());
    if (sender == null) {
        // Cache miss - load from DB
        sender = userService.getUserById(message.getSender().getId());
        userProfileCache.put(sender.getId(), sender);
    }
    
    response.setSender(sender);
    return response;
}
```

---

## 📊 Memory Estimation

| Cache | Max Size | Avg Entry Size | Total Memory |
|-------|----------|----------------|--------------|
| OnlineStatusCache | 100,000 | 16 bytes | ~1.6 MB |
| UserProfileCache | 50,000 | 1 KB | ~50 MB |
| ConversationCache | 100,000 | 1 KB | ~100 MB |
| MessageCache | 5,000 | 50 KB | ~250 MB |
| **Total** | | | **~400 MB** |

**Note:** Đây là ước tính tối đa. Thực tế sẽ thấp hơn do:
- LRU eviction
- Time-based expiration
- Không phải lúc nào cũng đầy

---

## ⚡ Performance Impact

### Before Caching
```
Conversation List Load: 500-1000ms (JOIN 4 tables)
Message List Load: 200-500ms (JOIN 2 tables)
Online Status Check: 10-20ms (DB query)
User Profile Load: 50-100ms (DB query)
```

### After Caching
```
Conversation List Load: 50-100ms (cache hit) | 500-1000ms (cache miss)
Message List Load: 20-50ms (cache hit) | 200-500ms (cache miss)
Online Status Check: <1ms (cache hit)
User Profile Load: <1ms (cache hit) | 50-100ms (cache miss)
```

**Expected Hit Rates:**
- OnlineStatusCache: 95-99%
- UserProfileCache: 80-90%
- ConversationCache: 70-80%
- MessageCache: 60-70%

---

## 🔄 Cache Invalidation Strategy

### User Updates Profile
```java
cacheManager.invalidateUserCaches(userId);
// Invalidates:
// - UserProfileCache
// - ConversationCache (all conversations of user)
```

### New Message Sent
```java
conversationCache.invalidateForAllParticipants(conversationId, participantIds);
messageCache.addMessage(conversationId, message);
// Invalidates:
// - ConversationCache (for all participants)
// Updates:
// - MessageCache (prepend new message)
```

### Message Edited
```java
messageCache.updateMessage(conversationId, updatedMessage);
// Updates:
// - MessageCache (replace message)
```

### Message Deleted
```java
messageCache.removeMessage(conversationId, messageId);
conversationCache.invalidateForAllParticipants(conversationId, participantIds);
// Updates:
// - MessageCache (remove message)
// Invalidates:
// - ConversationCache (lastMessage might change)
```

### User Logout
```java
onlineStatusCache.markOffline(userId);
// Updates:
// - OnlineStatusCache (remove from cache)
```

---

## 🧪 Testing Cache

### Manual Testing
```java
// Get cache stats
String stats = cacheManager.getAllStats();
System.out.println(stats);

// Output:
// === Cache Statistics ===
// OnlineStatusCache - Size: 1234, Online users: 567
// UserProfileCache - Size: 5678, Hit Rate: 85.23%
// ConversationCache - Size: 12345, Hit Rate: 78.45%
// MessageCache - Size: 2345, Hit Rate: 65.12%
```

### Health Check Endpoint
```java
@GET
@Path("/health/cache")
public Response getCacheHealth() {
    CacheHealthStatus health = cacheManager.getHealthStatus();
    return Response.ok(health).build();
}
```

---

## 🎯 Best Practices

### 1. Cache-Aside Pattern
```java
// Always try cache first
T cached = cache.get(key);
if (cached != null) {
    return cached;
}

// Cache miss - load from DB
T data = loadFromDB(key);

// Cache for next time
cache.put(key, data);

return data;
```

### 2. Write-Through Pattern
```java
// Update DB first
T data = updateDB(key, value);

// Then update cache
cache.put(key, data);
```

### 3. Invalidate on Write
```java
// Update DB
updateDB(key, value);

// Invalidate cache (let next read refresh)
cache.invalidate(key);
```

### 4. Batch Operations
```java
// Get multiple from cache
Map<Long, UserResponse> cached = userProfileCache.getAll(userIds);

// Find missing
Set<Long> missing = userIds.stream()
    .filter(id -> !cached.containsKey(id))
    .collect(Collectors.toSet());

// Load missing from DB
Map<Long, UserResponse> loaded = loadFromDB(missing);

// Cache missing
userProfileCache.putAll(loaded);

// Merge results
cached.putAll(loaded);
return cached;
```

---

## 🚨 Monitoring & Alerts

### Metrics to Monitor
- Cache hit rate (should be > 70%)
- Cache size (should not exceed max)
- Memory usage (should be < 500MB)
- Eviction rate (should be low)

### Alerts
- Hit rate < 50% → Investigate cache strategy
- Memory > 500MB → Reduce cache size or TTL
- Eviction rate > 10% → Increase cache size

---

## 🔧 Configuration

### Adjust Cache Settings
```java
// In each cache service
private static final int CACHE_EXPIRY_MINUTES = 10;  // Adjust TTL
private static final int MAX_CACHE_SIZE = 100_000;   // Adjust size
```

### Disable Cache (for testing)
```java
// Set TTL to 0
private static final int CACHE_EXPIRY_MINUTES = 0;
```

---

## 📚 References

- [Caffeine Cache Documentation](https://github.com/ben-manes/caffeine)
- [Cache-Aside Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/cache-aside)
- [Caching Best Practices](https://aws.amazon.com/caching/best-practices/)

---

**Cache implementation hoàn tất!** 🎉
