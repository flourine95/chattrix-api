# Requirements Document

## Introduction

This feature implements real-time WebSocket communication for video and audio call signaling in the Chattrix application. Currently, call operations (accept, reject, end) are handled through REST API endpoints, which introduces latency and is not truly real-time. This feature will integrate call signaling directly into the existing WebSocket connection at `/ws/chat`, allowing for instant bidirectional communication between call participants.

## Glossary

- **Call System**: The subsystem responsible for managing video and audio calls between users
- **WebSocket Endpoint**: The server-side endpoint that handles WebSocket connections at `/ws/chat`
- **Call Signaling**: The process of exchanging call control messages (invitation, accept, reject, end) between participants
- **ChatServerEndpoint**: The Jakarta WebSocket server endpoint class that handles all WebSocket messages
- **CallService**: The business logic service that manages call lifecycle operations
- **WebSocketNotificationService**: The service that sends WebSocket notifications to users
- **Client**: The Flutter mobile application that connects to the WebSocket endpoint
- **Message Type**: A string identifier in WebSocket messages that determines how the message should be processed

## Requirements

### Requirement 1

**User Story:** As a caller, I want to initiate a call through REST API and have the invitation delivered instantly via WebSocket, so that the callee receives the invitation without delay.

#### Acceptance Criteria

1. WHEN a caller initiates a call via REST API THEN the Call System SHALL create a call record with INITIATING status
2. WHEN the call record is created THEN the Call System SHALL send a WebSocket message with type "call_invitation" to the callee
3. WHEN sending the invitation THEN the WebSocket message SHALL include callId, channelId, callerId, callerName, callerAvatar, and callType
4. WHEN the callee is not connected to WebSocket THEN the Call System SHALL handle the failure gracefully without throwing exceptions
5. WHEN the invitation is sent THEN the Call System SHALL update the call status to RINGING

### Requirement 2

**User Story:** As a callee, I want to accept an incoming call through WebSocket, so that the caller is notified instantly without HTTP request overhead.

#### Acceptance Criteria

1. WHEN a callee sends a WebSocket message with type "call.accept" THEN the WebSocket Endpoint SHALL validate the user is authenticated
2. WHEN the accept message is received THEN the WebSocket Endpoint SHALL extract the callId from the message payload
3. WHEN the callId is extracted THEN the WebSocket Endpoint SHALL invoke CallService to process the acceptance
4. WHEN CallService processes acceptance THEN the Call System SHALL verify the user is the callee of the call
5. WHEN acceptance is verified THEN the Call System SHALL update the call status to CONNECTING
6. WHEN the status is updated THEN the Call System SHALL send a WebSocket message with type "call_accepted" to the caller
7. WHEN sending the accepted notification THEN the message SHALL include callId and acceptedBy fields

### Requirement 3

**User Story:** As a callee, I want to reject an incoming call through WebSocket, so that the caller is notified instantly of my decision.

#### Acceptance Criteria

1. WHEN a callee sends a WebSocket message with type "call.reject" THEN the WebSocket Endpoint SHALL validate the user is authenticated
2. WHEN the reject message is received THEN the WebSocket Endpoint SHALL extract callId and reason from the message payload
3. WHEN the data is extracted THEN the WebSocket Endpoint SHALL invoke CallService to process the rejection
4. WHEN CallService processes rejection THEN the Call System SHALL verify the user is the callee of the call
5. WHEN rejection is verified THEN the Call System SHALL update the call status to REJECTED
6. WHEN the status is updated THEN the Call System SHALL send a WebSocket message with type "call_rejected" to the caller
7. WHEN sending the rejected notification THEN the message SHALL include callId, rejectedBy, and reason fields

### Requirement 4

**User Story:** As a call participant, I want to end an active call through WebSocket, so that the other participant is notified instantly when I hang up.

#### Acceptance Criteria

1. WHEN a participant sends a WebSocket message with type "call.end" THEN the WebSocket Endpoint SHALL validate the user is authenticated
2. WHEN the end message is received THEN the WebSocket Endpoint SHALL extract callId and durationSeconds from the message payload
3. WHEN the data is extracted THEN the WebSocket Endpoint SHALL invoke CallService to process the call ending
4. WHEN CallService processes ending THEN the Call System SHALL verify the user is a participant of the call
5. WHEN verification succeeds THEN the Call System SHALL update the call status to ENDED
6. WHEN the status is updated THEN the Call System SHALL calculate the call duration if not provided
7. WHEN duration is calculated THEN the Call System SHALL send a WebSocket message with type "call_ended" to the other participant
8. WHEN sending the ended notification THEN the message SHALL include callId, endedBy, and durationSeconds fields

