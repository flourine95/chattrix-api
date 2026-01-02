# Poll & Event Testing Guide

## Prerequisites
- Server running: `docker compose up -d --build`
- Get JWT token from login endpoint
- Have at least 1 conversation created

## Environment Variables
```bash
# Set your token
export TOKEN="your_jwt_token_here"

# Or for Windows PowerShell
$TOKEN = "your_jwt_token_here"
```

---

## POLL ENDPOINTS

### 1. Create Poll
**Endpoint:** `POST /api/v1/conversations/{conversationId}/polls`

**Request Body:**
```json
{
  "question": "What time works best for the meeting?",
  "options": ["9 AM", "2 PM", "5 PM"],
  "allowMultipleVotes": false,
  "expiresAt": "2026-01-31T23:59:59Z"
}
```

**Validation Rules:**
- `question`: Required, 1-500 characters
- `options`: Required, 2-10 options, each 1-200 characters
- `allowMultipleVotes`: Required boolean
- `expiresAt`: Optional ISO-8601 timestamp

**Curl Command:**
```bash
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What time works best for the meeting?",
    "options": ["9 AM", "2 PM", "5 PM"],
    "allowMultipleVotes": false,
    "expiresAt": "2026-01-31T23:59:59Z"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Poll created successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "senderId": 1,
    "senderUsername": "user1",
    "content": "What time works best for the meeting?",
    "type": "POLL",
    "sentAt": "2025-12-31T12:00:00Z",
    "createdAt": "2025-12-31T12:00:00Z",
    ...
  }
}
```

---

### 2. Vote on Poll
**Endpoint:** `POST /api/v1/conversations/{conversationId}/polls/{messageId}/vote`

**Request Body:**
```json
{
  "optionIds": [0]
}
```

**Validation Rules:**
- `optionIds`: Required, non-empty array of Long
- If `allowMultipleVotes` is false, only 1 option allowed
- Option IDs must exist in the poll

**Curl Command:**
```bash
# Vote for option 0 (9 AM)
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls/123/vote" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "optionIds": [0]
  }'

# Vote for multiple options (if allowMultipleVotes is true)
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls/123/vote" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "optionIds": [0, 1]
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Vote recorded successfully",
  "data": {
    "id": 123,
    "conversationId": 1,
    "type": "POLL",
    ...
  }
}
```

---

### 3. Get Poll Details
**Endpoint:** `GET /api/v1/conversations/{conversationId}/polls/{messageId}`

**Curl Command:**
```bash
curl "http://localhost:8080/api/v1/conversations/1/polls/123" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Poll retrieved successfully",
  "data": {
    "messageId": 123,
    "question": "What time works best for the meeting?",
    "options": [
      {
        "id": 0,
        "text": "9 AM",
        "voteCount": 5,
        "voterIds": [1, 2, 3, 4, 5],
        "hasVoted": true
      },
      {
        "id": 1,
        "text": "2 PM",
        "voteCount": 3,
        "voterIds": [6, 7, 8],
        "hasVoted": false
      },
      {
        "id": 2,
        "text": "5 PM",
        "voteCount": 2,
        "voterIds": [9, 10],
        "hasVoted": false
      }
    ],
    "allowMultiple": false,
    "anonymous": false,
    "closesAt": "2026-01-31T23:59:59Z",
    "isClosed": false,
    "totalVotes": 10,
    "createdBy": 1,
    "createdByUsername": "user1",
    "createdAt": "2025-12-31T12:00:00Z"
  }
}
```

---

### 4. Get All Polls in Conversation
**Endpoint:** `GET /api/v1/conversations/{conversationId}/polls?status={status}`

**Query Parameters:**
- `status`: Optional, values: `all` (default), `active`, `closed`
  - `active`: Polls that haven't expired (closesAt is null or in future)
  - `closed`: Polls that have expired (closesAt is in past)

**Curl Commands:**
```bash
# Get all polls
curl "http://localhost:8080/api/v1/conversations/1/polls?status=all" \
  -H "Authorization: Bearer $TOKEN"

# Get only active polls
curl "http://localhost:8080/api/v1/conversations/1/polls?status=active" \
  -H "Authorization: Bearer $TOKEN"

# Get only closed polls
curl "http://localhost:8080/api/v1/conversations/1/polls?status=closed" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Polls retrieved successfully",
  "data": {
    "polls": [
      {
        "messageId": 123,
        "question": "What time works best?",
        "options": [...],
        "isClosed": false,
        "totalVotes": 10,
        ...
      },
      {
        "messageId": 124,
        "question": "Favorite color?",
        "options": [...],
        "isClosed": true,
        "totalVotes": 15,
        ...
      }
    ],
    "totalCount": 2,
    "activeCount": 1,
    "closedCount": 1
  }
}
```

