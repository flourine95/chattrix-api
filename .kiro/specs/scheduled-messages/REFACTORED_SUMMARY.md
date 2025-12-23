# Scheduled Messages - Refactored Implementation Summary

## ✅ Refactored Approach

Thay vì tạo bảng `scheduled_messages` riêng, chúng ta đã **thêm các trường scheduling vào bảng `messages` hiện có**. Cách tiếp cận này đơn giản và hiệu quả hơn nhiều.

## 🎯 Lợi Ích

### 1. **Đơn giản hơn**
- Không cần entity riêng (`ScheduledMessage`)
- Không cần repository riêng (`ScheduledMessageRepository`)
- Không cần mapper riêng (`ScheduledMessageMapper`)
- Không cần response DTO riêng (`ScheduledMessageResponse`)

### 2. **Ít duplicate code**
- Không cần copy các trường: `content`, `type`, `mediaUrl`, `thumbnailUrl`, `fileName`, `fileSize`, `duration`, `replyToMessageId`, etc.
- Tất cả logic xử lý message đã có sẵn

### 3. **Dễ query và maintain**
- Tất cả messages (đã gửi và scheduled) ở cùng một bảng
- Dễ dàng query messages theo conversation
- Dễ dàng chuyển đổi từ scheduled → sent

### 4. **Nhất quán với các hệ thống messaging khác**
- Telegram, WhatsApp, Discord đều dùng cách này
- Scheduled message chỉ là message với thêm metadata

## 📋 Thay Đổi Implementation

### 1. Message Entity - Thêm 4 trường mới

```java
// Scheduled message fields
@Column(name = "scheduled", nullable = false)
private boolean scheduled = false;

@Column(name = "scheduled_time")
private Instant scheduledTime;

@Enumerated(EnumType.STRING)
@Column(name = "scheduled_status", length = 20)
private ScheduledStatus scheduledStatus;

@Column(name = "failed_reason", columnDefinition = "TEXT")
private String failedReason;

public enum ScheduledStatus {
    PENDING,    // Chờ gửi
    SENT,       // Đã gửi thành công
    FAILED,     // Gửi thất bại
    CANCELLED   // Đã hủy
}
```

**Note**: Dùng `scheduled` thay vì `is_scheduled` theo convention tốt hơn.

### 2. MessageRepository - Thêm methods cho scheduled messages

```java
// Find scheduled messages that are due to be sent
List<Message> findScheduledMessagesDue(Instant time)

// Find scheduled messages by sender and status
List<Message> findScheduledMessagesBySenderAndStatus(Long senderId, ScheduledStatus status, int page, int size)

// Find all scheduled messages by sender
List<Message> findScheduledMessagesBySender(Long senderId, int page, int size)

// Find scheduled messages by sender, conversation and status
List<Message> findScheduledMessagesBySenderConversationAndStatus(...)

// Count methods
long countScheduledMessagesBySenderAndStatus(...)
long countScheduledMessagesBySender(...)

// Cancel all scheduled messages for a user in a conversation
int cancelScheduledMessagesByUserAndConversation(Long senderId, Long conversationId)
```

### 3. ScheduledMessageService - Sử dụng Message entity

- Tạo `Message` với `isScheduled = true`
- Khi đến giờ, chỉ cần update `isScheduled = false` và `scheduledStatus = SENT`
- Không cần copy data giữa 2 bảng

### 4. ScheduledMessageResource - Trả về MessageResponse

- Dùng `MessageResponse` thay vì `ScheduledMessageResponse`
- Tất cả thông tin message đều có sẵn

## 🗄️ Database Migration

```sql
-- Thêm 4 cột vào bảng messages
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS scheduled BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN IF NOT EXISTS scheduled_time TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS scheduled_status VARCHAR(20) NULL,
ADD COLUMN IF NOT EXISTS failed_reason TEXT NULL;

-- Tạo indexes cho performance
CREATE INDEX IF NOT EXISTS idx_messages_scheduled 
ON messages(scheduled, scheduled_status, scheduled_time) 
WHERE scheduled = true;

CREATE INDEX IF NOT EXISTS idx_messages_sender_scheduled 
ON messages(sender_id, scheduled) 
WHERE scheduled = true;
```

**Note**: Dùng `scheduled` thay vì `is_scheduled` - convention tốt hơn trong database naming.

## 📊 So Sánh

### Cách Cũ (Bảng Riêng)
```
scheduled_messages table:
- id
- conversation_id
- sender_id
- content
- message_type
- media_url
- thumbnail_url
- file_name
- file_size
- duration
- reply_to_message_id
- scheduled_time
- status
- sent_at
- sent_message_id  ← Phải lưu ID của message thật
- failed_reason
- created_at
- updated_at

→ Khi gửi: Tạo Message mới, copy tất cả data từ ScheduledMessage
→ Phức tạp, nhiều duplicate code
```

### Cách Mới (Thêm Trường)
```
messages table (existing fields + new fields):
- ... (tất cả fields hiện có)
- scheduled           ← NEW (không dùng is_scheduled)
- scheduled_time      ← NEW
- scheduled_status    ← NEW
- failed_reason       ← NEW

→ Khi gửi: Chỉ cần update scheduled = false, scheduled_status = SENT
→ Đơn giản, không duplicate
```

