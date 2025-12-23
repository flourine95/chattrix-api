# Message Filtering - Scheduled Messages

## Vấn Đề

API `GET /api/v1/conversations/{conversationId}/messages` trước đây trả về **TẤT CẢ** tin nhắn, bao gồm:
- ✅ Tin nhắn thường (scheduled=false)
- ✅ Tin nhắn scheduled đã gửi (scheduled=true, status=SENT)
- ❌ **Tin nhắn scheduled chưa gửi** (scheduled=true, status=PENDING) ← **Không nên hiển thị**
- ❌ **Tin nhắn scheduled đã hủy** (scheduled=true, status=CANCELLED) ← **Không nên hiển thị**
- ❌ **Tin nhắn scheduled thất bại** (scheduled=true, status=FAILED) ← **Không nên hiển thị**

### Tại Sao Đây Là Vấn Đề?

1. **User Experience** - User thấy tin nhắn chưa được gửi trong conversation (confusing)
2. **Security** - User có thể thấy scheduled messages của người khác (nếu cùng conversation)
3. **Data Consistency** - Conversation hiển thị tin nhắn không đúng thứ tự thời gian

### Example

```json
// Trước khi fix - API trả về cả PENDING messages
{
  "data": [
    {
      "id": 4,
      "content": "Test 123123",
      "scheduled": true,
      "scheduledTime": "2025-12-23T10:00:00Z",
      "scheduledStatus": "PENDING",  // ❌ Chưa được gửi nhưng vẫn hiển thị
      "sentAt": null
    },
    {
      "id": 21,
      "content": "123",
      "scheduled": true,
      "scheduledTime": "2025-12-22T10:21:00Z",
      "scheduledStatus": "SENT",  // ✅ Đã gửi, nên hiển thị
      "sentAt": "2025-12-22T10:21:00.007963Z"
    }
  ]
}
```

---

## Giải Pháp

### Quyết Định: Xử Lý Ở Backend ✅

**Lý do:**
1. **Security** - Backend control được ai thấy gì
2. **Consistency** - Tất cả clients thấy cùng data
3. **Performance** - Database filter nhanh hơn client filter
4. **Business Logic** - Logic về "tin nhắn nào được hiển thị" thuộc về backend

### Implementation

**File:** `src/main/java/com/chattrix/api/repositories/MessageRepository.java`

**Methods Updated:**
1. `findByConversationIdWithSort()` - Main method for getting messages
2. `findByConversationIdOrderBySentAtDesc()` - Alternative method
3. `findByConversationId()` - Simple method
4. `countByConversationId()` - Count method

**Filter Logic:**
```sql
WHERE m.conversation.id = :conversationId 
AND (
    m.sentAt IS NOT NULL  -- Regular messages that have been sent
    OR 
    (m.scheduled = true AND m.scheduledStatus = 'SENT')  -- Scheduled messages that have been sent
)
```

**Code Example:**
```java
public List<Message> findByConversationIdWithSort(Long conversationId, int page, int size, String sortDirection) {
    String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

    EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndReply");

    // Only return messages that have been sent:
    // 1. Regular messages (sentAt is not null)
    // 2. Scheduled messages that have been sent (scheduled=true AND scheduledStatus=SENT)
    // Exclude: PENDING, CANCELLED, FAILED scheduled messages
    TypedQuery<Message> query = em.createQuery(
            "SELECT m FROM Message m " +
                    "WHERE m.conversation.id = :conversationId " +
                    "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus)) " +
                    "ORDER BY COALESCE(m.sentAt, m.scheduledTime, m.createdAt) " + orderClause,
            Message.class
    );
    query.setHint("jakarta.persistence.fetchgraph", entityGraph);
    query.setParameter("conversationId", conversationId);
    query.setParameter("sentStatus", Message.ScheduledStatus.SENT);
    query.setFirstResult(page * size);
    query.setMaxResults(size);

    return query.getResultList();
}
```

---

## Kết Quả

### Trước Khi Fix ❌

```bash
GET /api/v1/conversations/1/messages

Response:
{
  "data": [
    {"id": 4, "scheduledStatus": "PENDING", "sentAt": null},  // ❌ Hiển thị
    {"id": 3, "scheduledStatus": "CANCELLED", "sentAt": null},  // ❌ Hiển thị
    {"id": 21, "scheduledStatus": "SENT", "sentAt": "2025-12-22T10:21:00Z"},  // ✅ Hiển thị
    {"id": 12, "scheduled": false, "sentAt": "2025-12-22T05:39:17Z"}  // ✅ Hiển thị
  ]
}
```

### Sau Khi Fix ✅

```bash
GET /api/v1/conversations/1/messages

Response:
{
  "data": [
    {"id": 21, "scheduledStatus": "SENT", "sentAt": "2025-12-22T10:21:00Z"},  // ✅ Hiển thị
    {"id": 12, "scheduled": false, "sentAt": "2025-12-22T05:39:17Z"}  // ✅ Hiển thị
  ]
}
```

**Tin nhắn PENDING, CANCELLED, FAILED không còn xuất hiện trong conversation!**

---

