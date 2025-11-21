# 🚀 WebSocket Quick Reference

Tài liệu tham khảo nhanh cho WebSocket testing.

## 👥 Add Contact (Bắt buộc trước khi gọi)

**Lưu ý:** 2 users phải là contacts trước khi có thể gọi nhau!

### Cách 1: Dùng script (Nhanh nhất)

```bash
chmod +x chattrix-api/.spec/test-add-contact.sh

# User 1 add User 2 làm contact
./chattrix-api/.spec/test-add-contact.sh <USER1_TOKEN> 10

# User 2 add User 1 làm contact (cần cả 2 chiều)
./chattrix-api/.spec/test-add-contact.sh <USER2_TOKEN> 1
```

### Cách 2: Dùng curl

```bash
# User 1 (ID: 1) add User 2 (ID: 10) làm contact
curl -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"contactUserId": 10}'

# User 2 (ID: 10) add User 1 (ID: 1) làm contact
curl -X POST http://localhost:8080/api/v1/contacts \
  -H "Authorization: Bearer <USER2_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"contactUserId": 1}'
```

**Response:**
```json
{
  "success": true,
  "message": "Contact added successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "contactUserId": 10,
    "contactUsername": "phong",
    "contactFullName": "hoang phong",
    "contactEmail": "phong@example.com",
    "nickname": null,
    "isFavorite": false,
    "createdAt": "2025-11-21T13:50:00Z"
  }
}
```

---

## 📞 Lấy Call ID để test

### Cách 1: Dùng curl (Nhanh nhất)

```bash
# User 1 (ID: 1) gọi User 2 (ID: 10)
curl -X POST http://localhost:8080/api/v1/calls/initiate \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "calleeId": "10",
    "callType": "VIDEO",
    "channelId": "test-channel-123"
  }' | jq -r '.data.id'
```

Output: `550e8400-e29b-41d4-a716-446655440000` (Call ID)

### Cách 2: Dùng script

```bash
chmod +x chattrix-api/.spec/test-call-initiate.sh
./chattrix-api/.spec/test-call-initiate.sh <USER1_TOKEN> 10 VIDEO
```

### Cách 3: Dùng Postman

1. Import collection: `Chattrix_API_Collection.postman_collection.json`
2. Chạy request: `Calls > Initiate Call`
3. Copy `data.id` từ response

---

## 🎯 Test Flow Nhanh

### 1. Chuẩn bị (1 lần)

```bash
# Lấy tokens
USER1_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"long","password":"your_password"}' | jq -r '.data.accessToken')

USER2_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"phong","password":"your_password"}' | jq -r '.data.accessToken')

echo "User 1 Token: $USER1_TOKEN"
echo "User 2 Token: $USER2_TOKEN"
```

### 2. Mở WebSocket Test Client

```bash
# Mở file trong browser
open chattrix-api/.spec/websocket-test-client.html
```

### 3. Connect cả 2 users

- User 1: Paste `$USER1_TOKEN` → Connect
- User 2: Paste `$USER2_TOKEN` → Connect

### 4. Test Chat

- User 1: Nhập Conversation ID → Send message
- User 2: Xem message trong log

### 5. Test Call

```bash
# Tạo call
CALL_ID=$(curl -s -X POST http://localhost:8080/api/v1/calls/initiate \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "calleeId": "10",
    "callType": "VIDEO",
    "channelId": "test-'$(date +%s)'"
  }' | jq -r '.data.id')

echo "Call ID: $CALL_ID"
```

- User 2: Nhận invitation → Call ID tự động điền → Click "Accept Call"
- User 1: Nhận `call_accepted` → Click "End Call"
- User 2: Nhận `call_ended`

---

## 📋 Message Types Cheat Sheet

### Client → Server

| Action | Type | Payload |
|--------|------|---------|
| Send message | `chat.message` | `{ conversationId, content }` |
| Start typing | `typing.start` | `{ conversationId }` |
| Stop typing | `typing.stop` | `{ conversationId }` |
| Heartbeat | `heartbeat` | `{}` |
| Accept call | `call.accept` | `{ callId }` |
| Reject call | `call.reject` | `{ callId, reason }` |
| End call | `call.end` | `{ callId, durationSeconds? }` |

### Server → Client

| Event | Type | Data |
|-------|------|------|
| New message | `chat.message` | Message object |
| Typing indicator | `typing.indicator` | `{ conversationId, typingUsers[] }` |
| User status | `user.status` | `{ userId, isOnline, ... }` |
| Heartbeat ack | `heartbeat.ack` | `{ userId, timestamp }` |
| Call invitation | `call_invitation` | `{ callId, callerId, ... }` |
| Call accepted | `call_accepted` | `{ callId, acceptedBy }` |
| Call rejected | `call_rejected` | `{ callId, rejectedBy, reason }` |
| Call ended | `call_ended` | `{ callId, endedBy, durationSeconds }` |
| Call timeout | `call_timeout` | `{ callId }` |
| Call error | `call_error` | `{ callId, errorType, message }` |

---

## 🐛 Common Issues

### Issue: "Call not found"

**Cause:** Call ID không tồn tại hoặc đã bị xóa.

**Fix:** Tạo call mới qua REST API.

### Issue: "Unauthorized"

**Cause:** User không phải là participant của call.

**Fix:** Kiểm tra lại caller/callee ID.

### Issue: "Invalid status"

**Cause:** Call không ở trạng thái phù hợp.

**Fix:** Kiểm tra call status trước khi thực hiện action.

### Issue: WebSocket disconnect sau 90s

**Cause:** Server timeout.

**Fix:** Test client tự động gửi heartbeat mỗi 30s. Nếu vẫn bị, check network.

### Issue: Typing indicator không hoạt động

**Cause:** 2 users không cùng conversation.

**Fix:** Đảm bảo Conversation ID giống nhau.

---

## 🔧 Useful Commands

### Check server logs

```bash
# Docker
docker logs -f chattrix-api

# Local WildFly
tail -f wildfly/standalone/log/server.log
```

### Get user info

```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <TOKEN>" | jq .
```

### Get conversations

```bash
curl -X GET http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer <TOKEN>" | jq .
```

### Get call details

```bash
curl -X GET http://localhost:8080/api/v1/calls/<CALL_ID> \
  -H "Authorization: Bearer <TOKEN>" | jq .
```

---

## 📊 Test Status Checklist

```
✅ WebSocket connection
✅ Chat messages
✅ Typing indicators
✅ Heartbeat
✅ Call invitation
✅ Call accept
✅ Call reject
✅ Call end
✅ Call timeout
✅ Error handling
```

---

## 🎓 Pro Tips

1. **Mở Developer Console (F12)** để xem WebSocket frames
2. **Dùng jq** để format JSON output: `curl ... | jq .`
3. **Save tokens vào biến** để dùng lại: `export USER1_TOKEN=...`
4. **Test với nhiều tabs** để simulate nhiều users
5. **Clear logs thường xuyên** để dễ theo dõi
6. **Check server logs** khi có lỗi không rõ nguyên nhân

---

**Need help?** Xem [WEBSOCKET_TESTING_GUIDE.md](./WEBSOCKET_TESTING_GUIDE.md) để có hướng dẫn chi tiết hơn.
