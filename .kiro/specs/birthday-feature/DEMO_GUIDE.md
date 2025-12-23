# 🎂 Birthday Feature - Demo Guide

## 🎯 Quick Demo Setup (5 phút)

### Step 1: Setup Demo Data (Database)

**Option A: Quick SQL (Recommended)**
```bash
# Connect to database
docker compose exec postgres psql -U postgres -d chattrix

# Set 3 users with birthday TODAY
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '25 years' WHERE id = 1;
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '30 years' WHERE id = 2;
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '28 years' WHERE id = 3;

# Verify
SELECT id, username, full_name, 
       EXTRACT(YEAR FROM AGE(date_of_birth)) as age
FROM users 
WHERE EXTRACT(MONTH FROM date_of_birth) = EXTRACT(MONTH FROM CURRENT_DATE)
  AND EXTRACT(DAY FROM date_of_birth) = EXTRACT(DAY FROM CURRENT_DATE);

# Exit
\q
```

**Option B: Run Demo Script**
```bash
# Run the complete demo setup script
docker compose exec -T postgres psql -U postgres -d chattrix < demo-birthday-setup.sql
```

### Step 2: Test APIs

**2.1. Get Today's Birthdays**
```bash
curl -X GET http://localhost:8080/v1/birthdays/today \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```json
[
  {
    "userId": 1,
    "username": "user1",
    "fullName": "Nguyen Van A",
    "avatarUrl": "https://...",
    "dateOfBirth": "1999-12-21T00:00:00Z",
    "age": 25,
    "birthdayMessage": "Hôm nay"
  },
  {
    "userId": 2,
    "username": "user2",
    "fullName": "Tran Thi B",
    "avatarUrl": "https://...",
    "dateOfBirth": "1994-12-21T00:00:00Z",
    "age": 30,
    "birthdayMessage": "Hôm nay"
  }
]
```

**2.2. Get Upcoming Birthdays**
```bash
curl -X GET "http://localhost:8080/v1/birthdays/upcoming?days=7" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**2.3. Manually Trigger Birthday Check (Test Scheduler)**
```bash
curl -X GET http://localhost:8080/v1/birthdays/trigger-check \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected Response:**
```
"Birthday check triggered successfully"
```

**2.4. Check System Messages in Chat**
```bash
# Get messages from a group conversation
curl -X GET http://localhost:8080/v1/conversations/1/messages \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Look for messages with `type: "SYSTEM"` and content containing "sinh nhật".

---

## 🎬 Demo Scenarios

### Scenario 1: Show Birthday Banner (Frontend)

**Setup:**
1. Set user birthday to today (SQL above)
2. Open app
3. Call `GET /v1/birthdays/today`
4. Show banner if users.length > 0

**Expected UI:**
```
┌────────────────────────────────────┐
│ 🎂  Hôm nay có 2 người sinh nhật!  │
│     Nhấn để gửi lời chúc      [>]  │
└────────────────────────────────────┘
```

### Scenario 2: Send Birthday Wishes (Manual)

**Setup:**
1. User clicks "Gửi lời chúc" on banner
2. Select conversations (e.g., Family Group, Work Team)
3. Choose or write message
4. Call API

**API Call:**
```bash
curl -X POST http://localhost:8080/v1/birthdays/send-wishes \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "conversationIds": [1, 2],
    "customMessage": "Chúc mừng sinh nhật! 🎉"
  }'
```

**Expected:**
- ✅ Messages sent to conversations 1 and 2
- ✅ Messages have type `TEXT`
- ✅ Messages mention user (userId: 1)
- ✅ WebSocket broadcast to all participants

### Scenario 3: Auto Birthday Messages (System)

**Setup:**
1. Set user birthday to today
2. Trigger scheduler manually:
   ```bash
   curl -X GET http://localhost:8080/v1/birthdays/trigger-check \
     -H "Authorization: Bearer YOUR_JWT_TOKEN"
   ```

**Expected:**
- ✅ System messages sent to all GROUP conversations
- ✅ Messages have type `SYSTEM`
- ✅ Format: `🎂 Hôm nay là sinh nhật của @username (25 tuổi)! Hãy cùng chúc mừng nhé! 🎉🎈`
- ✅ WebSocket broadcast

