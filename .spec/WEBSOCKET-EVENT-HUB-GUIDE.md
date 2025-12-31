# WebSocket Event Management - Simple & Monitored

## 🎯 Mục tiêu

Quản lý và monitor tất cả WebSocket events được gửi đi mà **không thay đổi code hiện tại**.

---

## ✅ Giải pháp: WebSocketEventHub (Monitoring Wrapper)

### Architecture

```
Services (không đổi code)
    ↓
WebSocketEventHub (monitoring wrapper)
    ↓ (track metrics, log events)
ChatSessionService
    ↓
WebSocket Sessions
```

---

## 📊 Usage - 2 cách sử dụng

### Cách 1: Giữ nguyên code hiện tại (Recommended)

**Không cần thay đổi gì!** Chỉ thay `chatSessionService` → `eventHub`:

```java
// BEFORE
@Inject
private ChatSessionService chatSessionService;

WebSocketMessage<OutgoingMessageDto> wsMsg = 
    new WebSocketMessage<>("chat.message", outgoingDto);
conv.getParticipants().forEach(p ->
    chatSessionService.sendMessageToUser(p.getUser().getId(), wsMsg)
);

// AFTER (chỉ đổi tên service)
@Inject
private WebSocketEventHub eventHub;

WebSocketMessage<OutgoingMessageDto> wsMsg = 
    new WebSocketMessage<>("chat.message", outgoingDto);
conv.getParticipants().forEach(p ->
    eventHub.send(p.getUser().getId(), wsMsg)  // ← Chỉ đổi method name
);
```

### Cách 2: Dùng helper methods (Optional)

```java
@Inject
private WebSocketEventHub eventHub;

// Gửi cho 1 user
eventHub.sendToUser(userId, "chat.message", messageDto);

// Gửi cho nhiều users
List<Long> userIds = List.of(1L, 2L, 3L);
eventHub.sendToUsers(userIds, "user.status", statusPayload);
```

---

## 📈 Monitoring & Metrics

### 1. REST API Endpoints

```bash
# Xem metrics (JSON)
GET /v1/admin/websocket/metrics

# Xem metrics report (text)
GET /v1/admin/websocket/metrics/report

# Reset metrics
POST /v1/admin/websocket/metrics/reset
```

### 2. Metrics Response

```json
{
  "success": true,
  "data": {
    "totalEventsSent": 15234,
    "eventCountByType": {
      "chat.message": 8500,
      "user.status": 3200,
      "typing.indicator": 2100,
      "call.incoming": 850,
      "message.reaction": 584
    },
    "lastEventTimeByType": {
      "chat.message": "2025-12-31T05:30:15Z",
      "user.status": "2025-12-31T05:30:20Z",
      "typing.indicator": "2025-12-31T05:30:18Z"
    }
  }
}
```

### 3. Text Report

```
=== WebSocket Metrics ===
Total events sent: 15234

Events by type:
  chat.message                  :   8500 (last: 2025-12-31T05:30:15Z)
  user.status                   :   3200 (last: 2025-12-31T05:30:20Z)
  typing.indicator              :   2100 (last: 2025-12-31T05:30:18Z)
  call.incoming                 :    850 (last: 2025-12-31T05:29:45Z)
  message.reaction              :    584 (last: 2025-12-31T05:30:10Z)
```

---

## 🔧 Migration Examples

### ChatServerEndpoint

**BEFORE:**
```java
@Inject
private ChatSessionService chatSessionService;

WebSocketMessage<OutgoingMessageDto> wsMsg = 
    new WebSocketMessage<>("chat.message", outgoingDto);
conv.getParticipants().forEach(p ->
    chatSessionService.sendMessageToUser(p.getUser().getId(), wsMsg)
);
```

**AFTER:**
```java
@Inject
private WebSocketEventHub eventHub;

WebSocketMessage<OutgoingMessageDto> wsMsg = 
    new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);
conv.getParticipants().forEach(p ->
    eventHub.send(p.getUser().getId(), wsMsg)
);
```

### WebSocketNotificationService

