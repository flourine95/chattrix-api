# Authentication API Documentation

## Base URL
```
/v1/auth
```

## Mục lục
1. [Register](#1-register)
2. [Verify Email](#2-verify-email)
3. [Resend Verification](#3-resend-verification)
4. [Login](#4-login)
5. [Get Current User](#5-get-current-user)
6. [Refresh Token](#6-refresh-token)
7. [Change Password](#7-change-password)
8. [Forgot Password](#8-forgot-password)
9. [Reset Password](#9-reset-password)
10. [Logout](#10-logout)
11. [Logout All Devices](#11-logout-all-devices)

---

## 1. Register

Đăng ký tài khoản mới. Sau khi đăng ký thành công, hệ thống sẽ gửi email xác thực với mã OTP.

### Endpoint
```
POST /v1/auth/register
```

### Rate Limit
- **3 requests per 5 minutes**

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| username | string | Yes | - 4-20 ký tự<br>- Phải chứa ít nhất 1 chữ cái<br>- Chỉ chứa chữ cái, số, dấu chấm (.) và gạch dưới (_)<br>- Không bắt đầu/kết thúc bằng . hoặc _<br>- Không chứa 2 ký tự đặc biệt liên tiếp<br>- Username phải duy nhất |
| email | string | Yes | - Định dạng email hợp lệ<br>- Email phải duy nhất |
| password | string | Yes | - Tối thiểu 6 ký tự<br>- Tối đa 100 ký tự |
| fullName | string | Yes | - 1-100 ký tự |

### Response

#### Success Response (201 Created)
```json
{
  "success": true,
  "message": "Registration successful. Please check your email to verify your account.",
  "data": null,
  "errors": null
}
```

#### Error Responses

**400 Bad Request - Validation Error**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "username",
      "errorCode": "VALIDATION_ERROR",
      "message": "Username must be between 4 and 20 characters"
    }
  ]
}
```

**409 Conflict - Username/Email đã tồn tại**
```json
{
  "success": false,
  "message": "Username already exists",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "USERNAME_EXISTS",
      "message": "Username already exists"
    }
  ]
}
```

**429 Too Many Requests - Vượt quá rate limit**
```json
{
  "success": false,
  "message": "Too many requests. Please try again later.",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "RATE_LIMIT_EXCEEDED",
      "message": "Too many requests. Please try again later."
    }
  ]
}
```

---

## 2. Verify Email

Xác thực email bằng mã OTP được gửi qua email sau khi đăng ký.

### Endpoint
```
POST /v1/auth/verify-email
```

### Rate Limit
- **5 requests per 5 minutes**

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "email": "string",
  "otp": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | string | Yes | Định dạng email hợp lệ |
| otp | string | Yes | Đúng 6 chữ số (0-9) |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Email verified successfully. You can now login.",
  "data": null,
  "errors": null
}
```

#### Error Responses

**400 Bad Request - OTP không hợp lệ hoặc hết hạn**
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_OTP",
      "message": "Invalid or expired OTP"
    }
  ]
}
```

**404 Not Found - Email không tồn tại**
```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "USER_NOT_FOUND",
      "message": "User not found"
    }
  ]
}
```

---

## 3. Resend Verification

Gửi lại email xác thực với mã OTP mới.

### Endpoint
```
POST /v1/auth/resend-verification
```

### Rate Limit
- **3 requests per 10 minutes**

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "email": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | string | Yes | Định dạng email hợp lệ |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Verification email sent successfully",
  "data": null,
  "errors": null
}
```

#### Error Responses

**404 Not Found - Email không tồn tại**
```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "USER_NOT_FOUND",
      "message": "User not found"
    }
  ]
}
```

**400 Bad Request - Email đã được xác thực**
```json
{
  "success": false,
  "message": "Email already verified",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "EMAIL_ALREADY_VERIFIED",
      "message": "Email already verified"
    }
  ]
}
```

---

## 4. Login

Đăng nhập vào hệ thống. Có thể sử dụng username hoặc email để đăng nhập.

### Endpoint
```
POST /v1/auth/login
```

### Rate Limit
- **10 requests per minute**

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "usernameOrEmail": "string",
  "password": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| usernameOrEmail | string | Yes | Không được để trống |
