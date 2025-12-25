# 📱 Chattrix Conversation API Documentation

Tài liệu hướng dẫn sử dụng các API liên quan đến hội thoại (Conversations) và thành viên (Members) dành cho Client.

## 🔐 Authentication & Base URL

Tất cả các API yêu cầu JWT token trong header:
- **Base URL:** `http://localhost:8080/api/v1`
- **Header:** `Authorization: Bearer <your_jwt_token>`
- **Content-Type:** `application/json`

---

## 📁 1. QUẢN LÝ HỘI THOẠI (CONVERSATION MANAGEMENT)

### 1.1 Lấy danh sách hội thoại
Lấy danh sách các cuộc hội thoại của người dùng hiện tại (có phân trang).

- **Endpoint:** `GET /conversations`
- **Query Parameters:**
    - `filter`: `all` (mặc định), `unread`, `archived`
    - `limit`: Số lượng item mỗi trang (mặc định 20)
    - `cursor`: ID của item cuối cùng từ trang trước

**Example Request:**
```bash
curl --location 'http://localhost:8080/api/v1/conversations?filter=all&limit=5' \
--header 'Authorization: Bearer <token>'
```

**Example Response:**
```json
{
    "success": true,
    "message": "Conversations retrieved successfully",
    "data": {
        "items": [
            {
                "id": 1,
                "type": "DIRECT",
                "createdAt": "2025-12-23T14:54:36.140765Z",
                "updatedAt": "2025-12-24T04:48:58.790454Z",
                "participants": [
                    {
                        "userId": 1,
                        "username": "user1",
                        "fullName": "Nguyen Linh La",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                        "role": "ADMIN",
                        "online": true,
                        "lastSeen": "2025-12-24T12:20:34.058031Z"
                    },
                    {
                        "userId": 2,
                        "username": "user2",
                        "fullName": "Tran Van Binh",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                        "role": "MEMBER",
                        "online": true,
                        "lastSeen": "2025-12-24T12:20:38.214026Z"
                    }
                ],
                "lastMessage": {
                    "id": 7,
                    "content": "asd",
                    "senderId": 1,
                    "senderUsername": "user1",
                    "senderFullName": "Nguyen Linh La",
                    "senderAvatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                    "sentAt": "2025-12-24T04:48:58.768612Z",
                    "type": "TEXT",
                    "readCount": 0,
                    "readBy": []
                },
                "unreadCount": 0
            }
        ],
        "meta": {
            "nextCursor": null,
            "hasNextPage": false,
            "itemsPerPage": 5
        }
    }
}
```

---

### 1.2 Lấy chi tiết hội thoại
Lấy thông tin chi tiết của một cuộc hội thoại cụ thể.

- **Endpoint:** `GET /conversations/{conversationId}`

**Example Request:**
```bash
curl --location 'http://localhost:8080/api/v1/conversations/1' \
--header 'Authorization: Bearer <token>'
```

**Example Response:**
```json
{
    "success": true,
    "message": "Conversation retrieved successfully",
    "data": {
        "id": 1,
        "type": "DIRECT",
        "createdAt": "2025-12-23T14:54:36.140765Z",
        "updatedAt": "2025-12-24T04:48:58.790454Z",
        "participants": [
            {
                "userId": 2,
                "username": "user2",
                "fullName": "Tran Van Binh",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                "role": "MEMBER",
                "online": true,
                "lastSeen": "2025-12-24T12:20:38.214026Z"
            },
            {
                "userId": 1,
                "username": "user1",
                "fullName": "Nguyen Linh La",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                "role": "ADMIN",
                "online": true,
                "lastSeen": "2025-12-24T12:21:34.063524Z"
            }
        ],
        "lastMessage": {
            "id": 7,
            "content": "asd",
            "senderId": 1,
            "senderUsername": "user1",
            "senderFullName": "Nguyen Linh La",
            "senderAvatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "sentAt": "2025-12-24T04:48:58.768612Z",
            "type": "TEXT"
        }
    }
}
```

---

### 1.3 Tạo hội thoại mới
Tạo cuộc hội thoại 1-1 (DIRECT) hoặc nhóm (GROUP).

- **Endpoint:** `POST /conversations`

**Example Request (DIRECT):**
```json
{
    "type": "DIRECT",
    "participantIds": [1]
}
```

