# 💬 Chattrix Message Features API

Tài liệu hướng dẫn sử dụng các tính năng nâng cao của tin nhắn: Ghim tin nhắn, Hẹn giờ gửi và Tìm kiếm.

## 🔐 Authentication & Base URL

- **Base URL:** `http://localhost:8080/api/v1`
- **Header:** `Authorization: Bearer <your_jwt_token>`
- **Content-Type:** `application/json`

---

## 📌 1. GHIM TIN NHẮN (PINNED MESSAGES)

### 1.1 Ghim một tin nhắn
- **Endpoint:** `POST /conversations/{conversationId}/messages/{messageId}/pin`

**Example Response:**
```json
{
    "success": true,
    "message": "Message pinned successfully",
    "data": {
        "id": 1,
        "conversationId": 1,
        "senderId": 1,
        "senderUsername": "user1",
        "senderFullName": "Nguyen Linh La",
        "content": "hii",
        "type": "TEXT",
        "reactions": {},
        "sentAt": "2025-12-23T14:54:39.731556Z",
        "createdAt": "2025-12-23T14:54:39.731557Z",
        "updatedAt": "2025-12-23T14:54:39.731558Z",
        "edited": false,
        "deleted": false,
        "forwarded": false,
        "forwardCount": 0,
        "pinned": true,
        "pinnedAt": "2025-12-24T13:33:23.812822724Z",
        "pinnedBy": 1,
        "pinnedByUsername": "user1",
        "pinnedByFullName": "Nguyen Linh La",
        "scheduled": false
    }
}
```

---

### 1.2 Bỏ ghim tin nhắn
- **Endpoint:** `DELETE /conversations/{conversationId}/messages/{messageId}/pin`

**Example Response:**
```json
{
    "success": true,
    "message": "Message unpinned successfully"
}
```

---

### 1.3 Lấy danh sách tin nhắn đã ghim
- **Endpoint:** `GET /conversations/{conversationId}/messages/pinned`

**Example Response:**
```json
{
    "success": true,
    "message": "Pinned messages retrieved",
    "data": [
        {
            "id": 7,
            "conversationId": 1,
            "senderId": 1,
            "senderUsername": "user1",
            "senderFullName": "Nguyen Linh La",
            "content": "asd",
            "type": "TEXT",
            "reactions": {},
            "sentAt": "2025-12-24T04:48:58.768612Z",
            "createdAt": "2025-12-24T04:48:58.768613Z",
            "updatedAt": "2025-12-24T13:33:49.156415Z",
            "edited": false,
            "deleted": false,
            "forwarded": false,
            "forwardCount": 0,
            "pinned": true,
            "pinnedAt": "2025-12-24T13:33:49.155266Z",
            "pinnedBy": 1,
            "pinnedByUsername": "user1",
            "pinnedByFullName": "Nguyen Linh La",
            "scheduled": false
        },
        {
            "id": 1,
            "conversationId": 1,
            "senderId": 1,
            "senderUsername": "user1",
            "senderFullName": "Nguyen Linh La",
            "content": "hii",
            "type": "TEXT",
            "reactions": {},
            "sentAt": "2025-12-23T14:54:39.731556Z",
            "createdAt": "2025-12-23T14:54:39.731557Z",
            "updatedAt": "2025-12-24T13:33:23.817946Z",
            "edited": false,
            "deleted": false,
            "forwarded": false,
            "forwardCount": 0,
            "pinned": true,
            "pinnedAt": "2025-12-24T13:33:23.812823Z",
            "pinnedBy": 1,
            "pinnedByUsername": "user1",
            "pinnedByFullName": "Nguyen Linh La",
            "scheduled": false
        }
    ]
}
```

---

## ⏰ 2. HẸN GIỜ GỬI TIN NHẮN (SCHEDULED MESSAGES)

### 2.1 Tạo tin nhắn hẹn giờ
- **Endpoint:** `POST /conversations/{conversationId}/messages/schedule`

**Example Request:**
```json
{
    "content": "jq2222k",
    "type": "TEXT",
    "scheduledTime": "2025-12-25T08:00:00Z"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Scheduled message created successfully",
    "data": {
        "id": 58,
        "conversationId": 6,
        "senderId": 1,
        "senderUsername": "user1",
        "senderFullName": "Nguyen Linh La",
        "content": "jq2222k",
        "type": "TEXT",
        "reactions": {},
        "createdAt": "2025-12-24T13:57:16.980132487Z",
        "updatedAt": "2025-12-24T13:57:16.980132648Z",
        "edited": false,
        "deleted": false,
        "forwarded": false,
        "forwardCount": 0,
        "pinned": false,
        "scheduled": true,
        "scheduledTime": "2025-12-25T08:00:00Z",
        "scheduledStatus": "PENDING"
    }
}
```

---

### 2.2 Lấy danh sách tin nhắn hẹn giờ
- **Endpoint:** `GET /conversations/{conversationId}/messages/scheduled`

**Example Response:**
```json
{
    "success": true,
    "message": "Scheduled messages retrieved successfully",
    "data": {
        "items": [
            {
                "id": 58,
                "conversationId": 6,
                "senderId": 1,
                "senderUsername": "user1",
                "senderFullName": "Nguyen Linh La",
                "content": "jq2222k",
                "type": "TEXT",
                "reactions": {},
                "createdAt": "2025-12-24T13:57:16.980132Z",
                "updatedAt": "2025-12-24T13:57:16.980133Z",
                "edited": false,
                "deleted": false,
                "forwarded": false,
                "forwardCount": 0,
                "pinned": false,
                "scheduled": true,
                "scheduledTime": "2025-12-25T08:00:00Z",
                "scheduledStatus": "PENDING"
            }
        ],
        "meta": {
            "nextCursor": null,
            "hasNextPage": false,
            "itemsPerPage": 20
        }
    }
}
```

