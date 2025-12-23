# Poll API - Hướng dẫn sử dụng cho Client

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
Tất cả endpoints yêu cầu JWT token trong header:
```
Authorization: Bearer <access_token>
```

---

## 1. Tạo Poll

**Endpoint:** `POST /polls/conversation/{conversationId}`

**Request Body:**
```json
{
  "question": "Chúng ta đi ăn gì hôm nay?",
  "options": [
    "Phở",
    "Bún bò",
    "Cơm tấm",
    "Bánh mì"
  ],
  "allowMultipleVotes": false,
  "expiresAt": "2025-12-25T18:00:00"
}
```

**Validation:**
- `question`: 1-500 ký tự, bắt buộc
- `options`: 2-10 options, mỗi option 1-200 ký tự, bắt buộc
- `allowMultipleVotes`: true/false, bắt buộc
- `expiresAt`: ISO datetime, optional (null = không hết hạn)

**Response:** `201 Created`
```json
{
    "success": true,
    "message": "Poll created successfully",
    "data": {
        "id": 1,
        "question": "Chúng ta đi ăn gì hôm nay?",
        "conversationId": 1,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0914184231",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Just another day in paradise. 🌴",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": false,
            "lastSeen": "2025-12-23T01:48:49.597433Z",
            "createdAt": "2025-12-22T04:07:50.413789Z",
            "updatedAt": "2025-12-23T01:51:02.109311Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-25T18:00:00",
        "isClosed": false,
        "isExpired": false,
        "isActive": true,
        "createdAt": "2025-12-23T01:59:03.514285431",
        "totalVoters": 0,
        "options": [
            {
                "id": 1,
                "optionText": "Phở",
                "optionOrder": 0,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 2,
                "optionText": "Bún bò",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Cơm tấm",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 4,
                "optionText": "Bánh mì",
                "optionOrder": 3,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            }
        ],
        "currentUserVotedOptionIds": []
    }
}
```

**WebSocket Event:** Gửi đến tất cả participants
```json
{
  "type": "POLL_CREATED",
  "poll": { /* PollResponse object */ }
}
```

---

## 2. Vote cho Poll

**Endpoint:** `POST /polls/{pollId}/vote`

**Request Body:**
```json
{
  "optionIds": [1]
}
```

**Multiple choice (nếu allowMultipleVotes = true):**
```json
{
  "optionIds": [1, 3, 4]
}
```

**Validation:**
- `optionIds`: Array không rỗng, bắt buộc
- Nếu `allowMultipleVotes = false`: chỉ được 1 optionId
- Nếu `allowMultipleVotes = true`: được nhiều optionIds

