# Chattrix API Specification

**Base URL:** `/api/v1`

**Authentication:** Hầu hết endpoints yêu cầu JWT Bearer Token trong header `Authorization: Bearer <token>`

---

## 📋 Table of Contents

1. [Authentication API](#authentication-api)
2. [Contact API](#contact-api)
3. [Conversation API](#conversation-api)
4. [Message API](#message-api)
5. [User Search API](#user-search-api)
6. [User Status API](#user-status-api)
7. [Typing Indicator API](#typing-indicator-api)
8. [Common Response Format](#common-response-format)
9. [Error Codes](#error-codes)

---

## Authentication API

**Base Path:** `/v1/auth`

Xem chi tiết tại [AUTH_API_DOCUMENTATION.md](AUTH_API_DOCUMENTATION.md)

### Summary of Auth Endpoints:

- `POST /auth/register` - Đăng ký tài khoản mới
- `POST /auth/login` - Đăng nhập
- `GET /auth/me` - Lấy thông tin user hiện tại 🔒
- `POST /auth/logout` - Đăng xuất device hiện tại 🔒
- `POST /auth/logout-all` - Đăng xuất tất cả devices 🔒
- `POST /auth/refresh` - Refresh access token
- `PUT /auth/change-password` - Đổi mật khẩu 🔒
- `POST /auth/verify-email` - Xác thực email
- `POST /auth/resend-verification` - Gửi lại email xác thực
- `POST /auth/forgot-password` - Quên mật khẩu
- `POST /auth/reset-password` - Reset mật khẩu

🔒 = Requires authentication

---

## Contact API

**Base Path:** `/v1/contacts`

**Authentication:** All endpoints require Bearer Token 🔒

### 1. Get All Contacts

Lấy danh sách tất cả contacts của user hiện tại.

**Endpoint:** `GET /v1/contacts`

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Contacts retrieved successfully",
  "data": [
    {
      "id": 1,
      "contactUser": {
        "id": 5,
        "username": "john_doe",
        "fullName": "John Doe",
        "email": "john@example.com",
        "profilePictureUrl": "https://...",
        "isOnline": true,
        "lastSeen": "2025-10-16T10:30:00Z"
      },
      "nickname": "Johnny",
      "isFavorite": false,
      "createdAt": "2025-10-15T08:20:00Z",
      "updatedAt": "2025-10-15T08:20:00Z"
    }
  ],
  "errorCode": null
}
```

---

### 2. Get Favorite Contacts

Lấy danh sách contacts yêu thích.

**Endpoint:** `GET /v1/contacts/favorites`

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Favorite contacts retrieved successfully",
  "data": [
    {
      "id": 1,
      "contactUser": {
        "id": 5,
        "username": "john_doe",
        "fullName": "John Doe"
      },
      "nickname": "Johnny",
      "isFavorite": true,
      "createdAt": "2025-10-15T08:20:00Z"
    }
  ],
  "errorCode": null
}
```

---

### 3. Add Contact

Thêm user vào danh sách contacts.

**Endpoint:** `POST /v1/contacts`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "contactUserId": 5,
  "nickname": "Johnny"
}
```

**Validation:**
- `contactUserId`: Required, Long
- `nickname`: Optional, String

**Success Response (201 CREATED):**
```json
{
  "success": true,
  "message": "Contact added successfully",
  "data": {
    "id": 1,
    "contactUser": {
      "id": 5,
      "username": "john_doe",
      "fullName": "John Doe"
    },
    "nickname": "Johnny",
    "isFavorite": false,
    "createdAt": "2025-10-16T10:30:00Z"
  },
  "errorCode": null
}
```

**Error Responses:**

- **400 BAD REQUEST** - Trying to add yourself:
```json
{
  "success": false,
  "message": "Cannot add yourself as contact",
  "data": null,
  "errorCode": "INVALID_REQUEST"
}
```

- **404 NOT FOUND** - User not found:
```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

- **409 CONFLICT** - Contact already exists:
```json
{
  "success": false,
  "message": "Contact already exists",
  "data": null,
  "errorCode": "DUPLICATE_CONTACT"
}
```

---

### 4. Update Contact

Cập nhật thông tin contact (nickname, favorite status).

**Endpoint:** `PUT /v1/contacts/{contactId}`

**Path Parameters:**
- `contactId`: Long - ID của contact cần update

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "nickname": "Johnny Boy",
  "isFavorite": true
}
```

**Validation:**
- `nickname`: Optional, String
- `isFavorite`: Optional, Boolean

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Contact updated successfully",
  "data": {
    "id": 1,
    "contactUser": {
      "id": 5,
      "username": "john_doe",
      "fullName": "John Doe"
    },
    "nickname": "Johnny Boy",
    "isFavorite": true,
    "updatedAt": "2025-10-16T11:00:00Z"
  },
  "errorCode": null
}
```

**Error Responses:**

- **403 FORBIDDEN** - Contact không thuộc về user hiện tại:
```json
{
  "success": false,
  "message": "You do not have access to this contact",
  "data": null,
  "errorCode": "FORBIDDEN"
}
```

- **404 NOT FOUND** - Contact không tồn tại

---

### 5. Delete Contact

Xóa contact khỏi danh sách.

**Endpoint:** `DELETE /v1/contacts/{contactId}`

**Path Parameters:**
- `contactId`: Long - ID của contact cần xóa

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Contact deleted successfully",
  "data": null,
  "errorCode": null
}
```

**Error Responses:**

- **403 FORBIDDEN** - Contact không thuộc về user hiện tại
- **404 NOT FOUND** - Contact không tồn tại

---

## Conversation API

**Base Path:** `/v1/conversations`

**Authentication:** All endpoints require Bearer Token 🔒

### 1. Create Conversation

Tạo cuộc trò chuyện mới (Direct hoặc Group).

**Endpoint:** `POST /v1/conversations`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Project Team Chat",
  "type": "GROUP",
  "participantIds": [2, 3, 4, 5]
}
```

**Validation:**
- `name`: Optional (required for GROUP), String
- `type`: Required, Enum ["DIRECT", "GROUP"]
- `participantIds`: Required, List<Long> (minimum 1)

**Success Response (201 CREATED):**
```json
{
  "success": true,
  "message": "Conversation created successfully",
  "data": {
    "id": 10,
    "name": "Project Team Chat",
    "type": "GROUP",
    "participants": [
      {
        "userId": 1,
        "username": "current_user",
        "fullName": "Current User",
        "role": "ADMIN"
      },
      {
        "userId": 2,
        "username": "john_doe",
        "fullName": "John Doe",
        "role": "MEMBER"
      }
    ],
    "createdAt": "2025-10-16T10:30:00Z",
    "updatedAt": "2025-10-16T10:30:00Z"
  },
  "errorCode": null
}
```

**Error Responses:**

- **400 BAD REQUEST** - Validation errors:
```json
{
  "success": false,
  "message": "At least one participant is required",
  "data": null,
  "errorCode": "BAD_REQUEST"
}
```

---

### 2. Get All Conversations

Lấy danh sách tất cả conversations của user hiện tại.

**Endpoint:** `GET /v1/conversations`

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Conversations retrieved successfully",
  "data": [
    {
      "id": 10,
      "name": "Project Team Chat",
      "type": "GROUP",
      "participants": [
        {
          "userId": 1,
          "username": "current_user",
          "role": "ADMIN"
        }
      ],
      "lastMessage": {
        "id": 100,
        "content": "Hello everyone!",
        "sentAt": "2025-10-16T09:00:00Z"
      },
      "createdAt": "2025-10-15T08:00:00Z",
      "updatedAt": "2025-10-16T09:00:00Z"
    }
  ],
  "errorCode": null
}
```

---

### 3. Get Conversation by ID

Lấy chi tiết conversation cụ thể.

**Endpoint:** `GET /v1/conversations/{conversationId}`

**Path Parameters:**
- `conversationId`: Long - ID của conversation

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Conversation retrieved successfully",
  "data": {
    "id": 10,
    "name": "Project Team Chat",
    "type": "GROUP",
    "participants": [
      {
        "userId": 1,
        "username": "current_user",
        "fullName": "Current User",
        "role": "ADMIN",
        "joinedAt": "2025-10-15T08:00:00Z"
      },
      {
        "userId": 2,
        "username": "john_doe",
        "fullName": "John Doe",
        "role": "MEMBER",
        "joinedAt": "2025-10-15T08:00:00Z"
      }
    ],
    "createdAt": "2025-10-15T08:00:00Z",
    "updatedAt": "2025-10-16T09:00:00Z"
  },
  "errorCode": null
}
```

**Error Responses:**

- **400 BAD REQUEST** - User không phải là participant:
```json
{
  "success": false,
  "message": "You do not have access to this conversation",
  "data": null,
  "errorCode": "BAD_REQUEST"
}
```

- **404 NOT FOUND** - Conversation không tồn tại

---

## Message API

**Base Path:** `/v1/conversations/{conversationId}/messages`

**Authentication:** All endpoints require Bearer Token 🔒

### 1. Get Messages

Lấy danh sách messages trong conversation (có phân trang).

**Endpoint:** `GET /v1/conversations/{conversationId}/messages`

**Path Parameters:**
- `conversationId`: Long - ID của conversation

**Query Parameters:**
- `page`: Integer (default: 0) - Số trang
- `size`: Integer (default: 50) - Số messages mỗi trang

**Headers:**
```
Authorization: Bearer <token>
```

**Example Request:**
```
GET /v1/conversations/10/messages?page=0&size=20
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Messages retrieved successfully",
  "data": [
    {
      "id": 100,
      "content": "Hello everyone!",
      "contentType": "TEXT",
      "sender": {
        "id": 1,
        "username": "current_user",
        "fullName": "Current User"
      },
      "conversationId": 10,
      "sentAt": "2025-10-16T09:00:00Z",
      "updatedAt": "2025-10-16T09:00:00Z",
      "isEdited": false,
      "isDeleted": false
    }
  ],
  "errorCode": null
}
```

**Error Responses:**

- **400 BAD REQUEST** - User không phải là participant
- **404 NOT FOUND** - Conversation không tồn tại

---

### 2. Get Message by ID

Lấy chi tiết message cụ thể.

**Endpoint:** `GET /v1/conversations/{conversationId}/messages/{messageId}`

**Path Parameters:**
- `conversationId`: Long - ID của conversation
- `messageId`: Long - ID của message

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Message retrieved successfully",
  "data": {
    "id": 100,
    "content": "Hello everyone!",
    "contentType": "TEXT",
    "sender": {
      "id": 1,
      "username": "current_user",
      "fullName": "Current User"
    },
    "conversationId": 10,
    "sentAt": "2025-10-16T09:00:00Z",
    "isEdited": false,
    "isDeleted": false
  },
  "errorCode": null
}
```

**Error Responses:**

- **400 BAD REQUEST** - User không phải là participant
- **404 NOT FOUND** - Message hoặc Conversation không tồn tại

---

## User Search API

**Base Path:** `/v1/users/search`

**Authentication:** All endpoints require Bearer Token 🔒

### 1. Search Users

Tìm kiếm người dùng để tạo đoạn chat mới. API này cho phép tìm kiếm theo username, email hoặc full name.

**Endpoint:** `GET /v1/users/search`

**Query Parameters:**
- `query`: String (required) - Từ khóa tìm kiếm (username, email, hoặc full name)
- `limit`: Integer (optional, default: 20, max: 50) - Số lượng kết quả tối đa

**Headers:**
```
Authorization: Bearer <token>
```

**Example Request:**
```
GET /v1/users/search?query=john&limit=10
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Users found successfully",
  "data": [
    {
      "id": 5,
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "avatarUrl": "https://example.com/avatar.jpg",
      "isOnline": true,
      "lastSeen": "2025-10-20T10:30:00Z",
      "contact": true,
      "hasConversation": true,
      "conversationId": 15
    },
    {
      "id": 8,
      "username": "johnny_smith",
      "email": "johnny@example.com",
      "fullName": "Johnny Smith",
      "avatarUrl": null,
      "isOnline": false,
      "lastSeen": "2025-10-19T15:20:00Z",
      "contact": false,
      "hasConversation": false,
      "conversationId": null
    }
  ],
  "errors": null
}
```

**Response Fields:**
- `id`: ID của user
- `username`: Tên đăng nhập
- `email`: Email của user
- `fullName`: Tên đầy đủ
- `avatarUrl`: URL ảnh đại diện (có thể null)
- `isOnline`: Trạng thái online
- `lastSeen`: Thời gian online lần cuối
- `contact`: User này có phải là contact của bạn không
- `hasConversation`: Đã có conversation trực tiếp với user này chưa
- `conversationId`: ID của conversation (nếu đã có), null nếu chưa có

**Error Responses:**

- **400 BAD REQUEST** - Query rỗng hoặc limit không hợp lệ
```json
{
  "success": false,
  "message": "Search query cannot be empty",
  "data": null,
  "errors": [
    {
      "field": null,
      "code": "INVALID_QUERY",
      "message": "Search query cannot be empty"
    }
  ]
}
```

- **401 UNAUTHORIZED** - Token không hợp lệ hoặc hết hạn

**Notes:**
- Kết quả tìm kiếm được sắp xếp theo độ liên quan:
  1. Khớp chính xác với username
  2. Khớp chính xác với full name
  3. Username bắt đầu bằng từ khóa
  4. Full name bắt đầu bằng từ khóa
  5. Các kết quả khác chứa từ khóa
- User hiện tại sẽ không xuất hiện trong kết quả tìm kiếm
- Tìm kiếm không phân biệt chữ hoa chữ thường

---

## User Status API

**Base Path:** `/v1/users/status`

**Authentication:** All endpoints require Bearer Token 🔒

### 1. Get Online Users

Lấy danh sách tất cả users đang online.

**Endpoint:** `GET /v1/users/status/online`

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Online users retrieved successfully",
  "data": [
    {
      "id": 2,
      "username": "john_doe",
      "fullName": "John Doe",
      "email": "john@example.com",
      "profilePictureUrl": "https://...",
      "isOnline": true,
      "lastSeen": "2025-10-16T10:30:00Z"
    },
    {
      "id": 3,
      "username": "jane_smith",
      "fullName": "Jane Smith",
      "isOnline": true
    }
  ],
  "errorCode": null
}
```

---

### 2. Get Online Users in Conversation

Lấy danh sách users đang online trong một conversation cụ thể.

**Endpoint:** `GET /v1/users/status/online/conversation/{conversationId}`

**Path Parameters:**
- `conversationId`: Long - ID của conversation

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Online users in conversation retrieved successfully",
  "data": [
    {
      "id": 2,
      "username": "john_doe",
      "fullName": "John Doe",
      "isOnline": true
    }
  ],
  "errorCode": null
}
```

---

### 3. Get User Status

Kiểm tra trạng thái online của một user cụ thể.

**Endpoint:** `GET /v1/users/status/{userId}`

**Path Parameters:**
- `userId`: Long - ID của user cần kiểm tra

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "User status retrieved successfully",
  "data": {
    "userId": 2,
    "isOnline": true,
    "activeSessionCount": 2
  },
  "errorCode": null
}
```

**Notes:**
- `activeSessionCount`: Số lượng sessions/devices đang active (ví dụ: web + mobile = 2)

---

## Typing Indicator API

**Base Path:** `/v1/typing`

**Authentication:** All endpoints require Bearer Token 🔒

**Note:** Đây là các test endpoints để simulate typing behavior. Trong production, typing events thường được gửi qua WebSocket.

### 1. Start Typing

Báo hiệu user bắt đầu typing trong conversation.

**Endpoint:** `POST /v1/typing/start`

**Query Parameters:**
- `conversationId`: Long - ID của conversation

**Headers:**
```
Authorization: Bearer <token>
```

**Example Request:**
```
POST /v1/typing/start?conversationId=10
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Started typing successfully",
  "data": "User 1 started typing in conversation 10",
  "errorCode": null
}
```

---

### 2. Stop Typing

Báo hiệu user dừng typing trong conversation.

**Endpoint:** `POST /v1/typing/stop`

**Query Parameters:**
- `conversationId`: Long - ID của conversation

**Headers:**
```
Authorization: Bearer <token>
```

**Example Request:**
```
POST /v1/typing/stop?conversationId=10
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Stopped typing successfully",
  "data": "User 1 stopped typing in conversation 10",
  "errorCode": null
}
```

---

### 3. Get Typing Status

Lấy danh sách users đang typing trong conversation.

**Endpoint:** `GET /v1/typing/status/{conversationId}`

**Path Parameters:**
- `conversationId`: Long - ID của conversation

**Query Parameters:**
- `excludeUserId`: Long (optional) - Loại trừ user ID này khỏi kết quả

**Headers:**
```
Authorization: Bearer <token>
```

**Example Request:**
```
GET /v1/typing/status/10?excludeUserId=1
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Typing status retrieved successfully",
  "data": {
    "conversationId": 10,
    "typingUserIds": [2, 5, 7],
    "count": 3
  },
  "errorCode": null
}
```

---

### 4. Check User Typing

Kiểm tra xem một user cụ thể có đang typing không.

**Endpoint:** `GET /v1/typing/check`

**Query Parameters:**
- `userId`: Long - ID của user cần kiểm tra
- `conversationId`: Long - ID của conversation

**Headers:**
```
Authorization: Bearer <token>
```

**Example Request:**
```
GET /v1/typing/check?userId=2&conversationId=10
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Typing status checked successfully",
  "data": {
    "userId": 2,
    "conversationId": 10,
    "isTyping": true
  },
  "errorCode": null
}
```

---

### 5. Clear User Typing

Xóa tất cả typing indicators của user hiện tại ở mọi conversations.

**Endpoint:** `DELETE /v1/typing/clear`

**Headers:**
```
Authorization: Bearer <token>
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Typing indicators cleared successfully",
  "data": "Cleared all typing indicators for user 1",
  "errorCode": null
}
```

---

## Common Response Format

Tất cả API responses đều tuân theo format chung:

### Success Response

```json
{
  "success": true,
  "message": "Human-readable success message",
  "data": { /* Response data object or array */ },
  "errorCode": null
}
```

### Error Response

```json
{
  "success": false,
  "message": "Human-readable error message",
  "data": null,
  "errorCode": "ERROR_CODE_CONSTANT"
}
```

---

## Error Codes

### HTTP Status Codes

| Status Code | Meaning | When Used |
|-------------|---------|-----------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request data, validation failed |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | Authenticated but no permission to access resource |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., duplicate entry) |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