## Message Status Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Message Visibility                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Regular Message (scheduled=false)                          │
│  ├─ sentAt != null  →  ✅ VISIBLE in conversation          │
│  └─ sentAt == null  →  ❌ NOT VISIBLE (shouldn't happen)   │
│                                                              │
│  Scheduled Message (scheduled=true)                         │
│  ├─ status=PENDING   →  ❌ NOT VISIBLE in conversation     │
│  ├─ status=SENT      →  ✅ VISIBLE in conversation         │
│  ├─ status=CANCELLED →  ❌ NOT VISIBLE in conversation     │
│  └─ status=FAILED    →  ❌ NOT VISIBLE in conversation     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## API Behavior

### GET /api/v1/conversations/{conversationId}/messages

**Returns:**
- ✅ Regular messages that have been sent (sentAt != null)
- ✅ Scheduled messages with status=SENT
- ❌ Scheduled messages with status=PENDING (not sent yet)
- ❌ Scheduled messages with status=CANCELLED (user cancelled)
- ❌ Scheduled messages with status=FAILED (failed to send)

### GET /api/v1/messages/scheduled

**Returns:**
- ✅ ALL scheduled messages (PENDING, SENT, CANCELLED, FAILED)
- ✅ Can filter by status
- ✅ Only returns messages created by current user

**Purpose:** Manage scheduled messages (view, edit, cancel)

---

## Client Implementation

### Conversation View

```javascript
// Client không cần filter gì cả - backend đã filter rồi
async function loadConversationMessages(conversationId) {
  const response = await fetch(
    `${API_BASE}/v1/conversations/${conversationId}/messages`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  
  const result = await response.json();
  
  // Tất cả messages trong result.data đều là messages đã được gửi
  result.data.forEach(message => {
    displayMessage(message);
  });
}
```

### Scheduled Messages Management

```javascript
// Để xem scheduled messages (bao gồm PENDING), dùng API riêng
async function loadScheduledMessages() {
  const response = await fetch(
    `${API_BASE}/v1/messages/scheduled?status=PENDING`,
    {
      headers: { 'Authorization': `Bearer ${token}` }
    }
  );
  
  const result = await response.json();
  
  // Hiển thị scheduled messages để user có thể edit/cancel
  result.data.data.forEach(scheduledMessage => {
    displayScheduledMessage(scheduledMessage);
  });
}
```

---

## Testing

### Test 1: Verify PENDING messages không hiển thị

```bash
# 1. Tạo scheduled message với thời gian tương lai
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Test PENDING message",
    "scheduledTime": "2025-12-31T23:59:00Z"
  }'

# Response: {"data": {"id": 100, "scheduledStatus": "PENDING"}}

# 2. Get conversation messages
curl http://localhost:8080/api/v1/conversations/1/messages \
  -H "Authorization: Bearer YOUR_TOKEN"

# ✅ Expected: Message ID 100 KHÔNG có trong response
```

### Test 2: Verify SENT messages hiển thị

```bash
# 1. Tạo scheduled message với thời gian gần (1 phút)
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Test SENT message",
    "scheduledTime": "'$(date -u -d '+1 minute' +%Y-%m-%dT%H:%M:%SZ)'"
  }'

# 2. Đợi 1-2 phút để message được gửi

# 3. Get conversation messages
curl http://localhost:8080/api/v1/conversations/1/messages \
  -H "Authorization: Bearer YOUR_TOKEN"

# ✅ Expected: Message CÓ trong response với scheduledStatus="SENT"
```

### Test 3: Verify CANCELLED messages không hiển thị

```bash
# 1. Tạo scheduled message
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "content": "Test CANCELLED message",
    "scheduledTime": "2025-12-31T23:59:00Z"
  }'

# Response: {"data": {"id": 101}}

# 2. Cancel message
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/101 \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. Get conversation messages
curl http://localhost:8080/api/v1/conversations/1/messages \
  -H "Authorization: Bearer YOUR_TOKEN"

# ✅ Expected: Message ID 101 KHÔNG có trong response
```

---

## Migration Notes

### Existing Data

Nếu database đã có scheduled messages với status PENDING/CANCELLED/FAILED, chúng sẽ **tự động bị ẩn** sau khi deploy fix này. Không cần migration script.

### Backward Compatibility

**Breaking Change:** ⚠️ Có thể ảnh hưởng đến client nếu client đang expect PENDING messages trong conversation.

**Recommendation:** 
- Update client code để không expect PENDING messages trong conversation API
- Dùng `/api/v1/messages/scheduled` để xem scheduled messages

---

## Summary

### Changes Made

1. ✅ Updated `findByConversationIdWithSort()` - Filter sent messages only
2. ✅ Updated `findByConversationIdOrderBySentAtDesc()` - Filter sent messages only
3. ✅ Updated `findByConversationId()` - Filter sent messages only
4. ✅ Updated `countByConversationId()` - Count sent messages only

### Filter Logic

```
VISIBLE in conversation:
  - Regular messages: sentAt IS NOT NULL
  - Scheduled messages: scheduled=true AND scheduledStatus=SENT

NOT VISIBLE in conversation:
  - Scheduled messages: status=PENDING, CANCELLED, FAILED
```

### Benefits

1. ✅ **Better UX** - Users only see messages that have been sent
2. ✅ **Security** - Users can't see pending scheduled messages of others
3. ✅ **Consistency** - All clients see the same data
4. ✅ **Performance** - Database-level filtering is faster

### Status

✅ **DEPLOYED** - Changes applied and tested successfully
