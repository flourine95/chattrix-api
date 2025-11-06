# API Endpoints - Advanced Features

## 📋 Tổng quan

Document này mô tả các REST API endpoints cho các tính năng nâng cao đã được implement.

---

## 1. Friend Request APIs

### 1.1. Gửi lời mời kết bạn
```http
POST /api/friend-requests/send
Authorization: Bearer {token}
Content-Type: application/json

{
  "receiverUserId": 123,
  "nickname": "Bạn thân" // Optional
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "userId": 123,
  "username": "john_doe",
  "fullName": "John Doe",
  "avatarUrl": "https://...",
  "status": "PENDING",
  "nickname": "Bạn thân",
  "isOnline": true,
  "requestedAt": "2024-01-15T10:30:00.000Z",
  "acceptedAt": null,
  "rejectedAt": null
}
```

### 1.2. Chấp nhận lời mời kết bạn
```http
POST /api/friend-requests/{requestId}/accept
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": 456,
  "username": "jane_smith",
  "fullName": "Jane Smith",
  "status": "ACCEPTED",
  "acceptedAt": "2024-01-15T11:00:00.000Z"
}
```

### 1.3. Từ chối lời mời kết bạn
```http
POST /api/friend-requests/{requestId}/reject
Authorization: Bearer {token}
```

**Response (204 No Content)**

### 1.4. Hủy lời mời đã gửi
```http
DELETE /api/friend-requests/{requestId}/cancel
Authorization: Bearer {token}
```

**Response (204 No Content)**

### 1.5. Lấy danh sách lời mời nhận được
```http
GET /api/friend-requests/received
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "userId": 789,
    "username": "bob_wilson",
    "fullName": "Bob Wilson",
    "status": "PENDING",
    "requestedAt": "2024-01-15T09:00:00.000Z"
  }
]
```

### 1.6. Lấy danh sách lời mời đã gửi
```http
GET /api/friend-requests/sent
Authorization: Bearer {token}
```

---

## 2. Message Edit/Delete APIs

### 2.1. Chỉnh sửa tin nhắn
```http
PUT /api/messages/{messageId}/edit
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "Nội dung đã chỉnh sửa"
}
```

**Response (200 OK):**
```json
{
  "id": 100,
  "conversationId": 5,
  "senderId": 1,
  "content": "Nội dung đã chỉnh sửa",
  "type": "TEXT",
  "sentAt": "2024-01-15T10:00:00.000Z",
  "isEdited": true,
  "editedAt": "2024-01-15T10:30:00.000Z"
}
```

### 2.2. Xóa tin nhắn (soft delete)
```http
DELETE /api/messages/{messageId}
Authorization: Bearer {token}
```

**Response (204 No Content)**

### 2.3. Xem lịch sử chỉnh sửa
```http
GET /api/messages/{messageId}/edit-history
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "previousContent": "Nội dung cũ",
    "editedBy": 1,
    "editedByUsername": "john_doe",
    "editedAt": "2024-01-15T10:30:00.000Z"
  },
  {
    "id": 2,
    "previousContent": "Nội dung ban đầu",
    "editedBy": 1,
    "editedByUsername": "john_doe",
    "editedAt": "2024-01-15T10:15:00.000Z"
  }
]
```

---

## 3. Message Forward API

### 3.1. Chuyển tiếp tin nhắn
```http
POST /api/messages/forward
Authorization: Bearer {token}
Content-Type: application/json

{
  "messageId": 100,
  "conversationIds": [5, 7, 9]
}
```

**Response (201 Created):**
```json
[
  {
    "id": 101,
    "conversationId": 5,
    "content": "Nội dung được chuyển tiếp",
    "isForwarded": true,
    "originalMessageId": 100
  },
  {
    "id": 102,
    "conversationId": 7,
    "content": "Nội dung được chuyển tiếp",
    "isForwarded": true,
    "originalMessageId": 100
  }
]
```

---

## 4. Read Receipt APIs

### 4.1. Đánh dấu tin nhắn đã đọc
```http
POST /api/messages/{messageId}/mark-read
Authorization: Bearer {token}
```

**Response (204 No Content)**

### 4.2. Đánh dấu toàn bộ conversation đã đọc
```http
POST /api/conversations/{conversationId}/mark-read?lastMessageId=100
Authorization: Bearer {token}
```

**Response (204 No Content)**

