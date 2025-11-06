# Chat Info API - Đề xuất API mới cho tính năng thông tin hội thoại

## 📋 Tổng quan

Tài liệu này đề xuất các API endpoints cần thiết để hỗ trợ tính năng **Chat Info** (Thông tin hội thoại) trong ứng dụng Chattrix.

---

## 🔧 API Endpoints cần bổ sung

### 1. Conversation Management API

#### 1.1. Cập nhật thông tin conversation
**Endpoint:** `PUT /v1/conversations/{conversationId}`

**Headers:**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "New Group Name",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Conversation updated successfully",
  "data": {
    "id": 10,
    "name": "New Group Name",
    "avatarUrl": "https://example.com/avatar.jpg",
    "type": "GROUP",
    "updatedAt": "2025-11-05T10:30:00.000Z"
  }
}
```

---

#### 1.2. Xóa conversation
**Endpoint:** `DELETE /v1/conversations/{conversationId}`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Conversation deleted successfully",
  "data": null
}
```

**Note:** Soft delete - conversation vẫn tồn tại trong database nhưng bị đánh dấu là deleted

---

#### 1.3. Rời khỏi conversation (GROUP)
**Endpoint:** `POST /v1/conversations/{conversationId}/leave`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Left conversation successfully",
  "data": null
}
```

---

### 2. Group Members Management API

#### 2.1. Thêm thành viên vào group
**Endpoint:** `POST /v1/conversations/{conversationId}/members`

**Request Body:**
```json
{
  "userIds": [5, 7, 9]
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Members added successfully",
  "data": {
    "conversationId": 10,
    "addedMembers": [
      {
        "userId": 5,
        "username": "alice_wonder",
        "fullName": "Alice Wonder",
        "role": "MEMBER",
        "joinedAt": "2025-11-05T10:30:00.000Z"
      }
    ]
  }
}
```

---

#### 2.2. Xóa thành viên khỏi group
**Endpoint:** `DELETE /v1/conversations/{conversationId}/members/{userId}`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Member removed successfully",
  "data": null
}
```

**Note:** Chỉ ADMIN mới có quyền xóa thành viên

---

#### 2.3. Cập nhật role thành viên
**Endpoint:** `PUT /v1/conversations/{conversationId}/members/{userId}/role`

**Request Body:**
```json
{
  "role": "ADMIN"
}
```

**Allowed values:** `ADMIN`, `MEMBER`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Member role updated successfully",
  "data": {
    "userId": 5,
    "role": "ADMIN",
    "updatedAt": "2025-11-05T10:30:00.000Z"
  }
}
```

---

#### 3.4. Block/Unblock user (DIRECT chat only)
**Endpoint:** `POST /v1/conversations/{conversationId}/block`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "User blocked successfully",
  "data": {
    "isBlocked": true,
    "blockedAt": "2025-11-05T10:30:00.000Z"
  }
}
```

**Endpoint:** `POST /v1/conversations/{conversationId}/unblock`

**Response:** `200 OK`

**Note:** Chỉ áp dụng cho DIRECT conversation

---

### 4. Message Search & Filter API

#### 4.1. Tìm kiếm messages trong conversation
**Endpoint:** `GET /v1/conversations/{conversationId}/messages/search`

**Query Parameters:**
- `query` (optional): Từ khóa tìm kiếm trong nội dung tin nhắn
- `type` (optional): Loại tin nhắn - `TEXT`, `IMAGE`, `VIDEO`, `AUDIO`, `DOCUMENT`, `LOCATION`
- `senderId` (optional): ID người gửi
- `fromDate` (optional): Tìm từ ngày (ISO 8601)
- `toDate` (optional): Tìm đến ngày (ISO 8601)
- `page` (optional): Số trang (mặc định: 0)
- `size` (optional): Số lượng kết quả (mặc định: 20)
- `sort` (optional): `ASC` hoặc `DESC` (mặc định: DESC)

**Example:**
```
GET /v1/conversations/10/messages/search?query=hello&type=TEXT&page=0&size=20
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Messages found successfully",
  "data": [
    {
      "id": 123,
      "content": "Hello everyone!",
      "type": "TEXT",
      "senderId": 1,
      "senderUsername": "john_doe",
      "sentAt": "2025-11-05T10:30:00.000Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

---

#### 4.2. Lấy media files trong conversation
**Endpoint:** `GET /v1/conversations/{conversationId}/media`

**Query Parameters:**
- `type` (optional): `IMAGE`, `VIDEO`, `AUDIO`, `DOCUMENT` (mặc định: tất cả)
- `page` (optional): Số trang
- `size` (optional): Số lượng kết quả

**Example:**
```
GET /v1/conversations/10/media?type=IMAGE&page=0&size=30
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Media retrieved successfully",
  "data": [
    {
      "id": 124,
      "type": "IMAGE",
      "mediaUrl": "https://example.com/image.jpg",
      "thumbnailUrl": "https://example.com/thumb.jpg",
      "fileName": "photo.jpg",
      "fileSize": 2048576,
      "senderId": 1,
      "senderUsername": "john_doe",
      "sentAt": "2025-11-05T10:30:00.000Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 30,
    "totalElements": 150,
    "totalPages": 5
  }
}
```

---

#### 4.3. Lấy shared links trong conversation
**Endpoint:** `GET /v1/conversations/{conversationId}/links`

**Query Parameters:**
- `page` (optional): Số trang
- `size` (optional): Số lượng kết quả

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Links retrieved successfully",
  "data": [
    {
      "id": 125,
      "content": "Check this out: https://example.com",
      "extractedUrl": "https://example.com",
      "urlTitle": "Example Website",
      "urlDescription": "An example website",
      "urlImage": "https://example.com/og-image.jpg",
      "senderId": 1,
      "sentAt": "2025-11-05T10:30:00.000Z"
    }
  ]
}
```

