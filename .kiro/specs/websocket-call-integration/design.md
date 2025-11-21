# Design Document

## Overview

This design implements real-time bidirectional call signaling through the existing WebSocket connection at `/ws/chat`. The current architecture uses REST API for call actions (accept, reject, end) and WebSocket only for server-to-client notifications, which introduces unnecessary latency. This design integrates call message handling directly into `ChatServerEndpoint`, allowing clients to send call actions through WebSocket and receive instant responses.

The design maintains backward compatibility with the existing REST API endpoints while providing a faster WebSocket-based alternative for real-time call operations.

## Architecture

### Current Architecture (Before)
```
Client → REST API → CallService → WebSocketNotificationService → ChatSessionService → WebSocket → Client
```

### New Architecture (After)
```
Client ←→ WebSocket ←→ ChatServerEndpoint ←→ CallService ←→ WebSocketNotificationService → ChatSessionService → WebSocket → Other Client
```

### Key Changes

1. **Bidirectional WebSocket Communication**: Clients can now send call actions (accept, reject, end) through WebSocket instead of REST API
2. **Unified Message Handler**: `ChatServerEndpoint.onMessage()` now handles both chat and call messages
3. **Consistent Message Format**: All call messages use the `WebSocketMessage<T>` wrapper with type and payload fields
4. **Single Connection**: Maintains one WebSocket connection per user for both chat and call features

## Components and Interfaces

### 1. ChatServerEndpoint (Modified)

**Responsibilities:**
- Handle incoming WebSocket messages for both chat and call operations
- Route call messages to appropriate handler methods
- Invoke CallService for call business logic
- Send error messages for invalid operations
- Maintain user authentication and session management

**New Dependencies:**
```java
@Inject
private CallService callService;
```

**New Message Handlers:**
```java
private void processCallAccept(Long userId, WebSocketMessage<?> message)
private void processCallReject(Long userId, WebSocketMessage<?> message)
private void processCallEnd(Long userId, WebSocketMessage<?> message)
```

### 2. CallService (Modified)

**New Methods:**
```java
public CallResponse acceptCallViaWebSocket(String callId, String userId)
public CallResponse rejectCallViaWebSocket(String callId, String userId, String reason)
public CallResponse endCallViaWebSocket(String callId, String userId, Integer durationSeconds)
```

These methods are similar to existing methods but optimized for WebSocket context (no need for request DTOs).

### 3. WebSocket Message DTOs (New)

**CallAcceptDto:**
```java
public class CallAcceptDto {
    private String callId;
}
```

**CallRejectDto:**
```java
public class CallRejectDto {
    private String callId;
    private String reason; // "busy", "declined", "unavailable"
}
```

**CallEndDto:**
```java
public class CallEndDto {
    private String callId;
    private Integer durationSeconds; // optional
}
```

**CallErrorDto:**
```java
public class CallErrorDto {
    private String callId;
    private String errorType; // "call_not_found", "unauthorized", "invalid_status"
    private String message;
}
```

### 4. Message Type Constants

**Server to Client (Outgoing):**
- `call_invitation` - Call invitation sent to callee
- `call_accepted` - Acceptance notification sent to caller
- `call_rejected` - Rejection notification sent to caller
- `call_ended` - End notification sent to other participant
- `call_timeout` - Timeout notification sent to both participants
- `call_quality_warning` - Quality warning sent to participant
- `call_error` - Error message sent to requester

**Client to Server (Incoming):**
- `call.accept` - Callee accepts the call
- `call.reject` - Callee rejects the call
- `call.end` - Participant ends the call

## Data Models

### WebSocketMessage<T> (Existing)
```java
public class WebSocketMessage<T> {
    private String type;
    private T payload;
}
```

### CallInvitationMessage (Existing)
```java
public class CallInvitationMessage {
    private String type = "call_invitation";
    private CallInvitationData data;
    private Instant timestamp;
}
```

### CallAcceptedMessage (Existing)
```java
public class CallAcceptedMessage {
    private String type = "call_accepted";
    private CallAcceptedData data;
    private Instant timestamp;
}
```

### CallRejectedMessage (Existing)
```java
public class CallRejectedMessage {
    private String type = "call_rejected";
    private CallRejectedData data;
    private Instant timestamp;
}
```