### Custom Error Codes

| Error Code | Description |
|------------|-------------|
| `INVALID_REQUEST` | Invalid request parameters or body |
| `RESOURCE_NOT_FOUND` | Requested resource not found |
| `DUPLICATE_CONTACT` | Contact already exists |
| `FORBIDDEN` | User doesn't have permission |
| `BAD_REQUEST` | General bad request error |
| `UNAUTHORIZED` | Authentication failed |
| `VALIDATION_ERROR` | Request validation failed |
| `INTERNAL_ERROR` | Internal server error |

---

## Authentication Flow

### 1. Register & Verify Email
```
POST /v1/auth/register
  → Email sent with verification token
POST /v1/auth/verify-email (with token)
  → Account activated
```

### 2. Login
```
POST /v1/auth/login
  → Returns: { accessToken, refreshToken }
```

### 3. Access Protected Endpoints
```
GET /v1/contacts
Headers: Authorization: Bearer <accessToken>
  → Returns data if token valid
```

### 4. Token Refresh
```
POST /v1/auth/refresh
Body: { refreshToken }
  → Returns: { newAccessToken, newRefreshToken }
```

### 5. Logout
```
POST /v1/auth/logout
  → Invalidates current device token
POST /v1/auth/logout-all
  → Invalidates all device tokens
```

---

## Rate Limiting

Một số endpoints có rate limiting:

| Endpoint | Max Requests | Time Window |
|----------|--------------|-------------|
| `POST /auth/register` | 3 | 5 minutes |
| `POST /auth/login` | 10 | 1 minute |
| `POST /auth/verify-email` | Unlimited | 5 minutes |
| `POST /auth/resend-verification` | 3 | 10 minutes |
| `POST /auth/forgot-password` | 3 | 10 minutes |
| `POST /auth/reset-password` | Unlimited | 5 minutes |

Khi vượt quá rate limit, API trả về **429 Too Many Requests**.

---

## WebSocket API

Để nhận real-time updates (messages, typing indicators, user status), kết nối đến WebSocket endpoint:

**WebSocket URL:** `ws://your-domain/api/ws`

Chi tiết WebSocket API sẽ được mô tả trong document riêng.

---

## Changelog

### Version 1.0.0 (2025-10-16)
- Initial API specification
- Contact management endpoints
- Conversation management endpoints
- Message retrieval endpoints
- User status tracking endpoints
- Typing indicator endpoints

---

## Support

Nếu có thắc mắc về API, vui lòng liên hệ development team hoặc tham khảo source code tại repository.


