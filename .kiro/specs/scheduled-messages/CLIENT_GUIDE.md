# Hướng Dẫn Sử Dụng Scheduled Messages API - Dành Cho Client

## Tổng Quan

API Scheduled Messages cho phép lên lịch gửi tin nhắn tự động trong tương lai. Backend sẽ tự động gửi tin nhắn khi đến thời gian đã định.

**Base URL:** `http://localhost:8080/api`

**Authentication:** Tất cả requests cần JWT token trong header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 1. Tạo Scheduled Message

### Endpoint
```
POST /api/v1/conversations/{conversationId}/messages/schedule
```

### Request Body
```json
{
  "content": "Nội dung tin nhắn",
  "type": "TEXT",
  "scheduledTime": "2025-12-25T10:00:00Z"
}
```

### Fields
- `content` (String, **required**) - Nội dung tin nhắn
- `type` (String, optional) - Loại: TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT (default: TEXT)
- `scheduledTime` (String, **required**) - Thời gian gửi (ISO 8601 format, UTC)
- `mediaUrl` (String, optional) - URL của media file
- `thumbnailUrl` (String, optional) - URL thumbnail
- `fileName` (String, optional) - Tên file
- `fileSize` (Long, optional) - Kích thước file (bytes)
- `duration` (Integer, optional) - Độ dài (giây, cho audio/video)
- `replyToMessageId` (Long, optional) - ID tin nhắn được reply

### Response Success (201 Created)
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
    "content": "Nội dung tin nhắn",
    "type": "TEXT",
    "scheduled": true,
    "scheduledTime": "2025-12-25T10:00:00Z",
    "scheduledStatus": "PENDING",
    "failedReason": null,
    "createdAt": "2025-12-22T10:00:00Z",
    "updatedAt": "2025-12-22T10:00:00Z"
  }
}
```

### Example cURL
```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Chúc mừng năm mới!",
    "type": "TEXT",
    "scheduledTime": "2025-12-31T23:59:00Z"
  }'
