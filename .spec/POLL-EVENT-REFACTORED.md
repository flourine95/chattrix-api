# Poll & Event Feature - Refactored with DTOs

## Overview
Đã refactor Poll và Event features để sử dụng DTO thay vì Map, và thêm endpoints để xem danh sách polls/events theo trạng thái.

## Kiến trúc mới

### DTOs cho data storage (JSONB)
- `PollData` - Cấu trúc poll trong metadata
- `PollOptionData` - Từng option trong poll
- `EventData` - Cấu trúc event trong metadata

### Response DTOs (API responses)
- `PollResponse` - Chi tiết 1 poll
- `PollListResponse` - Danh sách polls với thống kê
- `PollOptionResponse` - Chi tiết option với vote count
- `EventResponse` - Chi tiết 1 event
- `EventListResponse` - Danh sách events với thống kê

### Mapper utilities
- `PollDataMapper` - Convert giữa PollData DTO và Map (JSONB)
- `EventDataMapper` - Convert giữa EventData DTO và Map (JSONB)

---

## Poll Endpoints

### 1. Create Poll
```http
POST /api/v1/conversations/{conversationId}/polls
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "What's your favorite color?",
  "options": ["Red", "Blue", "Green"],
  "allowMultipleVotes": false,
  "expiresAt": "2025-12-31T23:59:59Z"
}

Response: MessageResponse (type=POLL)
```

### 2. Vote on Poll
```http
POST /api/v1/conversations/{conversationId}/polls/{messageId}/vote
Authorization: Bearer {token}
Content-Type: application/json

{
  "optionIds": [0, 1]
}

Response: MessageResponse (updated)
```

### 3. Get Poll Details
```http
GET /api/v1/conversations/{conversationId}/polls/{messageId}
Authorization: Bearer {token}

Response: PollResponse
{
  "messageId": 123,
  "question": "What's your favorite color?",
  "options": [
    {
      "id": 0,
      "text": "Red",
      "voteCount": 5,
      "voterIds": [1, 2, 3, 4, 5],
      "hasVoted": true
    },
    {
      "id": 1,
      "text": "Blue",
      "voteCount": 3,
      "voterIds": [6, 7, 8],
      "hasVoted": false
    }
  ],
  "allowMultiple": false,
  "anonymous": false,
  "closesAt": "2025-12-31T23:59:59Z",
  "isClosed": false,
  "totalVotes": 8,
  "createdBy": 1,
  "createdByUsername": "user1",
  "createdAt": "2025-12-30T10:00:00Z"
}
```

### 4. Get Conversation Polls (NEW!)
```http
GET /api/v1/conversations/{conversationId}/polls?status={status}
Authorization: Bearer {token}

Query Parameters:
- status: "all" | "active" | "closed" (default: "all")

Response: PollListResponse
{
  "polls": [
    {
      "messageId": 123,
      "question": "...",
      "options": [...],
      "isClosed": false,
      ...
    }
  ],
  "totalCount": 10,
  "activeCount": 7,
  "closedCount": 3
}
```

**Features:**
- Filter by status: active (chưa hết hạn), closed (đã hết hạn), all
- Sorted by creation date descending (mới nhất trước)
- Shows vote counts and current user's votes
- Anonymous mode hides voter IDs

---

## Event Endpoints

### 1. Create Event
```http
POST /api/v1/conversations/{conversationId}/events
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Team Meeting",
  "description": "Quarterly planning",
  "startTime": "2025-12-31T10:00:00Z",
  "endTime": "2025-12-31T11:00:00Z",
  "location": "Conference Room A"
}

Response: MessageResponse (type=EVENT)
```

### 2. RSVP to Event
```http
POST /api/v1/conversations/{conversationId}/events/{messageId}/rsvp
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "GOING"
}

Status values: "GOING" | "MAYBE" | "NOT_GOING"

Response: MessageResponse (updated)
```

### 3. Get Event Details
```http
GET /api/v1/conversations/{conversationId}/events/{messageId}
Authorization: Bearer {token}

Response: EventResponse
{
  "messageId": 456,
  "title": "Team Meeting",
  "description": "Quarterly planning",
  "startTime": "2025-12-31T10:00:00Z",
  "endTime": "2025-12-31T11:00:00Z",
  "location": "Conference Room A",
  "goingUserIds": [1, 2, 3],
  "maybeUserIds": [4],
  "notGoingUserIds": [5],
  "goingCount": 3,
  "maybeCount": 1,
  "notGoingCount": 1,
  "currentUserStatus": "GOING",
  "isPast": false,
  "createdBy": 1,
  "createdByUsername": "user1",
  "createdAt": "2025-12-30T10:00:00Z"
}
```

### 4. Get Conversation Events (NEW!)
```http
GET /api/v1/conversations/{conversationId}/events?status={status}
Authorization: Bearer {token}

Query Parameters:
- status: "all" | "upcoming" | "past" (default: "all")

Response: EventListResponse
{
  "events": [
    {
      "messageId": 456,
      "title": "Team Meeting",
      "startTime": "2025-12-31T10:00:00Z",
      "isPast": false,
      ...
    }
  ],
  "totalCount": 15,
  "upcomingCount": 10,
  "pastCount": 5
}
```

**Features:**
- Filter by status: upcoming (chưa diễn ra), past (đã qua), all
- Sorted by start time ascending (sắp diễn ra trước)
- Shows RSVP counts and current user's status
- isPast calculated based on endTime (or startTime if no endTime)

---

## Data Structure

### PollData (stored in message.metadata.poll)
```java
{
  "question": "string",
  "options": [
    {
      "id": long,
      "text": "string",
      "votes": [userId1, userId2, ...]
    }
  ],
  "allowMultiple": boolean,
  "anonymous": boolean,
  "closesAt": "ISO-8601 timestamp" (optional)
}
```

