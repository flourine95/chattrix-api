# Scheduled Messages API - Complete Examples

## Authentication

All endpoints require authentication. First, obtain an access token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "your_password"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": { ... }
  }
}
```

Save the `accessToken` and use it in the `Authorization` header for all subsequent requests.

---

## 1. Schedule a Message

### POST `/api/v1/conversations/{conversationId}/messages/schedule`

Schedule a text message to be sent 1 hour from now:

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "Happy Birthday! 🎉",
    "type": "TEXT",
    "scheduledTime": "2025-12-22T15:00:00Z"
  }'
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Scheduled message created successfully",
  "data": {
    "id": 1,
    "conversationId": 1,
    "conversationName": "Birthday Party Group",
    "conversationType": "GROUP",
    "senderId": 5,
    "content": "Happy Birthday! 🎉",
    "type": "TEXT",
    "mediaUrl": null,
    "thumbnailUrl": null,
    "fileName": null,
    "fileSize": null,
    "duration": null,
    "replyToMessageId": null,
    "scheduledTime": "2025-12-22T15:00:00Z",
    "status": "SCHEDULED",
    "sentAt": null,
    "sentMessageId": null,
    "failedReason": null,
    "createdAt": "2025-12-22T14:00:00Z",
    "updatedAt": "2025-12-22T14:00:00Z"
  }
}
```

### Schedule an Image Message

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "Check out this photo!",
    "type": "IMAGE",
    "scheduledTime": "2025-12-22T16:00:00Z",
    "mediaUrl": "https://example.com/images/photo.jpg",
    "thumbnailUrl": "https://example.com/images/photo_thumb.jpg",
    "fileName": "photo.jpg",
    "fileSize": 1024000
  }'
```

### Schedule a Reply Message

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "Thanks for the reminder!",
    "type": "TEXT",
    "scheduledTime": "2025-12-22T17:00:00Z",
    "replyToMessageId": 123
  }'
```

### Error: Scheduled Time in Past

```bash
curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "This will fail",
    "type": "TEXT",
    "scheduledTime": "2020-01-01T00:00:00Z"
  }'
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Scheduled time must be in the future",
  "code": "VALIDATION_ERROR"
}
```

---

## 2. List Scheduled Messages

### GET `/api/v1/messages/scheduled`

Get all scheduled messages (default: SCHEDULED status):

```bash
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Scheduled messages retrieved successfully",
  "data": {
    "data": [
      {
        "id": 1,
        "conversationId": 1,
        "conversationName": "Birthday Party Group",
        "conversationType": "GROUP",
        "senderId": 5,
        "content": "Happy Birthday! 🎉",
        "type": "TEXT",
        "scheduledTime": "2025-12-22T15:00:00Z",
        "status": "SCHEDULED",
        "createdAt": "2025-12-22T14:00:00Z",
        "updatedAt": "2025-12-22T14:00:00Z"
      },
      {
        "id": 2,
        "conversationId": 2,
        "conversationName": "Work Team",
        "conversationType": "GROUP",
        "senderId": 5,
        "content": "Meeting reminder",
        "type": "TEXT",
        "scheduledTime": "2025-12-23T09:00:00Z",
        "status": "SCHEDULED",
        "createdAt": "2025-12-22T14:05:00Z",
        "updatedAt": "2025-12-22T14:05:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 2,
    "totalPages": 1,
    "hasNextPage": false,
    "hasPrevPage": false
  }
}
```

### Filter by Conversation

```bash
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?conversationId=1&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Filter by Status

```bash
# Get sent messages
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=SENT&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Get failed messages
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=FAILED&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Get cancelled messages
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?status=CANCELLED&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Filter by Conversation and Status

```bash
curl -X GET "http://localhost:8080/api/v1/messages/scheduled?conversationId=1&status=SCHEDULED&page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 3. Get Single Scheduled Message

### GET `/api/v1/messages/scheduled/{scheduledMessageId}`

```bash
curl -X GET http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Scheduled message retrieved successfully",
  "data": {
    "id": 1,
    "conversationId": 1,
    "conversationName": "Birthday Party Group",
    "conversationType": "GROUP",
    "senderId": 5,
    "content": "Happy Birthday! 🎉",
    "type": "TEXT",
    "mediaUrl": null,
    "thumbnailUrl": null,
    "fileName": null,
    "fileSize": null,
    "duration": null,
    "replyToMessageId": null,
    "scheduledTime": "2025-12-22T15:00:00Z",
    "status": "SCHEDULED",
    "sentAt": null,
    "sentMessageId": null,
    "failedReason": null,
    "createdAt": "2025-12-22T14:00:00Z",
    "updatedAt": "2025-12-22T14:00:00Z"
  }
}
```

### Error: Not Found

```bash
curl -X GET http://localhost:8080/api/v1/messages/scheduled/999 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Scheduled message not found",
  "code": "RESOURCE_NOT_FOUND"
}
```

---

## 4. Update Scheduled Message

### PUT `/api/v1/messages/scheduled/{scheduledMessageId}`

Update content only:

```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "Happy Birthday! 🎂🎉 Hope you have a great day!"
  }'
```

Update scheduled time only:

```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "scheduledTime": "2025-12-22T16:00:00Z"
  }'
