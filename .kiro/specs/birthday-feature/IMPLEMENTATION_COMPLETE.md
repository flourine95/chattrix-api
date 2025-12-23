# 🎂 Birthday Feature - Implementation Complete

## ✅ Đã hoàn thành

### 1. Database
- ✅ Field `dateOfBirth` đã có sẵn trong User entity (type: Instant)

### 2. Backend APIs (3 endpoints)

#### GET /v1/birthdays/today
Lấy danh sách users có sinh nhật hôm nay

**Response:**
```json
[
  {
    "userId": 123,
    "username": "john_doe",
    "fullName": "John Doe",
    "avatarUrl": "https://...",
    "dateOfBirth": "1995-12-21T00:00:00Z",
    "age": 28,
    "birthdayMessage": "Hôm nay"
  }
]
```

#### GET /v1/birthdays/upcoming?days=7
Lấy danh sách users có sinh nhật trong N ngày tới (default: 7 ngày)

**Query Parameters:**
- `days` (optional): Số ngày tới (1-365), default = 7

**Response:**
```json
[
  {
    "userId": 124,
    "username": "jane_smith",
    "fullName": "Jane Smith",
    "avatarUrl": "https://...",
    "dateOfBirth": "1992-12-23T00:00:00Z",
    "age": 31,
    "birthdayMessage": "Còn 2 ngày"
  }
]
```

#### POST /v1/birthdays/send-wishes
Gửi lời chúc sinh nhật đến user trong các conversations

**Request Body:**
```json
{
  "userId": 123,
  "conversationIds": [1, 2, 3],
  "customMessage": "Chúc mừng sinh nhật! 🎉" // Optional
}
```

**Response:**
```json
"Birthday wishes sent successfully"
```

**Default Message Format:**
- Nếu không có customMessage: `🎂 Chúc mừng sinh nhật @username (28 tuổi)! 🎉`
- Message sẽ tự động mention user sinh nhật

### 3. Scheduled Job
- ✅ `BirthdayScheduler` chạy tự động mỗi ngày lúc 00:00
- ✅ Tự động check users có sinh nhật hôm nay
- ✅ Tự động gửi message vào tất cả GROUP conversations mà user đó là member
- ✅ Message format: `🎂 Hôm nay là sinh nhật của @username (28 tuổi)! Hãy cùng chúc mừng nhé! 🎉🎈`

### 4. Files đã tạo

**DTOs:**
- `src/main/java/com/chattrix/api/responses/BirthdayUserResponse.java`
- `src/main/java/com/chattrix/api/requests/SendBirthdayWishesRequest.java`

**Service Layer:**
- `src/main/java/com/chattrix/api/services/birthday/BirthdayService.java`
- `src/main/java/com/chattrix/api/services/birthday/BirthdayScheduler.java`

**REST API:**
- `src/main/java/com/chattrix/api/resources/BirthdayResource.java`

**Repository:**
- Updated `src/main/java/com/chattrix/api/repositories/UserRepository.java`
  - Added `findUsersWithBirthdayToday()`
  - Added `findUsersWithUpcomingBirthdays(int daysAhead)`
  - Added `findUsersByBirthdayMonthAndDay(int month, int day)`

---

## 🧪 Testing

### 1. Test Birthday APIs

**Get today's birthdays:**
```bash
curl -X GET http://localhost:8080/v1/birthdays/today \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Get upcoming birthdays (next 7 days):**
```bash
curl -X GET "http://localhost:8080/v1/birthdays/upcoming?days=7" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Send birthday wishes:**
```bash
curl -X POST http://localhost:8080/v1/birthdays/send-wishes \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "conversationIds": [1, 2, 3],
    "customMessage": "Chúc mừng sinh nhật! 🎉"
  }'
```

### 2. Test Scheduled Job

Scheduled job sẽ chạy tự động lúc 00:00 mỗi ngày. Để test ngay:

**Option 1: Uncomment hourly scheduler**
Trong `BirthdayScheduler.java`, uncomment method `checkBirthdaysHourly()` để test mỗi giờ.

**Option 2: Manually trigger**
Tạo test endpoint để trigger manually:
```java
@GET
@Path("/test-scheduler")
public Response testScheduler() {
    birthdayService.checkAndSendBirthdayWishes();
    return Response.ok("Birthday check triggered").build();
}
```

### 3. Test Data Setup

**Tạo user với sinh nhật hôm nay:**
```sql
-- Update user's birthday to today
UPDATE users 
SET date_of_birth = CURRENT_DATE - INTERVAL '25 years'
WHERE id = 1;
```

