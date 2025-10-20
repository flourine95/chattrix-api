# Tài liệu API Chattrix

Đây là tài liệu mô tả cách sử dụng các API của Chattrix, bao gồm cả RESTful API và WebSocket API.

**URL cơ sở:** `/api`

**Xác thực:** Hầu hết các endpoint yêu cầu xác thực bằng JWT. Client cần gửi token trong header `Authorization` theo định dạng `Bearer <JWT_TOKEN>`.

---

## 1. Conversations API (`/v1/conversations`)

Tài nguyên này quản lý các cuộc trò chuyện.

### `POST /v1/conversations`

Tạo một cuộc trò chuyện mới (trực tiếp hoặc nhóm).

- **Method:** `POST`
- **Path:** `/v1/conversations`
- **Authentication:** Yêu cầu (Bearer Token).
- **Request Body:**

  ```json
  {
    "name": "string (tùy chọn, cho chat nhóm)",
    "type": "string ('DIRECT' hoặc 'GROUP')",
    "participantIds": ["uuid", "uuid", "..."]
  }
  ```

- **Success Response (`201 CREATED`):**

  ```json
  {
    "success": true,
    "message": "Conversation created successfully",
    "data": {
      "id": "uuid",
      "name": "string",
      "type": "string",
      "createdAt": "datetime",
      "updatedAt": "datetime",
      "participants": [
        {
          "userId": "uuid",
          "username": "string",
          "fullName": "string",
          "role": "string ('ADMIN' hoặc 'MEMBER')"
        }
      ]
    }
  }
  ```

- **Error Responses:**
  - `400 BAD_REQUEST`: Dữ liệu không hợp lệ (ví dụ: thiếu `participantIds`).
  - `401 UNAUTHORIZED`: Token không hợp lệ hoặc bị thiếu.

### `GET /v1/conversations`

Lấy danh sách tất cả các cuộc trò chuyện của người dùng hiện tại.

- **Method:** `GET`
- **Path:** `/v1/conversations`
- **Authentication:** Yêu cầu (Bearer Token).
- **Success Response (`200 OK`):**

  ```json
  {
    "success": true,
    "message": "Conversations retrieved successfully",
    "data": [
      {
        "id": "uuid",
        "name": "string",
        "type": "string",
        "createdAt": "datetime",
        "updatedAt": "datetime",
        "participants": [
          {
            "userId": "uuid",
            "username": "string",
            "fullName": "string",
            "role": "string"
          }
        ]
      }
    ]
  }
  ```

- **Error Responses:**
  - `401 UNAUTHORIZED`: Token không hợp lệ hoặc bị thiếu.
  - `404 NOT_FOUND`: Không tìm thấy người dùng.

### `GET /v1/conversations/{conversationId}`

Lấy thông tin chi tiết của một cuộc trò chuyện.

- **Method:** `GET`
- **Path:** `/v1/conversations/{conversationId}`
- **Authentication:** Yêu cầu (Bearer Token).
- **Path Parameters:**
  - `conversationId` (uuid): ID của cuộc trò chuyện.
- **Success Response (`200 OK`):**

  ```json
  {
    "success": true,
    "message": "Conversation retrieved successfully",
    "data": {
      "id": "uuid",
      "name": "string",
      "type": "string",
      "createdAt": "datetime",
      "updatedAt": "datetime",
      "participants": [
        {
          "userId": "uuid",
          "username": "string",
          "fullName": "string",
          "role": "string"
        }
      ]
    }
  }
  ```

- **Error Responses:**
  - `401 UNAUTHORIZED`: Token không hợp lệ hoặc bị thiếu.
  - `403 FORBIDDEN`: Người dùng không có quyền truy cập cuộc trò chuyện này.
  - `404 NOT_FOUND`: Không tìm thấy cuộc trò chuyện hoặc người dùng.

---

## 2. Messages API (`/v1/conversations/{conversationId}/messages`)

Tài nguyên này quản lý các tin nhắn trong một cuộc trò chuyện.

### `GET /v1/conversations/{conversationId}/messages`

Lấy danh sách tin nhắn trong một cuộc trò chuyện.

- **Method:** `GET`
- **Path:** `/v1/conversations/{conversationId}/messages`
- **Authentication:** Yêu cầu (Bearer Token).
- **Path Parameters:**
  - `conversationId` (uuid): ID của cuộc trò chuyện.
- **Query Parameters:**
  - `page` (int, optional, default: `0`): Số trang.
  - `size` (int, optional, default: `50`): Số lượng tin nhắn mỗi trang.
- **Success Response (`200 OK`):**

  ```json
  {
    "success": true,
    "message": "Messages retrieved successfully",
    "data": [
      {
        "id": "uuid",
        "content": "string",
        "type": "string",
        "createdAt": "datetime",
        "sender": {
          "id": "uuid",
          "username": "string",
          "fullName": "string"
        }
      }
    ]
  }
  ```

