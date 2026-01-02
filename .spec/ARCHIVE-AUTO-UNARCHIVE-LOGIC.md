# Archive & Auto-Unarchive Logic

## Overview

Khi user archive conversation, conversation sẽ tự động unarchive trong các trường hợp sau:
1. Có tin nhắn mới
2. Tạo lại DIRECT conversation đã tồn tại

## Scenarios

### Scenario 1: Archive + Nhận tin nhắn mới

**DIRECT Conversation:**
```
1. User A archive conversation với User B
2. User B gửi tin nhắn mới
3. Conversation tự động unarchive cho User A
4. User A thấy conversation với tin nhắn mới + unread count
```

**GROUP Conversation:**
```
1. User A archive group
2. User B gửi tin nhắn trong group
3. Conversation tự động unarchive cho User A
4. User A thấy conversation với tin nhắn mới + unread count
```

**Implementation:**
```java
// In MessageService.sendMessage()
conversation.getParticipants().forEach(participant -> {
    if (participant.isArchived()) {
        participant.setArchived(false);
        participant.setArchivedAt(null);
        participantRepository.save(participant);
    }
});
```

### Scenario 2: Tạo DIRECT conversation đã tồn tại

**Case 1: Conversation bình thường (không archived)**
```
1. User A và User B đã có conversation
2. User A tạo conversation mới với User B
3. Hệ thống tìm thấy conversation cũ
4. Return conversation cũ với full history
5. Không tạo duplicate
```

**Case 2: Conversation đã archived**
```
1. User A archive conversation với User B
2. User A tạo conversation mới với User B (quên đã archive)
3. Hệ thống tìm thấy conversation cũ (archived)
4. Tự động unarchive cho cả 2 users
5. Return conversation cũ với full history
6. User A thấy lại conversation với history cũ
```

**Implementation:**
```java
// In ConversationService.createConversation()
if (existingConversation.isPresent()) {
    Conversation conv = existingConversation.get();
    
    // Auto-unarchive for both users if archived
    conv.getParticipants().forEach(participant -> {
        if (participant.isArchived()) {
            participant.setArchived(false);
            participant.setArchivedAt(null);
            participantRepository.save(participant);
        }
    });
    
    // Invalidate cache
    cacheManager.invalidateConversationCaches(conv.getId(), participantIds);
    
    return enrichConversationResponse(conv, currentUserId);
}
```

### Scenario 3: GROUP - User leave rồi được add lại

**Behavior:**
```
1. User A leave group (participant bị xóa)
2. Admin add User A lại
3. Tạo participant mới
4. User A thấy group từ thời điểm được add
5. User A KHÔNG thấy history trước khi leave (business decision)
```

**Note:** Đây là behavior hiện tại. Nếu muốn giữ history, cần:
- Không xóa participant khi leave
- Chỉ set flag `left = true`
- Khi add lại, set `left = false`

## Business Rules

### 1. Auto-Unarchive khi có tin nhắn mới
- ✅ Áp dụng cho cả DIRECT và GROUP
- ✅ Unarchive cho TẤT CẢ participants đã archive
- ✅ Kể cả người gửi tin nhắn (nếu họ đã archive)
- ✅ Invalidate cache sau khi unarchive

### 2. Auto-Unarchive khi tạo DIRECT conversation
- ✅ Chỉ áp dụng cho DIRECT conversation
- ✅ Unarchive cho CẢ 2 users
- ✅ Return conversation cũ với full history
- ✅ Không tạo duplicate conversation
- ✅ Invalidate cache sau khi unarchive

### 3. GROUP - Leave và rejoin
- ❌ KHÔNG auto-unarchive
- ❌ KHÔNG giữ history cũ
- ✅ Tạo participant mới
- ✅ User chỉ thấy messages từ lúc rejoin

## UX Flow

### Flow 1: Archive → Nhận tin nhắn
```
User A                          System                      User B
   |                               |                           |
   |-- Archive conversation ------>|                           |
   |                               |                           |
   |                               |<----- Send message -------|
   |                               |                           |
   |                               |-- Auto-unarchive A ------>|
   |                               |-- Increment unread ------>|
   |                               |-- Invalidate cache ------>|
   |                               |                           |
   |<-- WebSocket: New message ----|                           |
   |<-- Conversation unarchived ----|                          |
   |                               |                           |
```

### Flow 2: Archive → Tạo lại conversation
```
User A                          System
   |                               |
   |-- Archive conversation ------>|
   |                               |
   |-- Create conversation ------->|
   |                               |
   |                               |-- Find existing conv ---->|
   |                               |-- Check archived -------->|
   |                               |-- Auto-unarchive A ------>|
   |                               |-- Auto-unarchive B ------>|
   |                               |-- Invalidate cache ------>|
   |                               |                           |
   |<-- Return existing conv ------|
   |    (with full history)        |
   |                               |
```