**Tạo user với sinh nhật trong 3 ngày tới:**
```sql
UPDATE users 
SET date_of_birth = CURRENT_DATE + INTERVAL '3 days' - INTERVAL '30 years'
WHERE id = 2;
```

---

## 🎯 Features

### ✅ Implemented
1. **Birthday Storage** - Field `dateOfBirth` trong User entity
2. **Birthday Check API** - GET /v1/birthdays/today
3. **Upcoming Birthdays API** - GET /v1/birthdays/upcoming
4. **Send Birthday Wishes API** - POST /v1/birthdays/send-wishes
5. **Scheduled Job** - Auto check & send at 00:00 daily
6. **Auto Birthday Messages** - Tự động gửi vào group conversations
7. **User Mentions** - Tự động mention user sinh nhật
8. **Age Calculation** - Tự động tính tuổi

### 🎨 Frontend Integration (Cần làm)
1. Birthday input trong profile edit
2. Birthday display (🎂 icon, badge)
3. Birthday notification UI
4. Birthday wishes button
5. Birthday stickers (Giphy integration)

---

## 📊 API Status Summary

| API Endpoint | Status | Description |
|-------------|--------|-------------|
| GET /v1/users/profile | ✅ Có sẵn | Có field dateOfBirth |
| PUT /v1/users/profile | ✅ Có sẵn | Update dateOfBirth |
| GET /v1/birthdays/today | ✅ Mới tạo | List users sinh nhật hôm nay |
| GET /v1/birthdays/upcoming | ✅ Mới tạo | List users sinh nhật sắp tới |
| POST /v1/birthdays/send-wishes | ✅ Mới tạo | Gửi lời chúc sinh nhật |
| Scheduled Job | ✅ Mới tạo | Auto check & send at 00:00 |

---

## 🚀 Deployment

### Build & Deploy
```bash
# Build and restart containers
docker compose up -d --build

# Check logs
docker compose logs -f api

# Check if scheduler is running
docker compose logs api | grep "Birthday Scheduler"
```

### Verify Deployment
1. ✅ Server started successfully
2. ✅ APIs accessible at http://localhost:8080
3. ✅ Scheduler initialized (check logs for "Birthday Scheduler")

---

## 💡 Next Steps

### Backend Improvements (Optional)
1. **System User** - Tạo system user để gửi auto birthday messages thay vì dùng first participant
2. **Notification Service** - Integrate với notification service để push notifications
3. **Birthday Reminders** - Gửi reminder trước 1 ngày
4. **Birthday History** - Track birthday wishes đã gửi
5. **Birthday Templates** - Multiple message templates
6. **Birthday Settings** - User có thể tắt auto birthday messages

### Frontend Tasks
1. Birthday input trong profile
2. Birthday display trong chat list
3. Birthday notification banner
4. Birthday wishes UI
5. Birthday stickers integration

---

## 🐛 Troubleshooting

### Scheduler không chạy
```bash
# Check logs
docker compose logs api | grep "Birthday"

# Verify EJB Timer Service is enabled
docker compose exec api cat /opt/jboss/wildfly/standalone/configuration/standalone.xml | grep timer
```

### API returns 401 Unauthorized
- Đảm bảo gửi JWT token trong header: `Authorization: Bearer YOUR_TOKEN`
- Token phải valid và chưa expired

### Birthday messages không gửi
- Check user có `dateOfBirth` không null
- Check user là member của group conversations
- Check logs: `docker compose logs api | grep "Birthday"`

---

## 📝 Notes

- Birthday check dựa trên month & day, không quan tâm year
- Age được tính từ dateOfBirth đến hiện tại
- Auto messages chỉ gửi vào GROUP conversations, không gửi vào DIRECT conversations
- Scheduled job chạy với timezone của server (UTC)
- Messages có type SYSTEM để phân biệt với user messages

---

## ✨ Tính năng độc đáo

1. **Auto Birthday Detection** - Tự động phát hiện sinh nhật không cần manual check
2. **Smart Mentions** - Tự động mention user sinh nhật trong messages
3. **Age Display** - Hiển thị tuổi tự động
4. **Group Integration** - Tự động gửi vào tất cả groups mà user là member
5. **Flexible Messages** - Support custom messages hoặc dùng template
6. **Upcoming Birthdays** - Xem trước sinh nhật sắp tới (7 ngày)

---

**Status:** ✅ Backend Implementation Complete
**Build:** ✅ Success
**Deployment:** ✅ Running on http://localhost:8080
**Scheduler:** ✅ Active (runs at 00:00 daily)
