# Design Document

## Overview

This design fixes the call invitation flow by ensuring clients use the REST API for call initiation and preventing clients from sending call invitations directly via WebSocket. The current issue is that the Flutter client sends `call.invitation` messages through WebSocket, which bypasses the proper call creation flow and prevents the callee from receiving notifications.

The fix involves:
1. Adding a handler in `ChatServerEndpoint` to reject `call.invitation` messages from clients
2. Sending appropriate error responses when clients attempt invalid operations
3. Ensuring the existing REST API flow works correctly
4. Documenting the proper call flow for client developers

## Architecture

### Current (Broken) Flow
```
Client → WebSocket (call.invitation) → Server → ❌ No handler → No notification to callee
```

### Fixed Flow
```
Client → REST API (/api/v1/calls/initiate) → Server creates call → Server sends WebSocket (call_invitation) → Callee
```

### Error Handling for Invalid Operations
```
Client → WebSocket (call.invitation) → Server → Reject with error → Client receives call_error
```

## Components and Interfaces

### 1. ChatServerEndpoint (Modified)

**New Message Handler:**
```java
private void processCallInvitation(Session session, Long userId, WebSocketMessage<?> message)
```

This handler will:
- Detect when a client sends `call.invitation` via WebSocket
- Reject the operation as invalid
- Send an error response to the client
- Log the invalid operation attempt

**Integration Point:**
Add a new case in the `onMessage()` method:
```java
case "call.invitation" -> processCallInvitation(session, userId, message);
```

### 2. CallService (No Changes Required)

The existing `initiateCall()` method already implements the correct flow:
1. Creates call record with INITIATING status
2. Sends WebSocket invitation to callee via `WebSocketNotificationService`
3. Updates status to RINGING
4. Schedules timeout

### 3. WebSocketNotificationService (No Changes Required)

The existing `sendCallInvitation()` method already sends properly formatted invitations to callees.

### 4. Error Response Format

**CallErrorDto** (already exists):
```java
public class CallErrorDto {
    private String callId;
    private String errorType;
    private String message;
}
```

For this error, we'll use:
- `callId`: null (no call was created)
- `errorType`: "invalid_operation"
- `message`: "Call invitations must be initiated through the REST API, not WebSocket"

## Data Models

### WebSocketMessage<T> (Existing)
```java
public class WebSocketMessage<T> {
    private String type;
    private T payload;
}
```

### CallInvitationData (Existing)
```java
public class CallInvitationData {
    private String callId;
    private String channelId;
    private String callerId;
    private String callerName;
    private String callerAvatar;
    private String callType;
}
```

No new data models are required.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptence Criteria Testing Prework:

1.1 WHEN a caller initiates a call THEN the Client SHALL send a POST request to `/api/v1/calls/initiate`
  Thoughts: This is about client behavior, which we cannot test on the server side
  Testable: no

1.2 WHEN the REST API receives the initiate request THEN the Server SHALL create a call record with INITIATING status
  Thoughts: This is already tested in existing tests and is about the REST API flow
  Testable: no (already covered)

1.3 WHEN the call record is created THEN the Server SHALL send a WebSocket message with type "call_invitation" to the callee
  Thoughts: This is already implemented and tested in existing code
  Testable: no (already covered)

1.4 WHEN the invitation is sent THEN the Server SHALL update the call status to RINGING
  Thoughts: This is already implemented and tested in existing code
  Testable: no (already covered)

1.5 WHEN the callee is not connected to WebSocket THEN the Server SHALL handle the failure gracefully
  Thoughts: This is about error handling in the notification service, already implemented
  Testable: no (already covered)

2.1 WHEN a client sends a WebSocket message with type "call.invitation" THEN the Server SHALL reject the message
  Thoughts: This is a new requirement. For any WebSocket message with type "call.invitation", the server should reject it
  Testable: yes - property

2.2 WHEN rejecting the message THEN the Server SHALL send an error response with type "call_error"
  Thoughts: This is about what happens after rejection. For any rejected call.invitation, an error should be sent
  Testable: yes - property

2.3 WHEN sending the error THEN the message SHALL include errorType "invalid_operation"
  Thoughts: This is checking that the error message has the correct errorType field
  Testable: yes - example

2.4 WHEN the error is sent THEN the Server SHALL log the invalid operation attempt
  Thoughts: This is about logging behavior, which is difficult to test in unit tests
  Testable: no