| password | string | Yes | Không được để trống |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "errors": null
}
```

#### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| accessToken | string | JWT token để xác thực các API request |
| refreshToken | string | Token để lấy access token mới khi hết hạn |
| tokenType | string | Loại token (luôn là "Bearer") |
| expiresIn | long | Thời gian hết hạn của access token (giây) |

#### Error Responses

**401 Unauthorized - Sai username/email hoặc mật khẩu**
```json
{
  "success": false,
  "message": "Invalid credentials",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_CREDENTIALS",
      "message": "Invalid credentials"
    }
  ]
}
```

**403 Forbidden - Email chưa được xác thực**
```json
{
  "success": false,
  "message": "Email not verified",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "EMAIL_NOT_VERIFIED",
      "message": "Email not verified"
    }
  ]
}
```

---

## 5. Get Current User

Lấy thông tin của user hiện tại đang đăng nhập.

### Endpoint
```
GET /v1/auth/me
```

### Authentication
**Required** - Bearer Token

### Headers
```
Authorization: Bearer {accessToken}
```

### Request Body
Không có

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "avatarUrl": "https://example.com/avatars/john.jpg",
    "isOnline": true,
    "lastSeen": "2025-10-15T10:30:00Z"
  },
  "errors": null
}
```

#### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| id | UUID | ID duy nhất của user |
| username | string | Tên đăng nhập |
| email | string | Địa chỉ email |
| fullName | string | Họ tên đầy đủ |
| avatarUrl | string | URL của ảnh đại diện (có thể null) |
| isOnline | boolean | Trạng thái online/offline |
| lastSeen | ISO 8601 | Thời gian hoạt động cuối cùng |

#### Error Responses

**401 Unauthorized - Token không hợp lệ hoặc hết hạn**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_TOKEN",
      "message": "Invalid or expired token"
    }
  ]
}
```

---

## 6. Refresh Token

Lấy access token mới khi token hiện tại sắp hết hạn hoặc đã hết hạn.

### Endpoint
```
POST /v1/auth/refresh
```

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "refreshToken": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| refreshToken | string | Yes | Không được để trống |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "errors": null
}
```

#### Error Responses

**401 Unauthorized - Refresh token không hợp lệ hoặc hết hạn**
```json
{
  "success": false,
  "message": "Invalid or expired refresh token",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_REFRESH_TOKEN",
      "message": "Invalid or expired refresh token"
    }
  ]
}
```

---

## 7. Change Password

Thay đổi mật khẩu khi đã đăng nhập.

### Endpoint
```
PUT /v1/auth/change-password
```

### Authentication
**Required** - Bearer Token

### Headers
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### Request Body
```json
{
  "currentPassword": "string",
  "newPassword": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| currentPassword | string | Yes | Không được để trống |
| newPassword | string | Yes | - Tối thiểu 6 ký tự<br>- Tối đa 100 ký tự |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null,
  "errors": null
}
```

#### Error Responses

**400 Bad Request - Mật khẩu hiện tại không đúng**
```json
{
  "success": false,
  "message": "Current password is incorrect",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_PASSWORD",
      "message": "Current password is incorrect"
    }
  ]
}
```

**401 Unauthorized - Token không hợp lệ**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_TOKEN",
      "message": "Invalid or expired token"
    }
  ]
}
```

---

## 8. Forgot Password

Yêu cầu đặt lại mật khẩu. Hệ thống sẽ gửi mã OTP qua email.

### Endpoint
```
POST /v1/auth/forgot-password
```

### Rate Limit
- **3 requests per 10 minutes**

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "email": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | string | Yes | Định dạng email hợp lệ |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Password reset email sent successfully",
  "data": null,
  "errors": null
}
```

#### Error Responses

**404 Not Found - Email không tồn tại**
```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "USER_NOT_FOUND",
      "message": "User not found"
    }
  ]
}
```

---

## 9. Reset Password

Đặt lại mật khẩu mới sử dụng mã OTP được gửi qua email.

### Endpoint
```
POST /v1/auth/reset-password
```

### Rate Limit
- **5 requests per 5 minutes**

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "email": "string",
  "otp": "string",
  "newPassword": "string"
}
```

#### Field Validations
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | string | Yes | Không được để trống |
| otp | string | Yes | Đúng 6 chữ số (0-9) |
| newPassword | string | Yes | - Tối thiểu 6 ký tự<br>- Tối đa 100 ký tự |

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null,
  "errors": null
}
```

#### Error Responses

**400 Bad Request - OTP không hợp lệ hoặc hết hạn**
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_OTP",
      "message": "Invalid or expired OTP"
    }
  ]
}
```

**404 Not Found - Email không tồn tại**
```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "USER_NOT_FOUND",
      "message": "User not found"
    }
  ]
}
```

---

## 10. Logout

Đăng xuất khỏi thiết bị hiện tại. Token hiện tại sẽ bị vô hiệu hóa.

### Endpoint
```
POST /v1/auth/logout
```

### Authentication
**Required** - Bearer Token

### Headers
```
Authorization: Bearer {accessToken}
```

### Request Body
Không có

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Logged out from this device successfully",
  "data": null,
  "errors": null
}
```

#### Error Responses

**401 Unauthorized - Token không hợp lệ**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_TOKEN",
      "message": "Invalid or expired token"
    }
  ]
}
```

---

## 11. Logout All Devices

Đăng xuất khỏi tất cả các thiết bị. Tất cả token của user sẽ bị vô hiệu hóa.

### Endpoint
```
POST /v1/auth/logout-all
```

### Authentication
**Required** - Bearer Token

### Headers
```
Authorization: Bearer {accessToken}
```

### Request Body
Không có

### Response

#### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Logged out from all devices successfully",
  "data": null,
  "errors": null
}
```