### CallEndedMessage (Existing)
```java
public class CallEndedMessage {
    private String type = "call_ended";
    private CallEndedData data;
    private Instant timestamp;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Call invitation messages contain all required fields
*For any* call initiation, the WebSocket message sent to the callee should include callId, channelId, callerId, callerName, callerAvatar, and callType fields.
**Validates: Requirements 1.3**

### Property 2: Only the callee can accept a call
*For any* call and any user attempting to accept it, the acceptance should succeed if and only if the user is the callee of that call.
**Validates: Requirements 2.4**

### Property 3: Call acceptance triggers notification to caller
*For any* call that is successfully accepted, a WebSocket message with type "call_accepted" should be sent to the caller.
**Validates: Requirements 2.6**

### Property 4: Accepted notification contains required fields
*For any* call acceptance notification, the message should include callId and acceptedBy fields.
**Validates: Requirements 2.7**

### Property 5: Message parsing extracts all fields
*For any* valid call.reject message, the WebSocket endpoint should successfully extract both callId and reason from the payload.
**Validates: Requirements 3.2**

### Property 6: Only the callee can reject a call
*For any* call and any user attempting to reject it, the rejection should succeed if and only if the user is the callee of that call.
**Validates: Requirements 3.4**

### Property 7: Call rejection triggers notification to caller
*For any* call that is successfully rejected, a WebSocket message with type "call_rejected" should be sent to the caller.
**Validates: Requirements 3.6**

### Property 8: Rejected notification contains required fields
*For any* call rejection notification, the message should include callId, rejectedBy, and reason fields.
**Validates: Requirements 3.7**

### Property 9: Only participants can end a call
*For any* call and any user attempting to end it, the ending should succeed if and only if the user is either the caller or the callee of that call.
**Validates: Requirements 4.4**

### Property 10: Duration is calculated when not provided
*For any* call ending where durationSeconds is null and startTime is not null, the system should calculate duration as the difference between endTime and startTime.
**Validates: Requirements 4.6**

### Property 11: Call ending triggers notification to other participant
*For any* call that is successfully ended, a WebSocket message with type "call_ended" should be sent to the participant who did not initiate the ending.
**Validates: Requirements 4.7**

### Property 12: Ended notification contains required fields
*For any* call ended notification, the message should include callId, endedBy, and durationSeconds fields.
**Validates: Requirements 4.8**

### Property 13: Timeout notification contains callId
*For any* call timeout notification, the message should include the callId field.
**Validates: Requirements 5.2**

### Property 14: Quality level is valid enum value
*For any* call quality warning message, the quality field should be one of POOR, BAD, or VERY_BAD.
**Validates: Requirements 6.3**

### Property 15: All call messages use wrapper format
*For any* call-related WebSocket message sent by the server, the message should be wrapped in WebSocketMessage with type and payload fields.
**Validates: Requirements 7.1**

### Property 16: Wrapper contains type field
*For any* WebSocketMessage, the message should contain a non-null type field.
**Validates: Requirements 7.2**

### Property 17: Wrapper contains payload field
*For any* WebSocketMessage, the message should contain a payload field.
**Validates: Requirements 7.3**

### Property 18: Incoming messages are parsed correctly
*For any* valid WebSocketMessage sent by the client, the server should successfully parse it and extract the type and payload.
**Validates: Requirements 7.4**

### Property 19: Error messages contain required fields
*For any* error message sent by the WebSocket endpoint, the message should include errorType and message fields.
**Validates: Requirements 8.3**

### Property 20: Last seen is updated on call messages
*For any* call message received from a user, the user's last_seen timestamp should be updated.
**Validates: Requirements 10.4**

## Error Handling

### Error Types

1. **call_not_found**: The specified call ID does not exist
2. **unauthorized**: User is not authorized to perform the action (not a participant)
3. **invalid_status**: Call is not in the correct status for the requested action
4. **call_timeout**: Call has timed out (>60 seconds in RINGING status)
5. **parse_error**: Failed to parse the incoming message
6. **service_error**: Unexpected error in CallService

### Error Response Format

```json
{
  "type": "call_error",
  "payload": {
    "callId": "call-123",
    "errorType": "unauthorized",
    "message": "User is not a participant of this call"
  }
}
```

### Error Handling Strategy

1. **Parse Errors**: Log the error, send error message to client, continue processing
2. **Service Exceptions**: Catch exceptions from CallService, map to appropriate error type, send error message
3. **Authorization Failures**: Send error message without disconnecting WebSocket
4. **Not Found Errors**: Send error message with call_not_found type
5. **Invalid Status**: Send error message with invalid_status type

### Exception Mapping

```java
try {
    callService.acceptCallViaWebSocket(callId, userId);
} catch (ResourceNotFoundException e) {
    sendCallError(session, callId, "call_not_found", e.getMessage());
} catch (UnauthorizedException e) {
    sendCallError(session, callId, "unauthorized", e.getMessage());
} catch (BadRequestException e) {
    sendCallError(session, callId, "invalid_status", e.getMessage());
} catch (Exception e) {
    sendCallError(session, callId, "service_error", "An unexpected error occurred");
}
```

## Testing Strategy

### Unit Testing

**ChatServerEndpoint Tests:**
- Test message routing for each call message type
- Test error handling for malformed messages
- Test authentication validation
- Test error message sending

**CallService Tests:**
- Test acceptCallViaWebSocket with valid and invalid scenarios
- Test rejectCallViaWebSocket with different rejection reasons
- Test endCallViaWebSocket with and without duration
- Test authorization checks for each operation

**DTO Serialization Tests:**
- Test JSON serialization/deserialization of all call DTOs
- Test handling of missing optional fields
- Test handling of invalid field values

### Property-Based Testing

We will use **JUnit 5** with **jqwik** for property-based testing in Java.

**Property Test Configuration:**
- Minimum 100 iterations per property test
- Use `@Property` annotation from jqwik
- Tag each test with the property number from this design document

**Test Data Generators:**
- Random call IDs (UUIDs)
- Random user IDs (Long values)
- Random call types (AUDIO, VIDEO)
- Random call statuses
- Random rejection reasons
- Random durations (0-3600 seconds)

**Property Test Examples:**

```java
@Property
// Feature: websocket-call-integration, Property 1: Call invitation messages contain all required fields
void callInvitationContainsAllRequiredFields(@ForAll("validCalls") Call call) {
    CallInvitationData data = createInvitationData(call);
    
    assertNotNull(data.getCallId());
    assertNotNull(data.getChannelId());
    assertNotNull(data.getCallerId());
    assertNotNull(data.getCallerName());
    assertNotNull(data.getCallType());
}

