# 🐛 Birthday Feature - Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: "Cannot mention user who is not in this conversation"

**Error Message:**
```
Business error: Cannot mention user who is not in this conversation
```

**Status:** ✅ **FIXED in v1.0.2**

**When it happens:**
- Khi hệ thống tự động gửi birthday message
- User có sinh nhật nhưng không có trong conversation

**Solution:**
Backend đã được fix để kiểm tra user có trong conversation trước khi mention.

See: `BACKEND_FIXES.md` Issue 2

---

### Issue 2: MentionedUser userId is null

**Error Message:**
```
type 'Null' is not a subtype of type 'int' in type cast
```

**Status:** ✅ **FIXED in v1.0.2**

**When it happens:**
- Frontend parse message với mentionedUsers
- Backend chỉ gửi `id`, không gửi `userId`

**Solution:**
Backend đã được fix để gửi cả `id` và `userId`.

See: `BACKEND_FIXES.md` Issue 1

---

### Issue 3: Birthday Date Off by 1 Day

**Symptom:**
- Chọn ngày 21, lưu về ngày 20

**Solution:**
- Xem: `FRONTEND_TIMEZONE_FIX.md`
- Frontend gửi 12:00 UTC thay vì 00:00 UTC

---

### Issue 3: Birthday Date Off by 1 Day

**Symptom:**
- Chọn ngày 21, lưu về ngày 20

**Status:** ✅ **FIXED** (Frontend solution)

**Solution:**
- Xem: `FRONTEND_TIMEZONE_FIX.md`
- Frontend gửi 12:00 UTC thay vì 00:00 UTC
- Backend đã handle correctly (chỉ so sánh month/day)

See: `BACKEND_FIXES.md` Issue 3

---

### Issue 4: Birthday Not Detected Today

**Symptom:**
- User có ngày sinh hôm nay
- Nhưng `GET /v1/birthdays/today` không trả về

**Check:**

1. **Verify date in database:**
```sql
SELECT id, username, date_of_birth,
       EXTRACT(MONTH FROM date_of_birth) as birth_month,
       EXTRACT(DAY FROM date_of_birth) as birth_day,
       EXTRACT(MONTH FROM CURRENT_DATE) as current_month,
       EXTRACT(DAY FROM CURRENT_DATE) as current_day
FROM users 
WHERE id = YOUR_USER_ID;
```

2. **Check timezone:**
```bash
curl -X GET http://localhost:8080/v1/birthdays/debug/YOUR_USER_ID \
  -H "Authorization: Bearer TOKEN" | jq '.'
```

3. **Check query:**
```sql
-- This is what the backend runs
SELECT u.* FROM users u
WHERE EXTRACT(MONTH FROM u.date_of_birth) = EXTRACT(MONTH FROM CURRENT_DATE)
  AND EXTRACT(DAY FROM u.date_of_birth) = EXTRACT(DAY FROM CURRENT_DATE);
```

**Common Causes:**
- Timezone issue (date stored as 20 instead of 21)
- `dateOfBirth` is NULL
- Date format incorrect

**Solution:**
- Fix timezone (see Issue 2)
- Update date: `UPDATE users SET date_of_birth = '2024-12-21T12:00:00Z' WHERE id = 1;`

---

### Issue 4: Scheduler Not Running

**Symptom:**
- Đến 00:00 nhưng không có birthday message tự động

**Check:**

1. **Verify scheduler is registered:**
```bash
docker compose logs api | grep -i "birthday"
```

Should see:
```
JNDI bindings for session bean named 'BirthdayScheduler'
```

2. **Check scheduler logs:**
```bash
docker compose logs api | grep "Birthday Scheduler"
```

Should see (at 00:00):
```
Birthday Scheduler: Checking birthdays...
Birthday Scheduler: Found X users with birthday today
```

3. **Manually trigger:**
```bash
curl -X GET http://localhost:8080/v1/birthdays/trigger-check \
  -H "Authorization: Bearer TOKEN"
```

**Common Causes:**
- Container restarted after 00:00 (missed the schedule)
- Scheduler disabled
- No users with birthday today

**Solution:**
- Use trigger endpoint for testing
- Wait until next 00:00
- Check logs for errors

---

### Issue 5: System Messages Not Styled Differently

**Symptom:**
- Birthday messages look like normal messages
- No special styling

**Check:**

1. **Verify message type:**
```bash
curl -X GET http://localhost:8080/v1/conversations/1/messages \
  -H "Authorization: Bearer TOKEN" | jq '.[] | select(.content | contains("sinh nhật"))'
```

Should see:
```json
{
  "type": "SYSTEM",
  "content": "🎂 Hôm nay là sinh nhật của @user1 (25 tuổi)! Hãy cùng chúc mừng nhé! 🎉🎈"
}
```

2. **Frontend check:**
```dart
// In message builder
if (message.type == 'SYSTEM') {
  // Apply special styling
  return SystemMessageWidget(message: message);
}
```

**Solution:**
- Frontend cần check `message.type == 'SYSTEM'`
- Apply different styling (background color, icon, etc.)

---

### Issue 6: No Birthday Messages in Some Groups

**Symptom:**
- Birthday message sent to some groups
- But not all groups where user is member

**Check:**

