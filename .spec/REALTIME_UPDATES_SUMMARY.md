# Realtime Updates - Summary of Changes

## Mục tiêu
Thiết kế và xử lý realtime updates cho `last_seen` và `last_message` trong Chattrix API.

## Các thay đổi đã thực hiện

### 1. Last Message Tracking ✅

#### Files Modified:
- **ConversationResponse.java**
  - Cập nhật `MessageResponse` inner class để bao gồm `senderId`
  - Thêm static method `fromEntity()` để map từ Message entity
  - Cập nhật `fromEntity()` của ConversationResponse để map lastMessage

- **MessageService.java**
  - Thêm logic update `conversation.lastMessage` khi send message
  - Tự động save conversation sau khi update

- **ChatServerEndpoint.java**
  - Cập nhật `processChatMessage()` để update lastMessage
  - Thêm method `broadcastConversationUpdate()` để broadcast changes
  - Gọi broadcast sau khi send message qua WebSocket

- **ConversationMapper.java**
  - Thêm mapping cho `senderId` trong MessageResponse

#### Files Created:
- **ConversationUpdateDto.java**
  - DTO mới cho WebSocket event `conversation.update`
  - Chứa: conversationId, updatedAt, lastMessage

### 2. Last Seen Tracking ✅

#### Existing Implementation (Đã có sẵn):
- User.java có fields `isOnline` và `lastSeen`
- UserStatusService có methods để update status
- ChatServerEndpoint update last_seen mỗi khi user gửi message
- UserStatusCleanupService cleanup stale statuses mỗi 5 phút

#### New Enhancements:
- **ChatServerEndpoint.java**
  - Thêm handler cho `heartbeat` event
  - Thêm method `processHeartbeat()` để acknowledge
  - Client có thể ping server để keep connection alive

### 3. WebSocket Events

#### Existing Events:
- `chat.message` - Tin nhắn mới
- `typing.indicator` - Typing status
- `user.status` - User online/offline status

#### New Events:
- `conversation.update` - Conversation lastMessage changed
- `heartbeat` - Client ping (request)
- `heartbeat.ack` - Server pong (response)

## Luồng hoạt động

### Khi user gửi message qua WebSocket:
1. Message được lưu vào database
2. Conversation.lastMessage được update
3. Conversation.updatedAt được update (JPA @PreUpdate)
4. Broadcast `chat.message` event đến participants
5. Broadcast `conversation.update` event đến participants
6. User.lastSeen được update

### Khi user gửi message qua REST API:
1. Message được lưu vào database
2. Conversation.lastMessage được update
3. Conversation.updatedAt được update
4. Return response (không có WebSocket broadcast)

### Khi user connect/disconnect WebSocket:
1. User.isOnline được update
2. User.lastSeen được update
3. Broadcast `user.status` event đến tất cả users

## Testing Results

### Build Status: ✅ SUCCESS
```
mvn clean package
[INFO] BUILD SUCCESS
[INFO] Total time:  6.227 s
```

### Warnings Fixed:
- ✅ Fixed "Unmapped target property: senderId" trong ConversationMapper

## API Response Examples

### GET /v1/conversations
```json
{
  "success": true,
  "data": [
    {
      "id": 15,
      "type": "DIRECT",
      "updatedAt": "2025-10-20T10:30:00Z",
      "lastMessage": {
        "id": 100,
        "content": "Hello!",
        "senderId": 5,
        "senderUsername": "john_doe",
        "sentAt": "2025-10-20T10:30:00Z",
        "type": "TEXT"
      }
    }
  ]
}
```

### WebSocket: conversation.update
```json
{
  "type": "conversation.update",
  "payload": {
    "conversationId": 15,
    "updatedAt": "2025-10-20T10:30:00Z",
    "lastMessage": {
      "id": 100,
      "content": "Hello!",
      "senderId": 5,
      "senderUsername": "john_doe",
      "sentAt": "2025-10-20T10:30:00Z",
      "type": "TEXT"
    }
  }
}
```

### WebSocket: user.status
```json
{
  "type": "user.status",
  "payload": {
    "userId": "5",
    "username": "john_doe",
    "displayName": "John Doe",
    "isOnline": true,
    "lastSeen": "2025-10-20T10:30:00Z"
  }
}
```

## Client Implementation

### JavaScript Example
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'conversation.update') {
    // Update conversation list
    updateConversationLastMessage(
      message.payload.conversationId,
      message.payload.lastMessage
    );
  }
  
  if (message.type === 'user.status') {
    // Update user online status
    updateUserStatus(
      message.payload.userId,
      message.payload.isOnline,
      message.payload.lastSeen
    );
  }
};

// Send heartbeat every 30 seconds
setInterval(() => {
  ws.send(JSON.stringify({
    type: 'heartbeat',
    payload: {}
  }));
}, 30000);
```

## Performance Considerations

### Last Seen:
- ✅ Update throttled by WebSocket messages (không update liên tục)
- ✅ Cleanup job chạy mỗi 5 phút (không overhead)
- ✅ Sử dụng concurrent map cho session tracking

### Last Message:
- ✅ Chỉ update khi có message mới
- ✅ JPA @PreUpdate tự động update updatedAt
- ✅ Broadcast chỉ đến participants (không broadcast toàn bộ)

## Database Impact

### New Indexes (Recommended):
```sql
-- Already exists
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at DESC);
CREATE INDEX idx_users_is_online ON users(is_online);
CREATE INDEX idx_users_last_seen ON users(last_seen);
```

## Next Steps (Optional Enhancements)

1. **Read Receipts**: Track khi user đọc message
2. **Unread Count**: Đếm số message chưa đọc
3. **Message Reactions**: Emoji reactions cho messages
4. **Message Editing**: Cho phép edit message đã gửi
5. **Message Deletion**: Soft delete messages
6. **Presence Indicators**: Show typing, recording, etc.

## Documentation

- ✅ `REALTIME_UPDATES_DOCUMENTATION.md` - Chi tiết đầy đủ
- ✅ `REALTIME_UPDATES_SUMMARY.md` - Tóm tắt thay đổi (file này)

## Conclusion

Hệ thống realtime updates cho `last_seen` và `last_message` đã được triển khai thành công với:
- ✅ Tự động update khi có activity
- ✅ Broadcast realtime qua WebSocket
- ✅ REST API trả về đầy đủ thông tin
- ✅ Performance optimized
- ✅ Build success, no errors
- ✅ Documentation đầy đủ

Hệ thống sẵn sàng để deploy và test!

