# Scheduled Messages API Documentation

## Overview

API để lên lịch gửi tin nhắn tự động trong tương lai. Tin nhắn được lưu với trạng thái PENDING và sẽ được gửi tự động khi đến thời gian đã định.

**Base URL:** `http://localhost:8080/api`

**Authentication:** Tất cả endpoints yêu cầu JWT token trong header `Authorization: Bearer <token>`

---

## Endpoints

### 1. Tạo Scheduled Message

Lên lịch một tin nhắn để gửi trong tương lai.

**Endpoint:** `POST /v1/conversations/{conversationId}/messages/schedule`

**Path Parameters:**
- `conversationId` (Long, required) - ID của conversation

**Request Body:**
```json
{
  "content": "Happy New Year! 🎉",
  "type": "TEXT",
  "scheduledTime": "2025-12-31T23:59:00Z",
  "mediaUrl": null,
  "thumbnailUrl": null,
  "fileName": null,
  "fileSize": null,
  "duration": null,
  "replyToMessageId": null
}
```

**Request Fields:**
- `content` (String, required) - Nội dung tin nhắn
- `type` (String, optional) - Loại tin nhắn: TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT (default: TEXT)
- `scheduledTime` (Instant, required) - Thời gian gửi (ISO 8601 format, UTC)
- `mediaUrl` (String, optional) - URL của media file
- `thumbnailUrl` (String, optional) - URL của thumbnail (cho video/image)
- `fileName` (String, optional) - Tên file (cho document/media)
- `fileSize` (Long, optional) - Kích thước file (bytes)
- `duration` (Integer, optional) - Độ dài (giây, cho audio/video)
- `replyToMessageId` (Long, optional) - ID của tin nhắn được reply

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Scheduled message created successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 5,
    "senderUsername": "user1",
    "senderFullName": "John Doe",
    "content": "Happy New Year! 🎉",
    "type": "TEXT",
    "mediaUrl": null,
    "thumbnailUrl": null,
    "fileName": null,
    "fileSize": null,
    "duration": null,
    "latitude": null,
    "longitude": null,
    "locationName": null,
    "replyToMessageId": null,
    "replyToMessage": null,
    "reactions": {},
    "mentions": [],
    "mentionedUsers": [],
    "sentAt": null,
    "createdAt": "2025-12-22T10:00:00Z",
    "updatedAt": "2025-12-22T10:00:00Z",
    "edited": false,
    "editedAt": null,
    "deleted": false,
    "deletedAt": null,
    "forwarded": false,
    "originalMessageId": null,
    "forwardCount": 0,
    "readCount": 0,
    "readBy": []
  }
}
```

**Note:** Message entity có thêm các fields ẩn (không trả về trong response):
- `scheduled` (Boolean) - true nếu là scheduled message
- `scheduledTime` (Instant) - Thời gian đã lên lịch
- `scheduledStatus` (Enum) - PENDING, SENT, FAILED, CANCELLED
- `failedReason` (String) - Lý do thất bại (nếu có)

---

### 2. Lấy Danh Sách Scheduled Messages

Lấy danh sách các tin nhắn đã lên lịch của user.

**Endpoint:** `GET /v1/messages/scheduled`

**Query Parameters:**
- `conversationId` (Long, optional) - Lọc theo conversation
- `status` (String, optional) - Lọc theo trạng thái: PENDING, SENT, FAILED, CANCELLED (default: PENDING)
- `page` (Integer, optional) - Số trang (default: 0)
- `size` (Integer, optional) - Số items mỗi trang (default: 20)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Scheduled messages retrieved successfully",
  "data": {
    "messages": [
      {
        "id": 123,
        "conversationId": 1,
        "senderId": 5,
        "content": "Happy New Year! 🎉",
        "type": "TEXT",
        "scheduledTime": "2025-12-31T23:59:00Z",
        "scheduledStatus": "PENDING",
        "createdAt": "2025-12-22T10:00:00Z"
      },
      {
        "id": 124,
        "conversationId": 2,
        "senderId": 5,
        "content": "Meeting reminder",
        "type": "TEXT",
        "scheduledTime": "2025-12-23T09:00:00Z",
        "scheduledStatus": "PENDING",
        "createdAt": "2025-12-22T11:00:00Z"
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

---

### 3. Lấy Chi Tiết Scheduled Message

Lấy thông tin chi tiết của một scheduled message.

**Endpoint:** `GET /v1/messages/scheduled/{scheduledMessageId}`

**Path Parameters:**
- `scheduledMessageId` (Long, required) - ID của scheduled message

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Scheduled message retrieved successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 5,
    "senderUsername": "user1",
    "senderFullName": "John Doe",
    "content": "Happy New Year! 🎉",
    "type": "TEXT",
    "mediaUrl": null,
    "scheduledTime": "2025-12-31T23:59:00Z",
    "scheduledStatus": "PENDING",
    "createdAt": "2025-12-22T10:00:00Z",
    "updatedAt": "2025-12-22T10:00:00Z"
  }
}
```

**Error Response:** `404 Not Found`
```json
{
  "success": false,
  "message": "Scheduled message not found or you don't have permission to access it"
}
```

---

### 4. Cập Nhật Scheduled Message

Cập nhật nội dung hoặc thời gian của scheduled message (chỉ với status PENDING).

**Endpoint:** `PUT /v1/messages/scheduled/{scheduledMessageId}`

**Path Parameters:**
- `scheduledMessageId` (Long, required) - ID của scheduled message

**Request Body:**
```json
{
  "content": "Updated message content",
  "scheduledTime": "2025-12-31T23:00:00Z",
  "mediaUrl": null,
  "thumbnailUrl": null,
  "fileName": null
}
```

