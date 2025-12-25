# ⚙️ Chattrix Conversation Settings & Avatar API

Tài liệu hướng dẫn sử dụng các API liên quan đến cài đặt cá nhân trong hội thoại, quản lý ảnh đại diện nhóm và quyền hạn.

## 🔐 Authentication & Base URL

- **Base URL:** `http://localhost:8080/api/v1`
- **Header:** `Authorization: Bearer <your_jwt_token>`
- **Content-Type:** `application/json`

---

## 🛠️ 1. CÀI ĐẶT HỘI THOẠI (CONVERSATION SETTINGS)

Các cài đặt này mang tính chất **cá nhân**, mỗi người dùng trong cùng một cuộc hội thoại có thể có cấu hình khác nhau.

### 1.1 Lấy cài đặt hiện tại
Lấy toàn bộ cấu hình cá nhân cho một cuộc hội thoại.

- **Endpoint:** `GET /conversations/{conversationId}/settings`

**Example Response:**
```json
{
    "success": true,
    "message": "Settings retrieved successfully",
    "data": {
        "conversationId": 6,
        "muted": true,
        "blocked": false,
        "notificationsEnabled": true,
        "pinned": false,
        "archived": false,
        "hidden": false
    }
}
```

---

### 1.2 Cập nhật cài đặt chung
Thay đổi biệt danh, theme hoặc trạng thái thông báo.

- **Endpoint:** `PUT /conversations/{conversationId}/settings`
- **Body:**
    - `customNickname`: Biệt danh bạn đặt cho đối phương/nhóm.
    - `theme`: Giao diện (ví dụ: "red", "blue").
    - `notificationsEnabled`: `true`/`false`.

**Example Request:**
```json
{
    "notificationsEnabled": true,
    "customNickname": "cut tom",
    "theme": "red"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Settings updated successfully",
    "data": {
        "conversationId": 6,
        "muted": true,
        "blocked": false,
        "notificationsEnabled": true,
        "customNickname": "cut tom",
        "theme": "red"
    }
}
```

---

### 1.3 Tắt/Bật thông báo (Mute/Unmute)
Tắt hoặc bật lại thông báo cho cuộc hội thoại.

- **Mute:** `POST /conversations/{conversationId}/settings/mute`
- **Unmute:** `POST /conversations/{conversationId}/settings/unmute`

**Example Response (Mute):**
```json
{
    "success": true,
    "message": "Conversation muted",
    "data": {
        "conversationId": 6,
        "muted": true,
        "blocked": false,
        "notificationsEnabled": true,
        "customNickname": "cut tom",
        "theme": "red",
        "pinned": false,
        "archived": false,
        "hidden": false
    }
}
```

---

### 1.4 Ghim/Bỏ ghim (Pin/Unpin)
Ghim cuộc hội thoại lên đầu danh sách.

- **Pin:** `POST /conversations/{conversationId}/settings/pin`
- **Unpin:** `POST /conversations/{conversationId}/settings/unpin`

**Example Response (Pin):**
```json
{
    "success": true,
    "message": "Conversation pinned",
    "data": {
        "conversationId": 6,
        "muted": false,
        "blocked": false,
        "notificationsEnabled": true,
        "customNickname": "cut tom",
        "theme": "red",
        "pinned": true,
        "pinOrder": 1,
        "archived": false,
        "hidden": false
    }
}
```

---

### 1.5 Ẩn/Hiện hội thoại (Hide/Unhide)
Ẩn cuộc hội thoại khỏi danh sách chính.

- **Hide:** `POST /conversations/{conversationId}/settings/hide`
- **Unhide:** `POST /conversations/{conversationId}/settings/unhide`

**Example Response (Hide):**
```json
{
    "success": true,
    "message": "Conversation hidden",
    "data": {
        "conversationId": 6,
        "muted": false,
        "blocked": false,
        "notificationsEnabled": true,
        "customNickname": "cut tom",
        "theme": "red",
        "pinned": false,
        "archived": false,
        "hidden": true
    }
}
```