---

## EVENT ENDPOINTS

### 1. Create Event
**Endpoint:** `POST /api/v1/conversations/{conversationId}/events`

**Request Body:**
```json
{
  "title": "Team Lunch",
  "description": "Monthly team lunch at the new restaurant",
  "startTime": "2026-01-15T12:00:00Z",
  "endTime": "2026-01-15T13:30:00Z",
  "location": "Restaurant ABC, 123 Main St"
}
```

**Validation Rules:**
- `title`: Required, 1-200 characters
- `description`: Optional
- `startTime`: Required ISO-8601 timestamp
- `endTime`: Optional ISO-8601 timestamp
- `location`: Optional, max 200 characters

**Curl Command:**
```bash
curl -X POST "http://localhost:8080/api/v1/conversations/1/events" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Lunch",
    "description": "Monthly team lunch at the new restaurant",
    "startTime": "2026-01-15T12:00:00Z",
    "endTime": "2026-01-15T13:30:00Z",
    "location": "Restaurant ABC, 123 Main St"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Event created successfully",
  "data": {
    "id": 456,
    "conversationId": 1,
    "senderId": 1,
    "senderUsername": "user1",
    "content": "Team Lunch",
    "type": "EVENT",
    "sentAt": "2025-12-31T12:00:00Z",
    ...
  }
}
```

---

### 2. RSVP to Event
**Endpoint:** `POST /api/v1/conversations/{conversationId}/events/{messageId}/rsvp`

**Request Body:**
```json
{
  "status": "GOING"
}
```

**Validation Rules:**
- `status`: Required, must be one of: `GOING`, `MAYBE`, `NOT_GOING`

**Curl Commands:**
```bash
# RSVP as GOING
curl -X POST "http://localhost:8080/api/v1/conversations/1/events/456/rsvp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "GOING"
  }'

# RSVP as MAYBE
curl -X POST "http://localhost:8080/api/v1/conversations/1/events/456/rsvp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "MAYBE"
  }'

# RSVP as NOT_GOING
curl -X POST "http://localhost:8080/api/v1/conversations/1/events/456/rsvp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "NOT_GOING"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "RSVP recorded successfully",
  "data": {
    "id": 456,
    "conversationId": 1,
    "type": "EVENT",
    ...
  }
}
```

---

### 3. Get Event Details
**Endpoint:** `GET /api/v1/conversations/{conversationId}/events/{messageId}`

**Curl Command:**
```bash
curl "http://localhost:8080/api/v1/conversations/1/events/456" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Event retrieved successfully",
  "data": {
    "messageId": 456,
    "title": "Team Lunch",
    "description": "Monthly team lunch at the new restaurant",
    "startTime": "2026-01-15T12:00:00Z",
    "endTime": "2026-01-15T13:30:00Z",
    "location": "Restaurant ABC, 123 Main St",
    "goingUserIds": [1, 2, 3],
    "maybeUserIds": [4, 5],
    "notGoingUserIds": [6],
    "goingCount": 3,
    "maybeCount": 2,
    "notGoingCount": 1,
    "currentUserStatus": "GOING",
    "isPast": false,
    "createdBy": 1,
    "createdByUsername": "user1",
    "createdAt": "2025-12-31T12:00:00Z"
  }
}
```

---

### 4. Get All Events in Conversation
**Endpoint:** `GET /api/v1/conversations/{conversationId}/events?status={status}`

**Query Parameters:**
- `status`: Optional, values: `all` (default), `upcoming`, `past`
  - `upcoming`: Events that haven't ended yet (endTime or startTime is in future)
  - `past`: Events that have ended (endTime or startTime is in past)

**Curl Commands:**
```bash
# Get all events
curl "http://localhost:8080/api/v1/conversations/1/events?status=all" \
  -H "Authorization: Bearer $TOKEN"

# Get only upcoming events
curl "http://localhost:8080/api/v1/conversations/1/events?status=upcoming" \
  -H "Authorization: Bearer $TOKEN"

# Get only past events
curl "http://localhost:8080/api/v1/conversations/1/events?status=past" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Events retrieved successfully",
  "data": {
    "events": [
      {
        "messageId": 456,
        "title": "Team Lunch",
        "startTime": "2026-01-15T12:00:00Z",
        "isPast": false,
        "goingCount": 3,
        "currentUserStatus": "GOING",
        ...
      },
      {
        "messageId": 457,
        "title": "Project Kickoff",
        "startTime": "2025-12-20T10:00:00Z",
        "isPast": true,
        "goingCount": 8,
        "currentUserStatus": null,
        ...
      }
    ],
    "totalCount": 2,
    "upcomingCount": 1,
    "pastCount": 1
  }
}
```

