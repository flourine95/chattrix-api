# 🗳️ Chattrix Social, Polls & Events API

Tài liệu hướng dẫn sử dụng các tính năng cộng đồng: Bình chọn (Polls), Sự kiện (Events), Link mời (Invite Links) và Sinh nhật.

## 🔐 Authentication & Base URL

- **Base URL:** `http://localhost:8080/api/v1`
- **Header:** `Authorization: Bearer <your_jwt_token>`
- **Content-Type:** `application/json`

---

## 📊 1. BÌNH CHỌN (POLLS)

### 1.1 Tạo cuộc bình chọn
- **Endpoint:** `POST /conversations/{conversationId}/polls`

**Example Request:**
```json
{
    "question": "cxvxxx",
    "options": [
        "Offi2ce",
        "Caf123e",
        "Pa123rk"
    ],
    "allowMultipleVotes": true,
    "expiresAt": "2025-12-30T23:59:59Z"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Poll created successfully",
    "data": {
        "id": 6,
        "question": "cxvxxx",
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T14:23:37.332887Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T14:23:37.333083Z"
        },
        "allowMultipleVotes": true,
        "expiresAt": "2025-12-30T23:59:59",
        "closed": false,
        "expired": false,
        "active": true,
        "createdAt": "2025-12-24T14:23:52.991064424",
        "totalVoters": 0,
        "options": [
            {
                "id": 13,
                "optionText": "Offi2ce",
                "optionOrder": 0,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 14,
                "optionText": "Caf123e",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 15,
                "optionText": "Pa123rk",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            }
        ],
        "currentUserVotedOptionIds": []
    }
}
```

---

### 1.2 Bình chọn (Vote)
- **Endpoint:** `POST /conversations/{conversationId}/polls/{pollId}/vote`

**Example Request:**
```json
{
   "optionIds": [1]
}
```

**Example Response (Success):**
```json
{
    "success": true,
    "message": "Vote recorded successfully",
    "data": {
        "id": 2,
        "question": "Where should we meet?",
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T14:25:07.471340Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T14:25:07.471773Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-30T23:59:59",
        "closed": false,
        "expired": false,
        "active": true,
        "createdAt": "2025-12-24T14:23:19.874346",
        "totalVoters": 0,
        "options": [
            {
                "id": 1,
                "optionText": "Office",
                "optionOrder": 0,
                "voteCount": 1,
                "percentage": 0.0,
                "voters": [
                    {
                        "id": 1,
                        "username": "user1",
                        "email": "nguyenlinhla1@example.com",
                        "emailVerified": true,
                        "phone": "0940735692",
                        "fullName": "Nguyen Linh La",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                        "bio": "Photography is my passion. 📸",
                        "gender": "MALE",
                        "profileVisibility": "PUBLIC",
                        "online": true,
                        "lastSeen": "2025-12-24T14:25:07.471340Z",
                        "createdAt": "2025-12-23T14:52:32.117685Z",
                        "updatedAt": "2025-12-24T14:25:07.471773Z"
                    }
                ]
            },
            {
                "id": 2,
                "optionText": "Cafe",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Park",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            }
        ],
        "currentUserVotedOptionIds": []
    }
}
```

**Example Response (Error - Multiple Votes):**
```json
{
    "success": false,
    "message": "This poll does not allow multiple votes",
    "code": "MULTIPLE_VOTES_NOT_ALLOWED",
    "requestId": "93267f07-b86d-4de1-b1f9-2af7d1a983e3"
}
```

---

### 1.3 Hủy bình chọn (Remove Vote)
- **Endpoint:** `DELETE /conversations/{conversationId}/polls/{pollId}/vote`

**Example Request:**
```json
{
    "optionIds": [1]
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Vote removed successfully",
    "data": {
        "id": 2,
        "question": "Where should we meet?",
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T14:34:14.749775Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T14:34:14.749912Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-30T23:59:59",
        "closed": false,
        "expired": false,
        "active": true,
        "createdAt": "2025-12-24T14:23:19.874346",
        "totalVoters": 1,
        "options": [
            {
                "id": 1,
                "optionText": "Office",
                "optionOrder": 0,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 2,
                "optionText": "Cafe",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Park",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            }
        ],
        "currentUserVotedOptionIds": [
            1
        ]
    }
}
```

---

### 1.4 Đóng bình chọn (Close Poll)
- **Endpoint:** `POST /conversations/{conversationId}/polls/{pollId}/close`