---

### 2.3 Lấy chi tiết tin nhắn hẹn giờ
- **Endpoint:** `GET /conversations/{conversationId}/messages/scheduled/{scheduledMessageId}`

**Example Response:**
```json
{
    "success": true,
    "message": "Scheduled message retrieved successfully",
    "data": {
        "id": 58,
        "conversationId": 6,
        "senderId": 1,
        "senderUsername": "user1",
        "senderFullName": "Nguyen Linh La",
        "content": "jq2222k",
        "type": "TEXT",
        "reactions": {},
        "createdAt": "2025-12-24T13:57:16.980132Z",
        "updatedAt": "2025-12-24T13:57:16.980133Z",
        "edited": false,
        "deleted": false,
        "forwarded": false,
        "forwardCount": 0,
        "pinned": false,
        "scheduled": true,
        "scheduledTime": "2025-12-25T08:00:00Z",
        "scheduledStatus": "PENDING"
    }
}
```

---

### 2.4 Cập nhật tin nhắn hẹn giờ
- **Endpoint:** `PUT /conversations/{conversationId}/messages/scheduled/{scheduledMessageId}`

**Example Request:**
```json
{
    "content": "10qwe",
    "scheduledTime": "2026-12-24T13:57:16.980133Z"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Scheduled message updated successfully",
    "data": {
        "id": 58,
        "conversationId": 6,
        "senderId": 1,
        "senderUsername": "user1",
        "senderFullName": "Nguyen Linh La",
        "content": "10qwe",
        "type": "TEXT",
        "reactions": {},
        "createdAt": "2025-12-24T13:57:16.980132Z",
        "updatedAt": "2025-12-24T13:57:16.980133Z",
        "edited": false,
        "deleted": false,
        "forwarded": false,
        "forwardCount": 0,
        "pinned": false,
        "scheduled": true,
        "scheduledTime": "2026-12-24T13:57:16.980133Z",
        "scheduledStatus": "PENDING"
    }
}
```

---

### 2.5 Hủy tin nhắn hẹn giờ
- **Endpoint:** `DELETE /conversations/{conversationId}/messages/scheduled/{scheduledMessageId}`

**Example Response:**
```json
{
    "success": true,
    "message": "Scheduled message cancelled successfully"
}
```

---

### 2.6 Hủy tin nhắn hẹn giờ hàng loạt
- **Endpoint:** `DELETE /conversations/{conversationId}/messages/scheduled/bulk`

**Example Request:**
```json
{
    "scheduledMessageIds": [58]
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Scheduled messages cancelled successfully",
    "data": {
        "cancelledCount": 1,
        "failedIds": []
    }
}
```

---

## 🔍 3. TÌM KIẾM (SEARCH)

### 3.1 Tìm kiếm tin nhắn trong hội thoại
- **Endpoint:** `GET /conversations/{conversationId}/search/messages`

**Example Request:**
```bash
GET /v1/conversations/1/search/messages?query=a&limit=20
```

**Example Response:**
```json
{
    "success": true,
    "message": "Messages searched successfully",
    "data": {
        "items": [
            {
                "id": 7,
                "conversationId": 1,
                "senderId": 1,
                "senderUsername": "user1",
                "senderFullName": "Nguyen Linh La",
                "content": "asd",
                "type": "TEXT",
                "reactions": {},
                "sentAt": "2025-12-24T04:48:58.768612Z",
                "createdAt": "2025-12-24T04:48:58.768613Z",
                "updatedAt": "2025-12-24T13:33:49.156415Z",
                "edited": false,
                "deleted": false,
                "forwarded": false,
                "forwardCount": 0,
                "pinned": true,
                "pinnedAt": "2025-12-24T13:33:49.155266Z",
                "pinnedBy": 1,
                "pinnedByUsername": "user1",
                "pinnedByFullName": "Nguyen Linh La",
                "scheduled": false,
                "readCount": 0
            }
        ],
        "meta": {
            "nextCursor": null,
            "hasNextPage": false,
            "itemsPerPage": 20
        }
    }
}
```

---

### 3.2 Tìm kiếm Media trong hội thoại
- **Endpoint:** `GET /conversations/{conversationId}/search/media`

**Example Request:**
```bash
GET /v1/conversations/1/search/media?limit=20&type=IMAGE&cursor=0
```

**Example Response:**
```json
{
    "success": true,
    "message": "Media files retrieved successfully",
    "data": {
        "items": [],
        "meta": {
            "nextCursor": null,
            "hasNextPage": false,
            "itemsPerPage": 20
        }
    }
}
```

---

## 📝 Ghi chú cho Client

1. **Scheduled Status:** Tin nhắn hẹn giờ có các trạng thái: `PENDING`, `SENT`, `FAILED`, `CANCELLED`.
2. **Pinned UI:** Khi một tin nhắn được ghim, response trả về đầy đủ thông tin người ghim (`pinnedBy`) và thời gian ghim (`pinnedAt`).
3. **Search:** Hỗ trợ lọc theo `query`, `type` (IMAGE, VIDEO, FILE, ...) và `senderId`.

---
**Last Updated:** 2024-12-24
