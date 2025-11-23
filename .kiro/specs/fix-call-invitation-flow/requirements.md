# Requirements Document

## Introduction

This feature fixes the call invitation flow in the Chattrix application. Currently, the Flutter client is incorrectly sending call invitations directly through WebSocket messages, which bypasses the proper call initiation flow. The correct flow should be: client calls REST API → server creates call record → server sends WebSocket invitation to callee. This fix ensures the call invitation follows the designed architecture and that the callee receives notifications properly.

## Glossary

- **Call Initiation**: The process of starting a new call by invoking the REST API endpoint
- **Call Invitation**: A WebSocket message sent from server to callee notifying them of an incoming call
- **REST API**: The HTTP endpoint `/api/v1/calls/initiate` used to create new calls
- **WebSocket**: The bidirectional communication channel at `/ws/chat` used for real-time notifications
- **Client**: The Flutter mobile application
- **Server**: The Java backend API
- **Caller**: The user initiating the call
- **Callee**: The user receiving the call invitation

## Requirements

### Requirement 1

**User Story:** As a caller, I want to initiate a call through the REST API, so that the server properly creates the call record and sends the invitation to the callee.

#### Acceptance Criteria

1. WHEN a caller initiates a call THEN the Client SHALL send a POST request to `/api/v1/calls/initiate` with calleeId, channelId, and callType
2. WHEN the REST API receives the initiate request THEN the Server SHALL create a call record with INITIATING status
3. WHEN the call record is created THEN the Server SHALL send a WebSocket message with type "call_invitation" to the callee
4. WHEN the invitation is sent THEN the Server SHALL update the call status to RINGING
5. WHEN the callee is not connected to WebSocket THEN the Server SHALL handle the failure gracefully without throwing exceptions

### Requirement 2

**User Story:** As a developer, I want the server to reject client-sent call invitations via WebSocket, so that clients cannot bypass the proper call initiation flow.

#### Acceptance Criteria

1. WHEN a client sends a WebSocket message with type "call.invitation" THEN the Server SHALL reject the message
2. WHEN rejecting the message THEN the Server SHALL send an error response with type "call_error"
3. WHEN sending the error THEN the message SHALL include errorType "invalid_operation" and a descriptive error message
4. WHEN the error is sent THEN the Server SHALL log the invalid operation attempt
5. WHEN the error is sent THEN the WebSocket connection SHALL remain open

### Requirement 3

**User Story:** As a callee, I want to receive call invitations only from the server, so that I can trust the invitation is valid and properly recorded.

#### Acceptance Criteria

1. WHEN the Server sends a call invitation THEN the message type SHALL be "call_invitation"
2. WHEN the invitation is sent THEN the message SHALL include callId, channelId, callerId, callerName, callerAvatar, and callType
3. WHEN the callee receives the invitation THEN the Client SHALL display the incoming call UI
4. WHEN displaying the UI THEN the Client SHALL show caller information from the invitation payload
5. WHEN the invitation is received THEN the Client SHALL prepare to accept or reject the call via WebSocket

### Requirement 4

**User Story:** As a Flutter developer, I want clear documentation on the call flow, so that I understand when to use REST API versus WebSocket.

#### Acceptance Criteria

1. WHEN initiating a call THEN the Client SHALL use the REST API endpoint `/api/v1/calls/initiate`
2. WHEN accepting a call THEN the Client SHALL send a WebSocket message with type "call.accept"
3. WHEN rejecting a call THEN the Client SHALL send a WebSocket message with type "call.reject"
4. WHEN ending a call THEN the Client SHALL send a WebSocket message with type "call.end"
5. WHEN receiving server notifications THEN the Client SHALL listen for WebSocket messages with types "call_invitation", "call_accepted", "call_rejected", "call_ended", "call_timeout"

### Requirement 5

**User Story:** As a system administrator, I want proper logging of call initiation attempts, so that I can debug issues and monitor system usage.

#### Acceptance Criteria

1. WHEN a call is initiated via REST API THEN the Server SHALL log the caller ID, callee ID, and call type
2. WHEN a call invitation is sent THEN the Server SHALL log the callee ID and call ID
3. WHEN a client attempts to send call.invitation via WebSocket THEN the Server SHALL log the invalid operation with user ID
4. WHEN an error occurs during call initiation THEN the Server SHALL log the error with full context
5. WHEN logging errors THEN the Server SHALL include timestamp, user ID, and error details
