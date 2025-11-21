# 🧪 WebSocket Testing Guide

Hướng dẫn chi tiết để test WebSocket features của Chattrix API.

## 📋 Mục lục

1. [Chuẩn bị](#chuẩn-bị)
2. [Test Chat Features](#test-chat-features)
3. [Test Call Features](#test-call-features)
4. [Troubleshooting](#troubleshooting)

---

## Chuẩn bị

### 1. Mở WebSocket Test Client

Mở file `websocket-test-client.html` trong trình duyệt:

```bash
# Windows
start chattrix-api/.spec/websocket-test-client.html

# Mac
open chattrix-api/.spec/websocket-test-client.html

# Linux
xdg-open chattrix-api/.spec/websocket-test-client.html
```

### 2. Lấy JWT Tokens cho 2 users

**Option 1: Dùng Postman**

Import collection từ `.spec/Chattrix_API_Collection.postman_collection.json` và chạy Login request.

**Option 2: Dùng curl**

```bash
# Login User 1
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"long","password":"your_password"}'

# Login User 2
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"phong","password":"your_password"}'
```

Copy `accessToken` từ response.

### 3. Add Contacts (Bắt buộc cho Call)

**⚠️ Quan trọng:** 2 users phải là contacts trước khi có thể gọi nhau!

**Cách 1: Dùng script**

```bash
chmod +x chattrix-api/.spec/test-add-contact.sh

# User 1 add User 2
./chattrix-api/.spec/test-add-contact.sh <USER1_TOKEN> 10

# User 2 add User 1 (cần cả 2 chiều)
./chattrix-api/.spec/test-add-contact.sh <USER2_TOKEN> 1
```

**Cách 2: Dùng curl**

```bash
# User 1 add User 2
curl -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"contactUserId": 10}'

# User 2 add User 1
curl -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer <USER2_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"contactUserId": 1}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Contact added successfully",
  "data": {
    "id": 1,
    "contactUserId": 10,
    "contactUsername": "phong",
    "contactFullName": "hoang phong",
    "isFavorite": false
  }
}
```

### 4. Lấy Conversation ID

```bash
# Lấy danh sách conversations của user
curl -X GET http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

Copy `id` của conversation bạn muốn test.

**Nếu chưa có conversation:**

```bash
# Tạo conversation giữa 2 users
curl -X POST http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DIRECT",
    "participantIds": [1, 10]
  }'
```

---

## Test Chat Features

### ✅ Test 1: Kết nối WebSocket

**User 1:**
1. Paste JWT token vào ô "JWT Token"
2. Nhập Conversation ID
3. Click "Connect"
4. Kiểm tra status chuyển sang "Connected" (màu xanh)
5. Xem log: `ℹ️ INFO Connected to WebSocket`

**User 2:**
1. Làm tương tự với token của User 2
2. Cả 2 users sẽ nhận được `user.status` messages

**Expected logs:**
```json
⬇️ RECEIVED {
  "type": "user.status",
  "payload": {
    "userId": "1",
    "username": "long",
    "displayName": "phi long",
    "isOnline": true,
    "lastSeen": "2025-11-21T13:36:49.133962Z"
  }
}
```

---

### ✅ Test 2: Gửi Chat Message

**User 1:**
1. Nhập message vào ô "Type a message..."
2. Click "Send" hoặc nhấn Enter

**User 2:**
1. Sẽ nhận được message trong log

**Expected logs:**

**User 1 (Sender):**
```json
⬆️ SENT {
  "type": "chat.message",
  "payload": {
    "conversationId": "2",
    "content": "Hello from User 1!"
  }
}
```

**User 2 (Receiver):**
```json
⬇️ RECEIVED {
  "type": "chat.message",
  "payload": {
    "id": 19,
    "content": "Hello from User 1!",
    "type": "TEXT",
    "conversationId": 2,
    "createdAt": "2025-11-21T13:37:53.989310197Z",
    "sender": {
      "id": 1,
      "username": "long",
      "fullName": "phi long",
      "email": "long@example.com",
      "online": true
    }
  }
}
```

---

### ✅ Test 3: Typing Indicator

**User 1:**
1. Click "Start Typing"
2. Đợi vài giây
3. Click "Stop Typing"

**User 2:**
1. Sẽ nhận được typing indicators

**Expected logs:**

**User 1:**
```json
⬆️ SENT {
  "type": "typing.start",
  "payload": {
    "conversationId": "2"
  }
}
```

**User 2:**
```json
⬇️ RECEIVED {
  "type": "typing.indicator",
  "payload": {
    "conversationId": 2,
    "typingUsers": [
      {
        "id": 1,
        "username": "long",
        "fullName": "phi long"
      }
    ]
  }
}
```

Khi User 1 stop typing:
```json
⬇️ RECEIVED {
  "type": "typing.indicator",
  "payload": {
    "conversationId": 2,
    "typingUsers": []  // Empty array
  }
}
```

---

### ✅ Test 4: Heartbeat (Auto)

Heartbeat tự động gửi mỗi 30 giây để giữ kết nối.

**Expected logs (mỗi 30s):**
```json
⬆️ SENT {
  "type": "heartbeat",
  "payload": {}
}

⬇️ RECEIVED {
  "type": "heartbeat.ack",
  "payload": {
    "userId": "1",
    "timestamp": "2025-11-21T13:38:20.444873191Z"
  }
}
```

---

## Test Call Features

### ✅ Test 5: Initiate Call (REST API)

**Cách 1: Dùng script**

```bash
# Cho phép execute
chmod +x chattrix-api/.spec/test-call-initiate.sh

# Chạy script
./chattrix-api/.spec/test-call-initiate.sh <USER1_TOKEN> 10 VIDEO
```

**Cách 2: Dùng curl**

```bash
curl -X POST http://localhost:8080/api/v1/calls/initiate \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "calleeId": "10",
    "callType": "VIDEO",
    "channelId": "test-channel-123"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Call initiated successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",  // <-- Call ID
    "callerId": "1",
    "calleeId": "10",
    "callType": "VIDEO",
    "status": "RINGING",
    "channelId": "test-channel-123"
  }
}
```

**User 2 (Callee) sẽ nhận được:**
```json
⬇️ RECEIVED {
  "type": "call_invitation",
  "data": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "channelId": "test-channel-123",
    "callerId": "1",
    "callerName": "phi long",
    "callerAvatar": null,
    "callType": "VIDEO"
  },
  "timestamp": "2025-11-21T13:45:00Z"
}