```

### ⚠️ Lưu Ý Quan Trọng
1. **scheduledTime phải là thời gian trong tương lai**
2. **Sử dụng UTC timezone** (thêm 'Z' ở cuối)
3. **Format:** `YYYY-MM-DDTHH:mm:ssZ`
4. Backend sẽ tự động gửi tin nhắn khi đến thời gian (có thể delay tối đa 30 giây)

---

## 2. Lấy Danh Sách Scheduled Messages

### Endpoint
```
GET /api/v1/messages/scheduled
```

### Query Parameters
- `conversationId` (Long, optional) - Lọc theo conversation
- `status` (String, optional) - Lọc theo trạng thái (default: PENDING)
  - `PENDING` - Đang chờ gửi
  - `SENT` - Đã gửi thành công
  - `FAILED` - Gửi thất bại
  - `CANCELLED` - Đã bị hủy
- `page` (Integer, optional) - Số trang (default: 0)
- `size` (Integer, optional) - Số items/trang (default: 20)

### Response Success (200 OK)
```json
{
  "success": true,
  "message": "Scheduled messages retrieved successfully",
  "data": {
    "data": [
      {
        "id": 123,
        "conversationId": 1,
        "senderId": 5,
        "senderUsername": "user1",
        "senderFullName": "John Doe",
        "content": "Chúc mừng năm mới!",
        "type": "TEXT",
        "scheduled": true,
        "scheduledTime": "2025-12-31T23:59:00Z",
        "scheduledStatus": "PENDING",
        "failedReason": null,
        "createdAt": "2025-12-22T10:00:00Z",
        "updatedAt": "2025-12-22T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "hasNextPage": false,
    "hasPrevPage": false
  }
}
```

### Example cURL
```bash
# Lấy tất cả scheduled messages đang PENDING
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=PENDING" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Lấy scheduled messages của conversation 1
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?conversationId=1&status=PENDING" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Lấy tất cả messages đã gửi (SENT)
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=SENT" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Pagination
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 3. Lấy Chi Tiết 1 Scheduled Message

### Endpoint
```
GET /api/v1/messages/scheduled/{scheduledMessageId}
```

### Response Success (200 OK)
```json
{
  "success": true,
  "message": "Scheduled message retrieved successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "content": "Chúc mừng năm mới!",
    "scheduled": true,
    "scheduledTime": "2025-12-31T23:59:00Z",
    "scheduledStatus": "PENDING"
  }
}
```

### Response Error (404 Not Found)
```json
{
  "success": false,
  "message": "Scheduled message not found or you don't have permission to access it"
}
```

### Example cURL
```bash
curl -X GET http://localhost:8080/api/v1/messages/scheduled/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 4. Cập Nhật Scheduled Message

**Chỉ có thể update messages với status PENDING**

### Endpoint
```
PUT /api/v1/messages/scheduled/{scheduledMessageId}
```

### Request Body
```json
{
  "content": "Nội dung mới",
  "scheduledTime": "2025-12-31T22:00:00Z"
}
```

### Fields (Tất cả optional - chỉ gửi fields cần update)
- `content` (String) - Nội dung mới
- `scheduledTime` (String) - Thời gian gửi mới
- `mediaUrl` (String) - URL media mới
- `thumbnailUrl` (String) - URL thumbnail mới
- `fileName` (String) - Tên file mới

### Response Success (200 OK)
```json
{
  "success": true,
  "message": "Scheduled message updated successfully",
  "data": {
    "id": 123,
    "content": "Nội dung mới",
    "scheduledTime": "2025-12-31T22:00:00Z",
    "scheduledStatus": "PENDING",
    "updatedAt": "2025-12-22T12:00:00Z"
  }
}
```

### Response Error (400 Bad Request)
```json
{
  "success": false,
  "message": "Cannot update scheduled message with status: SENT"
}
```

### Example cURL
```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Nội dung đã sửa",
    "scheduledTime": "2025-12-31T22:00:00Z"
  }'
```

---

## 5. Hủy Scheduled Message

**Chỉ có thể hủy messages với status PENDING**

### Endpoint
```
DELETE /api/v1/messages/scheduled/{scheduledMessageId}
```

### Response Success (200 OK)
```json
{
  "success": true,
  "message": "Scheduled message cancelled successfully",
  "data": null
}
```

### Response Error (400 Bad Request)
```json
{
  "success": false,
  "message": "Cannot cancel scheduled message with status: SENT"
}
```

### Example cURL
```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 6. Hủy Nhiều Scheduled Messages

**Hủy nhiều messages cùng lúc**

### Endpoint
```
DELETE /api/v1/messages/scheduled/bulk
```

### Request Body
```json
{
  "scheduledMessageIds": [123, 124, 125]
}
```

### Response Success (200 OK)
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
- `cancelledCount` - Số lượng đã hủy thành công
- `failedIds` - Danh sách IDs không thể hủy (không tồn tại, không có quyền, hoặc đã gửi)

### Example cURL
```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "scheduledMessageIds": [123, 124, 125]
  }'
```

---

## Status Flow

```
PENDING → SENT (gửi thành công)
PENDING → FAILED (gửi thất bại)
PENDING → CANCELLED (user hủy)
```

**Giải thích:**
- `PENDING` - Đang chờ gửi, có thể update/cancel
- `SENT` - Đã gửi thành công, không thể update/cancel
- `FAILED` - Gửi thất bại, xem `failedReason` để biết lý do
- `CANCELLED` - Đã bị hủy bởi user

---

## WebSocket Events

Khi scheduled message được gửi, server sẽ gửi WebSocket event đến client:

### Success Event
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

### Failure Event
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

**Client nên:**
1. Listen cho events này qua WebSocket
2. Update UI khi nhận được event
3. Hiển thị notification cho user

---

## Error Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error, invalid status) |
| 401 | Unauthorized (missing/invalid token) |
| 404 | Not Found (message not found or no permission) |
| 500 | Internal Server Error |

---

## Validation Rules

1. ✅ `scheduledTime` phải là thời gian trong tương lai
2. ✅ `content` không được rỗng
3. ✅ Chỉ có thể update/cancel messages với status PENDING
4. ✅ User chỉ có thể thao tác với scheduled messages của chính mình
5. ✅ User phải là member của conversation để tạo scheduled message

---

## Best Practices

### 1. Xử Lý Timezone
```javascript
// ❌ SAI - Không dùng local time
const scheduledTime = "2025-12-31T23:59:00"; // Thiếu timezone

// ✅ ĐÚNG - Luôn dùng UTC
const scheduledTime = new Date("2025-12-31T23:59:00+07:00").toISOString();
// Result: "2025-12-31T16:59:00.000Z"
```

### 2. Validate Thời Gian
```javascript
function validateScheduledTime(scheduledTime) {
  const now = new Date();
  const scheduled = new Date(scheduledTime);
  
  if (scheduled <= now) {
    throw new Error("Scheduled time must be in the future");
  }
  
  return scheduled.toISOString();
}
```

### 3. Hiển Thị Status
```javascript
const statusLabels = {
  PENDING: "Đang chờ gửi",
  SENT: "Đã gửi",
  FAILED: "Gửi thất bại",
  CANCELLED: "Đã hủy"
};

const statusColors = {
  PENDING: "orange",
  SENT: "green",
  FAILED: "red",
  CANCELLED: "gray"
};
```

### 4. Polling vs WebSocket
```javascript
// ✅ RECOMMENDED - Dùng WebSocket để nhận real-time updates
websocket.on('scheduled.message.sent', (data) => {
  updateMessageStatus(data.scheduledMessageId, 'SENT');
  showNotification('Tin nhắn đã được gửi!');
});

// ❌ KHÔNG NÊN - Polling liên tục
// setInterval(() => fetchScheduledMessages(), 5000);
```

### 5. Error Handling
```javascript
async function scheduleMessage(conversationId, data) {
  try {
    const response = await fetch(
      `${API_BASE}/v1/conversations/${conversationId}/messages/schedule`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data)
      }
    );
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Failed to schedule message:', error);
    showErrorNotification(error.message);
    throw error;
  }
}
```

---

## Common Issues & Solutions

### Issue 1: "Scheduled time must be in the future"
**Nguyên nhân:** Thời gian đã lên lịch <= thời gian hiện tại

**Giải pháp:**
```javascript
// Đảm bảo scheduledTime > now
const minScheduledTime = new Date(Date.now() + 60000); // +1 phút
```

### Issue 2: "Cannot update scheduled message with status: SENT"
**Nguyên nhân:** Cố gắng update message đã được gửi

**Giải pháp:**
```javascript
// Check status trước khi cho phép edit
if (message.scheduledStatus !== 'PENDING') {
  showError('Không thể sửa tin nhắn đã gửi hoặc đã hủy');
  return;
}
```

### Issue 3: Messages không được gửi
**Nguyên nhân:** 
- Processor chưa chạy (chạy mỗi 30 giây)
- User không còn là member của conversation
- Conversation đã bị xóa

**Giải pháp:**
- Đợi tối đa 30 giây sau scheduledTime
- Listen WebSocket event để biết kết quả
- Check `failedReason` nếu status = FAILED

### Issue 4: Timezone sai
**Nguyên nhân:** Client gửi local time thay vì UTC

**Giải pháp:**
```javascript
// ✅ Convert local time sang UTC
const localTime = new Date('2025-12-31 23:59:00'); // Local
const utcTime = localTime.toISOString(); // UTC
```

---

## Example: Complete Flow

```javascript
// 1. Tạo scheduled message
async function createScheduledMessage() {
  const scheduledTime = new Date('2025-12-31T23:59:00+07:00').toISOString();
  
  const response = await fetch(
    'http://localhost:8080/api/v1/conversations/1/messages/schedule',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        content: 'Chúc mừng năm mới!',
        type: 'TEXT',
        scheduledTime: scheduledTime
      })
    }
  );
  
  const result = await response.json();
  console.log('Created:', result.data);
  return result.data.id;
}

// 2. Lấy danh sách scheduled messages
async function getScheduledMessages() {
  const response = await fetch(
    'http://localhost:8080/api/v1/messages/scheduled?status=PENDING',
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  const result = await response.json();
  console.log('Scheduled messages:', result.data.data);
  return result.data.data;
}

// 3. Update scheduled message
async function updateScheduledMessage(id) {
  const response = await fetch(
    `http://localhost:8080/api/v1/messages/scheduled/${id}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        content: 'Nội dung đã sửa',
        scheduledTime: new Date('2025-12-31T22:00:00+07:00').toISOString()
      })
    }
  );
  
  const result = await response.json();
  console.log('Updated:', result.data);
}