@Property
// Feature: websocket-call-integration, Property 2: Only the callee can accept a call
void onlyCalleeCanAcceptCall(@ForAll("validCalls") Call call, @ForAll Long userId) {
    boolean isCallee = call.getCalleeId().equals(userId);
    
    if (isCallee) {
        assertDoesNotThrow(() -> callService.acceptCallViaWebSocket(call.getId(), userId.toString()));
    } else {
        assertThrows(UnauthorizedException.class, 
            () -> callService.acceptCallViaWebSocket(call.getId(), userId.toString()));
    }
}

@Property
// Feature: websocket-call-integration, Property 10: Duration is calculated when not provided
void durationCalculatedWhenNotProvided(@ForAll("callsWithStartTime") Call call) {
    Instant endTime = Instant.now();
    Integer expectedDuration = (int) Duration.between(call.getStartTime(), endTime).getSeconds();
    
    CallResponse response = callService.endCallViaWebSocket(call.getId(), call.getCallerId().toString(), null);
    
    assertEquals(expectedDuration, response.getDurationSeconds(), 1); // Allow 1 second tolerance
}
```

### Integration Testing

**WebSocket Integration Tests:**
- Test full flow: connect → send call.accept → receive call_accepted
- Test full flow: connect → send call.reject → receive call_rejected
- Test full flow: connect → send call.end → receive call_ended
- Test error scenarios with invalid call IDs
- Test authorization failures
- Test concurrent call operations

**End-to-End Tests:**
- Test complete call flow: initiate (REST) → accept (WS) → end (WS)
- Test call rejection flow: initiate (REST) → reject (WS)
- Test call timeout flow: initiate (REST) → wait 60s → receive timeout
- Test two-client scenario with real WebSocket connections

## Implementation Notes

### Message Handler Pattern

```java
@OnMessage
@Transactional
public void onMessage(Session session, WebSocketMessage<?> message) throws IOException {
    Long userId = (Long) session.getUserProperties().get("userId");
    if (userId == null) return;

    userStatusService.updateLastSeen(userId);

    switch (message.getType()) {
        case "chat.message" -> processChatMessage(userId, message);
        case "typing.start" -> processTypingStart(userId, message);
        case "typing.stop" -> processTypingStop(userId, message);
        case "heartbeat" -> processHeartbeat(session, userId);
        
        // New call handlers
        case "call.accept" -> processCallAccept(session, userId, message);
        case "call.reject" -> processCallReject(session, userId, message);
        case "call.end" -> processCallEnd(session, userId, message);
        
        default -> LOGGER.warning("Unknown message type: " + message.getType());
    }
}
```

### Error Sending Helper

```java
private void sendCallError(Session session, String callId, String errorType, String errorMessage) {
    try {
        CallErrorDto errorDto = new CallErrorDto();
        errorDto.setCallId(callId);
        errorDto.setErrorType(errorType);
        errorDto.setMessage(errorMessage);
        
        WebSocketMessage<CallErrorDto> wsMessage = new WebSocketMessage<>("call_error", errorDto);
        session.getBasicRemote().sendObject(wsMessage);
        
        LOGGER.log(Level.WARNING, "Sent call error to user: {0} - {1}", 
            new Object[]{errorType, errorMessage});
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to send error message", e);
    }
}
```

### Backward Compatibility

The existing REST API endpoints will remain functional:
- `POST /v1/calls/{callId}/accept`
- `POST /v1/calls/{callId}/reject`
- `POST /v1/calls/{callId}/end`

Clients can choose to use either REST API or WebSocket based on their requirements. The WebSocket approach is recommended for lower latency.

### Performance Considerations

1. **Message Parsing**: Use Jackson's `convertValue()` for efficient DTO conversion
2. **Transaction Scope**: Keep transactions short by processing only the necessary database operations
3. **Notification Sending**: Use async sending where possible to avoid blocking
4. **Error Handling**: Catch exceptions early to prevent transaction rollbacks

### Security Considerations

1. **Authentication**: Reuse existing token-based authentication from WebSocket connection
2. **Authorization**: Verify user is a participant before allowing call actions
3. **Input Validation**: Validate all incoming message fields
4. **Rate Limiting**: Consider adding rate limiting for call actions to prevent abuse
5. **Logging**: Log all call actions for audit purposes