---

## 🎯 Use Cases

### 1. Xem thông tin hội thoại
- Hiển thị avatar, tên, số thành viên (group)
- Hiển thị trạng thái online/offline (direct)
- Quick actions: Call, Video call, Mute

### 2. Quản lý media
- Xem tất cả ảnh, video, file, audio đã chia sẻ
- Filter theo loại media
- Click để xem full screen
- Download media

### 3. Tìm kiếm tin nhắn
- Tìm kiếm theo nội dung
- Filter theo loại tin nhắn
- Filter theo người gửi
- Filter theo thời gian
- Sort theo thời gian
- Click để jump đến tin nhắn trong chat

### 4. Quản lý thành viên (Group)
- Xem danh sách thành viên
- Tìm kiếm thành viên
- Thêm thành viên mới (Admin)
- Xóa thành viên (Admin)
- Đặt/gỡ quyền Admin (Admin)

### 5. Cài đặt
- Tùy chỉnh theme, màu sắc
- Đặt biệt danh (Direct)
- Bật/tắt thông báo
- Mute conversation
- Block user (Direct)
- Báo cáo spam/abuse
- Rời nhóm (Group)
- Xóa cuộc trò chuyện

---

## 🔐 Permissions & Authorization

### Admin permissions (GROUP only):
- Thêm/xóa thành viên
- Đổi tên nhóm
- Đổi avatar nhóm
- Đặt/gỡ quyền Admin cho thành viên khác

### Member permissions:
- Xem thông tin nhóm
- Rời khỏi nhóm
- Tìm kiếm tin nhắn
- Xem media

### Direct chat permissions:
- Cả hai người dùng có quyền như nhau
- Block/unblock
- Xóa cuộc trò chuyện (chỉ xóa ở phía mình)

---

## 📱 WebSocket Events (Optional)

Để real-time updates, có thể thêm các WebSocket events:

### Group member changes
```json
{
  "type": "MEMBER_ADDED",
  "data": {
    "conversationId": 10,
    "member": {
      "userId": 5,
      "username": "alice_wonder",
      "fullName": "Alice Wonder",
      "role": "MEMBER"
    }
  }
}
```

### Conversation settings changed
```json
{
  "type": "CONVERSATION_UPDATED",
  "data": {
    "conversationId": 10,
    "name": "New Group Name",
    "updatedAt": "2025-11-05T10:30:00.000Z"
  }
}
```

---

## 🚀 Implementation Priority

### Phase 1 (High Priority):
1. ✅ Chat Info Page UI
2. ✅ Media Grid Widget
3. ✅ Message Search Widget
4. ✅ Settings Section
5. ✅ Members Management (Group)
6. 🔄 API Integration (cần backend implement)

### Phase 2 (Medium Priority):
- Mute/Unmute conversation
- Block/Unblock user
- Delete conversation
- Leave group

### Phase 3 (Low Priority):
- Theme customization
- Nickname for contacts
- Report spam/abuse
- Shared links viewer

---

## 📝 Notes

- Tất cả API endpoints đều yêu cầu authentication (Bearer Token)
- Pagination sử dụng page-based (page, size)
- Timestamps theo format ISO 8601 UTC
- File sizes tính bằng bytes
- Cần validate permissions trước khi thực hiện actions (Admin-only features)

---

**Version:** 1.0.0
**Created:** 2025-11-05
**Author:** Chattrix Development Team

### 3. Conversation Settings API

#### 3.1. Lấy settings của conversation
**Endpoint:** `GET /v1/conversations/{conversationId}/settings`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Settings retrieved successfully",
  "data": {
    "conversationId": 10,
    "isMuted": false,
    "mutedUntil": null,
    "isBlocked": false,
    "notificationsEnabled": true,
    "customNickname": "Alice",
    "theme": "blue"
  }
}
```

---

#### 3.2. Cập nhật settings
**Endpoint:** `PUT /v1/conversations/{conversationId}/settings`

**Request Body:**
```json
{
  "notificationsEnabled": false,
  "customNickname": "Alice Wonderland",
  "theme": "purple"
}
```

**Response:** `200 OK`

---

#### 3.3. Mute/Unmute conversation
**Endpoint:** `POST /v1/conversations/{conversationId}/mute`

**Request Body:**
```json
{
  "duration": 3600
}
```

**Parameters:**
- `duration`: Thời gian mute (giây). `null` hoặc `0` = unmute
- Ví dụ: 3600 = 1 giờ, 86400 = 1 ngày, -1 = mute vĩnh viễn

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Conversation muted successfully",
  "data": {
    "isMuted": true,
    "mutedUntil": "2025-11-05T11:30:00.000Z"
  }
}
```

---

