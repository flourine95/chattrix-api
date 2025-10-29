# API Endpoints Summary - Backend Implementation Guide

## 📋 Tổng Quan

File này tóm tắt tất cả API endpoints cần implement cho backend để hỗ trợ các tính năng chat nâng cao.

## 🔗 Endpoints Cần Thêm

### 1. Reply to Messages

#### Send Message with Reply
```
POST /v1/conversations/{conversationId}/messages
```

**Request Body:**
```json
{
  "content": "Thanks for sharing!",
  "type": "TEXT",
  "replyToMessageId": 123
}
```

**Response:**
```json
{
  "id": 456,
  "conversationId": 1,
  "senderId": 2,
  "content": "Thanks for sharing!",
  "type": "TEXT",
  "replyToMessageId": 123,
  "replyToMessage": {
    "id": 123,
    "senderId": 1,
    "content": "Check out this photo!",
    "type": "IMAGE",
    "mediaUrl": "https://...",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "createdAt": "2024-01-15T10:35:00Z"
}
```

**Notes:**
- Include `replyToMessage` object in response
- Only include basic fields (id, sender, content, type, mediaUrl)
- Validate that replyToMessageId exists

---

### 2. Reactions

#### Add Reaction
```
POST /v1/messages/{messageId}/reactions
```

**Request Body:**
```json
{
  "emoji": "👍"
}
```

**Response:**
```json
{
  "messageId": 123,
  "reactions": {
    "👍": [1, 2, 3],
    "❤️": [4, 5]
  }
}
```

**Logic:**
- If emoji doesn't exist, create new entry with user ID
- If emoji exists and user not in array, add user ID
- If emoji exists and user in array, remove user ID (toggle)
- Broadcast update via WebSocket

---

#### Remove Reaction
```
DELETE /v1/messages/{messageId}/reactions/{emoji}
```

**Example:**
```
DELETE /v1/messages/123/reactions/👍
```

**Response:**
```json
{
  "messageId": 123,
  "reactions": {
    "❤️": [4, 5]
  }
}
```

**Notes:**
- Remove current user from emoji array
- If array becomes empty, remove emoji key
- Broadcast update via WebSocket

---

#### Get Reactions (Optional - for detailed view)
```
GET /v1/messages/{messageId}/reactions
```

**Response:**
```json
{
  "messageId": 123,
  "reactions": {
    "👍": [
      {
        "userId": 1,
        "userName": "John Doe",
        "createdAt": "2024-01-15T10:30:00Z"
      },
      {
        "userId": 2,
        "userName": "Jane Smith",
        "createdAt": "2024-01-15T10:31:00Z"
      }
    ],
    "❤️": [
      {
        "userId": 4,
        "userName": "Bob Wilson",
        "createdAt": "2024-01-15T10:32:00Z"
      }
    ]
  }
}
```

**Notes:**
- This is optional, for showing who reacted
- Can be lazy loaded when user taps on reaction

---

### 3. Mentions

#### Send Message with Mentions
```
POST /v1/conversations/{conversationId}/messages
```

**Request Body:**
```json
{
  "content": "@john @jane Check this out!",
  "type": "TEXT",
  "mentions": [123, 456]
}
```

**Response:**
```json
{
  "id": 789,
  "conversationId": 1,
  "senderId": 2,
  "content": "@john @jane Check this out!",
  "type": "TEXT",
  "mentions": [123, 456],
  "mentionedUsers": [
    {
      "id": 123,
      "name": "John Doe"
    },
    {
      "id": 456,
      "name": "Jane Smith"
    }
  ],
  "createdAt": "2024-01-15T10:35:00Z"
}
```

**Logic:**
- Validate mentioned users are in conversation
- Send push notifications to mentioned users
- Store mentions as JSON array
- Broadcast via WebSocket

---

#### Get Conversation Members
```
GET /v1/conversations/{conversationId}/members
```

**Response:**
```json
{
  "members": [
    {
      "id": 1,
      "name": "John Doe",
      "username": "john",
      "avatarUrl": "https://...",
      "status": "ONLINE"
    },
    {
      "id": 2,
      "name": "Jane Smith",
      "username": "jane",
      "avatarUrl": "https://...",
      "status": "OFFLINE"
    }
  ]
}
```

**Notes:**
- Used for mention autocomplete
- Cache this on client side
- Include online status

---

### 4. Voice Messages

#### Send Voice Message
```
POST /v1/conversations/{conversationId}/messages
```

**Request Body:**
```json
{
  "content": "",
  "type": "VOICE",
  "mediaUrl": "https://res.cloudinary.com/.../voice_123.m4a",
  "duration": 15,
  "fileSize": 245760,
  "fileName": "voice_message.m4a"
}
```

**Response:**
```json
{
  "id": 789,
  "conversationId": 1,
  "senderId": 2,
  "content": "",
  "type": "VOICE",
  "mediaUrl": "https://res.cloudinary.com/.../voice_123.m4a",
  "duration": 15,
  "fileSize": 245760,
  "fileName": "voice_message.m4a",
  "createdAt": "2024-01-15T10:35:00Z"
}
```