**BEFORE:**
```java
@Inject
private ChatSessionService chatSessionService;

WebSocketMessage<FriendRequestResponse> message = 
    new WebSocketMessage<>("friend.request.received", friendRequest);
chatSessionService.sendDirectMessage(receiverId, message);
```

**AFTER:**
```java
@Inject
private WebSocketEventHub eventHub;

WebSocketMessage<FriendRequestResponse> message = 
    new WebSocketMessage<>(WebSocketEventType.FRIEND_REQUEST_RECEIVED, friendRequest);
eventHub.send(receiverId, message);
```

### UserStatusBroadcastService

**BEFORE:**
```java
@Inject
private ChatSessionService chatSessionService;

WebSocketMessage<Map<String, Object>> statusMessage = 
    new WebSocketMessage<>("user.status", payload);
for (Long recipientId : recipientUserIds) {
    chatSessionService.sendDirectMessage(recipientId, statusMessage);
}
```

**AFTER (Option 1 - Giữ nguyên style):**
```java
@Inject
private WebSocketEventHub eventHub;

WebSocketMessage<Map<String, Object>> statusMessage = 
    new WebSocketMessage<>(WebSocketEventType.USER_STATUS, payload);
for (Long recipientId : recipientUserIds) {
    eventHub.send(recipientId, statusMessage);
}
```

**AFTER (Option 2 - Dùng helper):**
```java
@Inject
private WebSocketEventHub eventHub;

eventHub.sendToUsers(recipientUserIds, WebSocketEventType.USER_STATUS, payload);
```

---

## 📊 Benefits

### 1. Monitoring
- ✅ Track tất cả events được gửi
- ✅ Biết event nào được gửi nhiều nhất
- ✅ Biết event nào lâu không gửi (có thể bug)
- ✅ Debug dễ dàng hơn

### 2. Minimal Changes
- ✅ Chỉ đổi `chatSessionService` → `eventHub`
- ✅ Chỉ đổi `sendMessageToUser` → `send`
- ✅ Logic không đổi
- ✅ Code style không đổi

### 3. Consistent Logging
- ✅ Tất cả events đều được log
- ✅ Format log nhất quán
- ✅ Dễ grep/search logs

### 4. Future Extensions
- ✅ Dễ thêm rate limiting
- ✅ Dễ thêm event filtering
- ✅ Dễ thêm audit trail
- ✅ Dễ thêm alerting

---

## 🚀 Migration Plan

### Phase 1: Core Services (High Priority)
1. ✅ Create WebSocketEventHub
2. ✅ Create WebSocketMetricsResource
3. ⏳ Update ChatServerEndpoint
4. ⏳ Update WebSocketNotificationService
5. ⏳ Update UserStatusBroadcastService

### Phase 2: Message Services
6. ⏳ Update MessageService
7. ⏳ Update ReactionService
8. ⏳ Update ScheduledMessageService
9. ⏳ Update AnnouncementService
10. ⏳ Update PinnedMessageService

### Phase 3: Feature Services
11. ⏳ Update PollService
12. ⏳ Update EventService

### Phase 4: Testing
13. ⏳ Test all WebSocket events
14. ⏳ Verify metrics accuracy
15. ⏳ Load testing

---

## 📝 Checklist

### Code Changes
- [ ] Replace `chatSessionService` with `eventHub`
- [ ] Replace string literals with `WebSocketEventType` constants
- [ ] Replace `sendMessageToUser`/`sendDirectMessage` with `send`
- [ ] Test each service after migration

### Monitoring
- [ ] Check metrics endpoint works
- [ ] Verify event counts are accurate
- [ ] Set up alerting for anomalies
- [ ] Document expected event rates

---

## 💡 Tips

### 1. Use Constants
```java
// ❌ BAD
new WebSocketMessage<>("chat.message", data)

// ✅ GOOD
new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, data)
```

### 2. Monitor Metrics
```bash
# Check metrics regularly
curl http://localhost:8080/v1/admin/websocket/metrics/report

# Look for anomalies:
# - Events with 0 count (not being sent?)
# - Events with very high count (spam?)
# - Events not sent recently (feature broken?)
```

### 3. Gradual Migration
- Migrate one service at a time
- Test after each migration
- Keep old code commented for rollback

---