**Example Response:**
```json
{
    "success": true,
    "message": "Poll closed successfully",
    "data": {
        "id": 2,
        "question": "Where should we meet?",
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T14:34:44.797732Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T14:34:44.797842Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-30T23:59:59",
        "closed": true,
        "expired": false,
        "active": false,
        "createdAt": "2025-12-24T14:23:19.874346",
        "totalVoters": 0,
        "options": [
            {
                "id": 1,
                "optionText": "Office",
                "optionOrder": 0,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 2,
                "optionText": "Cafe",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Park",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            }
        ],
        "currentUserVotedOptionIds": []
    }
}
```

---

### 1.5 Xóa bình chọn (Delete Poll)
- **Endpoint:** `DELETE /conversations/{conversationId}/polls/{pollId}`

**Example Response:**
```json
{
    "success": true,
    "message": "Request was successful",
    "data": "Poll deleted successfully"
}
```

---

## 📅 2. SỰ KIỆN (EVENTS)

### 2.1 Tạo sự kiện
- **Endpoint:** `POST /conversations/{conversationId}/events`

**Example Request:**
```json
{
    "title": "Team123123 Mee2222ting",
    "description": "Monthl1231231y sync m2312312eeting",
    "startTime": "2025-12-25T14:00:00Z",
    "endTime": "2025-12-25T15:00:00Z",
    "location": "Office Roo123123m 301"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Event created successfully",
    "data": {
        "id": 5,
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T14:55:00.288074Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T14:55:00.288176Z"
        },
        "title": "Team123123 Mee2222ting",
        "description": "Monthl1231231y sync m2312312eeting",
        "startTime": "2025-12-25T14:00:00Z",
        "endTime": "2025-12-25T15:00:00Z",
        "location": "Office Roo123123m 301",
        "createdAt": "2025-12-24T14:55:04.233646922Z",
        "updatedAt": "2025-12-24T14:55:04.233647056Z",
        "goingCount": 0,
        "maybeCount": 0,
        "notGoingCount": 0,
        "rsvps": []
    }
}
```

---

### 2.2 Cập nhật sự kiện
- **Endpoint:** `PUT /conversations/{conversationId}/events/{eventId}`

**Example Request:**
```json
{
    "title": "Updated Team Meeting",
    "description": "Rescheduled monthly sync",
    "startTime": "2025-12-26T14:00:00Z",
    "endTime": "2025-12-26T15:00:00Z",
    "location": "Office Room 302"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Event updated successfully",
    "data": {
        "id": 5,
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T14:55:29.751905Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T14:55:29.752006Z"
        },
        "title": "Updated Team Meeting",
        "description": "Rescheduled monthly sync",
        "startTime": "2025-12-26T14:00:00Z",
        "endTime": "2025-12-26T15:00:00Z",
        "location": "Office Room 302",
        "createdAt": "2025-12-24T14:55:04.233647Z",
        "updatedAt": "2025-12-24T14:55:04.233647Z",
        "goingCount": 0,
        "maybeCount": 0,
        "notGoingCount": 0,
        "rsvps": []
    }
}
```

---

### 2.3 Phản hồi tham gia (RSVP)
- **Endpoint:** `POST /conversations/{conversationId}/events/{eventId}/rsvp`

**Example Request:**
```json
{
    "status": "GOING"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "RSVP updated successfully",
    "data": {
        "id": 1,
        "conversationId": 6,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0940735692",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Photography is my passion. 📸",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": true,
            "lastSeen": "2025-12-24T15:13:46.386208Z",
            "createdAt": "2025-12-23T14:52:32.117685Z",
            "updatedAt": "2025-12-24T15:13:46.386352Z"
        },
        "title": "Team Meeting",
        "description": "Monthly sync meeting",
        "startTime": "2025-12-25T14:00:00Z",
        "endTime": "2025-12-25T15:00:00Z",
        "location": "Office Room 301",
        "createdAt": "2025-12-24T14:54:47.038079Z",
        "updatedAt": "2025-12-24T14:54:47.038079Z",
        "goingCount": 1,
        "maybeCount": 0,
        "notGoingCount": 0,
        "currentUserRsvpStatus": "GOING",
        "rsvps": [
            {
                "id": 2,
                "user": {
                    "id": 1,
                    "username": "user1",
                    "email": "nguyenlinhla1@example.com",
                    "emailVerified": true,
                    "phone": "0940735692",
                    "fullName": "Nguyen Linh La",
                    "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                    "bio": "Photography is my passion. 📸",
                    "gender": "MALE",
                    "profileVisibility": "PUBLIC",
                    "online": true,
                    "lastSeen": "2025-12-24T15:13:46.386208Z",
                    "createdAt": "2025-12-23T14:52:32.117685Z",
                    "updatedAt": "2025-12-24T15:13:46.386352Z"
                },
                "status": "GOING",
                "createdAt": "2025-12-24T15:12:44.605807Z",
                "updatedAt": "2025-12-24T15:12:44.605807Z"
            }
        ]
    }
}
```

