# Database JSONB Structure Documentation

## Overview

Chattrix sử dụng PostgreSQL JSONB columns để lưu trữ dữ liệu động. Document này mô tả chi tiết cấu trúc của các JSONB columns.

---

## 1. Message Table

### Column: `metadata` (JSONB)

Lưu trữ metadata của message tùy theo `type`.

#### Structure by MessageType:

### TEXT Message
```json
{}
// Empty - text messages không cần metadata
```

### IMAGE Message
```json
{
  "mediaUrl": "https://cdn.example.com/images/abc123.jpg",
  "thumbnailUrl": "https://cdn.example.com/images/abc123_thumb.jpg",
  "fileName": "photo.jpg",
  "fileSize": 2048576
}
```

### VIDEO Message
```json
{
  "mediaUrl": "https://cdn.example.com/videos/xyz789.mp4",
  "thumbnailUrl": "https://cdn.example.com/videos/xyz789_thumb.jpg",
  "fileName": "video.mp4",
  "fileSize": 10485760,
  "duration": 120
}
```

### AUDIO Message
```json
{
  "mediaUrl": "https://cdn.example.com/audio/voice123.mp3",
  "fileName": "voice_message.mp3",
  "fileSize": 524288,
  "duration": 45
}
```

### FILE Message
```json
{
  "mediaUrl": "https://cdn.example.com/files/doc456.pdf",
  "fileName": "document.pdf",
  "fileSize": 1048576
}
```

### LOCATION Message
```json
{
  "latitude": 10.762622,
  "longitude": 106.660172,
  "locationName": "Saigon Centre, District 1, HCMC"
}
```

### POLL Message
```json
{
  "poll": {
    "question": "What's your favorite programming language?",
    "options": [
      {
        "id": "opt1",
        "text": "Java",
        "votes": [123, 456, 789]
      },
      {
        "id": "opt2",
        "text": "Python",
        "votes": [111, 222]
      }
    ],
    "expiresAt": 1735632000000,
    "allowMultiple": false,
    "anonymous": true,
    "totalVotes": 5,
    "closed": false
  }
}
```

### EVENT Message
```json
{
  "event": {
    "title": "Team Meeting",
    "description": "Discuss Q1 roadmap",
    "startTime": 1735632000000,
    "endTime": 1735635600000,
    "location": "Office - Meeting Room A",
    "participants": [123, 456, 789],
    "rsvp": {
      "123": "GOING",
      "456": "MAYBE",
      "789": "NOT_GOING"
    }
  }
}
```

**RSVP Status:**
- `GOING` - User will attend
- `MAYBE` - User might attend
- `NOT_GOING` - User won't attend
- `null` - User hasn't responded

### SYSTEM Message

#### User Joined
```json
{
  // No additional metadata
}
```

#### User Left
```json
{
  // No additional metadata
}
```

#### User Kicked
```json
{
  "kickedBy": 456
}
```

#### User Added
```json
{
  "addedBy": 456
}
```

#### Multiple Users Added
```json
{
  "addedBy": 456,
  "addedUserIds": [123, 789, 101]
}
```

#### Group Name Changed
```json
{
  "oldName": "Old Group Name",
  "newName": "New Group Name"
}
```

#### User Promoted to Admin
```json
{
  "promotedBy": 456
}
```

#### User Demoted from Admin
```json
{
  "demotedBy": 456
}
```

#### Member Muted
```json
{
  "mutedBy": 456,
  "mutedUntil": 1735632000000
}
```

#### Member Unmuted
```json
{
  "unmutedBy": 456
}
```

#### User Joined via Invite Link
```json
{
  "invitedBy": 456
}
```

#### Scheduled Message Failed
```json
{
  "failedReason": "User has left the conversation"
}
```

---

### Column: `reactions` (JSONB)

Lưu trữ reactions của users cho message.

```json
{
  "👍": [123, 456, 789],
  "❤️": [123, 111],
  "😂": [456],
  "🎉": [789, 101, 112]
}
```

**Structure:**
- Key: Emoji string
- Value: Array of user IDs who reacted with that emoji

---

### Column: `mentions` (JSONB)

Lưu trữ danh sách user IDs được mention trong message.

```json
[123, 456, 789]
```

**Structure:**
- Array of user IDs (Long)

---

## 2. Conversation Table

### Column: `metadata` (JSONB)

#### Group Invite Link
```json
{
  "inviteLink": {
    "code": "abc123xyz",
    "createdBy": 456,
    "createdAt": 1735632000000,
    "expiresAt": 1735718400000,
    "maxUses": 100,
    "currentUses": 5,
    "active": true
  }
}
```

#### Group Settings
```json
{
  "settings": {
    "allowMemberInvite": true,
    "allowMemberPost": true,
    "requireAdminApproval": false
  }
}
```

---

## 3. User Table

### Column: `note_metadata` (JSONB)

Lưu trữ notes của user về các users khác.

```json
{
  "123": "My best friend from college",
  "456": "Project manager - very responsive",
  "789": "Met at conference 2024"
}
```

**Structure:**
- Key: User ID (as string)
- Value: Note text

---

## Usage in Code

### Reading Metadata (Old Way - Not Recommended)
```java
Map<String, Object> metadata = message.getMetadata();
String mediaUrl = (String) metadata.get("mediaUrl"); // Unsafe cast
Long fileSize = ((Number) metadata.get("fileSize")).longValue(); // Can throw NPE
```