// 4. Cancel scheduled message
async function cancelScheduledMessage(id) {
  const response = await fetch(
    `http://localhost:8080/api/v1/messages/scheduled/${id}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  const result = await response.json();
  console.log('Cancelled:', result.message);
}

// 5. Listen WebSocket events
websocket.on('scheduled.message.sent', (data) => {
  console.log('Message sent:', data);
  // Update UI
  updateMessageInList(data.scheduledMessageId, { status: 'SENT' });
  showNotification('Tin nhắn đã được gửi!');
});

websocket.on('scheduled.message.failed', (data) => {
  console.error('Message failed:', data);
  // Update UI
  updateMessageInList(data.scheduledMessageId, { 
    status: 'FAILED',
    failedReason: data.reason 
  });
  showErrorNotification(`Gửi thất bại: ${data.reason}`);
});
```

---

## Testing Checklist

- [ ] Tạo scheduled message với thời gian trong tương lai
- [ ] Tạo scheduled message với thời gian quá khứ (phải lỗi)
- [ ] Lấy danh sách scheduled messages với filter status
- [ ] Lấy danh sách scheduled messages với pagination
- [ ] Update scheduled message (status PENDING)
- [ ] Update scheduled message (status SENT) - phải lỗi
- [ ] Cancel scheduled message (status PENDING)
- [ ] Cancel scheduled message (status SENT) - phải lỗi
- [ ] Bulk cancel nhiều messages
- [ ] Verify WebSocket events khi message được gửi
- [ ] Verify timezone conversion đúng

---

## Support

Nếu gặp vấn đề, check:
1. JWT token còn valid không
2. User có quyền truy cập conversation không
3. Thời gian có đúng format UTC không
4. Status của message có phải PENDING không (khi update/cancel)

Xem thêm: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