---

### 2.4 Lấy danh sách sự kiện
- **Endpoint:** `GET /conversations/{conversationId}/events`

**Example Response:**
```json
{
    "success": true,
    "message": "Events retrieved successfully",
    "data": [
        {
            "id": 1,
            "conversationId": 6,
            "creator": {
                "id": 1,
                "username": "user1",
                "email": "nguyenlinhla1@example.com",
                "emailVerified": true,
                "phone": "0940735692",
                "fullName": "Nguyen Linh La",
                "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                "bio": "Photography is my passion. 📸",
                "gender": "MALE",
                "profileVisibility": "PUBLIC",
                "online": false,
                "lastSeen": "2025-12-24T15:14:17.166367Z",
                "createdAt": "2025-12-23T14:52:32.117685Z",
                "updatedAt": "2025-12-24T15:14:17.166450Z"
            },
            "title": "Team Meeting",
            "description": "Monthly sync meeting",
            "startTime": "2025-12-25T14:00:00Z",
            "endTime": "2025-12-25T15:00:00Z",
            "location": "Office Room 301",
            "createdAt": "2025-12-24T14:54:47.038079Z",
            "updatedAt": "2025-12-24T14:54:47.038079Z",
            "goingCount": 1,
            "maybeCount": 0,
            "notGoingCount": 0,
            "currentUserRsvpStatus": "GOING",
            "rsvps": [
                {
                    "id": 2,
                    "user": {
                        "id": 1,
                        "username": "user1",
                        "email": "nguyenlinhla1@example.com",
                        "emailVerified": true,
                        "phone": "0940735692",
                        "fullName": "Nguyen Linh La",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                        "bio": "Photography is my passion. 📸",
                        "gender": "MALE",
                        "profileVisibility": "PUBLIC",
                        "online": false,
                        "lastSeen": "2025-12-24T15:14:17.166367Z",
                        "createdAt": "2025-12-23T14:52:32.117685Z",
                        "updatedAt": "2025-12-24T15:14:17.166450Z"
                    },
                    "status": "GOING",
                    "createdAt": "2025-12-24T15:12:44.605807Z",
                    "updatedAt": "2025-12-24T15:12:44.605807Z"
                }
            ]
        }
    ]
}
```

---

## 🔗 3. LINK MỜI VÀO NHÓM (INVITE LINKS)

### 3.1 Tạo Link mời
- **Endpoint:** `POST /conversations/{conversationId}/invite-links`

**Example Request:**
```json
{
    "expiresInDays": 23,
    "maxUses": 100
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Invite link created successfully",
    "data": {
        "id": 7,
        "token": "dafdf278b3f34fec",
        "conversationId": 6,
        "createdBy": 1,
        "createdByUsername": "user1",
        "createdAt": "2025-12-24T14:43:58.869107040Z",
        "maxUses": 100,
        "currentUses": 0,
        "revoked": false,
        "valid": true
    }
}
```

---

### 3.2 Lấy danh sách Link mời
- **Endpoint:** `GET /conversations/{conversationId}/invite-links`

**Example Response:**
```json
{
    "success": true,
    "message": "Invite links retrieved successfully",
    "data": {
        "items": [
            {
                "id": 7,
                "token": "dafdf278b3f34fec",
                "conversationId": 6,
                "createdBy": 1,
                "createdByUsername": "user1",
                "createdAt": "2025-12-24T14:43:58.869107Z",
                "maxUses": 100,
                "currentUses": 0,
                "revoked": false,
                "valid": true
            }
        ],
        "meta": {
            "nextCursor": null,
            "hasNextPage": false,
            "itemsPerPage": 20
        }
    }
}
```

---

### 3.3 Thu hồi Link mời
- **Endpoint:** `DELETE /conversations/{conversationId}/invite-links/{linkId}`

**Example Response:**
```json
{
    "success": true,
    "message": "Invite link revoked successfully",
    "data": {
        "id": 7,
        "token": "dafdf278b3f34fec",
        "conversationId": 6,
        "createdBy": 1,
        "createdByUsername": "user1",
        "createdAt": "2025-12-24T14:43:58.869107Z",
        "maxUses": 100,
        "currentUses": 0,
        "revoked": true,
        "revokedAt": "2025-12-24T14:44:56.057275485Z",
        "revokedBy": 1,
        "valid": false
    }
}
```

---

### 3.4 Lấy thông tin Link (Công khai)
- **Endpoint:** `GET /invite-links/{token}`

