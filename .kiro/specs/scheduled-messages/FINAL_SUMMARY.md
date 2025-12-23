# Scheduled Messages - Final Implementation Summary

## ✅ Hoàn Thành

Đã implement thành công tính năng **Scheduled Messages** với cách tiếp cận tối ưu:

### Approach: Thêm trường vào bảng `messages` hiện có

Thay vì tạo bảng `scheduled_messages` riêng, chúng ta thêm 4 trường vào bảng `messages`:

```sql
ALTER TABLE messages 
ADD COLUMN scheduled BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN scheduled_time TIMESTAMP NULL,
ADD COLUMN scheduled_status VARCHAR(20) NULL,
ADD COLUMN failed_reason TEXT NULL;
```

**Note**: Dùng `scheduled` thay vì `is_scheduled` - convention tốt hơn.

## 📁 Files Implementation

### Đã Cập Nhật (5 files):
1. ✅ `Message.java` - Thêm 4 trường + enum `ScheduledStatus`
2. ✅ `MessageRepository.java` - Thêm 10 methods cho scheduled messages
3. ✅ `ScheduledMessageService.java` - Business logic (dùng Message entity)
4. ✅ `ScheduledMessageResource.java` - 6 REST endpoints
5. ✅ `ScheduledMessageProcessorService.java` - Background scheduler (30s interval)

### Đã Tạo (3 files):
1. ✅ `ScheduleMessageRequest.java` - Request DTO
2. ✅ `UpdateScheduledMessageRequest.java` - Update DTO
3. ✅ `BulkCancelScheduledMessagesRequest.java` - Bulk cancel DTO

### Đã Xóa (4 files không cần):
- ❌ `ScheduledMessage.java` entity
- ❌ `ScheduledMessageRepository.java`
- ❌ `ScheduledMessageMapper.java`
- ❌ `ScheduledMessageResponse.java`

## 🎯 Lợi Ích

1. **Đơn giản hơn** - Ít code, ít files, dễ maintain
2. **Không duplicate** - Tái sử dụng tất cả fields của Message
3. **Hiệu quả** - Không cần copy data khi gửi message
4. **Best practice** - Giống Telegram, WhatsApp, Discord

## 🔧 Database Schema

### Trường Mới trong `messages` table:

| Column | Type | Description |
|--------|------|-------------|
| `scheduled` | BOOLEAN | Message có phải scheduled không (default: false) |
| `scheduled_time` | TIMESTAMP | Thời gian gửi (UTC, nullable) |
| `scheduled_status` | VARCHAR(20) | PENDING, SENT, FAILED, CANCELLED |
| `failed_reason` | TEXT | Lý do thất bại (nullable) |

### Indexes:

```sql
-- Index cho scheduler queries
CREATE INDEX idx_messages_scheduled 
ON messages(scheduled, scheduled_status, scheduled_time) 
WHERE scheduled = true;

-- Index cho user queries
CREATE INDEX idx_messages_sender_scheduled 
ON messages(sender_id, scheduled) 
WHERE scheduled = true;
```

## 📡 API Endpoints

### 1. Schedule Message
```
POST /api/v1/conversations/{conversationId}/messages/schedule
```

### 2. List Scheduled Messages
```
GET /api/v1/messages/scheduled?conversationId=&status=&page=&size=
```

### 3. Get Scheduled Message
```
GET /api/v1/messages/scheduled/{messageId}
```

### 4. Update Scheduled Message
```
PUT /api/v1/messages/scheduled/{messageId}
```

### 5. Cancel Scheduled Message
```
DELETE /api/v1/messages/scheduled/{messageId}
```

### 6. Bulk Cancel
```
DELETE /api/v1/messages/scheduled/bulk
```

## 🔄 Workflow

### Tạo Scheduled Message:
```java
Message message = new Message();
message.setContent("Happy Birthday!");
message.setScheduled(true);  // Mark as scheduled
message.setScheduledTime(futureTime);
message.setScheduledStatus(ScheduledStatus.PENDING);
messageRepository.save(message);
```

### Scheduler Gửi Message:
```java
List<Message> dueMessages = messageRepository.findScheduledMessagesDue(now);

for (Message msg : dueMessages) {
    msg.setScheduledStatus(ScheduledStatus.SENT);
    msg.setSentAt(Instant.now());
    msg.setScheduled(false);  // Không còn là scheduled
    messageRepository.save(msg);
    
    // Update conversation, send WebSocket, etc.
}
```