```

Update multiple fields:

```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "Updated message content",
    "scheduledTime": "2025-12-22T17:00:00Z",
    "mediaUrl": "https://example.com/new-image.jpg"
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Scheduled message updated successfully",
  "data": {
    "id": 1,
    "conversationId": 1,
    "conversationName": "Birthday Party Group",
    "conversationType": "GROUP",
    "senderId": 5,
    "content": "Updated message content",
    "type": "TEXT",
    "scheduledTime": "2025-12-22T17:00:00Z",
    "status": "SCHEDULED",
    "updatedAt": "2025-12-22T14:30:00Z"
  }
}
```

### Error: Cannot Edit Sent Message

```bash
curl -X PUT http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "content": "This will fail if message was already sent"
  }'
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Cannot edit scheduled message: Message has already been sent",
  "code": "BAD_REQUEST"
}
```

---

## 5. Cancel Scheduled Message

### DELETE `/api/v1/messages/scheduled/{scheduledMessageId}`

```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Scheduled message cancelled successfully",
  "data": null
}
```

### Error: Cannot Cancel Sent Message

```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Cannot cancel scheduled message: Message has already been sent",
  "code": "BAD_REQUEST"
}
```

---

## 6. Bulk Cancel Scheduled Messages

### DELETE `/api/v1/messages/scheduled/bulk`

Cancel multiple messages at once:

```bash
curl -X DELETE http://localhost:8080/api/v1/messages/scheduled/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "scheduledMessageIds": [1, 2, 3, 4, 5]
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Scheduled messages cancelled successfully",
  "data": {
    "cancelledCount": 3,
    "failedIds": [4, 5]
  }
}
```

**Note**: `failedIds` contains IDs that couldn't be cancelled (already sent, not found, or not owned by user).

---

## WebSocket Events

### Connect to WebSocket

```javascript
const token = 'YOUR_ACCESS_TOKEN';
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

ws.onopen = () => {
    console.log('WebSocket connected');
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
    
    if (data.type === 'scheduled.message.sent') {
        console.log('Scheduled message sent:', data.payload);
    } else if (data.type === 'scheduled.message.failed') {
        console.log('Scheduled message failed:', data.payload);
    }
};
```

### Event: scheduled.message.sent

Received when a scheduled message is successfully sent:

```json
{
  "type": "scheduled.message.sent",
  "payload": {
    "scheduledMessageId": 1,
    "message": {
      "id": 456,
      "conversationId": 1,
      "sender": {
        "id": 5,
        "username": "john_doe",
        "fullName": "John Doe",
        "avatarUrl": "https://example.com/avatar.jpg"
      },
      "content": "Happy Birthday! 🎉",
      "type": "TEXT",
      "createdAt": "2025-12-22T15:00:05Z",
      "mediaUrl": null,
      "thumbnailUrl": null,
      "fileName": null,
      "fileSize": null,
      "duration": null,
      "replyToMessageId": null,
      "reactions": {},
      "mentions": []
    }
  }
}
```

### Event: scheduled.message.failed

Received when a scheduled message fails to send:

```json
{
  "type": "scheduled.message.failed",
  "payload": {
    "scheduledMessageId": 1,
    "conversationId": 1,
    "failedReason": "User has left the conversation",
    "failedAt": "2025-12-22T15:00:05Z"
  }
}
```

---

## Complete Workflow Example

### Scenario: Schedule a Birthday Message

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "john_doe", "password": "password"}' \
  | jq -r '.data.accessToken')

# 2. Schedule birthday message for tomorrow at 9 AM
SCHEDULED_MSG=$(curl -s -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "content": "Happy Birthday! 🎉🎂",
    "type": "TEXT",
    "scheduledTime": "2025-12-23T09:00:00Z"
  }' | jq -r '.data.id')

echo "Scheduled message ID: $SCHEDULED_MSG"

# 3. View scheduled messages
curl -s -X GET "http://localhost:8080/api/v1/messages/scheduled?status=SCHEDULED" \
  -H "Authorization: Bearer $TOKEN" | jq

# 4. Update the message (add more emojis)
curl -s -X PUT "http://localhost:8080/api/v1/messages/scheduled/$SCHEDULED_MSG" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "content": "Happy Birthday! 🎉🎂🎈🎁 Have an amazing day!"
  }' | jq

# 5. If plans change, cancel the message
# curl -s -X DELETE "http://localhost:8080/api/v1/messages/scheduled/$SCHEDULED_MSG" \
#   -H "Authorization: Bearer $TOKEN" | jq
```

---

## Testing Tips

### 1. Test with Near-Future Times

For testing, schedule messages 1-2 minutes in the future:

```bash
# Get current time + 2 minutes in UTC
SCHEDULED_TIME=$(date -u -d '+2 minutes' '+%Y-%m-%dT%H:%M:%SZ')

curl -X POST http://localhost:8080/api/v1/conversations/1/messages/schedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"content\": \"Test message\",
    \"type\": \"TEXT\",
    \"scheduledTime\": \"$SCHEDULED_TIME\"
  }"
```

### 2. Monitor Scheduler Logs

```bash
# Watch for scheduler activity
docker compose logs -f api | grep "Processing scheduled messages"

# Watch for sent messages
docker compose logs -f api | grep "SENT"
```

### 3. Check Database State

```sql
-- View all scheduled messages
SELECT id, content, scheduled_time, status, created_at 
FROM scheduled_messages 
ORDER BY scheduled_time ASC;

-- View upcoming messages
SELECT id, content, scheduled_time 
FROM scheduled_messages 
WHERE status = 'SCHEDULED' AND scheduled_time > NOW() 
ORDER BY scheduled_time ASC;
```

---

## Error Codes Reference

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid input (time in past, missing fields, etc.) |
| `BAD_REQUEST` | 400 | Cannot perform action (edit sent message, etc.) |
| `UNAUTHORIZED` | 401 | Not authenticated or not authorized |
| `RESOURCE_NOT_FOUND` | 404 | Scheduled message, conversation, or user not found |

---

## Rate Limiting

All endpoints respect the application's rate limiting rules. If you exceed the limit, you'll receive:

```json
{
  "success": false,
  "message": "Too many requests",
  "code": "RATE_LIMIT_EXCEEDED"
}
```

Wait for the rate limit window to reset before retrying.