**Example Response:**
```json
{
    "success": true,
    "message": "Invite link info retrieved successfully",
    "data": {
        "token": "553d4feab2654ae4",
        "groupId": 6,
        "memberCount": 7,
        "valid": true,
        "createdBy": 1,
        "createdByUsername": "user1",
        "createdByFullName": "Nguyen Linh La"
    }
}
```

---

### 3.5 Tham gia nhóm qua Link
- **Endpoint:** `POST /invite-links/{token}/join`

**Example Response:**
```json
{
    "success": true,
    "message": "Successfully joined conversation via invite link",
    "data": {
        "success": true,
        "conversationId": 6,
        "message": "You are already a member of this group"
    }
}
```

---

## 🎂 4. SINH NHẬT & THÔNG BÁO (BIRTHDAYS & ANNOUNCEMENTS)

### 4.1 Sinh nhật hôm nay
- **Endpoint:** `GET /birthdays/today`

**Example Response:**
```json
{
    "success": true,
    "message": "Users with birthday today retrieved successfully",
    "data": [
        {
            "userId": 3,
            "username": "lethihoa3",
            "fullName": "Le Thi Hoa",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376428/avatars/user_v2_3.png",
            "dateOfBirth": "2025-12-24T22:29:59.524Z",
            "age": 0,
            "birthdayMessage": "Hôm nay"
        }
    ]
}
```

---

### 4.2 Gửi lời chúc sinh nhật
- **Endpoint:** `POST /birthdays/send-wishes`

**Example Request:**
```json
{
    "userId": 1,
    "conversationIds": [4],
    "customMessage": "Happy Birthday!"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Birthday wishes sent successfully",
    "data": {
        "conversationCount": 1,
        "userId": 1
    }
}
```

---

### 4.3 Tạo thông báo (Announcement)
- **Endpoint:** `POST /conversations/{conversationId}/announcements`

**Example Request:**
```json
{
    "content": "Important: Server maintenance tonight at 10 PM"
}
```

**Example Response:**
```json
{
    "success": true,
    "message": "Announcement created successfully",
    "data": {
        "id": 65,
        "conversationId": 6,
        "senderId": 2,
        "senderUsername": "user2",
        "senderFullName": "Tran Van Binh",
        "content": "Important: Server maintenance tonight at 10 PM",
        "type": "ANNOUNCEMENT",
        "reactions": {},
        "sentAt": "2025-12-24T15:28:12.582625855Z",
        "createdAt": "2025-12-24T15:28:12.582626240Z",
        "updatedAt": "2025-12-24T15:28:12.582626423Z",
        "edited": false,
        "deleted": false,
        "forwarded": false,
        "forwardCount": 0,
        "pinned": false,
        "scheduled": false
    }
}
```

---

## 👥 5. NHÓM CHUNG (MUTUAL GROUPS)

### 5.1 Lấy danh sách nhóm chung
- **Endpoint:** `GET /users/{userId}/mutual-groups`

**Example Response:**
```json
{
    "success": true,
    "message": "Mutual groups retrieved successfully",
    "data": [
        {
            "id": 6,
            "type": "GROUP",
            "createdAt": "2025-12-24T12:52:21.961632Z",
            "updatedAt": "2025-12-24T12:54:02.325720Z",
            "participants": [
                {
                    "userId": 1,
                    "username": "user1",
                    "fullName": "Nguyen Linh La",
                    "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                    "role": "MEMBER",
                    "online": true,
                    "lastSeen": "2025-12-24T15:26:53.226178Z"
                },
                {
                    "userId": 2,
                    "username": "user2",
                    "fullName": "Tran Van Binh",
                    "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376427/avatars/user_v2_2.png",
                    "role": "ADMIN",
                    "online": false,
                    "lastSeen": "2025-12-24T12:32:50.205616Z"
                }
            ]
        }
    ]
}
```

---

## 📝 Ghi chú cho Client

1. **Poll Options:** Khi hiển thị Poll, hãy dùng `percentage` để vẽ biểu đồ tỉ lệ và `voters` để hiển thị avatar những người đã chọn.
2. **RSVP Status:** Luôn kiểm tra `currentUserRsvpStatus` trong Event response để hiển thị trạng thái hiện tại của người dùng.
3. **Invite QR:** Endpoint QR trả về binary ảnh, Client có thể gán trực tiếp vào thẻ `<img>` src.
4. **Birthday Wishes:** Tính năng `send-wishes` sẽ tự động gửi tin nhắn vào các cuộc hội thoại được chỉ định.

---
**Last Updated:** 2024-12-24