---

### 1.6 Lưu trữ hội thoại (Archive/Unarchive)
Đưa cuộc hội thoại vào mục lưu trữ.

- **Archive:** `POST /conversations/{conversationId}/settings/archive`
- **Unarchive:** `POST /conversations/{conversationId}/settings/unarchive`

---

### 1.7 Chặn/Bỏ chặn người dùng (Block/Unblock)
Chỉ áp dụng cho hội thoại 1-1 (`DIRECT`).

- **Block:** `POST /conversations/{conversationId}/settings/block`
- **Unblock:** `POST /conversations/{conversationId}/settings/unblock`

**Example Response (Block - DIRECT):**
```json
{
    "success": true,
    "message": "User blocked",
    "data": {
        "conversationId": 4,
        "muted": false,
        "blocked": true,
        "notificationsEnabled": true,
        "customNickname": "My Best Friend",
        "theme": "blue",
        "pinned": false,
        "archived": false,
        "hidden": false
    }
}
```

**Example Error (Block - GROUP):**
```json
{
    "success": false,
    "message": "Action only available for direct conversations",
    "code": "BAD_REQUEST"
}
```

---

## 🔇 2. TẮT TIẾNG THÀNH VIÊN (MEMBER MUTE - ADMIN ONLY)

Admin có thể tắt quyền gửi tin nhắn của một thành viên trong nhóm.

- **Mute Member:** `POST /conversations/{conversationId}/settings/members/{userId}/mute`
- **Unmute Member:** `POST /conversations/{conversationId}/settings/members/{userId}/unmute`
- **Body (Mute):** `{"duration": 10000}` (giây)

**Example Response (Mute Member):**
```json
{
    "success": true,
    "message": "Member muted successfully",
    "data": {
        "userId": 6,
        "username": "dangvanthanh6",
        "fullName": "Dang Van Thanh",
        "muted": true,
        "mutedUntil": "2025-12-24T16:12:15.801Z",
        "mutedAt": "2025-12-24T13:25:35.801Z",
        "mutedBy": 2
    }
}
```

---

## 🔐 3. QUYỀN HẠN NHÓM (GROUP PERMISSIONS - ADMIN ONLY)

Cấu hình những gì thành viên thường có thể làm trong nhóm.

- **Lấy quyền hạn:** `GET /conversations/{conversationId}/settings/permissions`
- **Cập nhật quyền hạn:** `PUT /conversations/{conversationId}/settings/permissions`

**Example Response:**
```json
{
    "success": true,
    "message": "Permissions retrieved successfully",
    "data": {
        "conversationId": 6,
        "sendMessages": "ALL",
        "addMembers": "ADMIN_ONLY",
        "removeMembers": "ADMIN_ONLY",
        "editGroupInfo": "ADMIN_ONLY",
        "pinMessages": "ADMIN_ONLY",
        "deleteMessages": "ADMIN_ONLY",
        "createPolls": "ALL"
    }
}
```

---

## 🖼️ 4. QUẢN LÝ ẢNH ĐẠI DIỆN NHÓM (GROUP AVATAR)

- **Cập nhật:** `PUT /conversations/{conversationId}/avatar`
- **Xóa:** `DELETE /conversations/{conversationId}/avatar`

---

## 📝 Ghi chú cho Client

1. **Block Logic:** Chỉ hoạt động trên hội thoại `DIRECT`. Nếu gọi trên `GROUP` sẽ nhận lỗi `400 Bad Request`.
2. **Member Mute:** Khi một member bị mute, họ sẽ không thể gửi tin nhắn cho đến khi hết thời gian `mutedUntil` hoặc được Admin `unmute`.
3. **Permissions:** Client nên dựa vào dữ liệu từ API Permissions để ẩn/hiện các tính năng tương ứng cho người dùng không phải Admin.

---
**Last Updated:** 2024-12-24