ℹ️ INFO 📞 Incoming call from phi long
```

**Call ID sẽ tự động được điền vào ô "Call ID" của User 2!**

---

### ✅ Test 6: Accept Call

**User 2 (Callee):**
1. Sau khi nhận call invitation, Call ID đã tự động điền
2. Click "Accept Call"

**Expected logs:**

**User 2:**
```json
⬆️ SENT {
  "type": "call.accept",
  "payload": {
    "callId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**User 1 (Caller):**
```json
⬇️ RECEIVED {
  "type": "call_accepted",
  "data": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "acceptedBy": "10"
  },
  "timestamp": "2025-11-21T13:45:15Z"
}
```

---

### ✅ Test 7: Reject Call

**User 2 (Callee):**
1. Sau khi nhận call invitation
2. Click "Reject Call"

**Expected logs:**

**User 2:**
```json
⬆️ SENT {
  "type": "call.reject",
  "payload": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "reason": "declined"
  }
}
```

**User 1 (Caller):**
```json
⬇️ RECEIVED {
  "type": "call_rejected",
  "data": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "rejectedBy": "10",
    "reason": "declined"
  },
  "timestamp": "2025-11-21T13:45:10Z"
}
```

---

### ✅ Test 8: End Call

**Bất kỳ user nào (Caller hoặc Callee):**
1. Nhập Call ID (hoặc đã có sẵn)
2. Click "End Call"

**Expected logs:**

**User 1 (Ending call):**
```json
⬆️ SENT {
  "type": "call.end",
  "payload": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "durationSeconds": 60
  }
}
```

**User 2 (Other participant):**
```json
⬇️ RECEIVED {
  "type": "call_ended",
  "data": {
    "callId": "550e8400-e29b-41d4-a716-446655440000",
    "endedBy": "1",
    "durationSeconds": 60
  },
  "timestamp": "2025-11-21T13:46:15Z"
}
```

---

### ✅ Test 9: Call Timeout

**Setup:**
1. User 1 initiate call
2. User 2 **KHÔNG** accept hoặc reject
3. Đợi 60 giây

**Expected logs (cả 2 users):**
```json
⬇️ RECEIVED {
  "type": "call_timeout",
  "data": {
    "callId": "550e8400-e29b-41d4-a716-446655440000"
  },
  "timestamp": "2025-11-21T13:46:00Z"
}
```

Call status tự động chuyển sang `MISSED`.

---

### ✅ Test 10: Call Error Handling

**Test với Call ID không tồn tại:**

**User 1:**
1. Nhập Call ID bất kỳ: "invalid-call-id"
2. Click "Accept Call"

**Expected logs:**
```json
⬆️ SENT {
  "type": "call.accept",
  "payload": {
    "callId": "invalid-call-id"
  }
}

⬇️ RECEIVED {
  "type": "call_error",
  "payload": {
    "callId": "invalid-call-id",
    "errorType": "call_not_found",
    "message": "Call not found: invalid-call-id"
  }
}
```

**Test với unauthorized user:**

**User 1:**
1. Lấy Call ID của cuộc gọi giữa User 2 và User 3
2. Cố gắng accept call đó

**Expected logs:**
```json
⬇️ RECEIVED {
  "type": "call_error",
  "payload": {
    "callId": "...",
    "errorType": "unauthorized",
    "message": "User is not the callee of this call"
  }
}
```

---

## Troubleshooting

### ❌ Vấn đề: WebSocket bị disconnect sau 90 giây

**Nguyên nhân:** Server có read timeout 90 giây.

**Giải pháp:** Test client đã tự động gửi heartbeat mỗi 30 giây. Nếu vẫn bị disconnect:
- Kiểm tra network connection
- Kiểm tra server logs
- Thử reconnect

---

### ❌ Vấn đề: Không nhận được call invitation

**Checklist:**
1. ✅ User 2 đã connect WebSocket?
2. ✅ Call được tạo thành công qua REST API?
3. ✅ Callee ID đúng (User 2 ID)?
4. ✅ Kiểm tra server logs có lỗi không?

**Debug:**
```bash
# Kiểm tra server logs
docker logs -f chattrix-api

# Hoặc nếu chạy local
tail -f wildfly/standalone/log/server.log
```

---

### ❌ Vấn đề: Typing indicator không hoạt động

**Nguyên nhân:** Có thể do cả 2 users không cùng conversation.

**Giải pháp:**
1. Kiểm tra Conversation ID của cả 2 users phải giống nhau
2. Kiểm tra cả 2 users đều là participants của conversation đó

---

### ❌ Vấn đề: Call error "invalid_status"

**Nguyên nhân:** Call không ở trạng thái phù hợp.

**Ví dụ:**
- Accept call đã được accept rồi
- Reject call đã ended
- End call chưa được accept

**Giải pháp:** Kiểm tra call status trước khi thực hiện action.

---

## 📊 Test Checklist

### Chat Features
- [ ] Connect WebSocket
- [ ] Send message
- [ ] Receive message
- [ ] Start typing
- [ ] Stop typing
- [ ] Receive typing indicator
- [ ] Heartbeat auto-send
- [ ] Reconnect after disconnect

### Call Features
- [ ] Initiate call (REST API)
- [ ] Receive call invitation
- [ ] Accept call
- [ ] Receive call accepted
- [ ] Reject call
- [ ] Receive call rejected
- [ ] End call
- [ ] Receive call ended
- [ ] Call timeout (60s)
- [ ] Error: call_not_found
- [ ] Error: unauthorized
- [ ] Error: invalid_status

---

## 🎯 Quick Test Scenarios

### Scenario 1: Happy Path - Video Call
```
1. User 1 initiate call (REST) → User 2
2. User 2 receive invitation → Accept
3. User 1 receive accepted
4. [Simulate video call for 2 minutes]
5. User 1 end call
6. User 2 receive ended
```

### Scenario 2: Call Rejection
```
1. User 1 initiate call (REST) → User 2
2. User 2 receive invitation → Reject (reason: busy)
3. User 1 receive rejected
```

### Scenario 3: Call Timeout
```
1. User 1 initiate call (REST) → User 2
2. User 2 receive invitation → [Do nothing]
3. Wait 60 seconds
4. Both users receive timeout
```

### Scenario 4: Chat During Call
```
1. User 1 initiate call → User 2 accept
2. User 1 send chat message
3. User 2 receive message
4. User 2 send chat message
5. User 1 receive message
6. User 1 end call
```

---

## 📚 Related Documentation

- [Call API Documentation](./CALL_API_DOCUMENTATION.md) - Đầy đủ API reference
- [API Documentation](./API_DOCUMENTATION.md) - Chat và User Status API
- [Postman Collection](./Chattrix_API_Collection.postman_collection.json) - REST API testing

---

**Happy Testing! 🚀**