### Requirement 5

**User Story:** As a system administrator, I want call timeout notifications to be sent via WebSocket, so that both participants are informed when a call is not answered within 60 seconds.

#### Acceptance Criteria

1. WHEN a call remains in RINGING status for 60 seconds THEN the Call System SHALL send a WebSocket message with type "call_timeout" to both caller and callee
2. WHEN sending timeout notifications THEN the message SHALL include the callId field
3. WHEN timeout occurs THEN the Call System SHALL update the call status to MISSED
4. WHEN the status is updated THEN the Call System SHALL create call history entries for both participants

### Requirement 6

**User Story:** As a call participant, I want to receive network quality warnings via WebSocket, so that I am aware of connection issues during the call.

#### Acceptance Criteria

1. WHEN network quality degrades below acceptable thresholds THEN the Call System SHALL send a WebSocket message with type "call_quality_warning" to the affected participant
2. WHEN sending quality warnings THEN the message SHALL include callId and quality level fields
3. WHEN quality level is included THEN the value SHALL be one of POOR, BAD, or VERY_BAD

### Requirement 7

**User Story:** As a developer, I want consistent message structure for all call-related WebSocket messages, so that the client can parse them reliably.

#### Acceptance Criteria

1. WHEN the WebSocket Endpoint sends any call message THEN the message SHALL follow the WebSocketMessage wrapper format
2. WHEN using the wrapper format THEN the message SHALL contain a "type" field indicating the message type
3. WHEN using the wrapper format THEN the message SHALL contain a "payload" field with the message data
4. WHEN the client sends a call message THEN the message SHALL follow the same WebSocketMessage wrapper format
5. WHEN parsing incoming messages THEN the WebSocket Endpoint SHALL use Jackson ObjectMapper for JSON deserialization

### Requirement 8

**User Story:** As a developer, I want proper error handling for call WebSocket messages, so that invalid messages do not crash the WebSocket connection.

#### Acceptance Criteria

1. WHEN a call message fails to parse THEN the WebSocket Endpoint SHALL log the error and continue processing other messages
2. WHEN CallService throws an exception THEN the WebSocket Endpoint SHALL catch the exception and send an error message to the client
3. WHEN sending error messages THEN the message SHALL include an error type and description
4. WHEN a user is not authorized for a call action THEN the WebSocket Endpoint SHALL send an error message without disconnecting the WebSocket
5. WHEN a call is not found THEN the WebSocket Endpoint SHALL send an error message with type "call_not_found"

### Requirement 9

**User Story:** As a Flutter developer, I want the client to receive call messages with correct type identifiers, so that the CallSignalingService can route them properly.

#### Acceptance Criteria

1. WHEN the server sends a call invitation THEN the message type SHALL be "call_invitation" (with underscore)
2. WHEN the server sends a call accepted notification THEN the message type SHALL be "call_accepted" (with underscore)
3. WHEN the server sends a call rejected notification THEN the message type SHALL be "call_rejected" (with underscore)
4. WHEN the server sends a call ended notification THEN the message type SHALL be "call_ended" (with underscore)
5. WHEN the server sends a call timeout notification THEN the message type SHALL be "call_timeout" (with underscore)
6. WHEN the client sends call actions THEN the message types SHALL use dot notation ("call.accept", "call.reject", "call.end")

### Requirement 10

**User Story:** As a system architect, I want call WebSocket handling to be integrated into the existing ChatServerEndpoint, so that we maintain a single WebSocket connection per user.

#### Acceptance Criteria

1. WHEN implementing call handlers THEN the WebSocket Endpoint SHALL add them to the existing onMessage method
2. WHEN adding call handlers THEN the WebSocket Endpoint SHALL inject CallService via CDI
3. WHEN processing call messages THEN the WebSocket Endpoint SHALL reuse the existing authentication mechanism
4. WHEN processing call messages THEN the WebSocket Endpoint SHALL update the user's last seen timestamp
5. WHEN a call message is processed THEN the WebSocket Endpoint SHALL use the existing ChatSessionService to send notifications
