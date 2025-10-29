# API Requirements for Rich Media Chat Features

## Overview
This document outlines the backend API requirements to support rich media messaging features in the Chattrix chat application.

## 1. Message Types
The application now supports the following message types:
- `TEXT` - Plain text messages
- `IMAGE` - Image files (JPEG, PNG, GIF, etc.)
- `VIDEO` - Video files (MP4, MOV, etc.)
- `AUDIO` - Audio/voice messages (MP3, WAV, M4A, etc.)
- `DOCUMENT` - Document files (PDF, DOCX, XLSX, etc.)
- `LOCATION` - Location sharing with coordinates

## 2. Send Message API

### Endpoint
`POST /v1/conversations/{conversationId}/messages`

### Request Body
The API should accept the following fields:

#### Required Fields
- `content` (string) - Message content/caption

#### Optional Fields
- `type` (string) - Message type (TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION)
  - Default: "TEXT"
  
- `mediaUrl` (string) - URL of the uploaded media file
  - Required for: IMAGE, VIDEO, AUDIO, DOCUMENT
  
- `thumbnailUrl` (string) - URL of the thumbnail/preview image
  - Used for: VIDEO (video thumbnail)
  
- `fileName` (string) - Original file name
  - Used for: DOCUMENT, AUDIO
  
- `fileSize` (integer) - File size in bytes
  - Used for: IMAGE, VIDEO, AUDIO, DOCUMENT
  
- `duration` (integer) - Duration in seconds
  - Used for: VIDEO, AUDIO
  
- `latitude` (double) - Latitude coordinate
  - Required for: LOCATION
  
- `longitude` (double) - Longitude coordinate
  - Required for: LOCATION
  
- `locationName` (string) - Human-readable location name
  - Optional for: LOCATION
  
- `replyToMessageId` (integer) - ID of the message being replied to
  - Used for: Reply feature
  
- `mentions` (string) - JSON array of user IDs being mentioned
  - Format: `"[1, 2, 3]"`
  - Used for: Mention feature
  
- `reactions` (string) - JSON object of reactions
  - Format: `"{\"👍\": [1, 2], \"❤️\": [3]}"`
  - Key: emoji, Value: array of user IDs
  - Note: This is typically updated via a separate reaction endpoint

### Example Request Bodies

#### Text Message
```json
{
  "content": "Hello, how are you?",
  "type": "TEXT"
}
```

#### Image Message
```json
{
  "content": "Check out this photo!",
  "type": "IMAGE",
  "mediaUrl": "https://res.cloudinary.com/demo/image/upload/v1234567890/chat/image.jpg",
  "fileSize": 245678
}
```

#### Video Message
```json
{
  "content": "Funny video",
  "type": "VIDEO",
  "mediaUrl": "https://res.cloudinary.com/demo/video/upload/v1234567890/chat/video.mp4",
  "thumbnailUrl": "https://res.cloudinary.com/demo/video/upload/v1234567890/chat/video.jpg",
  "fileSize": 5242880,
  "duration": 45
}
```

#### Audio Message
```json
{
  "content": "Voice message",
  "type": "AUDIO",
  "mediaUrl": "https://res.cloudinary.com/demo/video/upload/v1234567890/chat/audio.mp3",
  "fileName": "voice_message.mp3",
  "fileSize": 123456,
  "duration": 30
}
```

#### Document Message
```json
{
  "content": "Project proposal",
  "type": "DOCUMENT",
  "mediaUrl": "https://res.cloudinary.com/demo/raw/upload/v1234567890/chat/document.pdf",
  "fileName": "proposal.pdf",
  "fileSize": 987654
}
```

#### Location Message
```json
{
  "content": "Shared location",
  "type": "LOCATION",
  "latitude": 21.028511,
  "longitude": 105.804817,
  "locationName": "Hanoi, Vietnam"
}
```

#### Reply with Mention
```json
{
  "content": "@john Thanks for the info!",
  "type": "TEXT",
  "replyToMessageId": 123,
  "mentions": "[456]"
}
```

### Response
The API should return the created message with all fields populated:

```json
{
  "id": 789,
  "content": "Check out this photo!",
  "type": "IMAGE",
  "mediaUrl": "https://res.cloudinary.com/demo/image/upload/v1234567890/chat/image.jpg",
  "fileSize": 245678,
  "createdAt": "2025-10-27T10:30:00Z",
  "conversationId": "conv-123",
  "sender": {
    "id": 1,
    "username": "john_doe",
    "displayName": "John Doe",
    "avatarUrl": "https://example.com/avatar.jpg"
  }
}
```

## 3. WebSocket Events

