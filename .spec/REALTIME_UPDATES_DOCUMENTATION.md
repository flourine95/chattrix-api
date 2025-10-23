# Realtime Updates - Last Seen & Last Message

## Tổng quan

Hệ thống đã được thiết kế để tự động cập nhật và broadcast realtime cho:
1. **Last Seen** - Thời gian user online lần cuối
2. **Last Message** - Tin nhắn cuối cùng trong conversation

## 1. Last Seen Tracking

### Cơ chế hoạt động

#### Khi nào last_seen được cập nhật?

1. **User đăng nhập** (AuthService)
   - `last_seen` được set = `Instant.now()`
   - `isOnline` được set = `true`

2. **User kết nối WebSocket** (ChatServerEndpoint.onOpen)
   - `isOnline` được set = `true`
   - `last_seen` được cập nhật
   - Broadcast `user.status` event đến tất cả users

3. **User gửi bất kỳ WebSocket message nào** (ChatServerEndpoint.onMessage)
   - `last_seen` được cập nhật mỗi khi user:
     - Gửi chat message
     - Bắt đầu typing
     - Dừng typing
     - Gửi heartbeat

4. **User ngắt kết nối WebSocket** (ChatServerEndpoint.onClose)
   - `isOnline` được set = `false`
   - `last_seen` được cập nhật = thời điểm disconnect
   - Broadcast `user.status` event đến tất cả users

5. **Cleanup tự động** (UserStatusCleanupService)
   - Chạy mỗi 5 phút
   - Mark users offline nếu `last_seen` > 5 phút

### WebSocket Events

#### User Status Change Event
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

#### Heartbeat (Client → Server)
Client có thể gửi heartbeat để duy trì connection và update last_seen:
```json
{
  "type": "heartbeat",
  "payload": {}
}
```

#### Heartbeat Acknowledgment (Server → Client)
```json
{
  "type": "heartbeat.ack",
  "payload": {
    "userId": "5",
    "timestamp": "2025-10-20T10:30:00Z"
  }
}
```

### Implementation Details

**Files Modified:**
- `src/main/java/com/chattrix/api/entities/User.java` - Có fields `isOnline` và `lastSeen`
- `src/main/java/com/chattrix/api/services/UserStatusService.java` - Logic update status
- `src/main/java/com/chattrix/api/websocket/ChatServerEndpoint.java` - WebSocket handlers
- `src/main/java/com/chattrix/api/services/UserStatusCleanupService.java` - Scheduled cleanup

---

## 2. Last Message Tracking

### Cơ chế hoạt động

#### Khi nào lastMessage được cập nhật?

1. **User gửi message qua REST API** (MessageService.sendMessage)
   - Message được lưu vào database
   - `conversation.lastMessage` được set = message mới
   - `conversation.updatedAt` được cập nhật tự động (JPA @PreUpdate)

2. **User gửi message qua WebSocket** (ChatServerEndpoint.processChatMessage)
   - Message được lưu vào database
   - `conversation.lastMessage` được set = message mới
   - `conversation.updatedAt` được cập nhật
   - Broadcast `chat.message` event đến participants
   - Broadcast `conversation.update` event đến participants

### WebSocket Events

#### Chat Message Event
```json
{
  "type": "chat.message",
  "payload": {
    "id": 100,
    "conversationId": 15,
    "sender": {
      "id": 5,
      "username": "john_doe",
      "fullName": "John Doe",
      "email": "john@example.com",
      "avatarUrl": "https://...",
      "isOnline": true,
      "lastSeen": "2025-10-20T10:30:00Z"
    },
    "content": "Hello everyone!",
    "createdAt": "2025-10-20T10:30:00Z"
  }
}
```

#### Conversation Update Event (NEW!)
```json
{
  "type": "conversation.update",
  "payload": {
    "conversationId": 15,
    "updatedAt": "2025-10-20T10:30:00Z",
    "lastMessage": {
      "id": 100,
      "content": "Hello everyone!",
      "senderId": 5,
      "senderUsername": "john_doe",
      "sentAt": "2025-10-20T10:30:00Z",
      "type": "TEXT"
    }
  }
}
```

### REST API Response

#### GET /v1/conversations
Trả về danh sách conversations với lastMessage:
```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": [
    {
      "id": 15,
      "type": "DIRECT",
      "name": null,
      "createdAt": "2025-10-15T08:00:00Z",
      "updatedAt": "2025-10-20T10:30:00Z",
      "participants": [...],
      "lastMessage": {
        "id": 100,
        "content": "Hello everyone!",
        "senderId": 5,
        "senderUsername": "john_doe",
        "sentAt": "2025-10-20T10:30:00Z",
        "type": "TEXT"
      }
    }
  ]
}
```