**Response:** `200 OK`
```json
{
    "success": true,
    "message": "Vote recorded successfully",
    "data": {
        "id": 1,
        "question": "Chúng ta đi ăn gì hôm nay?",
        "conversationId": 1,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0914184231",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Just another day in paradise. 🌴",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": false,
            "lastSeen": "2025-12-23T01:48:49.597433Z",
            "createdAt": "2025-12-22T04:07:50.413789Z",
            "updatedAt": "2025-12-23T01:51:02.109311Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-25T18:00:00",
        "isClosed": false,
        "isExpired": false,
        "isActive": true,
        "createdAt": "2025-12-23T01:59:03.514285",
        "totalVoters": 1,
        "options": [
            {
                "id": 1,
                "optionText": "Phở",
                "optionOrder": 0,
                "voteCount": 1,
                "percentage": 100.0,
                "voters": [
                    {
                        "id": 1,
                        "username": "user1",
                        "email": "nguyenlinhla1@example.com",
                        "emailVerified": true,
                        "phone": "0914184231",
                        "fullName": "Nguyen Linh La",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                        "bio": "Just another day in paradise. 🌴",
                        "gender": "MALE",
                        "profileVisibility": "PUBLIC",
                        "online": false,
                        "lastSeen": "2025-12-23T01:48:49.597433Z",
                        "createdAt": "2025-12-22T04:07:50.413789Z",
                        "updatedAt": "2025-12-23T01:51:02.109311Z"
                    }
                ]
            },
            {
                "id": 2,
                "optionText": "Bún bò",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Cơm tấm",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 4,
                "optionText": "Bánh mì",
                "optionOrder": 3,
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

**WebSocket Event:**
```json
{
  "type": "POLL_VOTED",
  "poll": { /* Updated PollResponse */ }
}
```

**Lưu ý:**
- Vote lại sẽ thay thế vote cũ
- Không vote được nếu poll đã closed hoặc expired

---

## 3. Xóa Vote

**Endpoint:** `DELETE /polls/{pollId}/vote`

**Request Body:** Không cần

**Response:** `200 OK`
```json
{
    "success": true,
    "message": "Vote removed successfully",
    "data": {
        "id": 1,
        "question": "Chúng ta đi ăn gì hôm nay?",
        "conversationId": 1,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0914184231",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Just another day in paradise. 🌴",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": false,
            "lastSeen": "2025-12-23T01:48:49.597433Z",
            "createdAt": "2025-12-22T04:07:50.413789Z",
            "updatedAt": "2025-12-23T01:51:02.109311Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-25T18:00:00",
        "isClosed": false,
        "isExpired": false,
        "isActive": true,
        "createdAt": "2025-12-23T01:59:03.514285",
        "totalVoters": 0,
        "options": [
            {
                "id": 1,
                "optionText": "Phở",
                "optionOrder": 0,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 2,
                "optionText": "Bún bò",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Cơm tấm",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 4,
                "optionText": "Bánh mì",
                "optionOrder": 3,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            }
        ],
        "currentUserVotedOptionIds": []
    }
}
```

**WebSocket Event:**
```json
{
  "type": "POLL_VOTED",
  "poll": { /* Updated PollResponse */ }
}
```

---

## 4. Xem Chi tiết Poll

**Endpoint:** `GET /polls/{pollId}`

**Response:** `200 OK`
```json
{
    "success": true,
    "message": "Poll retrieved successfully",
    "data": {
        "id": 1,
        "question": "Chúng ta đi ăn gì hôm nay?",
        "conversationId": 1,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0914184231",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Just another day in paradise. 🌴",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": false,
            "lastSeen": "2025-12-23T01:48:49.597433Z",
            "createdAt": "2025-12-22T04:07:50.413789Z",
            "updatedAt": "2025-12-23T01:51:02.109311Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-25T18:00:00",
        "isClosed": false,
        "isExpired": false,
        "isActive": true,
        "createdAt": "2025-12-23T01:59:03.514285",
        "totalVoters": 1,
        "options": [
            {
                "id": 1,
                "optionText": "Phở",
                "optionOrder": 0,
                "voteCount": 1,
                "percentage": 100.0,
                "voters": [
                    {
                        "id": 1,
                        "username": "user1",
                        "email": "nguyenlinhla1@example.com",
                        "emailVerified": true,
                        "phone": "0914184231",
                        "fullName": "Nguyen Linh La",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                        "bio": "Just another day in paradise. 🌴",
                        "gender": "MALE",
                        "profileVisibility": "PUBLIC",
                        "online": false,
                        "lastSeen": "2025-12-23T01:48:49.597433Z",
                        "createdAt": "2025-12-22T04:07:50.413789Z",
                        "updatedAt": "2025-12-23T01:51:02.109311Z"
                    }
                ]
            },
            {
                "id": 2,
                "optionText": "Bún bò",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Cơm tấm",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 4,
                "optionText": "Bánh mì",
                "optionOrder": 3,
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

## 5. List Polls trong Conversation

**Endpoint:** `GET /polls/conversation/{conversationId}?page=0&size=20`

**Query Parameters:**
- `page`: Số trang (default: 0)
- `size`: Số items/trang (default: 20)

**Response:** `200 OK`
```json
{
    "success": true,
    "message": "Polls retrieved successfully",
    "data": {
        "data": [
            {
                "id": 1,
                "question": "Chúng ta đi ăn gì hôm nay?",
                "conversationId": 1,
                "creator": {
                    "id": 1,
                    "username": "user1",
                    "email": "nguyenlinhla1@example.com",
                    "emailVerified": true,
                    "phone": "0914184231",
                    "fullName": "Nguyen Linh La",
                    "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                    "bio": "Just another day in paradise. 🌴",
                    "gender": "MALE",
                    "profileVisibility": "PUBLIC",
                    "online": false,
                    "lastSeen": "2025-12-23T01:48:49.597433Z",
                    "createdAt": "2025-12-22T04:07:50.413789Z",
                    "updatedAt": "2025-12-23T01:51:02.109311Z"
                },
                "allowMultipleVotes": false,
                "expiresAt": "2025-12-25T18:00:00",
                "isClosed": false,
                "isExpired": false,
                "isActive": true,
                "createdAt": "2025-12-23T01:59:03.514285",
                "totalVoters": 0,
                "options": [
                    {
                        "id": 1,
                        "optionText": "Phở",
                        "optionOrder": 0,
                        "voteCount": 0,
                        "percentage": 0.0,
                        "voters": []
                    },
                    {
                        "id": 2,
                        "optionText": "Bún bò",
                        "optionOrder": 1,
                        "voteCount": 0,
                        "percentage": 0.0,
                        "voters": []
                    },
                    {
                        "id": 3,
                        "optionText": "Cơm tấm",
                        "optionOrder": 2,
                        "voteCount": 0,
                        "percentage": 0.0,
                        "voters": []
                    },
                    {
                        "id": 4,
                        "optionText": "Bánh mì",
                        "optionOrder": 3,
                        "voteCount": 0,
                        "percentage": 0.0,
                        "voters": []
                    }
                ],
                "currentUserVotedOptionIds": []
            }
        ],
        "page": 0,
        "size": 20,
        "total": 1,
        "totalPages": 1,
        "hasNextPage": false,
        "hasPrevPage": false
    }
}
```

**Lưu ý:** Polls được sắp xếp theo `createdAt` giảm dần (mới nhất trước)

---

## 6. Đóng Poll (Creator only)

**Endpoint:** `POST /polls/{pollId}/close`

**Request Body:** Không cần

**Response:** `200 OK`
```json
{
    "success": true,
    "message": "Poll closed successfully",
    "data": {
        "id": 1,
        "question": "Chúng ta đi ăn gì hôm nay?",
        "conversationId": 1,
        "creator": {
            "id": 1,
            "username": "user1",
            "email": "nguyenlinhla1@example.com",
            "emailVerified": true,
            "phone": "0914184231",
            "fullName": "Nguyen Linh La",
            "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
            "bio": "Just another day in paradise. 🌴",
            "gender": "MALE",
            "profileVisibility": "PUBLIC",
            "online": false,
            "lastSeen": "2025-12-23T01:48:49.597433Z",
            "createdAt": "2025-12-22T04:07:50.413789Z",
            "updatedAt": "2025-12-23T01:51:02.109311Z"
        },
        "allowMultipleVotes": false,
        "expiresAt": "2025-12-25T18:00:00",
        "isClosed": true,
        "isExpired": false,
        "isActive": false,
        "createdAt": "2025-12-23T01:59:03.514285",
        "totalVoters": 1,
        "options": [
            {
                "id": 1,
                "optionText": "Phở",
                "optionOrder": 0,
                "voteCount": 1,
                "percentage": 100.0,
                "voters": [
                    {
                        "id": 1,
                        "username": "user1",
                        "email": "nguyenlinhla1@example.com",
                        "emailVerified": true,
                        "phone": "0914184231",
                        "fullName": "Nguyen Linh La",
                        "avatarUrl": "https://res.cloudinary.com/dk3gud5kq/image/upload/v1766376425/avatars/user_v2_1.png",
                        "bio": "Just another day in paradise. 🌴",
                        "gender": "MALE",
                        "profileVisibility": "PUBLIC",
                        "online": false,
                        "lastSeen": "2025-12-23T01:48:49.597433Z",
                        "createdAt": "2025-12-22T04:07:50.413789Z",
                        "updatedAt": "2025-12-23T01:51:02.109311Z"
                    }
                ]
            },
            {
                "id": 2,
                "optionText": "Bún bò",
                "optionOrder": 1,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 3,
                "optionText": "Cơm tấm",
                "optionOrder": 2,
                "voteCount": 0,
                "percentage": 0.0,
                "voters": []
            },
            {
                "id": 4,
                "optionText": "Bánh mì",
                "optionOrder": 3,
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

**WebSocket Event:**
```json
{
  "type": "POLL_CLOSED",
  "poll": { /* Updated PollResponse */ }
}
```

**Lưu ý:** Chỉ creator mới đóng được poll

---

## 7. Xóa Poll (Creator only)

**Endpoint:** `DELETE /polls/{pollId}`

**Request Body:** Không cần

**Response:** `200 OK`
```json
{
    "success": true,
    "message": "Request was successful",
    "data": "Poll deleted successfully"
}
```

**WebSocket Event:**
```json
{
  "type": "POLL_DELETED",
  "poll": {
    "id": 1,
    "conversationId": 5
  }
}
```

**Lưu ý:** 
- Chỉ creator mới xóa được poll
- Xóa poll sẽ xóa tất cả votes

---

## WebSocket Events

Client cần lắng nghe các event types sau:

### Event Types:
- `POLL_CREATED` - Poll mới được tạo
- `POLL_VOTED` - Có người vote/thay đổi vote/xóa vote
- `POLL_CLOSED` - Poll bị đóng
- `POLL_DELETED` - Poll bị xóa

### Event Structure:
```json
{
  "type": "POLL_CREATED | POLL_VOTED | POLL_CLOSED | POLL_DELETED",
  "poll": {
    /* PollResponse object với data đầy đủ */
  }
}
```

### Xử lý Events:
```javascript
// Pseudo code
websocket.on('message', (event) => {
  const data = JSON.parse(event.data);
  
  switch(data.type) {
    case 'POLL_CREATED':
      // Thêm poll mới vào list
      addPollToList(data.poll);
      break;
      
    case 'POLL_VOTED':
      // Update vote counts và percentages
      updatePollResults(data.poll);
      break;
      
    case 'POLL_CLOSED':
      // Disable voting UI
      markPollAsClosed(data.poll);
      break;
      
    case 'POLL_DELETED':
      // Remove poll khỏi UI
      removePollFromList(data.poll.id);
      break;
  }
});
```

---

## Error Responses

### 400 Bad Request - Validation Error
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    "Question must be between 1 and 500 characters",
    "Poll must have between 2 and 10 options"
  ]
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Authentication required"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Only the poll creator can close the poll"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Poll not found"
}
```

### 400 Business Error
```json
{
  "success": false,
  "message": "Poll is not active"
}
```

---

## UI Implementation Tips

### 1. Hiển thị Poll
```javascript
// Tính percentage bar width
const barWidth = (option.voteCount / poll.totalVoters * 100) + '%';

// Check nếu user đã vote option này
const isVoted = poll.currentUserVotedOptionIds.includes(option.id);

// Check nếu poll còn active
const canVote = poll.isActive && !poll.isClosed && !poll.isExpired;
```

### 2. Vote UI
```javascript
// Single choice: Radio buttons
// Multiple choice: Checkboxes

if (!poll.allowMultipleVotes) {
  // Chỉ cho chọn 1 option
  selectedOptions = [optionId];
} else {
  // Cho chọn nhiều options
  selectedOptions.push(optionId);
}
```

### 3. Real-time Updates
```javascript
// Khi nhận WebSocket event POLL_VOTED
// Update UI ngay lập tức không cần reload
updatePollUI(event.poll);
```

### 4. Expiration Check
```javascript
// Check expiration ở client
const now = new Date();
const expiresAt = new Date(poll.expiresAt);
const isExpired = expiresAt < now;

// Hoặc dùng field từ server
const canVote = poll.isActive;
```

---

## Testing với cURL

### Tạo Poll
```bash
curl -X POST http://localhost:8080/api/v1/polls/conversation/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Test poll?",
    "options": ["Option 1", "Option 2", "Option 3"],
    "allowMultipleVotes": false,
    "expiresAt": null
  }'
```

### Vote
```bash
curl -X POST http://localhost:8080/api/v1/polls/1/vote \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "optionIds": [2]
  }'
```

### Get Poll
```bash
curl -X GET http://localhost:8080/api/v1/polls/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### List Polls
```bash
curl -X GET "http://localhost:8080/api/v1/polls/conversation/1?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Close Poll
```bash
curl -X POST http://localhost:8080/api/v1/polls/1/close \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Delete Poll
```bash
curl -X DELETE http://localhost:8080/api/v1/polls/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```
