# 🧪 HƯỚNG DẪN TEST API TRÊN POSTMAN

## 📋 Mục lục
1. [Setup ban đầu](#setup-ban-đầu)
2. [Luồng test Friend Request](#1-friend-request-flow)
3. [Luồng test Message Edit/Delete](#2-message-editdelete-flow)
4. [Luồng test Message Forward](#3-message-forward-flow)
5. [Luồng test Read Receipts](#4-read-receipts-flow)
6. [Luồng test Pinned Messages](#5-pinned-messages-flow)
7. [Luồng test Conversation Settings](#6-conversation-settings-flow)

---

## Setup ban đầu

### 1. Tạo Environment trong Postman
```
BASE_URL = http://localhost:8080
USER1_TOKEN = <JWT token của user 1>
USER2_TOKEN = <JWT token của user 2>
USER3_TOKEN = <JWT token của user 3>
```

### 2. Đăng ký/Đăng nhập để lấy token
**Giả sử bạn đã có API đăng nhập, lấy token và lưu vào environment variables**

---

## 1. FRIEND REQUEST FLOW

### 📌 Luồng hoạt động:
```
User 1 → Gửi lời mời kết bạn → User 2
User 2 → Xem lời mời nhận được
User 2 → Chấp nhận lời mời
User 1 → Gửi lời mời → User 3
User 3 → Từ chối lời mời
User 1 → Hủy lời mời đã gửi
```

---

### ✅ Test 1.1: User 1 gửi lời mời kết bạn cho User 2

**Request:**
```http
POST {{BASE_URL}}/api/friend-requests/send
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "receiverUserId": 2,
  "nickname": "Bạn thân Alice"
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "userId": 2,
  "username": "alice",
  "fullName": "Alice Nguyen",
  "avatarUrl": "https://example.com/avatar2.jpg",
  "status": "PENDING",
  "nickname": "Bạn thân Alice",
  "isOnline": true,
  "requestedAt": "2024-01-15T10:30:00.000Z",
  "acceptedAt": null,
  "rejectedAt": null
}
```

**Lưu lại:** `FRIEND_REQUEST_ID_1 = 1`

---

### ✅ Test 1.2: User 2 xem danh sách lời mời nhận được

**Request:**
```http
GET {{BASE_URL}}/api/friend-requests/received
```

**Headers:**
```
Authorization: Bearer {{USER2_TOKEN}}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 1,
    "userId": 1,
    "username": "john_doe",
    "fullName": "John Doe",
    "status": "PENDING",
    "requestedAt": "2024-01-15T10:30:00.000Z"
  }
]
```

---

### ✅ Test 1.3: User 2 chấp nhận lời mời kết bạn

**Request:**
```http
POST {{BASE_URL}}/api/friend-requests/{{FRIEND_REQUEST_ID_1}}/accept
```

**Headers:**
```
Authorization: Bearer {{USER2_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "userId": 1,
  "username": "john_doe",
  "fullName": "John Doe",
  "status": "ACCEPTED",
  "acceptedAt": "2024-01-15T11:00:00.000Z"
}
```

**Kết quả:** User 1 và User 2 đã là bạn bè (quan hệ 2 chiều)

---

### ✅ Test 1.4: User 1 gửi lời mời cho User 3

**Request:**
```http
POST {{BASE_URL}}/api/friend-requests/send
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
Content-Type: application/json
```

**Body:**
```json
{
  "receiverUserId": 3
}
```

**Lưu lại:** `FRIEND_REQUEST_ID_2 = 2`

---

### ✅ Test 1.5: User 3 từ chối lời mời

**Request:**
```http
POST {{BASE_URL}}/api/friend-requests/{{FRIEND_REQUEST_ID_2}}/reject
```

**Headers:**
```
Authorization: Bearer {{USER3_TOKEN}}
```

**Expected Response (204 No Content)**

---

### ✅ Test 1.6: User 1 xem danh sách lời mời đã gửi

**Request:**
```http
GET {{BASE_URL}}/api/friend-requests/sent
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 2,
    "userId": 3,
    "username": "bob",
    "fullName": "Bob Wilson",
    "status": "REJECTED",
    "requestedAt": "2024-01-15T11:15:00.000Z",
    "rejectedAt": "2024-01-15T11:20:00.000Z"
  }
]
```

---

### ✅ Test 1.7: User 1 hủy lời mời đã gửi

**Request:**
```http
DELETE {{BASE_URL}}/api/friend-requests/{{FRIEND_REQUEST_ID_2}}/cancel
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (204 No Content)**

---

## 2. MESSAGE EDIT/DELETE FLOW

### 📌 Luồng hoạt động:
```
User 1 → Gửi tin nhắn trong conversation
User 1 → Chỉnh sửa tin nhắn
User 1 → Xem lịch sử chỉnh sửa
User 1 → Xóa tin nhắn
```

**Giả sử:** `CONVERSATION_ID = 5`, `MESSAGE_ID = 100`

---

### ✅ Test 2.1: User 1 chỉnh sửa tin nhắn

**Request:**
```http
PUT {{BASE_URL}}/api/messages/{{MESSAGE_ID}}/edit
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
Content-Type: application/json
```

**Body:**
```json
{
  "content": "Nội dung đã được chỉnh sửa lần 1"
}
```

**Expected Response (200 OK):**
```json
{
  "id": 100,
  "conversationId": 5,
  "senderId": 1,
  "senderUsername": "john_doe",
  "content": "Nội dung đã được chỉnh sửa lần 1",
  "type": "TEXT",
  "sentAt": "2024-01-15T10:00:00.000Z",
  "isEdited": true,
  "editedAt": "2024-01-15T10:30:00.000Z",
  "isDeleted": false
}
```

---

### ✅ Test 2.2: User 1 chỉnh sửa lần 2

**Request:**
```http
PUT {{BASE_URL}}/api/messages/{{MESSAGE_ID}}/edit
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
Content-Type: application/json
```

**Body:**
```json
{
  "content": "Nội dung đã được chỉnh sửa lần 2"
}
```

---

### ✅ Test 2.3: Xem lịch sử chỉnh sửa

**Request:**
```http
GET {{BASE_URL}}/api/messages/{{MESSAGE_ID}}/edit-history
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 2,
    "previousContent": "Nội dung đã được chỉnh sửa lần 1",
    "editedBy": 1,
    "editedByUsername": "john_doe",
    "editedAt": "2024-01-15T10:45:00.000Z"
  },
  {
    "id": 1,
    "previousContent": "Nội dung ban đầu",
    "editedBy": 1,
    "editedByUsername": "john_doe",
    "editedAt": "2024-01-15T10:30:00.000Z"
  }
]
```

---

### ✅ Test 2.4: User 1 xóa tin nhắn (soft delete)

**Request:**
```http
DELETE {{BASE_URL}}/api/messages/{{MESSAGE_ID}}
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (204 No Content)**

**Kết quả:** Tin nhắn bị đánh dấu `isDeleted = true`, nội dung bị ẩn nhưng vẫn giữ record để reply references

---

## 3. MESSAGE FORWARD FLOW

### 📌 Luồng hoạt động:
```
User 1 → Chuyển tiếp tin nhắn từ Conversation 5 → Conversation 7, 9
```

**Giả sử:** `MESSAGE_ID = 100`, `TARGET_CONV_1 = 7`, `TARGET_CONV_2 = 9`

---

### ✅ Test 3.1: Chuyển tiếp tin nhắn

**Request:**
```http
POST {{BASE_URL}}/api/messages/forward
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
Content-Type: application/json
```

**Body:**
```json
{
  "messageId": 100,
  "conversationIds": [7, 9]
}
```

**Expected Response (201 Created):**
```json
[
  {
    "id": 101,
    "conversationId": 7,
    "senderId": 1,
    "content": "Nội dung được chuyển tiếp",
    "type": "TEXT",
    "isForwarded": true,
    "originalMessageId": 100,
    "sentAt": "2024-01-15T11:00:00.000Z"
  },
  {
    "id": 102,
    "conversationId": 9,
    "senderId": 1,
    "content": "Nội dung được chuyển tiếp",
    "type": "TEXT",
    "isForwarded": true,
    "originalMessageId": 100,
    "sentAt": "2024-01-15T11:00:00.000Z"
  }
]
```

**Kết quả:** Tin nhắn gốc (ID=100) có `forwardCount = 2`

---

## 4. READ RECEIPTS FLOW

### 📌 Luồng hoạt động:
```
User 2 → Đánh dấu tin nhắn đã đọc
User 3 → Đánh dấu tin nhắn đã đọc
User 1 → Xem danh sách người đã đọc
User 2 → Đánh dấu toàn bộ conversation đã đọc
User 1 → Xem tổng số tin chưa đọc
```

---

### ✅ Test 4.1: User 2 đánh dấu tin nhắn đã đọc

**Request:**
```http
POST {{BASE_URL}}/api/messages/{{MESSAGE_ID}}/mark-read
```

**Headers:**
```
Authorization: Bearer {{USER2_TOKEN}}
```

**Expected Response (204 No Content)**

---

### ✅ Test 4.2: User 3 đánh dấu tin nhắn đã đọc

**Request:**
```http
POST {{BASE_URL}}/api/messages/{{MESSAGE_ID}}/mark-read
```

**Headers:**
```
Authorization: Bearer {{USER3_TOKEN}}
```

**Expected Response (204 No Content)**

---

### ✅ Test 4.3: User 1 xem danh sách người đã đọc

**Request:**
```http
GET {{BASE_URL}}/api/messages/{{MESSAGE_ID}}/read-receipts
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
[
  {
    "userId": 2,
    "username": "alice",
    "fullName": "Alice Nguyen",
    "avatarUrl": "https://example.com/avatar2.jpg",
    "readAt": "2024-01-15T10:35:00.000Z"
  },
  {
    "userId": 3,
    "username": "bob",
    "fullName": "Bob Wilson",
    "avatarUrl": "https://example.com/avatar3.jpg",
    "readAt": "2024-01-15T10:40:00.000Z"
  }
]
```

---

### ✅ Test 4.4: User 2 đánh dấu toàn bộ conversation đã đọc

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/mark-read?lastMessageId=150
```

**Headers:**
```
Authorization: Bearer {{USER2_TOKEN}}
```

**Expected Response (204 No Content)**

**Kết quả:** `unreadCount` của User 2 trong conversation này = 0

---

### ✅ Test 4.5: User 1 xem tổng số tin chưa đọc

**Request:**
```http
GET {{BASE_URL}}/api/unread-count
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "unreadCount": 15
}
```

---

## 5. PINNED MESSAGES FLOW

### 📌 Luồng hoạt động:
```
User 1 (Admin) → Ghim tin nhắn 1
User 1 → Ghim tin nhắn 2
User 1 → Ghim tin nhắn 3
User 1 → Xem danh sách tin nhắn đã ghim
User 1 → Bỏ ghim tin nhắn 2
```

**Giả sử:** `CONVERSATION_ID = 5`, `MSG_1 = 100`, `MSG_2 = 105`, `MSG_3 = 110`

---

### ✅ Test 5.1: Ghim tin nhắn 1

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/pinned-messages/{{MSG_1}}/pin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "messageId": 100,
  "content": "Tin nhắn quan trọng số 1",
  "senderId": 1,
  "senderUsername": "john_doe",
  "pinnedBy": 1,
  "pinnedByUsername": "john_doe",
  "pinOrder": 1,
  "pinnedAt": "2024-01-15T11:00:00.000Z",
  "sentAt": "2024-01-15T10:00:00.000Z"
}
```

---

### ✅ Test 5.2: Ghim tin nhắn 2

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/pinned-messages/{{MSG_2}}/pin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

---

### ✅ Test 5.3: Ghim tin nhắn 3

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/pinned-messages/{{MSG_3}}/pin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

---

### ✅ Test 5.4: Xem danh sách tin nhắn đã ghim

**Request:**
```http
GET {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/pinned-messages
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 1,
    "messageId": 100,
    "content": "Tin nhắn quan trọng số 1",
    "pinOrder": 1,
    "pinnedAt": "2024-01-15T11:00:00.000Z"
  },
  {
    "id": 2,
    "messageId": 105,
    "content": "Tin nhắn quan trọng số 2",
    "pinOrder": 2,
    "pinnedAt": "2024-01-15T11:05:00.000Z"
  },
  {
    "id": 3,
    "messageId": 110,
    "content": "Tin nhắn quan trọng số 3",
    "pinOrder": 3,
    "pinnedAt": "2024-01-15T11:10:00.000Z"
  }
]
```

---

### ✅ Test 5.5: Bỏ ghim tin nhắn 2

**Request:**
```http
DELETE {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/pinned-messages/{{MSG_2}}/unpin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (204 No Content)**

---

### ✅ Test 5.6: Thử ghim tin nhắn thứ 4 (sẽ lỗi vì max = 3)

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/pinned-messages/115/pin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (400 Bad Request):**
```json
{
  "error": "Bad Request",
  "message": "Cannot pin more than 3 messages"
}
```

---

## 6. CONVERSATION SETTINGS FLOW

### 📌 Luồng hoạt động:
```
User 1 → Ẩn conversation
User 1 → Bỏ ẩn conversation
User 1 → Archive conversation
User 1 → Unarchive conversation
User 1 → Ghim conversation lên đầu
User 1 → Bỏ ghim conversation
User 1 → Tắt thông báo
User 1 → Bật thông báo
User 1 → Xem settings hiện tại
```

**Giả sử:** `CONVERSATION_ID = 5`

---

### ✅ Test 6.1: Ẩn conversation

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/hide
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 5,
  "userId": 1,
  "isHidden": true,
  "hiddenAt": "2024-01-15T11:00:00.000Z",
  "isArchived": false,
  "isPinned": false,
  "isMuted": false
}
```

---

### ✅ Test 6.2: Bỏ ẩn conversation

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/unhide
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

---

### ✅ Test 6.3: Archive conversation

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/archive
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 5,
  "userId": 1,
  "isHidden": false,
  "isArchived": true,
  "archivedAt": "2024-01-15T11:05:00.000Z",
  "isPinned": false,
  "isMuted": false
}
```

---

### ✅ Test 6.4: Unarchive conversation

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/unarchive
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

---

### ✅ Test 6.5: Ghim conversation lên đầu

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/pin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 5,
  "userId": 1,
  "isHidden": false,
  "isArchived": false,
  "isPinned": true,
  "pinOrder": 1,
  "pinnedAt": "2024-01-15T11:10:00.000Z",
  "isMuted": false
}
```

---

### ✅ Test 6.6: Bỏ ghim conversation

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/unpin
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

---

### ✅ Test 6.7: Tắt thông báo

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/mute
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 5,
  "userId": 1,
  "isHidden": false,
  "isArchived": false,
  "isPinned": false,
  "isMuted": true,
  "mutedAt": "2024-01-15T11:15:00.000Z"
}
```

---

### ✅ Test 6.8: Bật thông báo

**Request:**
```http
POST {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings/unmute
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

---

### ✅ Test 6.9: Xem settings hiện tại

**Request:**
```http
GET {{BASE_URL}}/api/conversations/{{CONVERSATION_ID}}/settings
```

**Headers:**
```
Authorization: Bearer {{USER1_TOKEN}}
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 5,
  "userId": 1,
  "isHidden": false,
  "isArchived": false,
  "isPinned": false,
  "isMuted": false,
  "customNickname": null,
  "theme": null,
  "notificationsEnabled": true
}
```

---

## 📊 TỔNG KẾT

### Tổng số test cases: **35 tests**

| Feature | Số tests |
|---------|----------|
| Friend Requests | 7 |
| Message Edit/Delete | 4 |
| Message Forward | 1 |
| Read Receipts | 5 |
| Pinned Messages | 6 |
| Conversation Settings | 9 |
| **TOTAL** | **32** |

---

## 🎯 THỨ TỰ TEST KHUYẾN NGHỊ

1. **Friend Requests** - Test trước để tạo quan hệ bạn bè
2. **Message Edit/Delete** - Test chỉnh sửa/xóa tin nhắn
3. **Read Receipts** - Test đánh dấu đã đọc
4. **Message Forward** - Test chuyển tiếp
5. **Pinned Messages** - Test ghim tin nhắn
6. **Conversation Settings** - Test các settings

---

## ⚠️ LƯU Ý

1. **JWT Token**: Cần có token hợp lệ cho mỗi user
2. **IDs**: Thay thế các ID (conversationId, messageId, userId) bằng giá trị thực tế trong database
3. **Permissions**: Một số API yêu cầu quyền ADMIN (ví dụ: pin message trong group)
4. **Timestamps**: Các timestamp trong response sẽ khác nhau tùy thời điểm test

---

## 🚀 IMPORT VÀO POSTMAN

### Cách 1: Import Collection JSON
File `Chattrix_API_Collection.postman_collection.json` đã được tạo sẵn trong thư mục `.spec/`

**Các bước:**
1. Mở Postman
2. Click **Import** → **Upload Files**
3. Chọn file `Chattrix_API_Collection.postman_collection.json`
4. Click **Import**

### Cách 2: Tạo Environment
File `Chattrix_Environment.postman_environment.json` đã được tạo sẵn

**Các bước:**
1. Mở Postman
2. Click **Environments** → **Import**
3. Chọn file `Chattrix_Environment.postman_environment.json`
4. Cập nhật các giá trị: `BASE_URL`, `USER1_TOKEN`, `USER2_TOKEN`, `USER3_TOKEN`

---

## 📝 CHECKLIST TRƯỚC KHI TEST

- [ ] Database đã chạy migration V2
- [ ] Application đang chạy (mvn quarkus:dev)
- [ ] Đã tạo ít nhất 3 users trong database
- [ ] Đã lấy JWT tokens cho 3 users
- [ ] Đã import Postman Collection
- [ ] Đã import và cấu hình Environment
- [ ] Đã tạo ít nhất 1 conversation với messages

---

## 🎯 QUICK START

1. **Tạo users và lấy tokens**
2. **Cập nhật Environment variables trong Postman**
3. **Chạy folder "1. Friend Requests" để tạo quan hệ bạn bè**
4. **Tạo conversation và gửi messages**
5. **Test các features theo thứ tự trong Collection**

