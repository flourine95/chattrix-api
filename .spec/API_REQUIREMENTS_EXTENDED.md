# Extended API Requirements - Rich Media Chat Features

## Overview

This document extends the original `API_REQUIREMENTS.md` with additional endpoints needed for:
- Reply to messages
- Reactions (emoji)
- Mentions (@username)
- Voice messages
- Image compression metadata

## Table of Contents

1. [Reply to Messages](#1-reply-to-messages)
2. [Reactions](#2-reactions)
3. [Mentions](#3-mentions)
4. [Voice Messages](#4-voice-messages)
5. [WebSocket Events](#5-websocket-events)
6. [Database Schema Updates](#6-database-schema-updates)

---

## 1. Reply to Messages

### 1.1 Send Message with Reply

**Endpoint:** `POST /v1/conversations/{conversationId}/messages`

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
  "senderName": "John Doe",
  "content": "Thanks for sharing!",
  "type": "TEXT",
  "replyToMessageId": 123,
  "replyToMessage": {
    "id": 123,
    "senderId": 1,
    "senderName": "Jane Smith",
    "content": "Check out this photo!",
    "type": "IMAGE",
    "mediaUrl": "https://...",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "createdAt": "2024-01-15T10:35:00Z",
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

### 1.2 Get Message with Reply Context

**Endpoint:** `GET /v1/messages/{messageId}`

**Response:**
```json
{
  "id": 456,
  "conversationId": 1,
  "senderId": 2,
  "senderName": "John Doe",
  "content": "Thanks for sharing!",
  "type": "TEXT",
  "replyToMessageId": 123,
  "replyToMessage": {
    "id": 123,
    "senderId": 1,
    "senderName": "Jane Smith",
    "content": "Check out this photo!",
    "type": "IMAGE",
    "mediaUrl": "https://...",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "createdAt": "2024-01-15T10:35:00Z"
}
```

**Notes:**
- When fetching messages, include `replyToMessage` object if `replyToMessageId` is not null
- The `replyToMessage` should contain basic info (id, sender, content preview, type, mediaUrl)
- Don't nest replies more than 1 level deep to avoid circular references

---

## 2. Reactions

### 2.1 Add Reaction to Message

**Endpoint:** `POST /v1/messages/{messageId}/reactions`

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

### 2.2 Remove Reaction from Message

**Endpoint:** `DELETE /v1/messages/{messageId}/reactions/{emoji}`

**Example:** `DELETE /v1/messages/123/reactions/👍`

**Response:**
```json
{
  "messageId": 123,
  "reactions": {
    "❤️": [4, 5]
  }
}
```

### 2.3 Get Message Reactions

**Endpoint:** `GET /v1/messages/{messageId}/reactions`

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
- Store reactions as JSON in database: `{"👍": [1, 2, 3], "❤️": [4, 5]}`
- Each emoji maps to an array of user IDs who reacted
- When user adds reaction:
  - If emoji doesn't exist, create new entry
  - If emoji exists and user not in array, add user ID
  - If emoji exists and user in array, remove user ID (toggle)
- Broadcast reaction updates via WebSocket

---

## 3. Mentions

### 3.1 Send Message with Mentions

**Endpoint:** `POST /v1/conversations/{conversationId}/messages`

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
  "senderName": "Bob Wilson",
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

### 3.2 Get Conversation Members (for mention autocomplete)

**Endpoint:** `GET /v1/conversations/{conversationId}/members`

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
- Parse mentions from content using regex: `@(\w+)`
- Store mentioned user IDs as JSON array: `[123, 456]`
- Send push notifications to mentioned users
- Highlight mentions in UI with different color

---

## 4. Voice Messages

### 4.1 Send Voice Message

**Endpoint:** `POST /v1/conversations/{conversationId}/messages`

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
  "senderName": "John Doe",
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
- Voice messages are uploaded to Cloudinary as audio files
- Duration is in seconds
- File format: M4A (AAC-LC codec)
- Typical bitrate: 128kbps
- Max duration: 5 minutes (300 seconds)

---

## 5. WebSocket Events

### 5.1 New Message with Reply

**Event:** `message.new`

**Payload:**
```json
{
  "type": "message.new",
  "data": {
    "id": 456,
    "conversationId": 1,
    "senderId": 2,
    "senderName": "John Doe",
    "content": "Thanks!",
    "type": "TEXT",
    "replyToMessageId": 123,
    "replyToMessage": {
      "id": 123,
      "senderId": 1,
      "senderName": "Jane Smith",
      "content": "Check this out!",
      "type": "TEXT"
    },
    "createdAt": "2024-01-15T10:35:00Z"
  }
}
```

### 5.2 Reaction Added/Removed

**Event:** `message.reaction`

**Payload:**
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
- Broadcast to all users in the conversation
- Update local message reactions in real-time

### 5.3 User Mentioned

**Event:** `message.mention`

**Payload:**
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
- Send separate event to each mentioned user
- Can be used to show notification badge
- Mentioned user should receive push notification

---

## 6. Database Schema Updates

### 6.1 Messages Table

```sql
-- Already exists from previous implementation
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS type VARCHAR(20) DEFAULT 'TEXT',
ADD COLUMN IF NOT EXISTS media_url TEXT,
ADD COLUMN IF NOT EXISTS thumbnail_url TEXT,
ADD COLUMN IF NOT EXISTS file_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS file_size BIGINT,
ADD COLUMN IF NOT EXISTS duration INTEGER,
ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS location_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS reply_to_message_id INTEGER,
ADD COLUMN IF NOT EXISTS reactions JSONB,
ADD COLUMN IF NOT EXISTS mentions JSONB;

-- Add foreign key for reply_to_message_id
ALTER TABLE messages
ADD CONSTRAINT fk_reply_to_message
FOREIGN KEY (reply_to_message_id) 
REFERENCES messages(id) 
ON DELETE SET NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_messages_type ON messages(type);
CREATE INDEX IF NOT EXISTS idx_messages_reply_to ON messages(reply_to_message_id);
CREATE INDEX IF NOT EXISTS idx_messages_reactions ON messages USING GIN (reactions);
CREATE INDEX IF NOT EXISTS idx_messages_mentions ON messages USING GIN (mentions);
```

### 6.2 Reactions Table (Alternative Approach)

If you prefer a separate table for reactions instead of JSON:

```sql
CREATE TABLE message_reactions (
  id SERIAL PRIMARY KEY,
  message_id INTEGER NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  emoji VARCHAR(10) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(message_id, user_id, emoji)
);

CREATE INDEX idx_reactions_message ON message_reactions(message_id);
CREATE INDEX idx_reactions_user ON message_reactions(user_id);
```

### 6.3 Mentions Table (Alternative Approach)

If you prefer a separate table for mentions:

```sql
CREATE TABLE message_mentions (
  id SERIAL PRIMARY KEY,
  message_id INTEGER NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(message_id, user_id)
);

CREATE INDEX idx_mentions_message ON message_mentions(message_id);
CREATE INDEX idx_mentions_user ON message_mentions(user_id);
```

---

## 7. API Response Examples

### 7.1 Complete Message Object

```json
{
  "id": 123,
  "conversationId": 1,
  "senderId": 2,
  "senderName": "John Doe",
  "senderAvatarUrl": "https://...",
  "content": "@jane Check out this photo!",
  "type": "IMAGE",
  "mediaUrl": "https://res.cloudinary.com/.../photo.jpg",
  "thumbnailUrl": "https://res.cloudinary.com/.../photo_thumb.jpg",
  "fileName": "vacation.jpg",
  "fileSize": 1024000,
  "width": 1920,
  "height": 1080,
  "replyToMessageId": 100,
  "replyToMessage": {
    "id": 100,
    "senderId": 1,
    "senderName": "Jane Smith",
    "content": "Where did you go?",
    "type": "TEXT",
    "createdAt": "2024-01-15T10:00:00Z"
  },
  "reactions": {
    "👍": [1, 3, 5],
    "❤️": [2, 4]
  },
  "mentions": [1],
  "mentionedUsers": [
    {
      "id": 1,
      "name": "Jane Smith"
    }
  ],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

---

## 8. Implementation Notes

### 8.1 Reply to Messages
- Include `replyToMessage` object when fetching messages
- Only include basic fields to avoid deep nesting
- Handle deleted messages gracefully (show "Message deleted")

### 8.2 Reactions
- Use JSON column for simple implementation
- Use separate table for better querying and analytics
- Limit to 6-8 different emojis per message
- Broadcast updates via WebSocket

### 8.3 Mentions
- Parse mentions from content using regex
- Validate mentioned users are in the conversation
- Send push notifications to mentioned users
- Store as JSON array of user IDs

### 8.4 Voice Messages
- Upload to Cloudinary as audio files
- Extract duration from audio file metadata
- Show waveform visualization in UI
- Support playback controls (play/pause/seek)

### 8.5 Image Compression
- Compress images on client before upload
- Target size: 1-2MB per image
- Max dimensions: 1920x1920
- Quality: 75-85%
- Store original dimensions in database

---

## 9. Error Handling

### 9.1 Reply to Non-existent Message
```json
{
  "error": "REPLY_MESSAGE_NOT_FOUND",
  "message": "The message you're replying to does not exist",
  "statusCode": 404
}
```

### 9.2 Invalid Emoji
```json
{
  "error": "INVALID_EMOJI",
  "message": "The emoji provided is not valid",
  "statusCode": 400
}
```

### 9.3 Mention Non-member
```json
{
  "error": "USER_NOT_IN_CONVERSATION",
  "message": "Cannot mention user who is not in this conversation",
  "statusCode": 400
}
```

---

## 10. Testing Checklist

- [ ] Send message with reply
- [ ] Display quoted message in UI
- [ ] Scroll to original message when tapping quote
- [ ] Add reaction to message
- [ ] Remove reaction from message
- [ ] Display reactions with user count
- [ ] Show who reacted when tapping reaction
- [ ] Send message with mentions
- [ ] Autocomplete mentions while typing
- [ ] Highlight mentions in message
- [ ] Send notification to mentioned users
- [ ] Record voice message
- [ ] Upload voice message to Cloudinary
- [ ] Play voice message with controls
- [ ] Compress image before upload
- [ ] Show upload progress
- [ ] Cancel upload
- [ ] WebSocket real-time updates for all features

---

## 11. Security Considerations

- Validate user has access to conversation before allowing actions
- Rate limit reactions (max 10 per minute per user)
- Sanitize emoji input to prevent injection
- Validate mentioned users exist and are in conversation
- Limit voice message duration (max 5 minutes)
- Validate file types and sizes on backend
- Use signed URLs for Cloudinary uploads (optional)

---

## 12. Performance Optimization

- Cache conversation members for mention autocomplete
- Debounce mention search queries
- Lazy load reactions details (show count first, load users on tap)
- Compress images before upload
- Use CDN for media delivery (Cloudinary)
- Index reactions and mentions columns for fast queries
- Paginate message history
- Use WebSocket for real-time updates instead of polling