## 🎯 Summary

### What Changed
- ✅ Added WebSocketEventHub (monitoring wrapper)
- ✅ Added WebSocketMetricsResource (admin endpoint)
- ✅ Added WebSocketEventType (constants)

### What Didn't Change
- ✅ Code style (still manual loops, still WebSocketMessage)
- ✅ Logic (same flow, same behavior)
- ✅ Architecture (still services → ChatSessionService)

### What You Get
- ✅ Full visibility into WebSocket events
- ✅ Metrics & monitoring
- ✅ Better debugging
- ✅ Minimal code changes

## 🎯 Vấn đề hiện tại

**WebSocket events rải rác khắp nơi:**
- ChatServerEndpoint
- WebSocketNotificationService
- UserStatusBroadcastService
- MessageService
- ReactionService
- PollService
- EventService
- ScheduledMessageService
- AnnouncementService
- PinnedMessageService
- ... và nhiều nơi khác

**Hậu quả:**
- ❌ Khó maintain (30+ event types ở 10+ files)
- ❌ Duplicate code (cùng logic broadcast lặp lại)
- ❌ Khó debug (không biết event nào đang được gửi)
- ❌ Không consistent (error handling khác nhau)
- ❌ Khó thêm middleware (rate limiting, filtering, audit)

---

## ✅ Giải pháp: WebSocket Event Hub

### Architecture mới

```
┌─────────────────────────────────────────────────┐
│         WebSocketEventHub (Centralized)         │
│  - Single source of truth                       │
│  - Consistent error handling                    │
│  - Easy middleware injection                    │
│  - Clear audit trail                            │
└─────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────┐
│           ChatSessionService                    │
│  - Session management                           │
│  - Low-level WebSocket send                     │
└─────────────────────────────────────────────────┘
                        ↓
                  WebSocket Sessions
```

### Components

#### 1. WebSocketEventType (Constants)
```java
public final class WebSocketEventType {
    public static final String CHAT_MESSAGE = "chat.message";
    public static final String USER_STATUS = "user.status";
    public static final String CALL_INCOMING = "call.incoming";
    // ... all event types
}
```

#### 2. WebSocketEventHub (Centralized Service)
```java
@ApplicationScoped
public class WebSocketEventHub {
    
    // Generic broadcast methods
    sendToUser(userId, eventType, payload)
    sendToUsers(userIds, eventType, payload)
    sendToConversation(conversation, eventType, payload)
    sendToConversationExcept(conversation, excludeUserId, eventType, payload)
    
    // Typed methods for each event
    sendChatMessage(conversation, messageDto)
    sendUserStatus(recipientIds, statusPayload)
    sendCallIncoming(userId, callDto)
    // ... all event types
}
```

---

## 📝 Migration Guide

### Before (Scattered)
```java
// In MessageService.java
WebSocketMessage<Map<String, Object>> wsMessage = 
    new WebSocketMessage<>("message.updated", payload);
message.getConversation().getParticipants().forEach(participant -> {
    chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
});

// In ReactionService.java
WebSocketMessage<ReactionEventDto> wsMessage = 
    new WebSocketMessage<>("message.reaction", reactionEvent);
conversation.getParticipants().forEach(participant -> {
    chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
});

// In UserStatusBroadcastService.java
WebSocketMessage<Map<String, Object>> statusMessage = 
    new WebSocketMessage<>("user.status", payload);
for (Long recipientId : recipientUserIds) {
    chatSessionService.sendDirectMessage(recipientId, statusMessage);
}
```

### After (Centralized)
```java
// In MessageService.java
@Inject
private WebSocketEventHub eventHub;

eventHub.sendMessageUpdated(conversation, payload);

// In ReactionService.java
@Inject
private WebSocketEventHub eventHub;

eventHub.sendMessageReaction(conversation, reactionEvent);

// In UserStatusBroadcastService.java
@Inject
private WebSocketEventHub eventHub;

eventHub.sendUserStatus(recipientUserIds, payload);
```

---

## 🔄 Step-by-Step Migration