1. **Verify user is in group:**
```sql
SELECT c.id, c.name, c.type
FROM conversations c
JOIN conversation_participants cp ON cp.conversation_id = c.id
WHERE cp.user_id = YOUR_USER_ID
  AND c.type = 'GROUP';
```

2. **Check logs:**
```bash
docker compose logs api | grep "Failed to send birthday message"
```

**Common Causes:**
- User not in group (left recently)
- Group has no other participants (only birthday user)
- Error sending message (check logs)

**Solution:**
- Verify user membership
- Check error logs
- Manually trigger to test

---

### Issue 7: Duplicate Birthday Messages

**Symptom:**
- Multiple birthday messages sent to same conversation

**Check:**

1. **Check scheduler runs:**
```bash
docker compose logs api | grep "Birthday Scheduler: Checking birthdays"
```

2. **Check for multiple containers:**
```bash
docker ps | grep chattrix-api
```

**Common Causes:**
- Multiple API containers running
- Scheduler triggered multiple times
- Manual trigger + automatic trigger

**Solution:**
- Ensure only 1 API container running
- Don't trigger manually at 00:00
- Add idempotency check (future enhancement)

---

## Debug Endpoints

### 1. Debug User Birthday
```bash
GET /v1/birthdays/debug/{userId}
```

**Response:**
```json
{
  "userId": 1,
  "username": "user1",
  "dateOfBirth_instant": "2024-12-21T12:00:00Z",
  "dateOfBirth_localDate_systemTZ": "2024-12-21",
  "dateOfBirth_localDate_UTC": "2024-12-21",
  "today_systemTZ": "2024-12-21",
  "today_UTC": "2024-12-21",
  "systemTimezone": "UTC",
  "month_match": true,
  "day_match": true,
  "is_birthday_today": true
}
```

### 2. Trigger Birthday Check
```bash
GET /v1/birthdays/trigger-check
```

**Use for:**
- Testing without waiting for 00:00
- Debugging scheduler issues
- Demo purposes

---

## Logging

### Enable Debug Logging

Add to `application.properties`:
```properties
# Birthday feature logging
logging.level.com.chattrix.api.services.birthday=DEBUG
logging.level.com.chattrix.api.services.message=DEBUG
```

### Check Logs

```bash
# All birthday-related logs
docker compose logs api | grep -i birthday

# Scheduler logs
docker compose logs api | grep "Birthday Scheduler"

# Error logs
docker compose logs api | grep ERROR | grep -i birthday

# Message sending logs
docker compose logs api | grep "Failed to send birthday message"
```

---

## Database Queries

### Check Users with Birthday Today
```sql
SELECT id, username, full_name, date_of_birth,
       EXTRACT(YEAR FROM AGE(date_of_birth)) as age
FROM users 
WHERE EXTRACT(MONTH FROM date_of_birth) = EXTRACT(MONTH FROM CURRENT_DATE)
  AND EXTRACT(DAY FROM date_of_birth) = EXTRACT(DAY FROM CURRENT_DATE);
```

### Check User's Group Conversations
```sql
SELECT c.id, c.name, c.type, COUNT(cp.user_id) as participant_count
FROM conversations c
JOIN conversation_participants cp ON cp.conversation_id = c.id
WHERE c.id IN (
    SELECT conversation_id 
    FROM conversation_participants 
    WHERE user_id = YOUR_USER_ID
)
AND c.type = 'GROUP'
GROUP BY c.id, c.name, c.type;
```

### Check Birthday Messages
```sql
SELECT m.id, m.content, m.type, m.sent_at, c.name as conversation_name
FROM messages m
JOIN conversations c ON c.id = m.conversation_id
WHERE m.type = 'SYSTEM'
  AND m.content LIKE '%sinh nhật%'
ORDER BY m.sent_at DESC
LIMIT 10;
```

---

## Performance Considerations

### Query Optimization

1. **Index on date_of_birth:**
```sql
CREATE INDEX idx_users_date_of_birth ON users(date_of_birth);
```

2. **Index on conversation participants:**
```sql
CREATE INDEX idx_conversation_participants_user_id ON conversation_participants(user_id);
```

### Scheduler Performance

- Runs once per day (00:00)
- Queries all users with birthday today
- Sends messages to all group conversations
- Complexity: O(users × groups)

**Optimization ideas:**
- Batch message sending
- Async processing
- Rate limiting

---

## Testing Checklist

- [ ] User birthday set correctly (12:00 UTC)
- [ ] Birthday detected today
- [ ] Manual wishes API works
- [ ] Automatic messages sent at 00:00
- [ ] Messages have correct type (SYSTEM)
- [ ] Mentions work correctly
- [ ] No errors in logs
- [ ] Frontend displays correctly
- [ ] Timezone handling correct
- [ ] No duplicate messages

---

## Contact & Support

**Documentation:**
- Main: `README.md`
- Frontend: `CLIENT_IMPLEMENTATION_GUIDE.md`
- Timezone: `FRONTEND_TIMEZONE_FIX.md`
- Demo: `DEMO_GUIDE.md`

**Logs:**
```bash
docker compose logs -f api
```

**Database:**
```bash
docker compose exec postgres psql -U postgres -d chattrix
```

---

**Last updated:** 2024-12-21