### EventData (stored in message.metadata.event)
```java
{
  "title": "string",
  "description": "string" (optional),
  "startTime": "ISO-8601 timestamp",
  "endTime": "ISO-8601 timestamp" (optional),
  "location": "string" (optional),
  "going": [userId1, userId2, ...],
  "maybe": [userId3, ...],
  "notGoing": [userId4, ...]
}
```

---

## Business Logic

### Poll Logic
1. **Create**: 
   - Check participant
   - Check `create_polls` permission for GROUP
   - Generate option IDs (0, 1, 2, ...)
   - Initialize empty votes arrays
   
2. **Vote**:
   - Validate poll not closed (check closesAt)
   - Validate option IDs exist
   - Check allowMultiple if voting for multiple options
   - Remove user's previous votes from all options
   - Add user to selected options
   
3. **Get List**:
   - Filter by status (active/closed based on closesAt)
   - Sort by createdAt DESC
   - Calculate vote counts
   - Hide voter IDs if anonymous

### Event Logic
1. **Create**:
   - Check participant
   - Check `create_events` permission for GROUP
   - Initialize empty RSVP lists
   
2. **RSVP**:
   - Remove user from all RSVP lists
   - Add user to selected list (going/maybe/notGoing)
   
3. **Get List**:
   - Filter by status (upcoming/past based on endTime or startTime)
   - Sort by startTime ASC
   - Calculate RSVP counts
   - Determine current user's status

---

## Cache & WebSocket

### Cache Invalidation
Both features invalidate caches after mutations:
```java
messageCache.invalidate(conversationId);
cacheManager.invalidateConversationCaches(conversationId, participantIds);
```

### WebSocket Events
- `CHAT_MESSAGE` - Poll/Event created
- `POLL_UPDATED` - Vote recorded
- `EVENT_RSVP` - RSVP recorded

---

## Testing Examples

### Test Poll Flow
```bash
# 1. Create poll
curl -X POST http://localhost:8080/api/v1/conversations/1/polls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Best meeting time?",
    "options": ["9 AM", "2 PM", "5 PM"],
    "allowMultipleVotes": false,
    "expiresAt": "2025-12-31T23:59:59Z"
  }'

# 2. Vote on poll
curl -X POST http://localhost:8080/api/v1/conversations/1/polls/123/vote \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [0]}'

# 3. Get poll details
curl http://localhost:8080/api/v1/conversations/1/polls/123 \
  -H "Authorization: Bearer $TOKEN"

# 4. Get all active polls
curl "http://localhost:8080/api/v1/conversations/1/polls?status=active" \
  -H "Authorization: Bearer $TOKEN"

# 5. Get all closed polls
curl "http://localhost:8080/api/v1/conversations/1/polls?status=closed" \
  -H "Authorization: Bearer $TOKEN"
```

### Test Event Flow
```bash
# 1. Create event
curl -X POST http://localhost:8080/api/v1/conversations/1/events \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Lunch",
    "description": "Monthly team lunch",
    "startTime": "2025-12-31T12:00:00Z",
    "endTime": "2025-12-31T13:00:00Z",
    "location": "Restaurant ABC"
  }'

# 2. RSVP to event
curl -X POST http://localhost:8080/api/v1/conversations/1/events/456/rsvp \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "GOING"}'

# 3. Get event details
curl http://localhost:8080/api/v1/conversations/1/events/456 \
  -H "Authorization: Bearer $TOKEN"

# 4. Get upcoming events
curl "http://localhost:8080/api/v1/conversations/1/events?status=upcoming" \
  -H "Authorization: Bearer $TOKEN"

# 5. Get past events
curl "http://localhost:8080/api/v1/conversations/1/events?status=past" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Implementation Files

### DTOs
- `dto/PollData.java` - Poll data structure
- `dto/PollOptionData.java` - Poll option structure
- `dto/EventData.java` - Event data structure

### Responses
- `responses/PollResponse.java` - Single poll response
- `responses/PollListResponse.java` - Poll list with stats
- `responses/PollOptionResponse.java` - Poll option with votes
- `responses/EventResponse.java` - Single event response
- `responses/EventListResponse.java` - Event list with stats

### Utilities
- `utils/PollDataMapper.java` - DTO ↔ Map conversion for polls
- `utils/EventDataMapper.java` - DTO ↔ Map conversion for events

### Services
- `services/message/PollService.java` - Poll business logic
- `services/message/EventService.java` - Event business logic

### Resources
- `resources/message/PollResource.java` - Poll REST endpoints
- `resources/message/EventResource.java` - Event REST endpoints

### Repository
- `repositories/MessageRepository.java` - Added `findByConversationIdAndType()`

---

## Benefits of Refactoring

### 1. Type Safety
- DTO classes với proper types thay vì `Map<String, Object>`
- Compile-time checking thay vì runtime errors
- IDE autocomplete và refactoring support

### 2. Maintainability
- Clear data structure với Lombok annotations
- Easy to add new fields
- Centralized conversion logic trong mapper utilities

### 3. Testability
- Easy to create test data với builders
- Mock DTOs thay vì complex Maps
- Clear assertions với typed fields

### 4. API Clarity
- Response DTOs show exactly what data is returned
- Separate DTOs for storage vs API responses
- Better documentation

### 5. Performance
- Jackson ObjectMapper handles conversion efficiently
- Reusable mapper instances
- Proper handling of Java time types

---

## Status
✅ Poll feature refactored with DTOs
✅ Event feature refactored with DTOs
✅ List endpoints added for both features
✅ Filter by status (active/closed, upcoming/past)
✅ Proper sorting and statistics
✅ Compilation successful
✅ Type-safe implementation