**Request Fields:** (Tất cả optional, chỉ gửi fields cần update)
- `content` (String) - Nội dung mới
- `scheduledTime` (Instant) - Thời gian gửi mới
- `mediaUrl` (String) - URL media mới
- `thumbnailUrl` (String) - URL thumbnail mới
- `fileName` (String) - Tên file mới

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Scheduled message updated successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 5,
    "content": "Updated message content",
    "scheduledTime": "2025-12-31T23:00:00Z",
    "scheduledStatus": "PENDING",
    "updatedAt": "2025-12-22T12:00:00Z"
  }
}
```

**Error Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "Cannot update scheduled message with status: SENT"
}
```

---

### 5. Hủy Scheduled Message

Hủy một scheduled message (chỉ với status PENDING).

**Endpoint:** `DELETE /v1/messages/scheduled/{scheduledMessageId}`

**Path Parameters:**
- `scheduledMessageId` (Long, required) - ID của scheduled message

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Scheduled message cancelled successfully",
  "data": null
}
```

**Error Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "Cannot cancel scheduled message with status: SENT"
}
```

---

### 6. Hủy Nhiều Scheduled Messages

Hủy nhiều scheduled messages cùng lúc.

**Endpoint:** `DELETE /v1/messages/scheduled/bulk`

**Request Body:**
```json
{
  "scheduledMessageIds": [123, 124, 125]
}
```

**Request Fields:**
- `scheduledMessageIds` (List<Long>, required) - Danh sách IDs cần hủy

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Scheduled messages cancelled successfully",
  "data": {
    "cancelledCount": 2,
    "failedIds": [125]
  }
}
```

**Response Fields:**
- `cancelledCount` (Integer) - Số lượng messages đã hủy thành công
- `failedIds` (List<Long>) - Danh sách IDs không thể hủy (không tồn tại, không có quyền, hoặc đã gửi)

---

## Message Status Flow

```
PENDING → SENT (khi gửi thành công)
PENDING → FAILED (khi gửi thất bại)
PENDING → CANCELLED (khi user hủy)
```

**Status Descriptions:**
- `PENDING` - Đang chờ gửi
- `SENT` - Đã gửi thành công
- `FAILED` - Gửi thất bại (lưu lý do trong `failedReason`)
- `CANCELLED` - Đã bị hủy bởi user

---

## WebSocket Events

Khi scheduled message được gửi thành công hoặc thất bại, server sẽ gửi WebSocket event:

**Success Event:**
```json
{
  "type": "scheduled.message.sent",
  "data": {
    "scheduledMessageId": 123,
    "messageId": 456,
    "conversationId": 1,
    "sentAt": "2025-12-31T23:59:00Z"
  }
}
```

**Failure Event:**
```json
{
  "type": "scheduled.message.failed",
  "data": {
    "scheduledMessageId": 123,
    "conversationId": 1,
    "reason": "Conversation not found or user is not a member",
    "failedAt": "2025-12-31T23:59:00Z"
  }
}
```

---

## Background Processing

- Scheduled messages được xử lý bởi `ScheduledMessageProcessorService`
- Chạy mỗi **30 giây** một lần
- Tìm tất cả messages có `scheduledTime <= now` và `status = PENDING`
- Gửi tin nhắn và cập nhật status
- Gửi WebSocket notification cho user

---

## Error Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error, invalid status) |
| 401 | Unauthorized (missing/invalid token) |
| 404 | Not Found (message not found or no permission) |
| 500 | Internal Server Error |

---

## Validation Rules

1. **scheduledTime** phải là thời gian trong tương lai
2. **content** không được rỗng
3. Chỉ có thể update/cancel messages với status PENDING
4. User chỉ có thể thao tác với scheduled messages của chính mình
5. User phải là member của conversation để tạo scheduled message

---

## Example Usage

### Tạo scheduled message đơn giản
```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Good morning team!",
    "type": "TEXT",
    "scheduledTime": "2025-12-23T08:00:00Z"
  }'
```

### Tạo scheduled message với media
```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Check out this video",
    "type": "VIDEO",
    "scheduledTime": "2025-12-23T15:00:00Z",
    "mediaUrl": "https://example.com/video.mp4",
    "thumbnailUrl": "https://example.com/thumb.jpg",
    "fileName": "presentation.mp4",
    "fileSize": 15728640,
    "duration": 180
  }'
```

### Lấy danh sách scheduled messages của một conversation
```bash
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?conversationId=1&status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Cập nhật scheduled message
```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Updated content",
    "scheduledTime": "2025-12-23T09:00:00Z"
  }'
```

### Hủy scheduled message
```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Hủy nhiều scheduled messages
```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "scheduledMessageIds": [123, 124, 125]
  }'
```

---

## Database Schema

Scheduled messages được lưu trong bảng `messages` với các fields bổ sung:

```sql
ALTER TABLE messages
ADD COLUMN scheduled BOOLEAN DEFAULT FALSE,
ADD COLUMN scheduled_time TIMESTAMP,
ADD COLUMN scheduled_status VARCHAR(20),
ADD COLUMN failed_reason TEXT;

CREATE INDEX idx_messages_scheduled ON messages(scheduled, scheduled_status, scheduled_time)
WHERE scheduled = TRUE AND scheduled_status = 'PENDING';
```

---

## Notes

1. **Timezone:** Tất cả thời gian đều sử dụng UTC (ISO 8601 format)
2. **Permissions:** User chỉ có thể xem/sửa/xóa scheduled messages của chính mình
3. **Conversation Access:** User phải là member của conversation để tạo scheduled message
4. **Processing Delay:** Messages có thể được gửi muộn tối đa 30 giây (do interval của background job)
5. **Failed Messages:** Nếu gửi thất bại, message vẫn được lưu với status FAILED và lý do trong `failedReason`