## 🔄 Workflow

### Tạo Scheduled Message
```java
Message message = new Message();
message.setContent("Happy Birthday!");
message.setScheduled(true);
message.setScheduledTime(futureTime);
message.setScheduledStatus(ScheduledStatus.PENDING);
// ... set other fields
messageRepository.save(message);
```

### Scheduler Gửi Message
```java
// Find messages due
List<Message> dueMessages = messageRepository.findScheduledMessagesDue(now);

for (Message msg : dueMessages) {
    // Update status
    msg.setScheduledStatus(ScheduledStatus.SENT);
    msg.setSentAt(Instant.now());
    msg.setScheduled(false);  // Không còn là scheduled message
    messageRepository.save(msg);
    
    // Update conversation lastMessage
    conversation.setLastMessage(msg);
    
    // Send WebSocket notification
    // ...
}
```

### Query Messages
```java
// Lấy tất cả messages trong conversation (bao gồm cả scheduled)
List<Message> allMessages = messageRepository.findByConversationId(conversationId);

// Lấy chỉ scheduled messages
List<Message> scheduledOnly = messageRepository.findScheduledMessagesBySender(userId, page, size);

// Lấy messages đã gửi (không bao gồm scheduled)
List<Message> sentOnly = messageRepository.findByConversationId(conversationId)
    .stream()
    .filter(m -> !m.isScheduled())
    .toList();
```

**Note**: Dùng `isScheduled()` trong Java code (Lombok tự generate getter), nhưng column name là `scheduled` trong database.

## 📁 Files Đã Xóa

- ❌ `ScheduledMessage.java` entity
- ❌ `ScheduledMessageRepository.java`
- ❌ `ScheduledMessageMapper.java`
- ❌ `ScheduledMessageResponse.java`

## 📁 Files Đã Cập Nhật

- ✅ `Message.java` - Thêm 4 trường mới + enum ScheduledStatus
- ✅ `MessageRepository.java` - Thêm methods cho scheduled messages
- ✅ `ScheduledMessageService.java` - Sử dụng Message entity
- ✅ `ScheduledMessageResource.java` - Trả về MessageResponse
- ✅ `ScheduledMessageProcessorService.java` - Không thay đổi
- ✅ `scheduled-messages-migration.sql` - ALTER TABLE thay vì CREATE TABLE

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
docker compose exec postgres psql -U postgres -d chattrix -c "\d messages"
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
    "scheduledTime": "2025-12-22T10:00:00Z"
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
WHERE scheduled = true 
ORDER BY scheduled_time ASC;

-- View all messages in a conversation (including scheduled)
SELECT id, content, scheduled, scheduled_status, sent_at 
FROM messages 
WHERE conversation_id = 1 
ORDER BY COALESCE(scheduled_time, sent_at) DESC;
```

## 💡 Best Practices

### 1. Query Scheduled Messages
```java
// ✅ GOOD: Sử dụng index
List<Message> scheduled = messageRepository.findScheduledMessagesBySender(userId, page, size);

// ❌ BAD: Query tất cả rồi filter
List<Message> all = messageRepository.findAll();
List<Message> scheduled = all.stream().filter(Message::isScheduled).toList();
```

### 2. Hiển thị Messages trong Conversation
```java
// Option 1: Hiển thị cả scheduled messages (chưa gửi)
List<Message> all = messageRepository.findByConversationId(conversationId);

// Option 2: Chỉ hiển thị messages đã gửi
List<Message> sentOnly = messageRepository.findByConversationId(conversationId)
    .stream()
    .filter(m -> !m.isScheduled() || m.getScheduledStatus() == ScheduledStatus.SENT)
    .toList();
```

### 3. Update Scheduled Message
```java
// ✅ GOOD: Check status trước khi update
if (message.getScheduledStatus() == ScheduledStatus.PENDING) {
    message.setContent(newContent);
    messageRepository.save(message);
}

// ❌ BAD: Không check status
message.setContent(newContent);  // Có thể update message đã gửi!
```

## 🔍 Monitoring

### Database Queries
```sql
-- Count scheduled messages by status
SELECT scheduled_status, COUNT(*) 
FROM messages 
WHERE scheduled = true 
GROUP BY scheduled_status;

-- Find overdue scheduled messages
SELECT id, content, scheduled_time, scheduled_status 
FROM messages 
WHERE scheduled = true 
AND scheduled_status = 'PENDING' 
AND scheduled_time < NOW() 
ORDER BY scheduled_time ASC;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan 
FROM pg_stat_user_indexes 
WHERE tablename = 'messages' 
AND indexname LIKE '%scheduled%';
```

## ✅ Advantages Summary

1. **Ít code hơn**: Xóa được 4 files không cần thiết
2. **Đơn giản hơn**: Chỉ cần thêm 4 trường vào bảng hiện có
3. **Hiệu quả hơn**: Không cần copy data giữa 2 bảng
4. **Dễ maintain hơn**: Tất cả message logic ở một chỗ
5. **Nhất quán hơn**: Giống cách các hệ thống messaging khác làm

## 🎉 Kết Luận

Refactoring này làm cho implementation **đơn giản hơn rất nhiều** mà vẫn giữ được tất cả functionality. Đây là best practice cho scheduled messages feature!