## API Behavior

### POST /conversations (Create)
**Request:**
```json
{
  "type": "DIRECT",
  "participantIds": [2]
}
```

**Response (existing conversation found & unarchived):**
```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": 1,
    "type": "DIRECT",
    "participants": [...],
    "lastMessage": {...},  // Full history preserved
    "unreadCount": 0
  }
}
```

### POST /conversations/{id}/messages (Send)
**Behavior:**
- Tự động unarchive cho tất cả participants đã archive
- Không có response khác biệt
- Frontend nhận WebSocket event về conversation update

**WebSocket Event:**
```json
{
  "type": "CONVERSATION_UPDATED",
  "data": {
    "conversationId": 1,
    "archived": false,  // Changed from true to false
    "lastMessage": {...}
  }
}
```

## Frontend Handling

### Listen for auto-unarchive
```javascript
// WebSocket listener
socket.on('CONVERSATION_UPDATED', (data) => {
  const { conversationId, archived } = data;
  
  if (!archived) {
    // Conversation was unarchived
    // Move from archived list to main list
    moveConversationToMainList(conversationId);
  }
});
```

### Handle create conversation response
```javascript
const createConversation = async (otherUserId) => {
  const response = await api.post('/conversations', {
    type: 'DIRECT',
    participantIds: [otherUserId]
  });
  
  // Response might be existing conversation (with history)
  // or new conversation (empty)
  const conversation = response.data;
  
  // Navigate to conversation
  navigateToConversation(conversation.id);
};
```

## Database Impact

### Archive Status
```sql
-- Check archived conversations
SELECT cp.user_id, cp.conversation_id, cp.archived, cp.archived_at
FROM conversation_participants cp
WHERE cp.archived = true;

-- Check auto-unarchive after message
-- Should be 0 if working correctly
SELECT COUNT(*)
FROM conversation_participants cp
WHERE cp.archived = true
  AND cp.conversation_id IN (
    SELECT DISTINCT conversation_id 
    FROM messages 
    WHERE created_at > cp.archived_at
  );
```

## Testing

### Test 1: Auto-unarchive on new message
```bash
# 1. Archive conversation
curl -X POST http://localhost:8080/api/v1/conversations/1/archive \
  -H "Authorization: Bearer $TOKEN_USER1"

# 2. Send message as other user
curl -X POST http://localhost:8080/api/v1/conversations/1/messages \
  -H "Authorization: Bearer $TOKEN_USER2" \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello!"}'

# 3. Check conversation list for user1 (should see conversation)
curl http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer $TOKEN_USER1"

# Expected: Conversation appears in list with unread count
```

### Test 2: Auto-unarchive on create DIRECT
```bash
# 1. Archive conversation
curl -X POST http://localhost:8080/api/v1/conversations/1/archive \
  -H "Authorization: Bearer $TOKEN_USER1"

# 2. Try to create new conversation with same user
curl -X POST http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer $TOKEN_USER1" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DIRECT",
    "participantIds": [2]
  }'

# Expected: Returns existing conversation (id=1) with full history
# Conversation is unarchived for both users
```

### Test 3: GROUP leave and rejoin
```bash
# 1. Leave group
curl -X POST http://localhost:8080/api/v1/conversations/1/leave \
  -H "Authorization: Bearer $TOKEN_USER1"

# 2. Admin adds user back
curl -X POST http://localhost:8080/api/v1/conversations/1/members \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"userIds": [1]}'

# 3. Check messages (should only see new messages)
curl http://localhost:8080/api/v1/conversations/1/messages \
  -H "Authorization: Bearer $TOKEN_USER1"

# Expected: Only messages after rejoin time
```

## Edge Cases

### Edge Case 1: Both users archived
```
User A archives conversation
User B archives conversation
User A sends message
→ Both users see conversation unarchived
```

### Edge Case 2: Archive → Unarchive → Archive → New message
```
User A archives
User A unarchives manually
User A archives again
User B sends message
→ User A sees conversation unarchived (works as expected)
```

### Edge Case 3: Multiple messages while archived
```
User A archives
User B sends 5 messages
→ Conversation unarchived after first message
→ Unread count = 5
```

### Edge Case 4: System messages
```
User A archives group
Admin changes group name (system message)
→ Conversation unarchived for User A
→ User A sees system message
```

## Future Enhancements

1. **Archive preferences**
   - Option: "Keep archived even with new messages"
   - Option: "Auto-unarchive only for mentions"

2. **Archive folder**
   - Separate view for archived conversations
   - Manual unarchive from archive folder

3. **Smart archive**
   - Auto-archive conversations with no activity for 30 days
   - Exclude pinned conversations from auto-archive

4. **Archive analytics**
   - Track archive/unarchive frequency
   - Suggest conversations to archive
