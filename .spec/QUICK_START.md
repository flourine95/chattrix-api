# 🚀 Quick Start - WebSocket Call Testing

Hướng dẫn nhanh nhất để test WebSocket call features.

## ⚡ One-Command Setup

```bash
# Cho phép execute scripts
chmod +x chattrix-api/.spec/*.sh

# Setup tất cả (login, add contacts, create conversation)
./chattrix-api/.spec/setup-test-users.sh
```

Script sẽ:
1. ✅ Login 2 users
2. ✅ Add contacts (cả 2 chiều)
3. ✅ Create conversation
4. ✅ In ra tokens và IDs để dùng

---

## 📱 Test WebSocket

### 1. Mở Test Client

```bash
# Mở trong browser
open chattrix-api/.spec/websocket-test-client.html
```

### 2. Paste Tokens

Copy tokens từ output của `setup-test-users.sh` và paste vào:
- User 1 panel: Paste User 1 token
- User 2 panel: Paste User 2 token

### 3. Connect

Click "Connect" cho cả 2 users.

---

## 📞 Test Call

### Initiate Call

```bash
# User 1 gọi User 2
./chattrix-api/.spec/test-call-initiate.sh <USER1_TOKEN> <USER2_ID> VIDEO
```

### Accept/Reject Call

User 2 sẽ nhận được call invitation trong WebSocket test client:
- Call ID tự động điền vào ô
- Click "Accept Call" hoặc "Reject Call"

### End Call

Bất kỳ user nào click "End Call" trong test client.

---

## 💬 Test Chat

### Send Message

1. Nhập Conversation ID (từ setup script)
2. Type message
3. Click "Send" hoặc Enter

### Typing Indicator

Click "Start Typing" → User kia sẽ thấy typing indicator

---

## 🔧 Manual Setup (Nếu không dùng script)

### 1. Login Users

```bash
# User 1
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"long","password":"your_password"}'

# User 2
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"phong","password":"your_password"}'
```

### 2. Add Contacts (Bắt buộc!)

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

### 3. Create Conversation

```bash
curl -X POST http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer <USER1_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"type":"DIRECT","participantIds":[1,10]}'
```

### 4. Initiate Call

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

---

## 📚 Full Documentation

- [WEBSOCKET_TESTING_GUIDE.md](./WEBSOCKET_TESTING_GUIDE.md) - Chi tiết từng bước
- [WEBSOCKET_QUICK_REFERENCE.md](./WEBSOCKET_QUICK_REFERENCE.md) - Commands reference
- [CALL_API_DOCUMENTATION.md](./CALL_API_DOCUMENTATION.md) - API documentation

---

## ❓ Troubleshooting

### "Users are not contacts"

**Fix:** Chạy lại add contacts cho cả 2 chiều:

```bash
./chattrix-api/.spec/test-add-contact.sh <USER1_TOKEN> <USER2_ID>
./chattrix-api/.spec/test-add-contact.sh <USER2_TOKEN> <USER1_ID>
```

### "Call not found"

**Fix:** Tạo call mới qua REST API trước:

```bash
./chattrix-api/.spec/test-call-initiate.sh <USER1_TOKEN> <USER2_ID> VIDEO
```

### WebSocket disconnect

**Fix:** Test client tự động gửi heartbeat mỗi 30s. Nếu vẫn bị, check network.

### "Invalid token"

**Fix:** Token hết hạn sau 1 giờ. Login lại để lấy token mới.

---

## 🎯 Test Scenarios

### Scenario 1: Video Call
```bash
# 1. Setup
./chattrix-api/.spec/setup-test-users.sh

# 2. Open test client & connect both users

# 3. Initiate call
./chattrix-api/.spec/test-call-initiate.sh <USER1_TOKEN> <USER2_ID> VIDEO

# 4. User 2 accept in test client

# 5. User 1 end call in test client
```

### Scenario 2: Call Rejection
```bash
# 1-3. Same as above

# 4. User 2 reject in test client
```

### Scenario 3: Chat + Call
```bash
# 1. Setup & connect

# 2. Send messages between users

# 3. Initiate call

# 4. Accept call

# 5. Continue chatting during call

# 6. End call
```

---

**Ready to test? Run the setup script! 🚀**

```bash
./chattrix-api/.spec/setup-test-users.sh
```