**Verify in Chat:**
```bash
# Check messages in group conversation
curl -X GET http://localhost:8080/v1/conversations/1/messages \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Look for:
```json
{
  "type": "SYSTEM",
  "content": "🎂 Hôm nay là sinh nhật của @user1 (25 tuổi)! Hãy cùng chúc mừng nhé! 🎉🎈",
  "mentions": [1]
}
```

---

## 🧪 Testing Checklist

### Backend APIs
- [ ] GET /v1/birthdays/today returns users
- [ ] GET /v1/birthdays/upcoming returns users
- [ ] POST /v1/birthdays/send-wishes sends messages
- [ ] GET /v1/birthdays/trigger-check triggers scheduler
- [ ] System messages appear in group chats
- [ ] Messages have correct type (SYSTEM vs TEXT)
- [ ] Mentions work correctly

### Frontend (if implemented)
- [ ] Birthday banner shows when users have birthday
- [ ] Banner shows correct count
- [ ] Click banner opens birthday list
- [ ] Send wishes dialog works
- [ ] Conversation selector works
- [ ] Message templates work
- [ ] Custom message works
- [ ] System messages styled differently
- [ ] Birthday badge shows on avatar
- [ ] Mentions render correctly

---

## 🎥 Demo Script (For Presentation)

### Part 1: Show Birthday Detection (30 seconds)

**Say:**
> "Hệ thống tự động phát hiện sinh nhật của users. Hôm nay có 2 người sinh nhật."

**Show:**
1. Call API: `GET /v1/birthdays/today`
2. Show JSON response with 2 users
3. Point out: userId, fullName, age, birthdayMessage

### Part 2: Show Auto Birthday Messages (1 minute)

**Say:**
> "Mỗi ngày lúc 00:00, hệ thống tự động gửi lời chúc vào các nhóm chat."

**Show:**
1. Trigger scheduler: `GET /v1/birthdays/trigger-check`
2. Open group chat
3. Show system message with yellow background
4. Point out: 🎂 icon, @mention, age

### Part 3: Show Manual Birthday Wishes (1 minute)

**Say:**
> "Users cũng có thể tự gửi lời chúc cá nhân."

**Show:**
1. Click "Gửi lời chúc" button
2. Select conversations
3. Choose message template or write custom
4. Send
5. Show messages in selected conversations

### Part 4: Show Upcoming Birthdays (30 seconds)

**Say:**
> "Users có thể xem sinh nhật sắp tới trong 7 ngày."

**Show:**
1. Call API: `GET /v1/birthdays/upcoming?days=7`
2. Show list with countdown: "Còn 2 ngày", "Còn 5 ngày"

---

## 🔧 Troubleshooting Demo Issues

### Issue: No birthdays showing

**Check:**
```sql
-- Verify data
SELECT id, username, date_of_birth FROM users WHERE date_of_birth IS NOT NULL;

-- Check if any match today
SELECT id, username, 
       EXTRACT(MONTH FROM date_of_birth) as birth_month,
       EXTRACT(DAY FROM date_of_birth) as birth_day,
       EXTRACT(MONTH FROM CURRENT_DATE) as current_month,
       EXTRACT(DAY FROM CURRENT_DATE) as current_day
FROM users 
WHERE date_of_birth IS NOT NULL;
```

**Fix:**
```sql
-- Force set to today
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '25 years' WHERE id = 1;
```

### Issue: API returns 401 Unauthorized

**Fix:**
```bash
# 1. Login to get token
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "your_username",
    "password": "your_password"
  }'

# 2. Copy accessToken from response
# 3. Use in Authorization header
```

### Issue: System messages not appearing

**Check:**
```bash
# 1. Verify scheduler ran
docker compose logs api | grep "Birthday Scheduler"

# 2. Check if user is in any GROUP conversations
curl -X GET http://localhost:8080/v1/conversations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 3. Manually trigger
curl -X GET http://localhost:8080/v1/birthdays/trigger-check \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Issue: Scheduler not running

**Check:**
```bash
# Check logs
docker compose logs api | grep -i "birthday"

# Should see:
# "JNDI bindings for session bean named 'BirthdayScheduler'"
```

**Fix:**
```bash
# Restart container
docker compose restart api

# Wait 30 seconds
sleep 30

# Check logs again
docker compose logs api --tail=50 | grep Birthday
```

---

## 📊 Demo Data Recommendations

### Minimal Demo (1-2 users)
```sql
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '25 years' WHERE id = 1;
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '30 years' WHERE id = 2;
```

### Full Demo (6 users)
```sql
-- Today (3 users)
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '25 years' WHERE id = 1;
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '30 years' WHERE id = 2;
UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '28 years' WHERE id = 3;

-- Upcoming (3 users)
UPDATE users SET date_of_birth = CURRENT_DATE + INTERVAL '2 days' - INTERVAL '35 years' WHERE id = 4;
UPDATE users SET date_of_birth = CURRENT_DATE + INTERVAL '5 days' - INTERVAL '27 years' WHERE id = 5;
UPDATE users SET date_of_birth = CURRENT_DATE + INTERVAL '7 days' - INTERVAL '42 years' WHERE id = 6;
```

---

## 🎯 Quick Commands Cheat Sheet

```bash
# Setup demo data
docker compose exec postgres psql -U postgres -d chattrix -c \
  "UPDATE users SET date_of_birth = CURRENT_DATE - INTERVAL '25 years' WHERE id = 1;"

# Get today's birthdays
curl http://localhost:8080/v1/birthdays/today -H "Authorization: Bearer TOKEN"

# Trigger scheduler
curl http://localhost:8080/v1/birthdays/trigger-check -H "Authorization: Bearer TOKEN"

# Check logs
docker compose logs api | grep Birthday

# Reset data
docker compose exec postgres psql -U postgres -d chattrix -c \
  "UPDATE users SET date_of_birth = NULL WHERE id IN (1,2,3,4,5,6);"
```

---

## ⚠️ Important Notes for Demo

1. **JWT Token:** Always get fresh token before demo
2. **Database:** Setup data right before demo (dates change daily)
3. **Scheduler:** Use trigger endpoint for instant demo
4. **Conversations:** Ensure demo users are in GROUP conversations
5. **Cleanup:** Reset data after demo if needed

---

## 🎉 Demo Success Criteria

✅ Birthday banner shows with correct count
✅ System messages appear in group chats
✅ Manual wishes can be sent
✅ Upcoming birthdays display correctly
✅ Mentions work (@username)
✅ Age calculation is correct
✅ UI styling is distinct for system messages

---

**Ready to Demo! 🚀**

*Last updated: 2024-12-21*
