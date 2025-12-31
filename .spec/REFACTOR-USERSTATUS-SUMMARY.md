# UserStatusService Refactoring Summary

## ✅ Completed

### 1. Updated ChatServerEndpoint
- ❌ Removed `UserStatusService`
- ✅ Added `OnlineStatusCache`
- ✅ Added `UserStatusBatchService`
- ✅ Direct injection of needed services

### 2. Updated HeartbeatMonitorService
- ❌ Removed `UserStatusService`
- ✅ Added `OnlineStatusCache`
- ✅ Added `UserStatusBatchService`

## 🔄 Files Need Refactoring

### 3. UserStatusResource
**Current:**
```java
@Inject
private UserStatusService userStatusService;

List<User> onlineUsers = userStatusService.getOnlineUsers();
boolean isOnline = userStatusService.isUserOnline(userId);
```

**Should be:**
```java
@Inject
private OnlineStatusCache onlineStatusCache;

@Inject
private UserRepository userRepository;

Set<Long> onlineIds = onlineStatusCache.getOnlineUserIds();
List<User> onlineUsers = userRepository.findByIds(List.copyOf(onlineIds));
boolean isOnline = onlineStatusCache.isOnline(userId);
```

### 4. FriendRequestService
**Current:**
```java
@Inject
private UserStatusService userStatusService;

response.setOnline(userStatusService.isUserOnline(otherUser.getId()));
```

**Should be:**
```java
@Inject
private OnlineStatusCache onlineStatusCache;

response.setOnline(onlineStatusCache.isOnline(otherUser.getId()));
```

### 5. CallCleanupScheduler
**Current:**
```java
private final UserStatusService userStatusService;

if (!userStatusService.isUserOnline(call.getCallerId())) {
    // cleanup
}
```

**Should be:**
```java
@Inject
private OnlineStatusCache onlineStatusCache;

if (!onlineStatusCache.isOnline(call.getCallerId())) {
    // cleanup
}
```

### 6. Delete UserStatusCleanupService
**Reason:** Caffeine cache auto-cleanup, không cần manual cleanup

## 📝 Refactoring Steps

1. ✅ Update ChatServerEndpoint
2. ✅ Update HeartbeatMonitorService
3. ⏳ Update UserStatusResource
4. ⏳ Update FriendRequestService
5. ⏳ Update CallCleanupScheduler
6. ⏳ Delete UserStatusService.java
7. ⏳ Delete UserStatusCleanupService.java
8. ⏳ Compile & test

## 🎯 Final Architecture

```
WebSocket/Resources
    ↓
Direct injection:
├── OnlineStatusCache (Caffeine)      ← Online status
├── UserStatusBatchService            ← DB updates
├── UserStatusBroadcastService        ← Broadcast
└── HeartbeatMonitorService           ← Heartbeat
```

No wrappers, clear responsibilities!
