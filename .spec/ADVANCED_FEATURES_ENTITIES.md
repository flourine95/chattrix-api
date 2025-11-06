# Advanced Features - Entity Design

## Tổng quan

Document này mô tả các entities đã được thêm/cập nhật để hỗ trợ các tính năng nâng cao:
- Friend Request System
- Message Edit/Delete/Forward
- Read Receipts
- Unread Count
- Pinned Messages
- Conversation Hide/Archive/Pin

---

## 1. Contact Entity - Friend Request System

### Cập nhật: `Contact.java`

**Thêm fields:**
```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private ContactStatus status = ContactStatus.ACCEPTED;

@Column(name = "requested_at")
private Instant requestedAt;

@Column(name = "accepted_at")
private Instant acceptedAt;

@Column(name = "rejected_at")
private Instant rejectedAt;

public enum ContactStatus {
    PENDING,    // Lời mời đang chờ
    ACCEPTED,   // Đã chấp nhận (là bạn bè)
    REJECTED,   // Đã từ chối
    BLOCKED     // Đã chặn
}
```

**Use Cases:**
- Gửi lời mời kết bạn: `status = PENDING`
- Chấp nhận lời mời: `status = ACCEPTED`, set `acceptedAt`
- Từ chối lời mời: `status = REJECTED`, set `rejectedAt`
- Chặn người dùng: `status = BLOCKED`

**Queries:**
```sql
-- Lấy danh sách bạn bè
SELECT * FROM contacts WHERE user_id = ? AND status = 'ACCEPTED'

-- Lấy lời mời đang chờ (received)
SELECT * FROM contacts WHERE contact_user_id = ? AND status = 'PENDING'

-- Lấy lời mời đã gửi (sent)
SELECT * FROM contacts WHERE user_id = ? AND status = 'PENDING'

-- Kiểm tra đã là bạn chưa
SELECT * FROM contacts WHERE user_id = ? AND contact_user_id = ? AND status = 'ACCEPTED'
```

---

## 2. Message Entity - Edit/Delete/Forward

### Cập nhật: `Message.java`

**Thêm fields:**
```java
// Message editing
@Column(name = "is_edited", nullable = false)
private boolean isEdited = false;

@Column(name = "edited_at")
private Instant editedAt;

// Message deletion (soft delete)
@Column(name = "is_deleted", nullable = false)
private boolean isDeleted = false;

@Column(name = "deleted_at")
private Instant deletedAt;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "deleted_by")
private User deletedBy;

// Message forwarding
@Column(name = "is_forwarded", nullable = false)
private boolean isForwarded = false;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "original_message_id")
private Message originalMessage;

@Column(name = "forward_count")
private Integer forwardCount = 0;
```

**Use Cases:**

**Edit Message:**
```java
// Save old content to history
MessageEditHistory history = new MessageEditHistory();
history.setMessage(message);
history.setPreviousContent(message.getContent());
history.setEditedBy(currentUser);

// Update message
message.setContent(newContent);
message.setEdited(true);
message.setEditedAt(Instant.now());
```

**Delete Message:**
```java
message.setDeleted(true);
message.setDeletedAt(Instant.now());
message.setDeletedBy(currentUser);
// Content vẫn giữ để reply references không bị lỗi
```

**Forward Message:**
```java
Message forwardedMessage = new Message();
forwardedMessage.setContent(originalMessage.getContent());
forwardedMessage.setForwarded(true);
forwardedMessage.setOriginalMessage(originalMessage);

// Increment forward count
originalMessage.setForwardCount(originalMessage.getForwardCount() + 1);
```

---

## 3. MessageEditHistory Entity

### Mới: `MessageEditHistory.java`

**Purpose:** Lưu lịch sử chỉnh sửa tin nhắn

**Fields:**
```java
@Id
private Long id;

@ManyToOne
private Message message;

@Column(columnDefinition = "TEXT")
private String previousContent;

@Column(name = "edited_at")
private Instant editedAt;

@ManyToOne
private User editedBy;
```

**Use Cases:**
```java
// Lấy lịch sử chỉnh sửa của 1 tin nhắn
SELECT * FROM message_edit_history 
WHERE message_id = ? 
ORDER BY edited_at DESC

// Hiển thị "Edited" badge nếu message.isEdited = true
// Click vào "Edited" → show edit history
```

---

## 4. MessageReadReceipt Entity

### Mới: `MessageReadReceipt.java`

**Purpose:** Tracking ai đã xem tin nhắn nào

**Fields:**
```java
@Id
private Long id;

@ManyToOne
private Message message;

@ManyToOne
private User user;

@Column(name = "read_at")
private Instant readAt;

// Unique constraint: (message_id, user_id)
```

**Use Cases:**
```java
// Đánh dấu đã đọc
MessageReadReceipt receipt = new MessageReadReceipt();
receipt.setMessage(message);
receipt.setUser(currentUser);
receipt.setReadAt(Instant.now());

// Lấy danh sách người đã xem
SELECT u.* FROM message_read_receipts mrr
JOIN users u ON mrr.user_id = u.id
WHERE mrr.message_id = ?
ORDER BY mrr.read_at ASC

// Kiểm tra user đã đọc chưa
SELECT COUNT(*) FROM message_read_receipts
WHERE message_id = ? AND user_id = ?
```

---

## 5. ConversationParticipant - Unread Count

### Cập nhật: `ConversationParticipant.java`