### Phase 1: Update WebSocketNotificationService
```java
@ApplicationScoped
public class WebSocketNotificationService {
    
    @Inject
    private WebSocketEventHub eventHub;
    
    public void sendFriendRequestReceived(Long receiverId, FriendRequestResponse friendRequest) {
        eventHub.sendFriendRequestReceived(receiverId, friendRequest);
    }
    
    public void sendCallInvitation(String calleeId, CallInvitationDto data) {
        eventHub.sendCallIncoming(Long.parseLong(calleeId), data);
    }
    
    // ... other methods
}
```

### Phase 2: Update UserStatusBroadcastService
```java
@ApplicationScoped
public class UserStatusBroadcastService {
    
    @Inject
    private WebSocketEventHub eventHub;
    
    public void broadcastUserStatusChange(Long userId, boolean isOnline) {
        // ... build payload
        eventHub.sendUserStatus(recipientUserIds, payload);
    }
}
```

### Phase 3: Update MessageService
```java
@ApplicationScoped
public class MessageService {
    
    @Inject
    private WebSocketEventHub eventHub;
    
    public void updateMessage(Long messageId, String newContent) {
        // ... update logic
        eventHub.sendMessageUpdated(conversation, payload);
    }
    
    public void deleteMessage(Long messageId) {
        // ... delete logic
        eventHub.sendMessageDeleted(conversation, payload);
    }
}
```

### Phase 4: Update ReactionService
```java
@ApplicationScoped
public class ReactionService {
    
    @Inject
    private WebSocketEventHub eventHub;
    
    public void addReaction(Long messageId, String emoji) {
        // ... add reaction logic
        eventHub.sendMessageReaction(conversation, reactionEvent);
    }
}
```

### Phase 5: Update ChatServerEndpoint
```java
@Dependent
public class ChatServerEndpoint {
    
    @Inject
    private WebSocketEventHub eventHub;
    
    private void handleChatMessage(Long senderId, Object payload) {
        // ... save message
        eventHub.sendChatMessage(conv, outgoingDto);
        
        // Send mentions
        for (Long mentionedUserId : dto.getMentions()) {
            eventHub.sendMessageMention(mentionedUserId, mentionEvent);
        }
        
        // Broadcast conversation update
        eventHub.sendConversationUpdate(conv, updateDto);
    }
    
    private void handleTypingEvent(Long userId, Object payload, boolean isStarting) {
        // ... typing logic
        eventHub.sendTypingIndicator(conv, typingDto);
    }
    
    private void handleHeartbeat(Session session, Long userId) {
        heartbeatMonitorService.recordHeartbeat(userId);
        eventHub.sendHeartbeatAck(userId, ackPayload);
    }
}
```

### Phase 6: Update remaining services
- ScheduledMessageService
- AnnouncementService
- PinnedMessageService
- PollService
- EventService

---

## 📊 Benefits

### Before
```
10+ services × 3-5 event types each = 30+ scattered broadcasts
- Duplicate code everywhere
- Inconsistent error handling
- Hard to debug
- No audit trail
```

### After
```
1 WebSocketEventHub = Single source of truth
- ✅ All events in one place
- ✅ Consistent error handling & logging
- ✅ Easy to add middleware
- ✅ Clear audit trail
- ✅ Type-safe event types
- ✅ Easy to test
```

---

## 🎯 Usage Examples

### Send to single user
```java
@Inject
private WebSocketEventHub eventHub;

eventHub.sendFriendRequestReceived(userId, friendRequest);
```

### Send to multiple users
```java
List<Long> userIds = List.of(1L, 2L, 3L);
eventHub.sendUserStatus(userIds, statusPayload);
```

### Send to conversation
```java
Conversation conversation = ...;
eventHub.sendChatMessage(conversation, messageDto);
```

### Send to conversation except sender
```java
eventHub.sendToConversationExcept(conversation, senderId, eventType, payload);
```

### Custom event type
```java
// For poll events: "poll.created", "poll.voted", etc.
eventHub.sendPollEvent(conversation, "created", pollDto);

// For event events: "event.created", "event.rsvp", etc.
eventHub.sendEventEvent(conversation, "rsvp", eventDto);
```

---

## 🔧 Advanced Features (Future)

