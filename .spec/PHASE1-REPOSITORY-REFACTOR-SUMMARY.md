# Phase 1 - Repository Refactor Summary

## ✅ HOÀN THÀNH

### 1. MessageRepository ✅

**File:** `src/main/java/com/chattrix/api/repositories/MessageRepository.java`

**Thay đổi:**
- ❌ **Xóa:** `LEFT JOIN FETCH m.poll` và `LEFT JOIN FETCH m.event` từ queries (Poll và Event giờ là JSONB metadata)
- ❌ **Xóa:** `deleteByPollId()` method (không còn Poll entity)
- ❌ **Xóa:** `deleteByEventId()` method (không còn Event entity)
- ❌ **Xóa:** `findUnreadMessages()` và `findUnreadMessagesUpTo()` (dùng MessageReadReceipt entity đã xóa)
- ✅ **Thêm:** `findUnreadMessagesByLastRead()` - Query unread messages dựa trên `ConversationParticipant.lastReadMessageId`
- ✅ **Thêm:** `countUnreadMessagesByLastRead()` - Count unread messages dựa trên `ConversationParticipant.lastReadMessageId`

**Logic mới:**
```java
// Unread messages = messages sau lastReadMessageId
// Nếu lastReadMessageId = null → tất cả messages từ người khác là unread
// Nếu lastReadMessageId = 100 → messages với id > 100 là unread
```

---

### 2. ConversationRepository ✅

**File:** `src/main/java/com/chattrix/api/repositories/ConversationRepository.java`

**Thay đổi:**
- ✅ **Thêm:** `findByInviteToken(String token)` - Query invite link từ JSONB metadata
- ✅ **Thêm:** `findConversationsWithActiveInviteLinks(Long userId)` - Query conversations có active invite links

**Native SQL cho JSONB:**
```sql
-- Find by invite token
SELECT c.* FROM conversations c 
WHERE c.metadata->>'inviteLink'->>'token' = :token 
AND c.metadata->>'inviteLink'->>'revoked' = 'false'

-- Find conversations with active invite links
SELECT DISTINCT c.* FROM conversations c 
INNER JOIN conversation_participants cp ON c.id = cp.conversation_id 
WHERE cp.user_id = :userId 
AND c.metadata->>'inviteLink' IS NOT NULL 
AND c.metadata->>'inviteLink'->>'revoked' = 'false'
```

---

### 3. CallRepository ✅

**File:** `src/main/java/com/chattrix/api/repositories/CallRepository.java`

**Thay đổi:**
- ❌ **Xóa imports:** `CallStatus` và `ParticipantStatus` (giờ là inner classes: `Call.CallStatus`, `Call.ParticipantStatus`)
- ✅ **Thêm import:** `TypedQuery` cho cursor pagination
- ✅ **Cập nhật:** Tất cả references từ `CallStatus` → `Call.CallStatus`
- ✅ **Cập nhật:** Tất cả references từ `ParticipantStatus` → `Call.ParticipantStatus`
- ✅ **Thêm:** `findCallHistoryByUserId()` - Query call history (ENDED, MISSED, REJECTED)
- ✅ **Thêm:** `findCallHistoryByUserIdWithCursor()` - Cursor-based pagination cho call history
- ✅ **Thêm:** `findCallHistoryByUserIdAndStatus()` - Filter call history by status
- ✅ **Thêm:** `countCallHistoryByUserId()` - Count call history

**Call History Logic:**
```java
// Call history = calls với status ENDED, MISSED, hoặc REJECTED
// Không cần CallHistory entity riêng, query trực tiếp từ Call entity
```

---

## 📊 Impact Summary

### Removed
- ❌ 2 methods từ MessageRepository (`deleteByPollId`, `deleteByEventId`)
- ❌ 2 methods từ MessageRepository (`findUnreadMessages`, `findUnreadMessagesUpTo`)
- ❌ 2 imports từ CallRepository (`CallStatus`, `ParticipantStatus`)
- ❌ Poll/Event JOIN FETCH từ queries

### Added
- ✅ 2 methods cho unread messages (dựa trên lastReadMessageId)
- ✅ 2 methods cho invite links (JSONB queries)
- ✅ 4 methods cho call history (thay thế CallHistory entity)

### Updated
- ✅ All enum references trong CallRepository (inner classes)
- ✅ All queries xóa Poll/Event references

---

## 🎯 Next Steps - Phase 2

Bây giờ repositories đã sẵn sàng, có thể refactor services:

1. **ReadReceiptService** - Sử dụng `findUnreadMessagesByLastRead()` và `countUnreadMessagesByLastRead()`
2. **CallHistoryService** - Sử dụng `findCallHistoryByUserId*()` methods
3. **GroupInviteLinkService** - Sử dụng `findByInviteToken()` và JSONB metadata
4. **PollService** - Lưu poll data vào `message.metadata` JSONB
5. **EventService** - Lưu event data vào `message.metadata` JSONB
6. **ConversationService** - Xóa ConversationSettings references

---

## 🧪 Testing Checklist

- [ ] Test `findUnreadMessagesByLastRead()` với lastReadMessageId = null
- [ ] Test `findUnreadMessagesByLastRead()` với lastReadMessageId có giá trị
- [ ] Test `countUnreadMessagesByLastRead()` accuracy
- [ ] Test `findByInviteToken()` với valid token
- [ ] Test `findByInviteToken()` với revoked token
- [ ] Test `findConversationsWithActiveInviteLinks()`
- [ ] Test `findCallHistoryByUserId()` với các status khác nhau
- [ ] Test `findCallHistoryByUserIdWithCursor()` pagination
- [ ] Test queries không còn Poll/Event JOIN FETCH
- [ ] Verify JSONB queries với GIN indexes

---

**Phase 1 hoàn tất!** 🎉

Repositories giờ đã:
- ✅ Không còn references đến removed entities
- ✅ Sử dụng inner class enums
- ✅ Query JSONB metadata cho invite links
- ✅ Query Call entity cho call history
- ✅ Query ConversationParticipant cho unread messages