## 🚀 Deployment

### 1. Run Migration
```bash
docker cp scheduled-messages-migration.sql chattrix-postgres:/tmp/
docker compose exec postgres psql -U postgres -d chattrix -f /tmp/scheduled-messages-migration.sql
```

### 2. Build & Deploy
```bash
docker compose up -d --build
```

### 3. Verify
```bash
# Check logs
docker compose logs -f api | grep "Processing scheduled messages"

# Check database
docker compose exec postgres psql -U postgres -d chattrix -c "
SELECT id, content, scheduled, scheduled_time, scheduled_status 
FROM messages 
WHERE scheduled = true 
ORDER BY scheduled_time ASC;
"
```

## 🧪 Testing

### Schedule a Message
```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "content": "Test scheduled message",
    "type": "TEXT",
    "scheduledTime": "2025-12-22T10:30:00Z"
  }'
```

### List Scheduled Messages
```bash
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=PENDING" \
  -H "Authorization: Bearer $TOKEN"
```

### Check Database
```sql
-- View scheduled messages
SELECT id, content, scheduled, scheduled_time, scheduled_status 
FROM messages 
WHERE scheduled = true;

-- Count by status
SELECT scheduled_status, COUNT(*) 
FROM messages 
WHERE scheduled = true 
GROUP BY scheduled_status;
```

## 📊 Key Features

### ✅ Implemented:
- [x] Schedule messages for future delivery
- [x] Support all message types (TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT)
- [x] Support media fields (URL, thumbnail, file info)
- [x] Support reply to message
- [x] List scheduled messages with pagination
- [x] Filter by conversation and status
- [x] Update scheduled messages (before sent)
- [x] Cancel scheduled messages
- [x] Bulk cancel multiple messages
- [x] Automatic delivery via scheduler (30s interval)
- [x] WebSocket notifications (success/failure)
- [x] Auto-cancel when user leaves conversation
- [x] Validation (time in future, max 1 year ahead)
- [x] Security (user can only manage own messages)
- [x] Error handling with descriptive messages

## 🔒 Security

- ✅ All endpoints protected with `@Secured`
- ✅ User can only view/edit/cancel own scheduled messages
- ✅ User must be conversation participant to schedule
- ✅ Scheduler verifies participant status before sending
- ✅ Input validation on all requests

## 📈 Performance

### Indexes:
- Partial index on `(scheduled, scheduled_status, scheduled_time)` WHERE scheduled = true
- Partial index on `(sender_id, scheduled)` WHERE scheduled = true

### Scheduler:
- Runs every 30 seconds
- Processes only due messages (scheduled_time <= now)
- Efficient queries with indexes

## 💡 Best Practices

### ✅ DO:
```java
// Query scheduled messages using repository methods
List<Message> scheduled = messageRepository.findScheduledMessagesBySender(userId, page, size);

// Check status before update
if (message.getScheduledStatus() == ScheduledStatus.PENDING) {
    message.setContent(newContent);
}
```

### ❌ DON'T:
```java
// Don't query all then filter
List<Message> all = messageRepository.findAll();
List<Message> scheduled = all.stream().filter(Message::isScheduled).toList();

// Don't update without checking status
message.setContent(newContent);  // Might update sent message!
```

## 🎉 Kết Luận

Implementation hoàn tất với:
- **Đơn giản**: Chỉ thêm 4 trường vào bảng hiện có
- **Hiệu quả**: Không duplicate data, không cần copy
- **Maintainable**: Tất cả message logic ở một chỗ
- **Scalable**: Indexes tối ưu cho performance
- **Best practice**: Theo chuẩn của các hệ thống messaging lớn

Code đã compile thành công! ✅

## 📝 Next Steps

1. Run migration script
2. Deploy application
3. Test all endpoints
4. Monitor scheduler logs
5. Verify WebSocket events
6. Check database performance

---

**Convention Note**: Dùng `scheduled` thay vì `is_scheduled` trong database - đây là naming convention tốt hơn và nhất quán với các trường boolean khác như `edited`, `deleted`, `forwarded` trong bảng messages.