### 4.3. Xem danh sách người đã đọc
```http
GET /api/messages/{messageId}/read-receipts
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "userId": 2,
    "username": "jane_smith",
    "fullName": "Jane Smith",
    "avatarUrl": "https://...",
    "readAt": "2024-01-15T10:35:00.000Z"
  },
  {
    "userId": 3,
    "username": "bob_wilson",
    "fullName": "Bob Wilson",
    "avatarUrl": "https://...",
    "readAt": "2024-01-15T10:40:00.000Z"
  }
]
```

### 4.4. Lấy tổng số tin chưa đọc
```http
GET /api/unread-count
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "unreadCount": 15
}
```

---

## 5. Pinned Message APIs

### 5.1. Ghim tin nhắn
```http
POST /api/conversations/{conversationId}/pinned-messages/{messageId}/pin
Authorization: Bearer {token}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "messageId": 100,
  "content": "Nội dung tin nhắn được ghim",
  "senderId": 1,
  "senderUsername": "john_doe",
  "pinnedBy": 2,
  "pinnedByUsername": "jane_smith",
  "pinOrder": 1,
  "pinnedAt": "2024-01-15T11:00:00.000Z",
  "sentAt": "2024-01-15T10:00:00.000Z"
}
```

### 5.2. Bỏ ghim tin nhắn
```http
DELETE /api/conversations/{conversationId}/pinned-messages/{messageId}/unpin
Authorization: Bearer {token}
```

**Response (204 No Content)**

### 5.3. Lấy danh sách tin nhắn đã ghim
```http
GET /api/conversations/{conversationId}/pinned-messages
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "messageId": 100,
    "content": "Tin nhắn quan trọng 1",
    "pinOrder": 1,
    "pinnedAt": "2024-01-15T11:00:00.000Z"
  },
  {
    "id": 2,
    "messageId": 95,
    "content": "Tin nhắn quan trọng 2",
    "pinOrder": 2,
    "pinnedAt": "2024-01-15T10:30:00.000Z"
  }
]
```

---

## 6. Conversation Settings APIs

### 6.1. Lấy settings của conversation
```http
GET /api/conversations/{conversationId}/settings
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "conversationId": 5,
  "userId": 1,
  "isMuted": false,
  "isHidden": false,
  "isArchived": false,
  "isPinned": true,
  "pinOrder": 1,
  "pinnedAt": "2024-01-15T09:00:00.000Z"
}
```

### 6.2. Ẩn conversation
```http
POST /api/conversations/{conversationId}/settings/hide
Authorization: Bearer {token}
```

### 6.3. Bỏ ẩn conversation
```http
POST /api/conversations/{conversationId}/settings/unhide
Authorization: Bearer {token}
```

### 6.4. Archive conversation
```http
POST /api/conversations/{conversationId}/settings/archive
Authorization: Bearer {token}
```

### 6.5. Unarchive conversation
```http
POST /api/conversations/{conversationId}/settings/unarchive
Authorization: Bearer {token}
```

### 6.6. Ghim conversation
```http
POST /api/conversations/{conversationId}/settings/pin
Authorization: Bearer {token}
```

### 6.7. Bỏ ghim conversation
```http
POST /api/conversations/{conversationId}/settings/unpin
Authorization: Bearer {token}
```

### 6.8. Tắt thông báo
```http
POST /api/conversations/{conversationId}/settings/mute
Authorization: Bearer {token}
```

### 6.9. Bật thông báo
```http
POST /api/conversations/{conversationId}/settings/unmute
Authorization: Bearer {token}
```

---

## 📊 Tổng kết

### Tổng số endpoints: **28 endpoints**

| Feature | Endpoints | Methods |
|---------|-----------|---------|
| Friend Requests | 6 | POST, DELETE, GET |
| Message Edit/Delete | 3 | PUT, DELETE, GET |
| Message Forward | 1 | POST |
| Read Receipts | 4 | POST, GET |
| Pinned Messages | 3 | POST, DELETE, GET |
| Conversation Settings | 11 | GET, POST |

---

## 🔐 Authentication

Tất cả endpoints đều yêu cầu JWT token trong header:
```
Authorization: Bearer {your_jwt_token}
```

User ID được lấy từ `SecurityContext.getUserPrincipal().getName()`

---

## ⚠️ Error Responses

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "Cannot send friend request to yourself"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Message not found"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or missing token"
}
```

### 403 Forbidden
```json
{
  "error": "Forbidden",
  "message": "You can only edit your own messages"
}
```