2.5 WHEN the error is sent THEN the WebSocket connection SHALL remain open
  Thoughts: This is about ensuring the connection doesn't close after an error. We can test that no close is called
  Testable: yes - property

3.1 WHEN the Server sends a call invitation THEN the message type SHALL be "call_invitation"
  Thoughts: This is already implemented and tested
  Testable: no (already covered)

3.2 WHEN the invitation is sent THEN the message SHALL include required fields
  Thoughts: This is already implemented and tested
  Testable: no (already covered)

3.3-3.5: Client-side behavior
  Thoughts: These are about client behavior, not server
  Testable: no

4.1-4.5: Documentation requirements
  Thoughts: These are about documentation, not code behavior
  Testable: no

5.1-5.5: Logging requirements
  Thoughts: Logging is difficult to test in unit tests
  Testable: no

### Property Reflection

After reviewing the prework, we have identified the following testable properties:
- Property 1 (2.1): Server rejects call.invitation from clients
- Property 2 (2.2): Rejection triggers error response
- Property 3 (2.5): Connection remains open after error

Property 2 is actually implied by Property 1 - if we test that rejection sends an error, we're testing both. We can combine these into a single comprehensive property.

Property 3 is also part of the error handling flow and can be verified as part of the same test.

Therefore, we'll have one main property that covers the complete error handling flow.

### Property 1: Client-sent call invitations are rejected with error
*For any* WebSocket message with type "call.invitation" sent by a client, the server should reject it, send a call_error response with errorType "invalid_operation", and keep the WebSocket connection open.
**Validates: Requirements 2.1, 2.2, 2.5**

## Error Handling

### Invalid Operation Error

**Scenario**: Client sends `call.invitation` via WebSocket

**Response**:
```json
{
  "type": "call_error",
  "payload": {
    "callId": null,
    "errorType": "invalid_operation",
    "message": "Call invitations must be initiated through the REST API, not WebSocket"
  }
}
```

**Behavior**:
1. Log the invalid operation with user ID
2. Send error response to client
3. Keep WebSocket connection open
4. Continue processing other messages normally

### Error Handling Strategy

1. **Detection**: Check message type in `onMessage()` switch statement
2. **Validation**: Identify that `call.invitation` is not a valid client-to-server message
3. **Response**: Send structured error using existing `sendCallError()` method
4. **Logging**: Log at WARNING level with user ID and message type
5. **Recovery**: Continue normal operation without disconnecting

## Testing Strategy

### Unit Testing

**ChatServerEndpoint Tests:**
- Test that `call.invitation` message triggers error handler
- Test that error response is sent with correct structure
- Test that WebSocket session remains open after error
- Test that other message types continue to work normally

**Integration Tests:**
- Test complete flow: client sends call.invitation → receives error → connection stays open
- Test that REST API call initiation still works correctly
- Test that server-sent call_invitation messages work correctly

### Property-Based Testing

We will use **JUnit 5** with **jqwik** for property-based testing in Java.

**Property Test Configuration:**
- Minimum 100 iterations per property test
- Use `@Property` annotation from jqwik
- Tag each test with the property number from this design document

**Test Data Generators:**
- Random user IDs (Long values)
- Random call invitation payloads
- Random session states

**Property Test Example:**

```java
@Property
// Feature: fix-call-invitation-flow, Property 1: Client-sent call invitations are rejected with error
void clientSentCallInvitationsAreRejected(@ForAll Long userId, @ForAll("callInvitationPayloads") Map<String, Object> payload) {
    // Create WebSocket message with type "call.invitation"
    WebSocketMessage<?> message = new WebSocketMessage<>("call.invitation", payload);
    
    // Create mock session
    Session mockSession = mock(Session.class);
    RemoteEndpoint.Basic mockRemote = mock(RemoteEndpoint.Basic.class);
    when(mockSession.getBasicRemote()).thenReturn(mockRemote);
    when(mockSession.getUserProperties()).thenReturn(Map.of("userId", userId));
    
    // Process the message
    chatServerEndpoint.onMessage(mockSession, message);
    
    // Verify error was sent
    ArgumentCaptor<WebSocketMessage> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
    verify(mockRemote).sendObject(captor.capture());
    
    WebSocketMessage<?> sentMessage = captor.getValue();
    assertEquals("call_error", sentMessage.getType());
    
    CallErrorDto error = (CallErrorDto) sentMessage.getPayload();
    assertEquals("invalid_operation", error.getErrorType());
    assertNotNull(error.getMessage());
    
    // Verify session was not closed
    verify(mockSession, never()).close(any());
}
```

