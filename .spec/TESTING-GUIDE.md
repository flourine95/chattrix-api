# Testing Guide - Cache & Performance Improvements

## 🎯 Mục Tiêu Testing

Kiểm tra các thay đổi sau audit:
1. ✅ Cache invalidation hoạt động đúng
2. ✅ Performance cải thiện (conversation loading)
3. ✅ Real-time updates qua WebSocket
4. ✅ Không có stale data
5. ✅ Không có regression bugs

---

## 🔧 Setup Testing Environment

### 1. Khởi động server
```bash
# Đã build xong, giờ chỉ cần start
docker compose up -d

# Xem logs
docker compose logs -f api
```

### 2. Kiểm tra server đã chạy
- API: http://localhost:8080
- WildFly Admin: http://localhost:9990

### 3. Chuẩn bị test data
Cần có:
- 2-3 user accounts để test
- Vài conversations (DIRECT và GROUP)
- Một số messages trong mỗi conversation

---

## 📋 TEST SCENARIOS

### TEST 1: Conversation List Performance ⚡
**Mục tiêu**: Kiểm tra performance cải thiện 50x

**Steps**:
1. Login với user có nhiều conversations (tốt nhất >100)
2. Gọi API: `GET /api/conversations?page=0&size=20`
3. Đo thời gian response

**Expected**:
- ✅ Response time: 50-200ms (lần đầu)
- ✅ Response time: ~5-10ms (lần 2 - cache hit)
- ✅ Chỉ trả về 20 conversations
- ✅ Có pagination info (total, hasNext, hasPrev)

**Test với curl**:
```bash
# Lần 1 (cache miss)
curl -X GET "http://localhost:8080/api/conversations?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -w "\nTime: %{time_total}s\n"

# Lần 2 (cache hit - should be faster)
curl -X GET "http://localhost:8080/api/conversations?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -w "\nTime: %{time_total}s\n"
```

**Pass Criteria**:
- ✅ Lần 2 nhanh hơn lần 1 đáng kể
- ✅ Response có đúng 20 items
- ✅ lastMessage hiển thị đúng

---

### TEST 2: Send Message & Cache Invalidation 💬
**Mục tiêu**: Kiểm tra cache invalidation khi gửi message

**Steps**:
1. Gọi `GET /api/conversations` → lưu lastMessage của conversation X
2. Gửi message mới vào conversation X: `POST /api/conversations/{id}/messages`
3. Gọi lại `GET /api/conversations` → kiểm tra lastMessage đã update

**Expected**:
- ✅ lastMessage trong conversation list đã thay đổi
- ✅ updatedAt đã thay đổi
- ✅ unreadCount tăng cho user khác
- ✅ WebSocket broadcast message đến tất cả participants

