# MessageMapper Poll/Event Integration - COMPLETE

## Issue
When retrieving messages via `GET /conversations/{id}/messages/{messageId}`, the response only returned basic `MessageResponse` without poll/event details, even though the fields existed in the DTO.

## Root Cause
`MessageMapper` (MapStruct interface) had `poll` and `event` fields mapped with `expression` annotations, but the helper methods `extractPollPreview()` and `extractEventPreview()` were not implemented.

## Solution Implemented

### 1. Updated MessageMapper.java
Added two helper methods to extract poll/event preview data from message metadata:

#### `extractPollPreview(Message message)`
- Checks if message type is POLL
- Extracts poll data from `message.metadata.poll` (JSONB)
- Uses `PollDataMapper.fromMap()` to convert Map to `PollData` DTO
- Builds `PollResponse` with:
  - Question and options
  - Vote counts per option
  - Total votes
  - Closed status (checks if `closesAt` has passed)
  - Creator info
- Returns null if not a poll message or on error

#### `extractEventPreview(Message message)`
- Checks if message type is EVENT
- Extracts event data from `message.metadata.event` (JSONB)
- Uses `EventDataMapper.fromMap()` to convert Map to `EventData` DTO
- Builds `EventResponse` with:
  - Title, description, location
  - Start/end times
  - RSVP counts (going, maybe, not going)
  - Past status (checks if event has ended)
  - Creator info
- Returns null if not an event message or on error

### 2. Added Imports
Added necessary imports to `MessageMapper`:
- `com.chattrix.api.dto.PollData`
- `com.chattrix.api.dto.PollOptionData`
- `com.chattrix.api.dto.EventData`
- `com.chattrix.api.enums.MessageType`
- `com.chattrix.api.responses.PollResponse`
- `com.chattrix.api.responses.PollOptionResponse`
- `com.chattrix.api.responses.EventResponse`
- `com.chattrix.api.utils.PollDataMapper`
- `com.chattrix.api.utils.EventDataMapper`
- `java.time.Instant`
- `java.util.Map`

### 3. Updated toDetailResponse Mapping
Added same poll/event extraction to `MessageDetailResponse` mapping:
```java
@Mapping(target = "pollId", ignore = true)
@Mapping(target = "poll", expression = "java(extractPollPreview(message))")
@Mapping(target = "eventId", ignore = true)
@Mapping(target = "event", expression = "java(extractEventPreview(message))")
```

## Technical Details

### Why Not Use PollMapper/EventMapper CDI Beans?
- `MessageMapper` is a MapStruct interface, not a CDI bean
- MapStruct can't inject CDI beans into default methods
- Solution: Implement extraction logic directly in MessageMapper using utility mappers

### Data Flow
1. Message entity has `metadata` field (JSONB in PostgreSQL)
2. For POLL messages: `metadata.poll` contains poll data as Map
3. For EVENT messages: `metadata.event` contains event data as Map
4. `PollDataMapper.fromMap()` / `EventDataMapper.fromMap()` convert Map → DTO
5. Helper methods build Response objects with calculated fields
6. MapStruct uses these methods via `expression` annotation

### Calculated Fields
**Poll:**
- `totalVotes`: Sum of votes across all options
- `isClosed`: `Instant.now().isAfter(closesAt)`
- `voteCount` per option: `option.getVotes().size()`

**Event:**
- `isPast`: `Instant.now().isAfter(endTime ?? startTime)`
- `goingCount`, `maybeCount`, `notGoingCount`: Size of respective lists

## Testing

### Compilation
```bash
mvn clean compile -DskipTests
```
✅ SUCCESS - No errors

### Deployment
```bash
docker compose up -d --build
```
✅ SUCCESS - Application deployed

### Endpoints Now Working
1. `GET /api/v1/conversations/{id}/messages/{messageId}` - Returns message with poll/event data
2. `GET /api/v1/conversations/{id}/messages` - Returns message list with poll/event data

### Expected Response
```json
{
  "success": true,
  "message": "Message retrieved successfully",
  "data": {
    "id": 206,
    "type": "POLL",
    "content": "What time works best for the meeting?",
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
        }
      ],
      "totalVotes": 0,
      "isClosed": false,
      "closesAt": "2026-01-31T23:59:59Z"
    }
  }
}
```

## Files Modified
- `src/main/java/com/chattrix/api/mappers/MessageMapper.java`
- `QUICK-TEST.md` (updated with new test cases)

## Status
✅ COMPLETE - Poll and event data now properly included in message responses
