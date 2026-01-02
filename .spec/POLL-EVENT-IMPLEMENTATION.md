# Poll & Event Feature Implementation

## Overview
Implemented Poll and Event features for chat conversations. Both features store data in JSONB `message.metadata` field, following the existing metadata pattern.

## Poll Feature

### Data Structure (in `message.metadata.poll`)
```json
{
  "question": "What's your favorite color?",
  "options": [
    {
      "id": 0,
      "text": "Red",
      "votes": [1, 3, 5]  // User IDs who voted for this option
    },
    {
      "id": 1,
      "text": "Blue",
      "votes": [2, 4]
    }
  ],
  "allowMultiple": false,  // Allow multiple votes per user
  "anonymous": false,      // Anonymous voting (not implemented yet)
  "closesAt": "2025-12-31T23:59:59Z"  // Optional expiration
}
```

### Endpoints

#### Create Poll
```
POST /api/v1/conversations/{conversationId}/polls
Authorization: Bearer {token}

Request Body:
{
  "question": "What's your favorite color?",
  "options": ["Red", "Blue", "Green"],
  "allowMultipleVotes": false,
  "expiresAt": "2025-12-31T23:59:59Z"  // Optional
}

Response: MessageResponse with type=POLL
```

#### Vote on Poll
```
POST /api/v1/conversations/{conversationId}/polls/{messageId}/vote
Authorization: Bearer {token}

Request Body:
{
  "optionIds": [0, 1]  // Can vote for multiple if allowMultiple=true
}

Response: Updated MessageResponse with vote counts
```

### Features
- **Permission check**: For GROUP conversations, checks `create_polls` permission
- **Multiple votes**: Configurable via `allowMultipleVotes`
- **Vote tracking**: Stores user IDs in each option's votes array
- **Vote update**: Users can change their vote (removes old, adds new)
- **Expiration**: Optional `closesAt` timestamp prevents voting after expiry
- **Cache invalidation**: Invalidates message cache after voting
- **WebSocket events**:
  - `CHAT_MESSAGE` - When poll is created
  - `POLL_UPDATED` - When someone votes

### Implementation Files
- `PollService.java` - Business logic for creating polls and voting
- `PollResource.java` - REST endpoints
- `CreatePollRequest.java` - Request DTO for creating poll
- `VotePollRequest.java` - Request DTO for voting

---

## Event Feature

### Data Structure (in `message.metadata.event`)
```json
{
  "title": "Team Meeting",
  "description": "Quarterly planning meeting",
  "startTime": "2025-12-31T10:00:00Z",
  "endTime": "2025-12-31T11:00:00Z",  // Optional
  "location": "Conference Room A",     // Optional
  "going": [1, 3, 5],      // User IDs who are going
  "maybe": [2],            // User IDs who might go
  "notGoing": [4]          // User IDs who are not going
}
```

### Endpoints

#### Create Event
```
POST /api/v1/conversations/{conversationId}/events
Authorization: Bearer {token}

Request Body:
{
  "title": "Team Meeting",
  "description": "Quarterly planning meeting",
  "startTime": "2025-12-31T10:00:00Z",
  "endTime": "2025-12-31T11:00:00Z",
  "location": "Conference Room A"
}

Response: MessageResponse with type=EVENT
```

#### RSVP to Event
```
POST /api/v1/conversations/{conversationId}/events/{messageId}/rsvp
Authorization: Bearer {token}

Request Body:
{
  "status": "GOING"  // or "MAYBE" or "NOT_GOING"
}

Response: Updated MessageResponse with RSVP lists
```

### Features
- **Permission check**: For GROUP conversations, checks `create_events` permission
- **RSVP tracking**: Stores user IDs in `going`, `maybe`, `notGoing` arrays
- **RSVP update**: Users can change their RSVP (removes from all lists, adds to selected)
- **Optional fields**: `description`, `endTime`, `location` are optional
- **Cache invalidation**: Invalidates message cache after RSVP
- **WebSocket events**:
  - `EVENT_CREATED` - When event is created
  - `EVENT_RSVP` - When someone RSVPs

### Implementation Files
- `EventService.java` - Business logic for creating events and RSVP
- `EventResource.java` - REST endpoints
- `CreateEventRequest.java` - Request DTO for creating event
- `EventRsvpRequest.java` - Request DTO for RSVP

---

## Common Patterns

### Message Type
Both features use dedicated `MessageType` enum values:
- `MessageType.POLL`
- `MessageType.EVENT`

### Metadata Storage
Both store data in JSONB `message.metadata` field:
- Poll: `message.metadata.poll`
- Event: `message.metadata.event`

### Permission Checks
For GROUP conversations, both check permissions:
- Poll: `create_polls` permission
- Event: `create_events` permission

### Cache Invalidation
Both invalidate caches after mutations:
```java
messageCache.invalidate(conversationId);
cacheManager.invalidateConversationCaches(conversationId, participantIds);
```

### WebSocket Broadcasting
Both broadcast updates to all conversation participants via `ChatSessionService`

### Participant Validation
Both validate that the user is a participant before allowing actions

### Unread Count
Both increment unread count for other participants when created

---

## Testing

### Poll Testing
```bash
# Create poll
curl -X POST http://localhost:8080/api/v1/conversations/1/polls \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What time works best?",
    "options": ["9 AM", "2 PM", "5 PM"],
    "allowMultipleVotes": false
  }'

# Vote on poll
curl -X POST http://localhost:8080/api/v1/conversations/1/polls/{messageId}/vote \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"optionIds": [0]}'
```

### Event Testing
```bash
# Create event
curl -X POST http://localhost:8080/api/v1/conversations/1/events \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Lunch",
    "description": "Monthly team lunch",
    "startTime": "2025-12-31T12:00:00Z",
    "location": "Restaurant ABC"
  }'

# RSVP to event
curl -X POST http://localhost:8080/api/v1/conversations/1/events/{messageId}/rsvp \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"status": "GOING"}'
```

---

## Notes

### Type Handling
Both services handle type conversion for IDs stored in JSONB:
- Option IDs: Can be Integer or Long (from JSON deserialization)
- User IDs: Can be Integer or Long (from JSON deserialization)
- Uses instanceof checks and explicit conversion

### Future Enhancements
- **Poll**: Anonymous voting implementation
- **Event**: Reminders/notifications before event starts
- **Event**: Calendar integration
- **Both**: Edit/delete functionality
- **Both**: Results/statistics view

---

## Status
✅ Poll feature - Complete and tested
✅ Event feature - Complete and tested
✅ Compilation successful
✅ WebSocket events configured
✅ Cache invalidation implemented