**Notes:**
- Voice files uploaded to Cloudinary first
- Duration in seconds
- Max duration: 300 seconds (5 minutes)
- Format: M4A (AAC-LC codec)

---

## 📡 WebSocket Events

### 1. New Message with Reply
```json
{
  "type": "message.new",
  "data": {
    "id": 456,
    "conversationId": 1,
    "senderId": 2,
    "content": "Thanks!",
    "type": "TEXT",
    "replyToMessageId": 123,
    "replyToMessage": {
      "id": 123,
      "senderId": 1,
      "content": "Check this out!",
      "type": "TEXT"
    },
    "createdAt": "2024-01-15T10:35:00Z"
  }
}
```

---

### 2. Reaction Added/Removed
```json
{
  "type": "message.reaction",
  "data": {
    "messageId": 123,
    "userId": 2,
    "userName": "John Doe",
    "emoji": "👍",
    "action": "add",
    "reactions": {
      "👍": [1, 2, 3],
      "❤️": [4, 5]
    },
    "timestamp": "2024-01-15T10:35:00Z"
  }
}
```

**Notes:**
- `action` can be "add" or "remove"
- Broadcast to all users in conversation
- Include full reactions object for easy update

---

### 3. User Mentioned
```json
{
  "type": "message.mention",
  "data": {
    "messageId": 789,
    "conversationId": 1,
    "senderId": 2,
    "senderName": "Bob Wilson",
    "content": "@john Check this out!",
    "mentionedUserId": 123,
    "createdAt": "2024-01-15T10:35:00Z"
  }
}
```

**Notes:**
- Send to mentioned user only
- Can trigger push notification
- Include message preview

---

## 🗄️ Database Changes

### Messages Table
```sql
ALTER TABLE messages 
ADD COLUMN reply_to_message_id INTEGER,
ADD COLUMN reactions JSONB DEFAULT '{}',
ADD COLUMN mentions JSONB DEFAULT '[]';

ALTER TABLE messages
ADD CONSTRAINT fk_reply_to_message
FOREIGN KEY (reply_to_message_id) 
REFERENCES messages(id) 
ON DELETE SET NULL;

CREATE INDEX idx_messages_reply_to ON messages(reply_to_message_id);
CREATE INDEX idx_messages_reactions ON messages USING GIN (reactions);
CREATE INDEX idx_messages_mentions ON messages USING GIN (mentions);
```

---

## 🔒 Security & Validation

### Reply to Messages
- ✅ Validate replyToMessageId exists
- ✅ Validate user has access to conversation
- ✅ Handle deleted messages gracefully

### Reactions
- ✅ Validate emoji is valid Unicode emoji
- ✅ Rate limit: max 10 reactions per minute per user
- ✅ Sanitize emoji input
- ✅ Validate user has access to message

### Mentions
- ✅ Validate mentioned users exist
- ✅ Validate mentioned users are in conversation
- ✅ Parse mentions from content
- ✅ Limit max mentions per message (e.g., 10)

### Voice Messages
- ✅ Validate file type (M4A, MP3, WAV)
- ✅ Validate file size (max 10MB)
- ✅ Validate duration (max 300 seconds)
- ✅ Validate mediaUrl is from Cloudinary

---

## 📊 Implementation Priority

### Phase 1 (Must Have)
1. ✅ Reply to messages
2. ✅ Reactions (add/remove)
3. ✅ Voice messages

### Phase 2 (Should Have)
4. ✅ Mentions
5. ✅ Get conversation members
6. ✅ WebSocket events

### Phase 3 (Nice to Have)
7. ⏳ Get reactions with user details
8. ⏳ Mention notifications
9. ⏳ Reaction analytics

---

## 🧪 Testing

### Reply to Messages
```bash
# Send message with reply
curl -X POST http://localhost:3000/v1/conversations/1/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Thanks!",
    "replyToMessageId": 123
  }'
```

### Reactions
```bash
# Add reaction
curl -X POST http://localhost:3000/v1/messages/123/reactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"emoji": "👍"}'

# Remove reaction
curl -X DELETE http://localhost:3000/v1/messages/123/reactions/👍 \
  -H "Authorization: Bearer $TOKEN"
```

### Mentions
```bash
# Send message with mentions
curl -X POST http://localhost:3000/v1/conversations/1/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "@john Check this!",
    "mentions": [123]
  }'

# Get members
curl -X GET http://localhost:3000/v1/conversations/1/members \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📝 Notes

- Tất cả endpoints cần authentication
- Sử dụng JWT token trong Authorization header
- Response format: JSON
- Error handling: Return appropriate HTTP status codes
- Rate limiting: Implement for all endpoints
- Logging: Log all API calls for debugging
- Monitoring: Track API performance

---

## 🎯 Summary

**Tổng số endpoints mới:** 5
- POST /v1/conversations/{id}/messages (updated)
- POST /v1/messages/{id}/reactions
- DELETE /v1/messages/{id}/reactions/{emoji}
- GET /v1/messages/{id}/reactions (optional)
- GET /v1/conversations/{id}/members

**Tổng số WebSocket events mới:** 3
- message.new (updated)
- message.reaction
- message.mention

**Database changes:** 3 columns + 3 indexes

**Estimated implementation time:** 2-3 days

