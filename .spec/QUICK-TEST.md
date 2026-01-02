# Quick Test Commands

## Your Token
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJ1c2VyMSIsImp0aSI6IjgzZGI0MWI0LTBlNjktNGVhYS04YmMzLWY0ODk2MzU3OGQ1YiIsImlhdCI6MTc2NzE3NzEyNiwiZXhwIjoxNzY3MzkzMTI2fQ.nVblVB5Bw3ZU0RYRc0r0WKWzhu4OCMCbTXSrqXZy9BQ
```

## Poll đã tạo: Message ID = 206

### 1. Get Poll Details (ĐÚNG ENDPOINT)
```bash
GET http://localhost:8080/api/v1/conversations/1/polls/206
```

**Postman:**
- Method: GET
- URL: `http://localhost:8080/api/v1/conversations/1/polls/206`
- Headers:
  - Authorization: `Bearer eyJhbGciOiJIUzI1NiJ9...`
  - Accept: `application/json`

**Expected Response:**
```json
{
  "success": true,
  "message": "Poll retrieved successfully",
  "data": {
    "messageId": 206,
    "question": "What time works best for the meeting?",
    "options": [
      {
        "id": 0,
        "text": "9 AM",
        "voteCount": 0,
        "voterIds": [],
        "hasVoted": false
      },
      {
        "id": 1,
        "text": "2 PM",
        "voteCount": 0,
        "voterIds": [],
        "hasVoted": false
      },
      {
        "id": 2,
        "text": "5 PM",
        "voteCount": 0,
        "voterIds": [],
        "hasVoted": false
      }
    ],
    "allowMultiple": false,
    "anonymous": false,
    "closesAt": "2026-01-31T23:59:59Z",
    "isClosed": false,
    "totalVotes": 0,
    "createdBy": 1,
    "createdByUsername": "user1",
    "createdAt": "2025-12-31T12:05:36.786588Z"
  }
}
```

---

### 2. Vote on Poll
```bash
POST http://localhost:8080/api/v1/conversations/1/polls/206/vote
```

**Body:**
```json
{
  "optionIds": [0]
}
```

**Postman:**
- Method: POST
- URL: `http://localhost:8080/api/v1/conversations/1/polls/206/vote`
- Headers:
  - Authorization: `Bearer eyJhbGciOiJIUzI1NiJ9...`
  - Content-Type: `application/json`
- Body (raw JSON):
```json
{
  "optionIds": [0]
}
```

---

### 3. Get Poll Details Again (sau khi vote)
```bash
GET http://localhost:8080/api/v1/conversations/1/polls/206
```

**Expected Response (sau khi vote):**
```json
{
  "success": true,
  "message": "Poll retrieved successfully",
  "data": {
    "messageId": 206,
    "question": "What time works best for the meeting?",
    "options": [
      {
        "id": 0,
        "text": "9 AM",
        "voteCount": 1,
        "voterIds": [1],
        "hasVoted": true  ← User đã vote
      },
      {
        "id": 1,
        "text": "2 PM",
        "voteCount": 0,
        "voterIds": [],
        "hasVoted": false
      },
      {
        "id": 2,
        "text": "5 PM",
        "voteCount": 0,
        "voterIds": [],
        "hasVoted": false
      }
    ],
    "allowMultiple": false,
    "anonymous": false,
    "closesAt": "2026-01-31T23:59:59Z",
    "isClosed": false,
    "totalVotes": 1,  ← Tăng lên 1
    "createdBy": 1,
    "createdByUsername": "user1",
    "createdAt": "2025-12-31T12:05:36.786588Z"
  }
}
```

---

### 4. Get All Polls in Conversation
```bash
GET http://localhost:8080/api/v1/conversations/1/polls?status=all
```

**Postman:**
- Method: GET
- URL: `http://localhost:8080/api/v1/conversations/1/polls?status=all`
- Headers:
  - Authorization: `Bearer eyJhbGciOiJIUzI1NiJ9...`

**Expected Response:**
```json
{
  "success": true,
  "message": "Polls retrieved successfully",
  "data": {
    "polls": [
      {
        "messageId": 206,
        "question": "What time works best for the meeting?",
        "options": [...],
        "isClosed": false,
        "totalVotes": 1,
        ...
      }
    ],
    "totalCount": 1,
    "activeCount": 1,
    "closedCount": 0
  }
}
```

---

### 5. Change Vote (vote cho option khác)
```bash
POST http://localhost:8080/api/v1/conversations/1/polls/206/vote
```

**Body:**
```json
{
  "optionIds": [1]
}
```

Sau đó GET lại poll details, sẽ thấy:
- Option 0: voteCount = 0, hasVoted = false
- Option 1: voteCount = 1, hasVoted = true ← Đã chuyển sang option 1

---

## Test Event

### 1. Create Event
```bash
POST http://localhost:8080/api/v1/conversations/1/events
```

**Body:**
```json
{
  "title": "Team Lunch",
  "description": "Monthly team lunch",
  "startTime": "2026-01-15T12:00:00Z",
  "endTime": "2026-01-15T13:30:00Z",
  "location": "Restaurant ABC"
}
```

**Postman:**
- Method: POST
- URL: `http://localhost:8080/api/v1/conversations/1/events`
- Headers:
  - Authorization: `Bearer eyJhbGciOiJIUzI1NiJ9...`
  - Content-Type: `application/json`
- Body (raw JSON): như trên