### 1. Rate Limiting
```java
public <T> void sendToUser(Long userId, String eventType, T payload) {
    if (rateLimiter.isAllowed(userId, eventType)) {
        // send
    } else {
        log.warn("Rate limit exceeded for user {} event {}", userId, eventType);
    }
}
```

### 2. Event Filtering
```java
public <T> void sendToUsers(List<Long> userIds, String eventType, T payload) {
    List<Long> filteredUsers = userIds.stream()
        .filter(id -> userPreferences.wantsEvent(id, eventType))
        .toList();
    // send to filtered users
}
```

### 3. Audit Trail
```java
public <T> void sendToUser(Long userId, String eventType, T payload) {
    auditLog.record(userId, eventType, Instant.now());
    // send
}
```

### 4. Metrics
```java
public <T> void sendToUser(Long userId, String eventType, T payload) {
    metrics.increment("websocket.events.sent", eventType);
    // send
}
```

---

## 📚 Event Type Reference

### Chat Events
- `chat.message` - New message
- `message.updated` - Message edited
- `message.deleted` - Message deleted
- `message.mention` - User mentioned
- `message.reaction` - Reaction added/removed
- `message.pin` - Message pinned/unpinned

### Conversation Events
- `conversation.update` - Conversation metadata changed
- `typing.indicator` - User typing status

### User Status Events
- `user.status` - User online/offline
- `heartbeat.ack` - Heartbeat acknowledgment

### Friend Request Events
- `friend.request.received` - New friend request
- `friend.request.accepted` - Request accepted
- `friend.request.rejected` - Request rejected
- `friend.request.cancelled` - Request cancelled

### Call Events
- `call.incoming` - Incoming call
- `call.accepted` - Call accepted
- `call.rejected` - Call rejected
- `call.ended` - Call ended
- `call.timeout` - Call timeout
- `call.participant_update` - Participant joined/left

### Scheduled Message Events
- `scheduled.message.sent` - Scheduled message sent
- `scheduled.message.failed` - Scheduled message failed

### Announcement Events
- `announcement.created` - New announcement
- `announcement.deleted` - Announcement deleted

### Poll Events
- `poll.created` - New poll
- `poll.voted` - Vote cast
- `poll.closed` - Poll closed
- `poll.updated` - Poll updated

### Event Events
- `event.created` - New event
- `event.updated` - Event updated
- `event.deleted` - Event deleted
- `event.rsvp` - RSVP changed

---

## ✅ Migration Checklist

### Phase 1: Setup (DONE)
- [x] Create `WebSocketEventType.java`
- [x] Create `WebSocketEventHub.java`
- [x] Create migration guide

### Phase 2: Core Services
- [ ] Update `WebSocketNotificationService`
- [ ] Update `UserStatusBroadcastService`
- [ ] Update `ChatServerEndpoint`

### Phase 3: Message Services
- [ ] Update `MessageService`
- [ ] Update `ReactionService`
- [ ] Update `PinnedMessageService`
- [ ] Update `ScheduledMessageService`
- [ ] Update `AnnouncementService`

### Phase 4: Feature Services
- [ ] Update `PollService`
- [ ] Update `EventService`

### Phase 5: Testing & Cleanup
- [ ] Test all WebSocket events
- [ ] Remove duplicate broadcast code
- [ ] Update documentation
- [ ] Performance testing

---

## 🚀 Next Steps

1. **Review this guide** - Make sure architecture makes sense
2. **Start migration** - Begin with Phase 2 (Core Services)
3. **Test incrementally** - Test each service after migration
4. **Monitor logs** - Check for any missed events
5. **Clean up** - Remove old broadcast code

---

## 📖 Summary

### Old Architecture (Scattered)
```
Service A → chatSessionService.sendMessageToUser()
Service B → chatSessionService.sendMessageToUser()
Service C → chatSessionService.sendDirectMessage()
... 10+ services doing the same thing
```

### New Architecture (Centralized)
```
All Services → WebSocketEventHub → ChatSessionService
```

**Result:**
- ✅ Single source of truth
- ✅ Consistent & maintainable
- ✅ Easy to extend
- ✅ Clear audit trail
- ✅ Type-safe events
