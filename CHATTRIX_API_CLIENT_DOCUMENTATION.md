# Chattrix API - Tài liệu cho Client Developer

**Base URL:** `http://localhost:8080/api` (hoặc domain production của bạn)

**API Version:** `v1`

**Authentication:** Hầu hết endpoints yêu cầu JWT Bearer Token trong header:
```
Authorization: Bearer <your_access_token>
```

---

## 📋 Mục lục

1. [Authentication API](#1-authentication-api)
2. [User Search API](#2-user-search-api)
3. [Contact API](#3-contact-api)
4. [Conversation API](#4-conversation-api)
5. [Message API](#5-message-api)
6. [Reaction API](#6-reaction-api)
7. [User Status API](#7-user-status-api)
8. [Typing Indicator API](#8-typing-indicator-api)
9. [WebSocket API](#9-websocket-api)
10. [Response Format](#10-response-format)
11. [Error Codes](#11-error-codes)

---

## 1. Authentication API

**Base Path:** `/v1/auth`

### 1.1. Đăng ký tài khoản

**Endpoint:** `POST /v1/auth/register`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe"
}
```

**Validation Rules:**
- `username`: 4-20 ký tự, phải có ít nhất 1 chữ cái, không bắt đầu/kết thúc bằng `.` hoặc `_`
- `email`: Email hợp lệ
- `password`: Tối thiểu 6 ký tự
- `fullName`: 1-100 ký tự

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Registration successful. Please check your email to verify your account.",
  "data": null
}
```

---

### 1.2. Đăng nhập

**Endpoint:** `POST /v1/auth/login`

**Request Body:**
```json
{
  "usernameOrEmail": "john_doe",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

---

### 1.3. Lấy thông tin user hiện tại 🔒

**Endpoint:** `GET /v1/auth/me`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "avatarUrl": "https://example.com/avatar.jpg",
    "isOnline": true,
    "lastSeen": "2025-10-29T10:30:00.000Z"
  }
}
```

---

### 1.4. Đăng xuất 🔒

**Endpoint:** `POST /v1/auth/logout`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

---

### 1.5. Đăng xuất tất cả thiết bị 🔒

**Endpoint:** `POST /v1/auth/logout-all`

**Response:** `200 OK`

---

### 1.6. Refresh token

**Endpoint:** `POST /v1/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "new_access_token",
    "refreshToken": "new_refresh_token",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

---

### 1.7. Đổi mật khẩu 🔒

**Endpoint:** `PUT /v1/auth/change-password`

**Request Body:**
```json
{
  "oldPassword": "old_password",
  "newPassword": "new_password"
}
```

**Response:** `200 OK`

---

## 2. User Search API

**Base Path:** `/v1/users/search`

### 2.1. Tìm kiếm người dùng 🔒

**Endpoint:** `GET /v1/users/search`

**Query Parameters:**
- `query` (required): Từ khóa tìm kiếm (username, email, hoặc full name)
- `limit` (optional): Số lượng kết quả (1-50, mặc định: 20)

**Example:**
```
GET /v1/users/search?query=john&limit=10
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Users found successfully",
  "data": [
    {
      "id": 2,
      "username": "john_smith",
      "fullName": "John Smith",
      "avatarUrl": "https://example.com/avatar2.jpg",
      "isOnline": false,
      "lastSeen": "2025-10-29T09:15:00.000Z"
    }
  ]
}
```

---

## 3. Contact API

**Base Path:** `/v1/contacts`

**Authentication:** Tất cả endpoints yêu cầu Bearer Token 🔒

### 3.1. Lấy danh sách contacts

**Endpoint:** `GET /v1/contacts`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Contacts retrieved successfully",
  "data": [
    {
      "id": 1,
      "contactUserId": 5,
      "username": "alice_wonder",
      "fullName": "Alice Wonder",
      "avatarUrl": "https://example.com/alice.jpg",
      "nickname": "Alice",
      "isFavorite": true,
      "isOnline": true,
      "lastSeen": "2025-10-29T10:30:00.000Z",
      "createdAt": "2025-10-20T08:00:00.000Z"
    }
  ]
}
```

---

### 3.2. Lấy danh sách contacts yêu thích

**Endpoint:** `GET /v1/contacts/favorites`

**Response:** `200 OK` (cùng format như 3.1)

---

### 3.3. Thêm contact mới

**Endpoint:** `POST /v1/contacts`

**Request Body:**
```json
{
  "contactUserId": 5,
  "nickname": "Alice"
}
```

**Response:** `201 Created`

---

### 3.4. Cập nhật contact

**Endpoint:** `PUT /v1/contacts/{contactId}`

**Request Body:**
```json
{
  "nickname": "Alice Wonderland",
  "isFavorite": true
}
```

**Response:** `200 OK`

---

### 3.5. Xóa contact

**Endpoint:** `DELETE /v1/contacts/{contactId}`

**Response:** `200 OK`

---

## 4. Conversation API

**Base Path:** `/v1/conversations`

**Authentication:** Tất cả endpoints yêu cầu Bearer Token 🔒

### 4.1. Tạo cuộc trò chuyện mới

**Endpoint:** `POST /v1/conversations`

**Request Body (Direct Chat):**
```json
{
  "type": "DIRECT",
  "participantIds": [5]
}
```

**Request Body (Group Chat):**
```json
{
  "type": "GROUP",
  "name": "Project Team",
  "participantIds": [5, 7, 9]
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": 10,
    "type": "GROUP",
    "name": "Project Team",
    "createdAt": "2025-10-29T10:30:00.000Z",
    "updatedAt": "2025-10-29T10:30:00.000Z",
    "participants": [
      {
        "userId": 1,
        "username": "john_doe",
        "role": "ADMIN"
      },
      {
        "userId": 5,
        "username": "alice_wonder",
        "role": "MEMBER"
      }
    ],
    "lastMessage": null
  }
}
```

---