**Test với curl**:
```bash
# 1. Get conversations (cache)
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. Send message
curl -X POST "http://localhost:8080/api/conversations/1/messages" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Test cache invalidation",
    "type": "TEXT"
  }'

# 3. Get conversations again (should show new lastMessage)
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Pass Criteria**:
- ✅ lastMessage.content = "Test cache invalidation"
- ✅ lastMessage.sentAt là thời gian mới nhất
- ✅ Conversation xuất hiện ở đầu list (sorted by updatedAt)

---

### TEST 3: Update Message & Cache 📝
**Mục tiêu**: Kiểm tra cache invalidation khi edit message

**Steps**:
1. Gửi message: "Original content"
2. Get conversations → lưu lastMessage
3. Edit message: "Updated content"
4. Get conversations → kiểm tra lastMessage đã update

**Expected**:
- ✅ lastMessage.content = "Updated content"
- ✅ lastMessage.isEdited = true
- ✅ WebSocket broadcast MESSAGE_UPDATED event

**Test với curl**:
```bash
# 1. Send message
MESSAGE_ID=$(curl -X POST "http://localhost:8080/api/conversations/1/messages" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Original content", "type": "TEXT"}' \
  | jq -r '.id')

# 2. Update message
curl -X PUT "http://localhost:8080/api/conversations/1/messages/$MESSAGE_ID" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Updated content"}'

# 3. Check conversation list
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data[0].lastMessage'
```

**Pass Criteria**:
- ✅ lastMessage.content = "Updated content"
- ✅ lastMessage.isEdited = true

---

### TEST 4: Delete Message & Cache 🗑️
**Mục tiêu**: Kiểm tra cache invalidation khi xóa message

**Steps**:
1. Gửi 2 messages: "Message 1", "Message 2"
2. Get conversations → lastMessage = "Message 2"
3. Xóa "Message 2"
4. Get conversations → lastMessage = "Message 1"

**Expected**:
- ✅ lastMessage rollback về message trước đó
- ✅ WebSocket broadcast MESSAGE_DELETED event
- ✅ Cache đã được invalidate

**Test với curl**:
```bash
# 1. Send 2 messages
curl -X POST "http://localhost:8080/api/conversations/1/messages" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Message 1", "type": "TEXT"}'

MESSAGE_ID=$(curl -X POST "http://localhost:8080/api/conversations/1/messages" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Message 2", "type": "TEXT"}' \
  | jq -r '.id')

# 2. Delete last message
curl -X DELETE "http://localhost:8080/api/conversations/1/messages/$MESSAGE_ID" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. Check conversation list
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data[0].lastMessage.content'
```

**Pass Criteria**:
- ✅ lastMessage.content = "Message 1"
- ✅ Message 2 không còn trong list

---

### TEST 5: Forward Message & Cache 📤
**Mục tiêu**: Kiểm tra forward message invalidate cache

**Steps**:
1. Forward message từ conversation A sang conversation B
2. Get conversations → conversation B có lastMessage mới
3. Kiểm tra WebSocket broadcast

**Expected**:
- ✅ Conversation B có lastMessage = forwarded message
- ✅ Conversation B xuất hiện ở đầu list
- ✅ WebSocket broadcast CHAT_MESSAGE event
- ✅ Message có flag isForwarded = true

**Test với curl**:
```bash
# Forward message
curl -X POST "http://localhost:8080/api/messages/forward" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": 123,
    "conversationIds": [2, 3]
  }'

# Check target conversations
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data[] | select(.id == 2 or .id == 3) | .lastMessage'
```

**Pass Criteria**:
- ✅ Cả 2 conversations đều có lastMessage mới
- ✅ lastMessage.isForwarded = true

---

### TEST 6: Reaction & Cache 👍
**Mục tiêu**: Kiểm tra reaction invalidate cache

**Steps**:
1. Add reaction vào message
2. Get messages → kiểm tra reactions đã update
3. Remove reaction
4. Get messages → kiểm tra reactions đã xóa

**Expected**:
- ✅ Reactions hiển thị đúng
- ✅ WebSocket broadcast MESSAGE_REACTION event
- ✅ Cache đã được invalidate

**Test với curl**:
```bash
# Add reaction
curl -X POST "http://localhost:8080/api/messages/123/reactions" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"emoji": "👍"}'

# Get message
curl -X GET "http://localhost:8080/api/conversations/1/messages/123" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.reactions'

# Remove reaction
curl -X DELETE "http://localhost:8080/api/messages/123/reactions/👍" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Pass Criteria**:
- ✅ Reactions update real-time
- ✅ Không có stale data

---

### TEST 7: Pin Message & Cache 📌
**Mục tiêu**: Kiểm tra pin message invalidate cache

**Steps**:
1. Pin message
2. Get pinned messages → message xuất hiện
3. Unpin message
4. Get pinned messages → message biến mất

**Expected**:
- ✅ Pin status update đúng
- ✅ WebSocket broadcast MESSAGE_PIN event
- ✅ Cache đã được invalidate

**Test với curl**:
```bash
# Pin message
curl -X POST "http://localhost:8080/api/conversations/1/messages/123/pin" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get pinned messages
curl -X GET "http://localhost:8080/api/conversations/1/pinned-messages" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Unpin message
curl -X DELETE "http://localhost:8080/api/conversations/1/messages/123/pin" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Pass Criteria**:
- ✅ Pinned messages list update đúng
- ✅ WebSocket broadcast đến tất cả participants

---

### TEST 8: User Profile Update & Cache 👤
**Mục tiêu**: Kiểm tra user profile cache invalidation

**Steps**:
1. Get conversations → lưu sender info của lastMessage
2. Update user profile (avatar, fullName)
3. Get conversations → kiểm tra sender info đã update

**Expected**:
- ✅ Sender avatar/fullName trong lastMessage đã update
- ✅ User info trong messages đã update
- ✅ Cache đã được invalidate

**Test với curl**:
```bash
# Update profile
curl -X PUT "http://localhost:8080/api/users/profile" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "New Name",
    "avatarUrl": "https://new-avatar.jpg"
  }'

# Get conversations (should show new name/avatar)
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data[0].lastMessage.senderFullName'
```

**Pass Criteria**:
- ✅ senderFullName = "New Name"
- ✅ Tất cả messages của user đều update

---

### TEST 9: Announcement & Cache 📢
**Mục tiêu**: Kiểm tra announcement invalidate cache

**Steps**:
1. Create announcement trong group
2. Get conversations → lastMessage = announcement
3. Delete announcement
4. Get conversations → lastMessage rollback

**Expected**:
- ✅ Announcement xuất hiện trong conversation list
- ✅ WebSocket broadcast ANNOUNCEMENT_CREATED event
- ✅ Cache đã được invalidate

**Test với curl**:
```bash
# Create announcement
curl -X POST "http://localhost:8080/api/conversations/1/announcements" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Important announcement!"}'

