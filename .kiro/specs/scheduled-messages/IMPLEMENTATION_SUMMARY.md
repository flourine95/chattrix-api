# Scheduled Messages Feature - Implementation Summary

## Overview

The Scheduled Messages feature has been fully implemented following the Jakarta EE architecture patterns used in the Chattrix API project. This feature allows users to schedule messages to be sent automatically at a specified future time.

## Implementation Components

### 1. Entity Layer

**File**: `src/main/java/com/chattrix/api/entities/ScheduledMessage.java`

- JPA entity with Lombok annotations (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- Relationships:
  - `@ManyToOne` to `Conversation` (LAZY fetch)
  - `@ManyToOne` to `User` (sender, LAZY fetch)
- Enum: `ScheduledMessageStatus` (SCHEDULED, SENT, FAILED, CANCELLED)
- Indexes for performance:
  - `idx_scheduled_time_status` - for scheduler queries
  - `idx_sender_conversation` - for user queries
  - `idx_status` - for status filtering
- Lifecycle callbacks: `@PrePersist`, `@PreUpdate` for timestamp management

### 2. Request DTOs

**Files**:
- `src/main/java/com/chattrix/api/requests/ScheduleMessageRequest.java`
  - Validation: `@NotBlank` for content, `@NotNull` for scheduledTime
  - Supports all message types and media fields
  
- `src/main/java/com/chattrix/api/requests/UpdateScheduledMessageRequest.java`
  - All fields optional for partial updates
  
- `src/main/java/com/chattrix/api/requests/BulkCancelScheduledMessagesRequest.java`
  - Validation: `@NotEmpty` for scheduledMessageIds list

### 3. Response DTOs

**Files**:
- `src/main/java/com/chattrix/api/responses/ScheduledMessageResponse.java`
  - Complete scheduled message details including conversation info
  
- `src/main/java/com/chattrix/api/responses/BulkCancelResponse.java`
  - Returns count of cancelled messages and list of failed IDs

### 4. Mapper

**File**: `src/main/java/com/chattrix/api/mappers/ScheduledMessageMapper.java`

- MapStruct mapper with CDI component model
- Maps `ScheduledMessage` entity to `ScheduledMessageResponse`
- Custom mappings for nested fields (conversation name, type, sender ID)

### 5. Repository

**File**: `src/main/java/com/chattrix/api/repositories/ScheduledMessageRepository.java`

- `@ApplicationScoped` CDI bean
- Uses `@PersistenceContext` for EntityManager injection
- Key methods:
  - `findByStatusAndScheduledTimeBefore()` - for scheduler
  - `findBySenderIdAndStatus()` - for user queries with filtering
  - `findBySenderIdAndConversationId()` - for conversation-specific queries
  - `cancelAllByUserAndConversation()` - bulk cancel when user leaves conversation
- All queries use LEFT JOIN FETCH for eager loading of relationships

### 6. Service Layer

**File**: `src/main/java/com/chattrix/api/services/message/ScheduledMessageService.java`

- `@ApplicationScoped` CDI bean
- `@Transactional` methods for data modification
- Key features:
  - **Validation**: Scheduled time must be in future and within 1 year
  - **Authorization**: Verifies user is conversation participant
  - **Reply validation**: Ensures reply-to message exists and belongs to same conversation
  - **Status management**: Prevents editing/cancelling sent messages
  - **Pagination**: Returns paginated results with metadata
  - **WebSocket notifications**: Sends real-time updates on success/failure

**Processing Logic** (`processScheduledMessages()`):
1. Query all SCHEDULED messages with scheduledTime <= now
2. For each message:
   - Verify sender is still a participant
   - Create actual Message entity
   - Update conversation's lastMessage
   - Increment unread counts
   - Update scheduled message status to SENT
   - Send WebSocket notification to all participants
3. On failure:
   - Update status to FAILED
   - Store failure reason
   - Send failure notification to sender only

### 7. Scheduler Service

**File**: `src/main/java/com/chattrix/api/services/message/ScheduledMessageProcessorService.java`

- `@Singleton` EJB with `@Startup` for automatic initialization
- `@Schedule` annotation: runs every 30 seconds
- Calls `ScheduledMessageService.processScheduledMessages()`
- Includes error handling and logging

### 8. REST Resource

**File**: `src/main/java/com/chattrix/api/resources/ScheduledMessageResource.java`

- JAX-RS resource with `@Path("/api/v1")`
- All endpoints protected with `@Secured` annotation
- Uses `UserContext.getCurrentUserId()` for authentication

**Endpoints**:

1. **POST** `/api/v1/conversations/{conversationId}/messages/schedule`
   - Create scheduled message
   - Returns 201 Created

2. **GET** `/api/v1/messages/scheduled`
   - List scheduled messages with pagination
   - Query params: conversationId, status, page, size
   - Default status: SCHEDULED

3. **GET** `/api/v1/messages/scheduled/{scheduledMessageId}`
   - Get single scheduled message
   - Returns 404 if not found or not owned by user

4. **PUT** `/api/v1/messages/scheduled/{scheduledMessageId}`
   - Update scheduled message
   - Only SCHEDULED messages can be edited
   - Returns 400 if already sent/failed/cancelled

5. **DELETE** `/api/v1/messages/scheduled/{scheduledMessageId}`
   - Cancel single scheduled message
   - Returns 400 if already sent

6. **DELETE** `/api/v1/messages/scheduled/bulk`
   - Cancel multiple scheduled messages
   - Returns count of cancelled and list of failed IDs

### 9. Database Schema

**File**: `scheduled-messages-migration.sql`

- PostgreSQL migration script
- Table: `scheduled_messages`
- Foreign keys with CASCADE and SET NULL constraints
- Indexes for query performance
- Comments for documentation

## WebSocket Events

### Success Event: `scheduled.message.sent`

**Payload**:
```json
{
  "scheduledMessageId": 123,
  "message": {
    "id": 456,
    "conversationId": 1,
    "senderId": 5,
    "content": "Happy Birthday!",
    "type": "TEXT",
    "sentAt": "2025-12-25T09:00:05Z",
    ...
  }
}
```

**Recipients**: All conversation participants

### Failure Event: `scheduled.message.failed`

**Payload**:
```json
{
  "scheduledMessageId": 123,
  "conversationId": 1,
  "failedReason": "User has left the conversation",
  "failedAt": "2025-12-25T09:00:05Z"
}
```

**Recipients**: Sender only

## Error Handling

### Validation Errors (400 Bad Request)
- Scheduled time in the past
- Scheduled time more than 1 year in future
- Invalid message type
- Cannot edit/cancel sent messages
- Reply-to message from different conversation

### Authorization Errors (401 Unauthorized)
- User not a conversation participant
- User not the message sender

### Not Found Errors (404 Not Found)
- Conversation not found
- Scheduled message not found
- Reply-to message not found
- User not found

## Security Features

1. **Authentication**: All endpoints require `@Secured` annotation
2. **Authorization**: 
   - Users can only schedule messages in conversations they participate in
   - Users can only view/edit/cancel their own scheduled messages
3. **Validation**: Bean Validation annotations on request DTOs
4. **Participant verification**: Scheduler verifies sender is still participant before sending

## Edge Cases Handled

1. **User leaves conversation**: 
   - Scheduler detects and marks message as FAILED
   - Sends failure notification to sender

2. **Conversation deleted**: 
   - CASCADE DELETE removes all scheduled messages

3. **Reply-to message deleted**: 
   - SET NULL constraint prevents errors
   - Message still sends without reply context

4. **System restart**: 
   - Scheduler automatically processes overdue messages on next run

5. **Concurrent modifications**: 
   - `@Transactional` ensures data consistency

## Performance Optimizations

1. **Indexes**: 
   - Composite index on (scheduled_time, status) for scheduler queries
   - Index on (sender_id, conversation_id) for user queries

2. **Eager loading**: 
   - LEFT JOIN FETCH in repository queries prevents N+1 problems

3. **Pagination**: 
   - All list endpoints support pagination

4. **Scheduler frequency**: 
   - 30-second interval balances responsiveness and resource usage

## Testing Recommendations

### Unit Tests
- Service validation logic
- Status transition rules
- Authorization checks
- Error handling

### Integration Tests
- End-to-end message scheduling flow
- Scheduler processing
- WebSocket event delivery
- Database constraints

### Property-Based Tests
- Scheduled time validation across random inputs
- Status transitions for all valid/invalid combinations
- Bulk operations with various input sizes

## Deployment Steps

1. **Database Migration**:
   ```bash
   psql -U postgres -d chattrix -f scheduled-messages-migration.sql
   ```

2. **Build Application**:
   ```bash
   docker compose up -d --build
   ```

3. **Verify Scheduler**:
   - Check logs for "Processing scheduled messages..." every 30 seconds
   - Create a test scheduled message 1 minute in future
   - Verify it sends automatically

4. **Test Endpoints**:
   - Use the provided API specification
   - Test with valid JWT token
   - Verify WebSocket events in browser console

## API Documentation

All endpoints follow the standard API response format:
```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... }
}
```

Error responses include:
```json
{
  "success": false,
  "message": "Error description",
  "code": "ERROR_CODE"
}
```

## Future Enhancements

1. **Recurring messages**: Support for daily/weekly/monthly schedules
2. **Timezone support**: Allow users to specify timezone for scheduling
3. **Preview notifications**: Remind users 5 minutes before sending
4. **Edit history**: Track changes to scheduled messages
5. **Batch scheduling**: Schedule multiple messages at once
6. **Templates**: Save and reuse message templates

## Maintenance Notes

- **Scheduler logs**: Monitor for processing errors
- **Failed messages**: Review failed_reason field for patterns
- **Performance**: Monitor query performance as data grows
- **Cleanup**: Consider archiving old SENT/CANCELLED messages

## Dependencies

No new dependencies required. Uses existing:
- Jakarta EE 10 (JAX-RS, CDI, JPA, Bean Validation)
- Hibernate 6.6
- Lombok 1.18.42
- MapStruct 1.5.5
- PostgreSQL 16

## Compliance with Project Standards

✅ Layered architecture (Resources → Services → Repositories → Entities)
✅ Lombok for boilerplate reduction
✅ MapStruct for entity-DTO mapping
✅ Field injection with `@Inject`
✅ `@Transactional` for data modifications
✅ Exception handling with BusinessException
✅ Proper HTTP status codes
✅ WebSocket integration
✅ Security with `@Secured` annotation
✅ Bean Validation on request DTOs
✅ Pagination support
✅ Docker-based deployment