**Example Response (DIRECT):**
```json
{
    "success": true,
    "message": "Conversation created successfully",
    "data": {
        "id": 4,
        "type": "DIRECT",
        "createdAt": "2025-12-24T12:23:52.772933905Z",
        "updatedAt": "2025-12-24T12:23:52.772934078Z",
        "participants": [
            {
                "userId": 2,
                "username": "user2",
                "fullName": "Tran Van Binh",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                "role": "ADMIN",
                "online": false,
                "lastSeen": "2025-12-24T12:20:38.214026Z"
            },
            {
                "userId": 1,
                "username": "user1",
                "fullName": "Nguyen Linh La",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                "role": "MEMBER",
                "online": true,
                "lastSeen": "2025-12-24T12:23:34.062943Z"
            }
        ]
    }
}
```

**Example Request (GROUP):**
```json
{
    "type": "GROUP",
    "participantIds": [1, 2, 3, 4]
}
```

**Example Response (GROUP):**
```json
{
    "success": true,
    "message": "Conversation created successfully",
    "data": {
        "id": 5,
        "type": "GROUP",
        "createdAt": "2025-12-24T12:24:20.137420604Z",
        "updatedAt": "2025-12-24T12:24:20.137420730Z",
        "participants": [
            {
                "userId": 3,
                "username": "lethihoa3",
                "fullName": "Le Thi Hoa",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376428/avatars/user_v2_3.png",
                "role": "MEMBER",
                "online": false,
                "lastSeen": "2025-12-23T14:52:32.117685Z"
            },
            {
                "userId": 2,
                "username": "user2",
                "fullName": "Tran Van Binh",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                "role": "ADMIN",
                "online": false,
                "lastSeen": "2025-12-24T12:20:38.214026Z"
            },
            {
                "userId": 1,
                "username": "user1",
                "fullName": "Nguyen Linh La",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                "role": "MEMBER",
                "online": true,
                "lastSeen": "2025-12-24T12:24:04.077043Z"
            },
            {
                "userId": 4,
                "username": "phamminhtuan4",
                "fullName": "Pham Minh Tuan",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376430/avatars/user_v2_4.png",
                "role": "MEMBER",
                "online": false,
                "lastSeen": "2025-12-23T14:52:32.117685Z"
            }
        ]
    }
}
```

---

### 1.4 Cập nhật thông tin hội thoại
Cập nhật tên hoặc mô tả của hội thoại (thường dùng cho GROUP).

- **Endpoint:** `PUT /conversations/{conversationId}`

**Example Request:**
```json
{
    "name": "Updated Group Name",
    "description": "This is our team group for project discussions"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Conversation updated successfully",
    "data": {
        "id": 5,
        "type": "GROUP",
        "name": "Updated Group Name",
        "description": "This is our team group for project discussions",
        "createdAt": "2025-12-24T12:24:20.137421Z",
        "updatedAt": "2025-12-24T12:24:20.137421Z",
        "participants": [
            {
                "userId": 4,
                "username": "phamminhtuan4",
                "fullName": "Pham Minh Tuan",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376430/avatars/user_v2_4.png",
                "role": "MEMBER",
                "online": false,
                "lastSeen": "2025-12-23T14:52:32.117685Z"
            },
            {
                "userId": 2,
                "username": "user2",
                "fullName": "Tran Van Binh",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                "role": "ADMIN",
                "online": false,
                "lastSeen": "2025-12-24T12:20:38.214026Z"
            },
            {
                "userId": 1,
                "username": "user1",
                "fullName": "Nguyen Linh La",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                "role": "MEMBER",
                "online": true,
                "lastSeen": "2025-12-24T12:25:34.040559Z"
            },
            {
                "userId": 3,
                "username": "lethihoa3",
                "fullName": "Le Thi Hoa",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376428/avatars/user_v2_3.png",
                "role": "MEMBER",
                "online": false,
                "lastSeen": "2025-12-23T14:52:32.117685Z"
            }
        ]
    }
}
```

---

### 1.5 Xóa hội thoại
Xóa hoàn toàn cuộc hội thoại.

- **Endpoint:** `DELETE /conversations/{conversationId}`

**Example Response:**
```json
{
    "success": true,
    "message": "Conversation deleted successfully"
}
```

---

## 👥 2. QUẢN LÝ THÀNH VIÊN (MEMBERS MANAGEMENT)

### 2.1 Lấy danh sách thành viên
Lấy danh sách người tham gia trong một cuộc hội thoại.

