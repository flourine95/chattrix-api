# 🎂 Birthday Feature - Quick Reference Card

## 📋 TL;DR

**Backend:** ✅ Complete & Running
**Frontend:** ⏳ Choose your level and implement

---

## 🚀 For Frontend Developers

### Step 1: Choose Implementation Level

| Level | Time | Features | Complexity |
|-------|------|----------|------------|
| **Level 1** | 0h | System messages only | ⭐ Easy |
| **Level 2** | 2-3h | Banner + Quick send | ⭐⭐ Medium |
| **Level 3** | 5-6h | Full UI + Animations | ⭐⭐⭐ Advanced |

### Step 2: Read Documentation

```bash
# For implementation guide
.kiro/specs/birthday-feature/CLIENT_IMPLEMENTATION_GUIDE.md

# For UI mockups
.kiro/specs/birthday-feature/UI_MOCKUPS.md
```

### Step 3: Implement

**Level 1 (0 hours):**
```dart
// Just style SYSTEM messages differently
if (message.type == MessageType.SYSTEM) {
  return buildSystemMessage(message);
}
```

**Level 2 (2-3 hours):**
```dart
// 1. Create BirthdayService
// 2. Add BirthdayBanner widget
// 3. Add BirthdayWishesDialog
// 4. Integrate APIs
```

**Level 3 (5-6 hours):**
```dart
// Everything from Level 2 +
// 1. Birthday stickers (Giphy)
// 2. Birthday animations
// 3. Birthday screen
// 4. Notifications
```

---

## 🔌 API Quick Reference

### 1. Get Today's Birthdays
```bash
GET /v1/birthdays/today
Authorization: Bearer {token}

Response: [
  {
    "userId": 123,
    "fullName": "John Doe",
    "age": 28,
    "birthdayMessage": "Hôm nay"
  }
]
```

### 2. Get Upcoming Birthdays
```bash
GET /v1/birthdays/upcoming?days=7
Authorization: Bearer {token}

Response: [
  {
    "userId": 124,
    "fullName": "Jane Smith",
    "age": 31,
    "birthdayMessage": "Còn 2 ngày"
  }
]
```

### 3. Send Birthday Wishes
```bash
POST /v1/birthdays/send-wishes
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 123,
  "conversationIds": [1, 2, 3],
  "customMessage": "Happy Birthday! 🎉"
}

Response: "Birthday wishes sent successfully"
```

---

## 🎨 Design Tokens

### Colors
```dart
primary: #FFD700        // Gold
background: #FFF9E6     // Light yellow
border: #FFE066         // Yellow
text: #8B6914           // Dark gold
```

### Components
```dart
// Birthday Banner
height: 60-80px
padding: 16px
background: #FFF9E6
border: 1px solid #FFE066

// Birthday Badge
size: 16-32px
position: bottom-right of avatar
background: #FFD700
border: 2px solid white

// System Message
padding: 12px
background: #FFF9E6
border: 1px solid #FFE066
icon: 🎂 (20px)
```

---

## 🔄 How It Works

### Automatic (System)
```
00:00 Daily
    ↓
Scheduler checks birthdays
    ↓
Auto send to GROUP chats
    ↓
Message type: SYSTEM
    ↓
Format: "🎂 Hôm nay là sinh nhật của @user! 🎉"
```

### Manual (User)
```
User opens app
    ↓
See birthday banner
    ↓
Click "Send wishes"
    ↓
Select conversations
    ↓
Choose/write message
    ↓
Send via API
    ↓
Message type: TEXT
```

---

## 🧪 Quick Test

### Test Backend
```bash
# 1. Get today's birthdays
curl http://localhost:8080/v1/birthdays/today \
  -H "Authorization: Bearer TOKEN"

# 2. Check scheduler logs
docker compose logs api | grep "Birthday"
```

### Test Frontend
```dart
// 1. Fetch birthdays
final users = await birthdayService.getTodayBirthdays();
print('Birthdays today: ${users.length}');

// 2. Send wishes
await birthdayService.sendBirthdayWishes(
  userId: 123,
  conversationIds: [1, 2],
  customMessage: 'Happy Birthday!',
);
```

---

## 📱 UI Examples

### Banner
```
┌────────────────────────────────────┐
│ 🎂  Hôm nay có 2 người sinh nhật!  │
│     Nhấn để gửi lời chúc      [>]  │
└────────────────────────────────────┘
```

### System Message
```
┌────────────────────────────────────┐
│ [Yellow background]                │
│ 🎂 Hôm nay là sinh nhật của @John │
│    (28 tuổi)! Hãy cùng chúc mừng  │
│    nhé! 🎉🎈                       │
└────────────────────────────────────┘
```

### Birthday Badge
```
[Avatar]
    🎂  <- Badge at bottom-right
```

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Birthday not showing | Check timezone, compare month/day only |
| API returns 401 | Add JWT token in Authorization header |
| System message not styled | Check `message.type == 'SYSTEM'` |
| Scheduler not running | Check logs: `docker compose logs api` |

---

## 📚 Full Documentation

| File | Purpose | Audience |
|------|---------|----------|
| `README.md` | Documentation index | Everyone |
| `CLIENT_IMPLEMENTATION_GUIDE.md` | Frontend guide | Frontend devs |
| `UI_MOCKUPS.md` | Design system | Designers, Frontend |
| `IMPLEMENTATION_COMPLETE.md` | Backend details | Backend devs |

---

## ✅ Checklist

### Backend (Done ✅)
- [x] APIs implemented
- [x] Scheduler configured
- [x] Documentation complete
- [x] Deployed & running

### Frontend (Your Turn ⏳)
- [ ] Choose implementation level
- [ ] Read documentation
- [ ] Implement BirthdayService
- [ ] Create UI components
- [ ] Test APIs
- [ ] Deploy

---

## 🎉 Quick Start Commands

```bash
# Backend: Check status
docker compose logs api | grep Birthday

# Backend: Test API
curl http://localhost:8080/v1/birthdays/today \
  -H "Authorization: Bearer TOKEN"

# Frontend: Read guide
cat .kiro/specs/birthday-feature/CLIENT_IMPLEMENTATION_GUIDE.md

# Frontend: Check mockups
cat .kiro/specs/birthday-feature/UI_MOCKUPS.md
```

---

**Need help? Check the full documentation! 📚**

*Last updated: 2024-12-21*