## Implementation Notes

### Message Handler Implementation

```java
/**
 * Process invalid call invitation from client
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
private void processCallInvitation(Session session, Long userId, WebSocketMessage<?> message) {
    // Log the invalid operation
    LOGGER.log(Level.WARNING, "User {0} attempted to send call.invitation via WebSocket, which is not allowed", userId);
    
    // Send error response
    sendCallError(
        session, 
        null, // No call ID since no call was created
        "invalid_operation", 
        "Call invitations must be initiated through the REST API endpoint /api/v1/calls/initiate, not via WebSocket"
    );
}
```

### Integration with Existing Code

The fix requires minimal changes:
1. Add one new case in `onMessage()` switch statement
2. Add one new private method `processCallInvitation()`
3. Reuse existing `sendCallError()` method

No changes to:
- CallService
- WebSocketNotificationService
- DTO classes
- Database schema
- REST API endpoints

### Documentation Updates

Update API documentation to clarify:
1. **Call Initiation**: Must use `POST /api/v1/calls/initiate`
2. **Call Actions**: Use WebSocket for accept, reject, end
3. **Call Notifications**: Server sends via WebSocket (call_invitation, call_accepted, etc.)
4. **Error Handling**: Invalid operations return call_error messages

### Client-Side Changes Required

The Flutter client needs to be updated to:
1. Remove code that sends `call.invitation` via WebSocket
2. Use REST API call to `/api/v1/calls/initiate` instead
3. Wait for server's `call_invitation` WebSocket message
4. Handle `call_error` messages with type "invalid_operation"

### Backward Compatibility

This change is **not backward compatible** with clients that send `call.invitation` via WebSocket. However:
- The current implementation is broken (doesn't work)
- Clients will receive clear error messages
- The fix enables the proper flow that was originally designed

### Performance Considerations

1. **Minimal Overhead**: One additional switch case adds negligible processing time
2. **Error Response**: Sending error message is fast (single WebSocket send)
3. **No Database Impact**: No database operations for this error case
4. **Logging**: WARNING level logging has minimal performance impact

### Security Considerations

1. **Authorization**: User must be authenticated to send any WebSocket message (already enforced)
2. **Rate Limiting**: Consider adding rate limiting for invalid operation attempts
3. **Audit Trail**: Log all invalid operation attempts for security monitoring
4. **Error Messages**: Error messages don't expose sensitive information
5. **Connection Stability**: Keeping connection open after error prevents DoS via repeated errors

## Call Flow Documentation

### Correct Call Flow

**1. Initiate Call (Caller)**
```
Client → POST /api/v1/calls/initiate
Body: {
  "calleeId": "123",
  "channelId": "channel_conv_456",
  "callType": "audio"
}
```

**2. Server Processing**
```
Server → Create call record (INITIATING)
Server → Send WebSocket to callee (call_invitation)
Server → Update call status (RINGING)
Server → Schedule 60s timeout
```

**3. Callee Receives Invitation**
```
Server → WebSocket → Callee
Message: {
  "type": "call_invitation",
  "payload": {
    "callId": "call_123",
    "channelId": "channel_conv_456",
    "callerId": "789",
    "callerName": "John Doe",
    "callerAvatar": "https://...",
    "callType": "audio"
  }
}
```

**4. Callee Accepts (via WebSocket)**
```
Callee → WebSocket → Server
Message: {
  "type": "call.accept",
  "payload": {
    "callId": "call_123"
  }
}
```

**5. Caller Receives Acceptance**
```
Server → WebSocket → Caller
Message: {
  "type": "call_accepted",
  "payload": {
    "callId": "call_123",
    "acceptedBy": "123"
  }
}
```

### Invalid Flow (Now Rejected)

**❌ Client Sends Invitation (WRONG)**
```
Client → WebSocket → Server
Message: {
  "type": "call.invitation",  // ❌ Invalid
  "payload": { ... }
}
```

**Server Response**
```
Server → WebSocket → Client
Message: {
  "type": "call_error",
  "payload": {
    "callId": null,
    "errorType": "invalid_operation",
    "message": "Call invitations must be initiated through the REST API, not WebSocket"
  }
}
```