### Outgoing: Send Message
When sending a message via WebSocket, the client sends:
```json
{
  "type": "chat.message",
  "conversationId": "conv-123",
  "content": "Hello",
  "messageType": "TEXT",
  "mediaUrl": null,
  "thumbnailUrl": null,
  "fileName": null,
  "fileSize": null,
  "duration": null,
  "latitude": null,
  "longitude": null,
  "locationName": null,
  "replyToMessageId": null,
  "mentions": null
}
```

### Incoming: Receive Message
When receiving a message via WebSocket, the server broadcasts:
```json
{
  "type": "chat.message",
  "message": {
    "id": 789,
    "content": "Hello",
    "type": "TEXT",
    "createdAt": "2025-10-27T10:30:00Z",
    "conversationId": "conv-123",
    "sender": {
      "id": 1,
      "username": "john_doe",
      "displayName": "John Doe",
      "avatarUrl": "https://example.com/avatar.jpg"
    },
    "mediaUrl": null,
    "thumbnailUrl": null,
    "fileName": null,
    "fileSize": null,
    "duration": null,
    "latitude": null,
    "longitude": null,
    "locationName": null,
    "replyToMessageId": null,
    "reactions": null,
    "mentions": null
  }
}
```

## 4. Additional API Endpoints (Recommended)

### Add Reaction
`POST /v1/messages/{messageId}/reactions`
```json
{
  "emoji": "👍"
}
```

### Remove Reaction
`DELETE /v1/messages/{messageId}/reactions/{emoji}`

### Get Message Details
`GET /v1/messages/{messageId}`
- Should return full message details including reactions and reply information

## 5. Database Schema Recommendations

### Messages Table
Add the following columns to your messages table:
```sql
ALTER TABLE messages ADD COLUMN type VARCHAR(20) DEFAULT 'TEXT';
ALTER TABLE messages ADD COLUMN media_url TEXT;
ALTER TABLE messages ADD COLUMN thumbnail_url TEXT;
ALTER TABLE messages ADD COLUMN file_name VARCHAR(255);
ALTER TABLE messages ADD COLUMN file_size BIGINT;
ALTER TABLE messages ADD COLUMN duration INTEGER;
ALTER TABLE messages ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE messages ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE messages ADD COLUMN location_name VARCHAR(255);
ALTER TABLE messages ADD COLUMN reply_to_message_id INTEGER REFERENCES messages(id);
ALTER TABLE messages ADD COLUMN reactions JSONB;
ALTER TABLE messages ADD COLUMN mentions JSONB;
```

### Indexes
```sql
CREATE INDEX idx_messages_type ON messages(type);
CREATE INDEX idx_messages_reply_to ON messages(reply_to_message_id);
```

## 6. Media Upload Flow

The client-side implementation follows this flow:
1. User selects media (image, video, audio, document, or location)
2. Client uploads media to Cloudinary
3. Client receives media URL from Cloudinary
4. Client sends message with media URL to backend API
5. Backend stores message with media URL
6. Backend broadcasts message to other participants via WebSocket

**Note:** Media files are NOT uploaded to your backend server. They are uploaded directly to Cloudinary, and only the URLs are stored in your database.

## 7. Cloudinary Configuration

The client uses Cloudinary for media storage. You don't need to handle file uploads on your backend. However, you may want to:
- Validate media URLs to ensure they're from your Cloudinary account
- Store file metadata (size, type, duration) for analytics
- Implement cleanup jobs to delete unused media from Cloudinary

## 8. Security Considerations

1. **URL Validation**: Validate that mediaUrl fields contain valid URLs from your Cloudinary account
2. **File Size Limits**: Enforce maximum file sizes in your API
3. **Content Type Validation**: Verify that the file type matches the message type
4. **Rate Limiting**: Implement rate limiting for message sending
5. **Mentions Validation**: Verify that mentioned user IDs exist and are participants in the conversation

## 9. Testing Checklist

- [ ] Send text message
- [ ] Send image message with mediaUrl
- [ ] Send video message with mediaUrl and thumbnailUrl
- [ ] Send audio message with duration
- [ ] Send document message with fileName and fileSize
- [ ] Send location message with coordinates
- [ ] Reply to a message (replyToMessageId)
- [ ] Mention users in a message
- [ ] Add reaction to a message
- [ ] Remove reaction from a message
- [ ] Receive messages via WebSocket with all fields
- [ ] Get message list with all message types
- [ ] Validate that optional fields can be null/omitted

## 10. Migration Guide

If you have existing messages in your database:
1. Add new columns with default values
2. Set `type = 'TEXT'` for all existing messages
3. Ensure all new fields are nullable
4. Update your API to return new fields (with null values for old messages)
5. Test backward compatibility with old clients