**Thêm fields:**
```java
@Column(name = "unread_count", nullable = false)
private int unreadCount = 0;

@Column(name = "last_read_message_id")
private Long lastReadMessageId;

@Column(name = "last_read_at")
private Instant lastReadAt;
```

**Use Cases:**
```java
// Khi user gửi message → tăng unread_count cho các participants khác
UPDATE conversation_participants 
SET unread_count = unread_count + 1
WHERE conversation_id = ? AND user_id != ?

// Khi user đọc messages → reset unread_count
UPDATE conversation_participants
SET unread_count = 0,
    last_read_message_id = ?,
    last_read_at = NOW()
WHERE conversation_id = ? AND user_id = ?

// Lấy tổng số tin chưa đọc của user
SELECT SUM(unread_count) FROM conversation_participants
WHERE user_id = ?

// Lấy danh sách conversations có tin chưa đọc
SELECT c.* FROM conversations c
JOIN conversation_participants cp ON c.id = cp.conversation_id
WHERE cp.user_id = ? AND cp.unread_count > 0
ORDER BY c.updated_at DESC
```

---

## 6. PinnedMessage Entity

### Mới: `PinnedMessage.java`

**Purpose:** Ghim tin nhắn trong conversation

**Fields:**
```java
@Id
private Long id;

@ManyToOne
private Conversation conversation;

@ManyToOne
private Message message;

@ManyToOne
private User pinnedBy;

@Column(name = "pinned_at")
private Instant pinnedAt;

@Column(name = "pin_order")
private Integer pinOrder;

// Unique constraint: (conversation_id, message_id)
```

**Use Cases:**
```java
// Ghim tin nhắn
PinnedMessage pinned = new PinnedMessage();
pinned.setConversation(conversation);
pinned.setMessage(message);
pinned.setPinnedBy(currentUser);
pinned.setPinOrder(1); // 1 = cao nhất

// Lấy danh sách tin nhắn đã ghim
SELECT m.* FROM pinned_messages pm
JOIN messages m ON pm.message_id = m.id
WHERE pm.conversation_id = ?
ORDER BY pm.pin_order ASC, pm.pinned_at DESC

// Bỏ ghim
DELETE FROM pinned_messages
WHERE conversation_id = ? AND message_id = ?

// Giới hạn số lượng ghim (VD: max 3)
SELECT COUNT(*) FROM pinned_messages WHERE conversation_id = ?
```

---

## 7. ConversationSettings - Hide/Archive/Pin

### Cập nhật: `ConversationSettings.java`

**Thêm fields:**
```java
// Conversation visibility
@Column(name = "is_hidden", nullable = false)
private boolean isHidden = false;

@Column(name = "hidden_at")
private Instant hiddenAt;

@Column(name = "is_archived", nullable = false)
private boolean isArchived = false;

@Column(name = "archived_at")
private Instant archivedAt;

// Conversation pinning
@Column(name = "is_pinned", nullable = false)
private boolean isPinned = false;

@Column(name = "pin_order")
private Integer pinOrder;

@Column(name = "pinned_at")
private Instant pinnedAt;
```

**Use Cases:**

**Hide Conversation:**
```java
settings.setHidden(true);
settings.setHiddenAt(Instant.now());

// Query: Lấy conversations không bị ẩn
SELECT c.* FROM conversations c
LEFT JOIN conversation_settings cs ON c.id = cs.conversation_id AND cs.user_id = ?
WHERE (cs.is_hidden IS NULL OR cs.is_hidden = FALSE)
```

**Archive Conversation:**
```java
settings.setArchived(true);
settings.setArchivedAt(Instant.now());

// Query: Lấy conversations đã archive
SELECT c.* FROM conversations c
JOIN conversation_settings cs ON c.id = cs.conversation_id
WHERE cs.user_id = ? AND cs.is_archived = TRUE
ORDER BY cs.archived_at DESC
```

**Pin Conversation:**
```java
settings.setPinned(true);
settings.setPinOrder(1); // 1 = cao nhất
settings.setPinnedAt(Instant.now());

// Query: Lấy conversations theo thứ tự (pinned trước)
SELECT c.* FROM conversations c
LEFT JOIN conversation_settings cs ON c.id = cs.conversation_id AND cs.user_id = ?
ORDER BY 
    COALESCE(cs.is_pinned, FALSE) DESC,
    COALESCE(cs.pin_order, 999999) ASC,
    c.updated_at DESC
```

---

## Database Migration

Chạy migration:
```bash
psql -U your_username -d your_database -f src/main/resources/db/migration/V2__add_advanced_features.sql
```

Hoặc nếu dùng Flyway/Liquibase, migration sẽ tự động chạy khi start application.

---

## Tổng kết

### Entities đã cập nhật:
1. ✅ `Contact` - Thêm friend request status
2. ✅ `Message` - Thêm edit/delete/forward
3. ✅ `ConversationParticipant` - Thêm unread count
4. ✅ `ConversationSettings` - Thêm hide/archive/pin

### Entities mới:
1. ✅ `MessageEditHistory` - Lịch sử chỉnh sửa
2. ✅ `MessageReadReceipt` - Tracking đã xem
3. ✅ `PinnedMessage` - Ghim tin nhắn

### Migration SQL:
1. ✅ `V2__add_advanced_features.sql` - Tất cả thay đổi database

---

## Next Steps

1. Tạo Repositories cho các entities mới
2. Tạo Services để xử lý business logic
3. Tạo REST APIs
4. Cập nhật WebSocket events
5. Viết tests