#### Error Responses

**401 Unauthorized - Token không hợp lệ**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null,
  "errors": [
    {
      "field": null,
      "errorCode": "INVALID_TOKEN",
      "message": "Invalid or expired token"
    }
  ]
}
```

---

## Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| VALIDATION_ERROR | 400 | Lỗi validation dữ liệu đầu vào |
| INVALID_CREDENTIALS | 401 | Sai username/email hoặc mật khẩu |
| INVALID_TOKEN | 401 | Token không hợp lệ hoặc đã hết hạn |
| INVALID_REFRESH_TOKEN | 401 | Refresh token không hợp lệ hoặc đã hết hạn |
| EMAIL_NOT_VERIFIED | 403 | Email chưa được xác thực |
| USER_NOT_FOUND | 404 | Không tìm thấy user |
| USERNAME_EXISTS | 409 | Username đã tồn tại |
| EMAIL_EXISTS | 409 | Email đã tồn tại |
| INVALID_OTP | 400 | OTP không hợp lệ hoặc đã hết hạn |
| EMAIL_ALREADY_VERIFIED | 400 | Email đã được xác thực |
| INVALID_PASSWORD | 400 | Mật khẩu không đúng |
| RATE_LIMIT_EXCEEDED | 429 | Vượt quá giới hạn số lượng request |

---

## Flow Diagram

### User Registration Flow
```
1. Client -> POST /v1/auth/register
2. Server -> Tạo user mới (chưa verified)
3. Server -> Gửi OTP qua email
4. Server -> Response 201 Created
5. Client -> POST /v1/auth/verify-email (với OTP)
6. Server -> Xác thực email
7. Server -> Response 200 OK
8. Client -> POST /v1/auth/login
9. Server -> Response với accessToken & refreshToken
```

### Password Reset Flow
```
1. Client -> POST /v1/auth/forgot-password
2. Server -> Gửi OTP qua email
3. Server -> Response 200 OK
4. Client -> POST /v1/auth/reset-password (với OTP và newPassword)
5. Server -> Đặt lại mật khẩu
6. Server -> Response 200 OK
7. Client -> POST /v1/auth/login (với mật khẩu mới)
```

### Token Refresh Flow
```
1. Client -> Gọi API với accessToken
2. Server -> Response 401 (Token expired)
3. Client -> POST /v1/auth/refresh (với refreshToken)
4. Server -> Response với accessToken & refreshToken mới
5. Client -> Retry API request với accessToken mới
```

---

## Notes

- **Rate Limiting**: Các endpoint có rate limit để bảo vệ hệ thống khỏi abuse. Vượt quá giới hạn sẽ trả về HTTP 429.
- **Token Expiry**: Access token có thời gian sống ngắn (15 phút), refresh token có thời gian sống dài hơn (7 ngày).
- **OTP Expiry**: Mã OTP có hiệu lực trong 10 phút kể từ khi được gửi.
- **Security**: Luôn sử dụng HTTPS khi gọi các API này để bảo vệ thông tin nhạy cảm.
- **Authorization Header**: Đối với các endpoint yêu cầu authentication, sử dụng format: `Authorization: Bearer {accessToken}`

---

## Example Usage (JavaScript/Fetch)

### Register
```javascript
const response = await fetch('https://api.example.com/v1/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'john_doe',
    email: 'john@example.com',
    password: 'securePassword123',
    fullName: 'John Doe'
  })
});

const result = await response.json();
console.log(result);
```

### Login
```javascript
const response = await fetch('https://api.example.com/v1/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    usernameOrEmail: 'john_doe',
    password: 'securePassword123'
  })
});

const result = await response.json();
const { accessToken, refreshToken } = result.data;

// Lưu tokens để sử dụng cho các request tiếp theo
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);
```

### Get Current User (Authenticated Request)
```javascript
const accessToken = localStorage.getItem('accessToken');

const response = await fetch('https://api.example.com/v1/auth/me', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

const result = await response.json();
console.log(result.data); // User information
```

### Refresh Token
```javascript
const refreshToken = localStorage.getItem('refreshToken');

const response = await fetch('https://api.example.com/v1/auth/refresh', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    refreshToken: refreshToken
  })
});

const result = await response.json();
const { accessToken: newAccessToken, refreshToken: newRefreshToken } = result.data;

// Cập nhật tokens
localStorage.setItem('accessToken', newAccessToken);
localStorage.setItem('refreshToken', newRefreshToken);
```

---

## Support

Nếu bạn gặp vấn đề hoặc có câu hỏi về API, vui lòng liên hệ team phát triển.