- **Endpoint:** `GET /conversations/{conversationId}/members`
- **Query Parameters:** `limit`, `cursor`

**Example Request:**
```bash
curl --location 'http://localhost:8080/api/v1/conversations/1/members?limit=5' \
--header 'Authorization: Bearer <token>'
```

**Example Response:**
```json
{
    "success": true,
    "message": "Members retrieved successfully",
    "data": {
        "items": [
            {
                "id": 2,
                "fullName": "Tran Van Binh",
                "username": "user2",
                "email": "tranvanbinh2@example.com",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                "online": false
            },
            {
                "id": 1,
                "fullName": "Nguyen Linh La",
                "username": "user1",
                "email": "nguyenlinhla1@example.com",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                "online": true
            }
        ],
        "meta": {
            "nextCursor": null,
            "hasNextPage": false,
            "itemsPerPage": 5
        }
    }
}
```

---

### 2.2 Thêm thành viên vào nhóm
Thêm một hoặc nhiều người dùng vào cuộc hội thoại nhóm.

- **Endpoint:** `POST /conversations/{conversationId}/members`

**Example Request:**
```json
{
    "userIds": [5, 6, 7]
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Members added successfully",
    "data": {
        "conversationId": 5,
        "addedMembers": [
            {
                "userId": 5,
                "username": "vothuthuy5",
                "fullName": "Vo Thu Thuy",
                "role": "MEMBER",
                "joinedAt": "2025-12-24T12:28:53.402646636Z"
            },
            {
                "userId": 6,
                "username": "dangvanthanh6",
                "fullName": "Dang Van Thanh",
                "role": "MEMBER",
                "joinedAt": "2025-12-24T12:28:53.408240145Z"
            },
            {
                "userId": 7,
                "username": "buithilan7",
                "fullName": "Bui Thi Lan",
                "role": "MEMBER",
                "joinedAt": "2025-12-24T12:28:53.411119522Z"
            }
        ]
    }
}
```

---

### 2.3 Xóa thành viên khỏi nhóm
Xóa một thành viên cụ thể khỏi nhóm (yêu cầu quyền ADMIN).

- **Endpoint:** `DELETE /conversations/{conversationId}/members/{userId}`

**Example Response:**
```json
{
    "success": true,
    "message": "Member removed successfully"
}
```

---

### 2.4 Cập nhật vai trò thành viên
Thay đổi vai trò của thành viên trong nhóm.

- **Endpoint:** `PUT /conversations/{conversationId}/members/{userId}/role`

**Example Request:**
```json
{
     "role": "ADMIN"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Member role updated successfully",
    "data": {
        "userId": 6,
        "username": "dangvanthanh6",
        "fullName": "Dang Van Thanh",
        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376433/avatars/user_v2_6.png",
        "role": "ADMIN"
    }
}
```

---

### 2.5 Rời khỏi hội thoại
Người dùng tự rời khỏi cuộc hội thoại.

- **Endpoint:** `POST /conversations/{conversationId}/members/leave`

**Example Response:**
```json
{
    "success": true,
    "message": "Left conversation successfully"
}
```

---

## 📝 Ghi chú quan trọng cho Client

1. **System Messages:** Các hành động như đổi tên nhóm, thêm/xóa người, rời nhóm sẽ tự động tạo ra các tin nhắn hệ thống (System Messages) để thông báo cho mọi người trong cuộc hội thoại.
2. **Logic Rời Nhóm (Leave Group):** 
    - Nếu Admin duy nhất rời nhóm, hệ thống sẽ tự động thăng chức cho thành viên tham gia lâu nhất làm Admin mới.
    - Nếu thành viên cuối cùng rời đi, cuộc hội thoại sẽ bị xóa hoàn toàn.
3. **Phân trang (Pagination):** Sử dụng `cursor` từ `meta.nextCursor` của response trước đó để lấy trang tiếp theo. Nếu `hasNextPage` là `false`, nghĩa là đã hết dữ liệu.
4. **Thời gian (Timestamps):** Tất cả thời gian được trả về theo định dạng ISO-8601 (UTC).
5. **Quyền hạn:** Các thao tác như xóa thành viên, cập nhật vai trò, đổi tên nhóm yêu cầu người thực hiện phải có role `ADMIN` trong cuộc hội thoại đó.

---
**Last Updated:** 2024-12-24