- **Error Responses:**
  - `401 UNAUTHORIZED`: Token không hợp lệ hoặc bị thiếu.
  - `403 FORBIDDEN`: Người dùng không có quyền truy cập cuộc trò chuyện này.
  - `404 NOT_FOUND`: Không tìm thấy cuộc trò chuyện hoặc người dùng.

---

## 3. User Status API (`/v1/users/status`)

Tài nguyên này cung cấp thông tin về trạng thái trực tuyến của người dùng.

### `GET /v1/users/status/online`

Lấy danh sách tất cả người dùng đang trực tuyến.

- **Method:** `GET`
- **Path:** `/v1/users/status/online`
- **Authentication:** Yêu cầu (Bearer Token).
- **Success Response (`200 OK`):**

  ```json
  {
    "success": true,
    "message": "Online users retrieved successfully",
    "data": [
      {
        "id": "uuid",
        "username": "string",
        "fullName": "string",
        "email": "string",
        "createdAt": "datetime"
      }
    ]
  }
  ```

### `GET /v1/users/status/online/conversation/{conversationId}`

Lấy danh sách người dùng đang trực tuyến trong một cuộc trò chuyện cụ thể.

- **Method:** `GET`
- **Path:** `/v1/users/status/online/conversation/{conversationId}`
- **Authentication:** Yêu cầu (Bearer Token).
- **Path Parameters:**
  - `conversationId` (uuid): ID của cuộc trò chuyện.
- **Success Response (`200 OK`):**

  ```json
  {
    "success": true,
    "message": "Online users in conversation retrieved successfully",
    "data": [
      {
        "id": "uuid",
        "username": "string",
        "fullName": "string",
        "email": "string",
        "createdAt": "datetime"
      }
    ]
  }
  ```

### `GET /v1/users/status/{userId}`

Kiểm tra trạng thái của một người dùng cụ thể.

- **Method:** `GET`
- **Path:** `/v1/users/status/{userId}`
- **Authentication:** Yêu cầu (Bearer Token).
- **Path Parameters:**
  - `userId` (uuid): ID của người dùng.
- **Success Response (`200 OK`):**

  ```json
  {
    "success": true,
    "message": "User status retrieved successfully",
    "data": {
      "userId": "uuid",
      "isOnline": true,
      "activeSessionCount": 1
    }
  }
  ```

---

## 4. Chat WebSocket API (`/v1/chat`)

Giao tiếp thời gian thực cho việc chat, chỉ báo gõ phím và trạng thái người dùng.

### Kết nối

- **URL:** `ws://<your-server-address>/v1/chat?token=<JWT_TOKEN>`
- **Phương thức:** `GET`
- Client phải cung cấp JWT token hợp lệ qua query parameter `token` để xác thực khi kết nối.

### Tin nhắn từ Client đến Server

Client gửi tin nhắn đến server dưới dạng một đối tượng JSON có cấu trúc:

```json
{
  "type": "string",
  "payload": {}
}
```

- **`type`**: Loại sự kiện.
- **`payload`**: Dữ liệu của sự kiện.

#### Gửi tin nhắn
- **`type`**: `chat.message`
- **`payload`**:
  ```json
  {
    "conversationId": "uuid",
    "content": "string"
  }
  ```

#### Bắt đầu gõ phím
- **`type`**: `typing.start`
- **`payload`**:
  ```json
  {
    "conversationId": "uuid"
  }
  ```

#### Dừng gõ phím
- **`type`**: `typing.stop`
- **`payload`**:
  ```json
  {
    "conversationId": "uuid"
  }
  ```

### Tin nhắn từ Server đến Client

Server gửi tin nhắn đến client với cùng cấu trúc `{"type": "...", "payload": ...}`.

#### Tin nhắn mới
- **`type`**: `chat.message`
- **`payload`**:
  ```json
  {
    "id": "uuid",
    "content": "string",
    "type": "TEXT",
    "createdAt": "datetime",
    "conversationId": "uuid",
    "sender": {
      "id": "uuid",
      "username": "string",
      "fullName": "string"
    }
  }
  ```

#### Chỉ báo gõ phím
- **`type`**: `typing.indicator`
- **`payload`**:
  ```json
  {
    "conversationId": "uuid",
    "typingUsers": [
      {
        "id": "uuid",
        "username": "string",
        "fullName": "string"
      }
    ]
  }
  ```
  - `typingUsers` là danh sách những người dùng *khác* đang gõ phím trong cuộc trò chuyện.

#### Cập nhật trạng thái người dùng
- **`type`**: `user.status`
- **`payload`**:
  ```json
  {
    "userId": "uuid",
    "username": "string",
    "displayName": "string",
    "isOnline": true,
    "lastSeen": "datetime"
  }
  ```
  - Sự kiện này được phát đến tất cả các client được kết nối khi một người dùng kết nối hoặc ngắt kết nối.

---
*Lưu ý: Các endpoint trong `TypingIndicatorResource.java` được đánh dấu là "Test endpoint" và có vẻ không dành cho client chính thức sử dụng, vì các chức năng này được xử lý qua WebSocket. Do đó, chúng không được đưa vào tài liệu này.*