**Response sẽ trả về MessageResponse, lấy ID từ đó**

---

### 2. Get Event Details (giả sử message ID = 207)
```bash
GET http://localhost:8080/api/v1/conversations/1/events/207
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Event retrieved successfully",
  "data": {
    "messageId": 207,
    "title": "Team Lunch",
    "description": "Monthly team lunch",
    "startTime": "2026-01-15T12:00:00Z",
    "endTime": "2026-01-15T13:30:00Z",
    "location": "Restaurant ABC",
    "goingUserIds": [],
    "maybeUserIds": [],
    "notGoingUserIds": [],
    "goingCount": 0,
    "maybeCount": 0,
    "notGoingCount": 0,
    "currentUserStatus": null,
    "isPast": false,
    "createdBy": 1,
    "createdByUsername": "user1",
    "createdAt": "..."
  }
}
```

---

### 3. RSVP to Event
```bash
POST http://localhost:8080/api/v1/conversations/1/events/207/rsvp
```

**Body:**
```json
{
  "status": "GOING"
}
```

---

### 4. Get Event Details Again
```bash
GET http://localhost:8080/api/v1/conversations/1/events/207
```

**Expected Response (sau RSVP):**
```json
{
  "success": true,
  "message": "Event retrieved successfully",
  "data": {
    "messageId": 207,
    "title": "Team Lunch",
    ...
    "goingUserIds": [1],  ← User đã RSVP
    "maybeUserIds": [],
    "notGoingUserIds": [],
    "goingCount": 1,  ← Tăng lên 1
    "maybeCount": 0,
    "notGoingCount": 0,
    "currentUserStatus": "GOING",  ← Status của user hiện tại
    "isPast": false,
    ...
  }
}
```

---

## Tóm tắt Endpoints

### Poll Endpoints
1. ✅ `POST /api/v1/conversations/{id}/polls` - Tạo poll
2. ✅ `POST /api/v1/conversations/{id}/polls/{messageId}/vote` - Vote
3. ✅ `GET /api/v1/conversations/{id}/polls/{messageId}` - Chi tiết poll
4. ✅ `GET /api/v1/conversations/{id}/polls?status=all|active|closed` - Danh sách

### Event Endpoints
1. ✅ `POST /api/v1/conversations/{id}/events` - Tạo event
2. ✅ `POST /api/v1/conversations/{id}/events/{messageId}/rsvp` - RSVP
3. ✅ `GET /api/v1/conversations/{id}/events/{messageId}` - Chi tiết event
4. ✅ `GET /api/v1/conversations/{id}/events?status=all|upcoming|past` - Danh sách

### ✅ NOW WORKS - Message Retrieval with Poll/Event Data
- `GET /api/v1/conversations/{id}/messages/{messageId}` - Now returns poll/event data embedded in MessageResponse

---

## NEW: Test Message Retrieval with Poll/Event Data

### Get Poll Message (Message ID = 206)
```bash
GET http://localhost:8080/api/v1/conversations/1/messages/206
```

**Postman:**
- Method: GET
- URL: `http://localhost:8080/api/v1/conversations/1/messages/206`
- Headers:
  - Authorization: `Bearer eyJhbGciOiJIUzI1NiJ9...`
  - Accept: `application/json`

**Expected Response (NOW includes poll object):**
```json
{
  "success": true,
  "message": "Message retrieved successfully",
  "data": {
    "id": 206,
    "conversationId": 1,
    "senderId": 1,
    "senderUsername": "user1",
    "senderFullName": "Nguyen Linh La",
    "content": "What time works best for the meeting?",
    "type": "POLL",
    "poll": {
      "messageId": 206,
      "question": "What time works best for the meeting?",
      "options": [
        {
          "id": 0,
          "text": "9 AM",
          "voteCount": 0,
          "voterIds": [],
          "hasVoted": false
        },
        {
          "id": 1,
          "text": "2 PM",
          "voteCount": 0,
          "voterIds": [],
          "hasVoted": false
        },
        {
          "id": 2,
          "text": "5 PM",
          "voteCount": 0,
          "voterIds": [],
          "hasVoted": false
        }
      ],
      "allowMultiple": false,
      "anonymous": false,
      "closesAt": "2026-01-31T23:59:59Z",
      "isClosed": false,
      "totalVotes": 0,
      "createdBy": 1,
      "createdByUsername": "user1",
      "createdAt": "2025-12-31T12:05:36.786588Z"
    },
    "reactions": {},
    "sentAt": "2025-12-31T12:05:36.786589Z",
    "createdAt": "2025-12-31T12:05:36.786588Z",
    "updatedAt": "2025-12-31T12:05:36.842875Z",
    "edited": false,
    "deleted": false,
    "forwarded": false,
    "forwardCount": 0,
    "pinned": false,
    "scheduled": false
  }
}
```

### Get Message List (includes poll/event data)
```bash
GET http://localhost:8080/api/v1/conversations/1/messages?page=0&size=20
```

**Postman:**
- Method: GET
- URL: `http://localhost:8080/api/v1/conversations/1/messages?page=0&size=20`
- Headers:
  - Authorization: `Bearer eyJhbGciOiJIUzI1NiJ9...`

**Expected:** Each POLL/EVENT message in the list will now include the `poll` or `event` object with full details.

---

### ❌ KHÔNG DÙNG
- `GET /api/v1/conversations/{id}/messages/{messageId}` - Endpoint này chỉ trả MessageResponse thông thường