### Reading Metadata (New Way - Recommended)
```java
MessageMetadata metadata = MessageMetadata.fromMap(message.getMetadata());
String mediaUrl = metadata.getMediaUrl(); // Type-safe
Long fileSize = metadata.getFileSize(); // Null-safe

// Convenience methods
if (metadata.hasMedia()) {
    // Handle media
}
if (metadata.hasLocation()) {
    // Handle location
}
```

### Writing Metadata (Old Way - Not Recommended)
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("mediaUrl", url);
metadata.put("fileSize", size);
message.setMetadata(metadata);
```

### Writing Metadata (New Way - Recommended)
```java
MessageMetadata metadata = MessageMetadata.builder()
    .mediaUrl(url)
    .fileSize(size)
    .build();
message.setMetadata(metadata.toMap());
```

---

## Database Queries

### Query messages with specific metadata
```sql
-- Find messages with media
SELECT * FROM messages 
WHERE metadata->>'mediaUrl' IS NOT NULL;

-- Find messages with file size > 1MB
SELECT * FROM messages 
WHERE (metadata->>'fileSize')::bigint > 1048576;

-- Find location messages in specific area
SELECT * FROM messages 
WHERE type = 'LOCATION'
  AND (metadata->>'latitude')::numeric BETWEEN 10.0 AND 11.0
  AND (metadata->>'longitude')::numeric BETWEEN 106.0 AND 107.0;

-- Find system messages where user was kicked
SELECT * FROM messages 
WHERE type = 'SYSTEM'
  AND content::json->>'type' = 'user_kicked'
  AND (metadata->>'kickedBy')::bigint = 456;
```

### Query reactions
```sql
-- Find messages with specific reaction
SELECT * FROM messages 
WHERE reactions ? '👍';

-- Count reactions for a message
SELECT 
  jsonb_object_keys(reactions) as emoji,
  jsonb_array_length(reactions->jsonb_object_keys(reactions)) as count
FROM messages 
WHERE id = 123;

-- Find messages where user reacted
SELECT * FROM messages 
WHERE reactions @> '{"👍": [123]}';
```

### Query mentions
```sql
-- Find messages mentioning specific user
SELECT * FROM messages 
WHERE mentions @> '[123]';

-- Find messages with any mentions
SELECT * FROM messages 
WHERE mentions IS NOT NULL 
  AND jsonb_array_length(mentions) > 0;
```

---

## Migration Considerations

### Adding New Metadata Fields

✅ **Safe** - JSONB is schema-less:
```java
// Just add to MessageMetadata class
private String newField;

// Update toMap() and fromMap()
```

### Removing Metadata Fields

⚠️ **Requires cleanup**:
```sql
-- Remove field from all messages
UPDATE messages 
SET metadata = metadata - 'oldField';
```

### Renaming Metadata Fields

⚠️ **Requires migration**:
```sql
-- Rename field
UPDATE messages 
SET metadata = jsonb_set(
  metadata - 'oldName',
  '{newName}',
  metadata->'oldName'
)
WHERE metadata ? 'oldName';
```

---

## Best Practices

### ✅ DO:
- Use `MessageMetadata` wrapper class for type safety
- Document new metadata fields in this file
- Use builder pattern for creating metadata
- Check for null before accessing nested fields
- Use convenience methods (`hasMedia()`, `hasLocation()`)

### ❌ DON'T:
- Don't use `Map.put()` directly
- Don't cast without null checks
- Don't store large binary data in JSONB
- Don't use JSONB for frequently queried fields (use regular columns)
- Don't forget to update this documentation

---

## Performance Tips

1. **Index JSONB fields** if queried frequently:
```sql
CREATE INDEX idx_messages_media_url ON messages 
USING GIN ((metadata->'mediaUrl'));
```

2. **Use `?` operator** for existence checks (faster than `IS NOT NULL`):
```sql
WHERE metadata ? 'mediaUrl'
```

3. **Use `@>` operator** for containment checks:
```sql
WHERE reactions @> '{"👍": [123]}'
```

4. **Avoid deep nesting** - keep JSONB structure flat when possible

---

## Về việc gộp reactions và mentions vào metadata

### ❌ Không nên gộp vì:

1. **Reactions và mentions được query riêng biệt rất nhiều**
   - "Tìm tất cả messages mà user X đã react"
   - "Tìm tất cả messages mention user Y"
   - Nếu gộp vào metadata, query sẽ chậm hơn

2. **Reactions và mentions là first-class features**
   - Không phải metadata phụ
   - Có logic riêng (add/remove reaction, count reactions)
   - Cần index riêng để query nhanh

3. **Separation of concerns**
   - `metadata`: Dữ liệu phụ thuộc vào message type
   - `reactions`: Social interaction data
   - `mentions`: User reference data

### ✅ Nên giữ nguyên 3 columns riêng:
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "metadata", columnDefinition = "jsonb")
private Map<String, Object> metadata;  // Message-type specific data

@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "reactions", columnDefinition = "jsonb")
private Map<String, List<Long>> reactions;  // Social interactions

@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "mentions", columnDefinition = "jsonb")
private List<Long> mentions;  // User references
```

---

## Summary

- **metadata**: Flexible, type-specific data (media, location, system events)
- **reactions**: Social engagement data
- **mentions**: User references
- Use `MessageMetadata` wrapper for type safety
- Keep this document updated when adding new fields