### Implementation Details

**Files Modified:**
- `src/main/java/com/chattrix/api/entities/Conversation.java` - Có field `lastMessage`
- `src/main/java/com/chattrix/api/services/MessageService.java` - Update lastMessage khi send
- `src/main/java/com/chattrix/api/websocket/ChatServerEndpoint.java` - Broadcast updates
- `src/main/java/com/chattrix/api/responses/ConversationResponse.java` - Include lastMessage

**Files Created:**
- `src/main/java/com/chattrix/api/websocket/dto/ConversationUpdateDto.java` - DTO cho event

---

## 3. Client Implementation Guide

### WebSocket Connection
```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/chat?token=YOUR_JWT_TOKEN');

ws.onopen = () => {
  console.log('Connected to WebSocket');
  
  // Start heartbeat to keep connection alive
  setInterval(() => {
    ws.send(JSON.stringify({
      type: 'heartbeat',
      payload: {}
    }));
  }, 30000); // Every 30 seconds
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  switch(message.type) {
    case 'chat.message':
      handleNewMessage(message.payload);
      break;
      
    case 'conversation.update':
      handleConversationUpdate(message.payload);
      break;
      
    case 'user.status':
      handleUserStatusChange(message.payload);
      break;
      
    case 'heartbeat.ack':
      console.log('Heartbeat acknowledged');
      break;
  }
};
```

### Handling Conversation Updates
```javascript
function handleConversationUpdate(update) {
  // Update conversation list UI
  const conversation = conversations.find(c => c.id === update.conversationId);
  if (conversation) {
    conversation.updatedAt = update.updatedAt;
    conversation.lastMessage = update.lastMessage;
    
    // Re-sort conversations by updatedAt
    conversations.sort((a, b) => 
      new Date(b.updatedAt) - new Date(a.updatedAt)
    );
    
    // Update UI
    renderConversationList();
  }
}
```

### Handling User Status Changes
```javascript
function handleUserStatusChange(status) {
  // Update user status in UI
  const userElement = document.querySelector(`[data-user-id="${status.userId}"]`);
  if (userElement) {
    if (status.isOnline) {
      userElement.classList.add('online');
      userElement.querySelector('.status-text').textContent = 'Online';
    } else {
      userElement.classList.remove('online');
      const lastSeen = new Date(status.lastSeen);
      userElement.querySelector('.status-text').textContent = 
        `Last seen ${formatRelativeTime(lastSeen)}`;
    }
  }
}
```

---

## 4. Database Schema

### Users Table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(500),
  is_online BOOLEAN NOT NULL DEFAULT FALSE,
  last_seen TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

### Conversations Table
```sql
CREATE TABLE conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(150),
  type VARCHAR(20) NOT NULL, -- 'DIRECT' or 'GROUP'
  last_message_id BIGINT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  FOREIGN KEY (last_message_id) REFERENCES messages(id)
);
```

---

## 5. Performance Considerations

### Last Seen Updates
- Updates chỉ xảy ra khi có activity thực sự
- Không update quá thường xuyên (throttled by WebSocket messages)
- Cleanup job chạy mỗi 5 phút để tránh overhead

### Last Message Updates
- Chỉ update khi có message mới
- Sử dụng JPA @PreUpdate để tự động update `updatedAt`
- Index trên `updated_at` để sort conversations nhanh

### WebSocket Broadcasting
- Chỉ broadcast đến participants của conversation
- Sử dụng concurrent map để track active sessions
- Tự động cleanup invalid sessions

---

## 6. Testing

### Test Last Seen
1. Login user → Check `isOnline = true`, `lastSeen` updated
2. Connect WebSocket → Check status broadcast
3. Send message → Check `lastSeen` updated
4. Disconnect → Check `isOnline = false`, status broadcast
5. Wait 5+ minutes → Check cleanup job marks user offline

### Test Last Message
1. Send message via REST API → Check `lastMessage` updated
2. Send message via WebSocket → Check broadcast events
3. Get conversations → Check `lastMessage` included
4. Multiple messages → Check `lastMessage` always latest

---

## 7. Troubleshooting

### Last Seen không update
- Check WebSocket connection
- Check UserStatusService được inject đúng
- Check database transaction commit

### Last Message không hiển thị
- Check ConversationResponse mapping
- Check lastMessage relationship trong entity
- Check fetch strategy (LAZY vs EAGER)

### WebSocket events không nhận được
- Check client WebSocket connection
- Check token authentication
- Check ChatSessionService tracking sessions
- Check browser console for errors

