# 🚀 HƯỚNG DẪN NHANH - TEST API CHATTRIX

## 📦 Files trong thư mục `.spec/`

| File | Mô tả |
|------|-------|
| `POSTMAN_TEST_GUIDE.md` | Hướng dẫn chi tiết từng API endpoint với body, headers, response |
| `Chattrix_API_Collection.postman_collection.json` | Postman Collection - Import vào Postman để test |
| `Chattrix_Environment.postman_environment.json` | Postman Environment - Chứa biến môi trường |
| `API_ENDPOINTS.md` | Document tổng hợp tất cả API endpoints |
| `ADVANCED_FEATURES_ENTITIES.md` | Document thiết kế entities |

---

## ⚡ QUICK START (5 phút)

### Bước 1: Import vào Postman

1. Mở **Postman**
2. Click **Import** → Chọn 2 files:
   - `Chattrix_API_Collection.postman_collection.json`
   - `Chattrix_Environment.postman_environment.json`

### Bước 2: Cấu hình Environment

1. Click **Environments** → Chọn **Chattrix API Environment**
2. Cập nhật các giá trị:

```
BASE_URL = http://localhost:8080
USER1_TOKEN = <JWT token của user 1>
USER2_TOKEN = <JWT token của user 2>
USER3_TOKEN = <JWT token của user 3>
CONVERSATION_ID = <ID của conversation để test>
MESSAGE_ID = <ID của message để test>
```

### Bước 3: Chạy Application

```bash
cd D:/Projects/chattrix/chattrix-api
mvn quarkus:dev
```

### Bước 4: Test APIs

1. Chọn **Chattrix API Environment** ở góc trên bên phải
2. Mở folder **1. Friend Requests**
3. Click **Send** từng request theo thứ tự

---

## 📋 THỨ TỰ TEST KHUYẾN NGHỊ

### 1️⃣ Friend Requests (7 tests)
```
✅ Send Friend Request (User1 → User2)
✅ Get Received Requests (User2)
✅ Accept Request (User2)
✅ Send Request (User1 → User3)
✅ Reject Request (User3)
✅ Get Sent Requests (User1)
✅ Cancel Request (User1)
```

### 2️⃣ Message Edit & Delete (4 tests)
```
✅ Edit Message (1st time)
✅ Edit Message (2nd time)
✅ Get Edit History
✅ Delete Message (Soft Delete)
```

### 3️⃣ Message Forward (1 test)
```
✅ Forward Message to Multiple Conversations
```

### 4️⃣ Read Receipts (5 tests)
```
✅ Mark Message as Read (User2)
✅ Mark Message as Read (User3)
✅ Get Read Receipts (User1)
✅ Mark Conversation as Read (User2)
✅ Get Total Unread Count (User1)
```

### 5️⃣ Pinned Messages (6 tests)
```
✅ Pin Message 1
✅ Pin Message 2
✅ Pin Message 3
✅ Get Pinned Messages
✅ Unpin Message 2
✅ Try Pin 4th Message (Should Fail - Max 3)
```

### 6️⃣ Conversation Settings (9 tests)
```
✅ Hide Conversation
✅ Unhide Conversation
✅ Archive Conversation
✅ Unarchive Conversation
✅ Pin Conversation
✅ Unpin Conversation
✅ Mute Conversation
✅ Unmute Conversation
✅ Get Conversation Settings
```

---

## 🔧 CHUẨN BỊ DỮ LIỆU TEST

### Tạo Users (Giả sử bạn có API đăng ký)

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe"
}
```

Lặp lại cho User 2 (alice) và User 3 (bob).

### Lấy JWT Tokens (Giả sử bạn có API đăng nhập)

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "username": "john_doe"
}
```

Copy token và paste vào Environment variable `USER1_TOKEN`.

### Tạo Conversation và Messages

Sử dụng API hiện có để:
1. Tạo conversation giữa User 1 và User 2
2. Gửi vài messages trong conversation
3. Lưu lại `conversationId` và `messageId`

---

## 🎯 CHECKLIST TRƯỚC KHI TEST

- [ ] ✅ Database đã chạy migration V2
- [ ] ✅ Application đang chạy (`mvn quarkus:dev`)
- [ ] ✅ Đã tạo 3 users trong database
- [ ] ✅ Đã lấy JWT tokens cho 3 users
- [ ] ✅ Đã import Postman Collection
- [ ] ✅ Đã import và cấu hình Environment
- [ ] ✅ Đã tạo conversation với messages
- [ ] ✅ Đã cập nhật `CONVERSATION_ID` và `MESSAGE_ID` trong Environment

---

## 📊 EXPECTED RESULTS

### ✅ Success Cases

| Test | Expected Status | Expected Result |
|------|----------------|-----------------|
| Send Friend Request | 201 Created | Tạo contact với status PENDING |
| Accept Friend Request | 200 OK | Tạo quan hệ 2 chiều, status ACCEPTED |
| Edit Message | 200 OK | isEdited = true, editedAt updated |
| Delete Message | 204 No Content | isDeleted = true, content ẩn |
| Forward Message | 201 Created | Tạo messages mới với isForwarded = true |
| Mark as Read | 204 No Content | Tạo read receipt, unreadCount giảm |
| Pin Message | 201 Created | Tạo pinned message với pinOrder |
| Hide Conversation | 200 OK | isHidden = true, hiddenAt updated |

### ❌ Error Cases

| Test | Expected Status | Expected Error |
|------|----------------|----------------|
| Pin 4th Message | 400 Bad Request | "Cannot pin more than 3 messages" |
| Edit Other's Message | 403 Forbidden | "You can only edit your own messages" |
| Send Friend Request to Self | 400 Bad Request | "Cannot send friend request to yourself" |

---

## 🐛 TROUBLESHOOTING

### Lỗi 401 Unauthorized
**Nguyên nhân:** Token không hợp lệ hoặc đã hết hạn  
**Giải pháp:** Đăng nhập lại để lấy token mới

### Lỗi 404 Not Found
**Nguyên nhân:** ID không tồn tại trong database  
**Giải pháp:** Kiểm tra lại `CONVERSATION_ID`, `MESSAGE_ID` trong Environment

### Lỗi 403 Forbidden
**Nguyên nhân:** Không có quyền thực hiện action  
**Giải pháp:** Kiểm tra user có phải owner/admin không

### Application không start
**Nguyên nhân:** Port 8080 đã được sử dụng  
**Giải pháp:** 
```bash
# Tìm process đang dùng port 8080
netstat -ano | findstr :8080

# Kill process
taskkill /PID <process_id> /F
```

---

## 📞 HỖ TRỢ

Nếu gặp vấn đề, kiểm tra:
1. **Application logs** - Xem console output của `mvn quarkus:dev`
2. **Database** - Kiểm tra dữ liệu trong PostgreSQL
3. **Postman Console** - Xem request/response details

---

## 🎉 HOÀN THÀNH

Sau khi test xong tất cả 32 test cases, bạn đã verify được:

✅ Friend Request System hoạt động đúng  
✅ Message Edit/Delete với history tracking  
✅ Message Forward đến nhiều conversations  
✅ Read Receipts và Unread Count  
✅ Pinned Messages với giới hạn 3 messages  
✅ Conversation Settings (Hide/Archive/Pin/Mute)  

**Chúc mừng! 🎊 API của bạn đã sẵn sàng!**