---

## COMPLETE TEST FLOW

### Poll Test Flow
```bash
# 1. Login and get token
TOKEN=$(curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password"}' \
  | jq -r '.data.accessToken')

# 2. Create a poll
POLL_RESPONSE=$(curl -X POST "http://localhost:8080/api/v1/conversations/1/polls" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Best meeting time?",
    "options": ["Morning", "Afternoon", "Evening"],
    "allowMultipleVotes": false
  }')

# Extract message ID
MESSAGE_ID=$(echo $POLL_RESPONSE | jq -r '.data.id')

# 3. Vote on the poll
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls/$MESSAGE_ID/vote" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [0]}'

# 4. Get poll details
curl "http://localhost:8080/api/v1/conversations/1/polls/$MESSAGE_ID" \
  -H "Authorization: Bearer $TOKEN"

# 5. Get all active polls
curl "http://localhost:8080/api/v1/conversations/1/polls?status=active" \
  -H "Authorization: Bearer $TOKEN"
```

### Event Test Flow
```bash
# 1. Login and get token (if not already done)
TOKEN=$(curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password"}' \
  | jq -r '.data.accessToken')

# 2. Create an event
EVENT_RESPONSE=$(curl -X POST "http://localhost:8080/api/v1/conversations/1/events" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Meeting",
    "description": "Weekly sync",
    "startTime": "2026-01-10T14:00:00Z",
    "endTime": "2026-01-10T15:00:00Z",
    "location": "Conference Room A"
  }')

# Extract message ID
MESSAGE_ID=$(echo $EVENT_RESPONSE | jq -r '.data.id')

# 3. RSVP to the event
curl -X POST "http://localhost:8080/api/v1/conversations/1/events/$MESSAGE_ID/rsvp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "GOING"}'

# 4. Get event details
curl "http://localhost:8080/api/v1/conversations/1/events/$MESSAGE_ID" \
  -H "Authorization: Bearer $TOKEN"

# 5. Get all upcoming events
curl "http://localhost:8080/api/v1/conversations/1/events?status=upcoming" \
  -H "Authorization: Bearer $TOKEN"
```

---

## ERROR CASES TO TEST

### Poll Errors
```bash
# 1. Invalid option count (less than 2)
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Yes or no?",
    "options": ["Yes"],
    "allowMultipleVotes": false
  }'
# Expected: 400 Bad Request - "Poll must have between 2 and 10 options"

# 2. Vote on closed poll
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls/123/vote" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [0]}'
# Expected: 400 Bad Request - "Poll is closed"

# 3. Multiple votes when not allowed
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls/123/vote" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [0, 1]}'
# Expected: 400 Bad Request - "Multiple votes not allowed"

# 4. Invalid option ID
curl -X POST "http://localhost:8080/api/v1/conversations/1/polls/123/vote" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [999]}'
# Expected: 400 Bad Request - "Invalid option ID: 999"
```

### Event Errors
```bash
# 1. Missing required field
curl -X POST "http://localhost:8080/api/v1/conversations/1/events" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "No title provided",
    "startTime": "2026-01-10T14:00:00Z"
  }'
# Expected: 400 Bad Request - "Title is required"

# 2. Invalid RSVP status
curl -X POST "http://localhost:8080/api/v1/conversations/1/events/456/rsvp" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "INVALID"}'
# Expected: 400 Bad Request - "Status must be GOING, MAYBE, or NOT_GOING"
```

---

## NOTES

1. **Authentication**: All endpoints require `@Secured` - must include valid JWT token
2. **Participant Check**: User must be a participant of the conversation
3. **Permissions**: For GROUP conversations, check `create_polls` and `create_events` permissions
4. **Timestamps**: Use ISO-8601 format (e.g., `2026-01-15T12:00:00Z`)
5. **Option IDs**: Start from 0 (first option is 0, second is 1, etc.)
6. **RSVP Changes**: User can change RSVP - removes from all lists and adds to selected
7. **Vote Changes**: User can change vote - removes previous votes and adds new ones
8. **Sorting**: 
   - Polls sorted by `createdAt DESC` (newest first)
   - Events sorted by `startTime ASC` (soonest first)