# Get conversations
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data[0].lastMessage'
```

**Pass Criteria**:
- ✅ lastMessage.type = "ANNOUNCEMENT"
- ✅ lastMessage.content = "Important announcement!"

---

### TEST 10: Scheduled Message & Cache ⏰
**Mục tiêu**: Kiểm tra scheduled message khi được gửi

**Steps**:
1. Schedule message 1 phút sau
2. Đợi 1 phút
3. Get conversations → lastMessage = scheduled message
4. Kiểm tra WebSocket broadcast

**Expected**:
- ✅ Scheduled message tự động gửi đúng giờ
- ✅ Conversation list update
- ✅ WebSocket broadcast SCHEDULED_MESSAGE_SENT event
- ✅ Cache đã được invalidate

**Test với curl**:
```bash
# Schedule message (1 minute from now)
SCHEDULED_TIME=$(date -u -d '+1 minute' +"%Y-%m-%dT%H:%M:%SZ")
curl -X POST "http://localhost:8080/api/conversations/1/messages/schedule" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"content\": \"Scheduled message\",
    \"scheduledTime\": \"$SCHEDULED_TIME\"
  }"

# Wait 1 minute, then check
sleep 60
curl -X GET "http://localhost:8080/api/conversations" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  | jq '.data[0].lastMessage'
```

**Pass Criteria**:
- ✅ Message tự động gửi đúng giờ
- ✅ lastMessage update

---

## 🔍 MONITORING & DEBUGGING

### 1. Xem logs
```bash
# Real-time logs
docker compose logs -f api

# Filter cache logs
docker compose logs -f api | grep -i cache

# Filter error logs
docker compose logs -f api | grep -i error
```

### 2. Kiểm tra cache statistics
```bash
# Nếu có endpoint để xem cache stats
curl -X GET "http://localhost:8080/api/admin/cache/stats" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### 3. Database queries
```bash
# Connect to database
docker compose exec postgres psql -U postgres -d chattrix

# Check conversation count
SELECT COUNT(*) FROM conversations;

# Check message count
SELECT COUNT(*) FROM messages;

# Check latest messages
SELECT id, content, created_at FROM messages ORDER BY created_at DESC LIMIT 10;
```

---

## ✅ ACCEPTANCE CRITERIA

### Performance
- ✅ Conversation list load < 200ms (first time)
- ✅ Conversation list load < 10ms (cached)
- ✅ No memory leaks
- ✅ No N+1 queries

### Functionality
- ✅ All cache invalidations work
- ✅ No stale data
- ✅ WebSocket events broadcast correctly
- ✅ Real-time updates work

### Stability
- ✅ No compilation errors
- ✅ No runtime exceptions
- ✅ No data corruption
- ✅ Rollback works if needed

---

## 🐛 COMMON ISSUES & SOLUTIONS

### Issue 1: Cache không invalidate
**Symptom**: Stale data vẫn hiển thị
**Solution**: 
- Check logs xem có exception không
- Verify participant IDs được collect đúng
- Check CacheManager được inject đúng

### Issue 2: WebSocket không broadcast
**Symptom**: User khác không nhận được update
**Solution**:
- Check ChatSessionService
- Verify WebSocket connection
- Check participant list

### Issue 3: Performance không cải thiện
**Symptom**: Vẫn chậm như cũ
**Solution**:
- Check cache có được enable không
- Verify query sử dụng LIMIT/OFFSET
- Check database indexes

---

## 📊 TEST REPORT TEMPLATE

```markdown
# Test Report - [Date]

## Environment
- Server: http://localhost:8080
- Database: PostgreSQL 16
- Cache: Caffeine (in-memory)

## Test Results

### TEST 1: Conversation List Performance
- Status: ✅ PASS / ❌ FAIL
- Response time (first): XXXms
- Response time (cached): XXXms
- Notes: ...

### TEST 2: Send Message & Cache
- Status: ✅ PASS / ❌ FAIL
- Cache invalidated: Yes/No
- WebSocket broadcast: Yes/No
- Notes: ...

[... continue for all tests ...]

## Summary
- Total tests: 10
- Passed: X
- Failed: Y
- Performance improvement: XXx faster

## Issues Found
1. [Issue description]
2. [Issue description]

## Recommendations
1. [Recommendation]
2. [Recommendation]
```

---

## 🚀 NEXT STEPS

Sau khi test xong:
1. ✅ Document kết quả test
2. ✅ Fix bugs nếu có
3. ✅ Deploy lên staging
4. ✅ Load test với nhiều users
5. ✅ Monitor production metrics

