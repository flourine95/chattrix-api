# Call API Documentation

This document describes the Call API for Chattrix, which supports both REST API and WebSocket-based real-time communication for video and audio calls.

**Base URL:** `/api`

**Authentication:** All endpoints require JWT authentication. Include the token in the `Authorization` header as `Bearer <JWT_TOKEN>`.

---

## Table of Contents

1. [REST API Endpoints](#rest-api-endpoints)
2. [WebSocket Call Signaling](#websocket-call-signaling)
3. [Message Types Reference](#message-types-reference)
4. [Error Handling](#error-handling)
5. [Examples](#examples)

---

## REST API Endpoints

The REST API provides traditional HTTP endpoints for call operations. These endpoints remain available for backward compatibility, but WebSocket-based signaling is recommended for lower latency.

### 1. Initiate Call

Initiates a new video or audio call.

- **Method:** `POST`
- **Path:** `/v1/calls/initiate`
- **Authentication:** Required (Bearer Token)
- **Rate Limited:** Yes (Call initiation rate limit applies)

**Request Body:**
```json
{
  "calleeId": "string (user ID)",
  "callType": "string (AUDIO or VIDEO)",
  "channelId": "string (Agora channel ID)"
}
```

**Success Response (201 CREATED):**
```json
{
  "success": true,
  "message": "Call initiated successfully",
  "data": {
    "id": "string (call ID)",
    "callerId": "string",
    "calleeId": "string",
    "callType": "AUDIO | VIDEO",
    "status": "INITIATING",
    "channelId": "string",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid request data
- `401 UNAUTHORIZED`: Invalid or missing token
- `429 TOO_MANY_REQUESTS`: Rate limit exceeded

---

### 2. Accept Call

Accepts an incoming call (REST endpoint - WebSocket recommended).

- **Method:** `POST`
- **Path:** `/v1/calls/{callId}/accept`
- **Authentication:** Required (Bearer Token)

**Path Parameters:**
- `callId` (string): The ID of the call to accept

**Request Body:**
```json
{}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Call accepted successfully",
  "data": {
    "id": "string",
    "callerId": "string",
    "calleeId": "string",
    "callType": "AUDIO | VIDEO",
    "status": "CONNECTING",
    "channelId": "string",
    "startTime": "datetime",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid call status
- `401 UNAUTHORIZED`: Invalid or missing token
- `403 FORBIDDEN`: User is not the callee
- `404 NOT_FOUND`: Call not found

---

### 3. Reject Call

Rejects an incoming call (REST endpoint - WebSocket recommended).

- **Method:** `POST`
- **Path:** `/v1/calls/{callId}/reject`
- **Authentication:** Required (Bearer Token)

**Path Parameters:**
- `callId` (string): The ID of the call to reject

**Request Body:**
```json
{
  "reason": "string (busy | declined | unavailable)"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Call rejected successfully",
  "data": {
    "id": "string",
    "callerId": "string",
    "calleeId": "string",
    "callType": "AUDIO | VIDEO",
    "status": "REJECTED",
    "channelId": "string",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid call status
- `401 UNAUTHORIZED`: Invalid or missing token
- `403 FORBIDDEN`: User is not the callee
- `404 NOT_FOUND`: Call not found

---

### 4. End Call

Ends an active call (REST endpoint - WebSocket recommended).

- **Method:** `POST`
- **Path:** `/v1/calls/{callId}/end`
- **Authentication:** Required (Bearer Token)

**Path Parameters:**
- `callId` (string): The ID of the call to end

**Request Body:**
```json
{
  "durationSeconds": "integer (optional)"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Call ended successfully",
  "data": {
    "id": "string",
    "callerId": "string",
    "calleeId": "string",
    "callType": "AUDIO | VIDEO",
    "status": "ENDED",
    "channelId": "string",
    "startTime": "datetime",
    "endTime": "datetime",
    "durationSeconds": "integer",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid call status
- `401 UNAUTHORIZED`: Invalid or missing token
- `403 FORBIDDEN`: User is not a participant
- `404 NOT_FOUND`: Call not found

---

### 5. Update Call Status

Updates the status of a call (e.g., from CONNECTING to ACTIVE).

- **Method:** `PATCH`
- **Path:** `/v1/calls/{callId}/status`
- **Authentication:** Required (Bearer Token)

**Path Parameters:**
- `callId` (string): The ID of the call

**Request Body:**
```json
{
  "status": "string (CONNECTING | ACTIVE | ENDED)"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Call status updated successfully",
  "data": {
    "id": "string",
    "callerId": "string",
    "calleeId": "string",
    "callType": "AUDIO | VIDEO",
    "status": "ACTIVE",
    "channelId": "string",
    "startTime": "datetime",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid status transition
- `401 UNAUTHORIZED`: Invalid or missing token
- `403 FORBIDDEN`: User is not a participant
- `404 NOT_FOUND`: Call not found

---

### 6. Get Call Details

Retrieves details of a specific call.

- **Method:** `GET`
- **Path:** `/v1/calls/{callId}`
- **Authentication:** Required (Bearer Token)

**Path Parameters:**
- `callId` (string): The ID of the call

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Call details retrieved successfully",
  "data": {
    "id": "string",
    "callerId": "string",
    "calleeId": "string",
    "callType": "AUDIO | VIDEO",
    "status": "string",
    "channelId": "string",
    "startTime": "datetime (nullable)",
    "endTime": "datetime (nullable)",
    "durationSeconds": "integer (nullable)",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
}
```

**Error Responses:**
- `401 UNAUTHORIZED`: Invalid or missing token
- `403 FORBIDDEN`: User is not a participant
- `404 NOT_FOUND`: Call not found

---

## WebSocket Call Signaling

WebSocket-based call signaling provides real-time, bidirectional communication for call operations. This is the **recommended approach** for production use due to lower latency.

### Connection

- **URL:** `ws://<server-address>/ws/chat?token=<JWT_TOKEN>`
- **Protocol:** WebSocket
- **Authentication:** JWT token provided as query parameter

The WebSocket connection is shared between chat and call features. All messages use a consistent wrapper format.

### Message Format

All WebSocket messages follow this structure:

```json
{
  "type": "string",
  "payload": {}
}
```

- **`type`**: Message type identifier (see Message Types Reference)
- **`payload`**: Message-specific data

---

### Client to Server Messages

These are messages sent from the client to the server to perform call actions.

#### 1. Accept Call

Accept an incoming call invitation.

**Message Type:** `call.accept`

**Payload:**
```json
{
  "callId": "string"
}
```

**Example:**
```json
{
  "type": "call.accept",
  "payload": {
    "callId": "call-uuid-123"
  }
}
```

**Server Response:**
- On success: Sends `call_accepted` notification to the caller
- On error: Sends `call_error` message to the client

---

#### 2. Reject Call

Reject an incoming call invitation.

**Message Type:** `call.reject`

**Payload:**
```json
{
  "callId": "string",
  "reason": "string (busy | declined | unavailable)"
}
```

**Example:**
```json
{
  "type": "call.reject",
  "payload": {
    "callId": "call-uuid-123",
    "reason": "busy"
  }
}
```

**Server Response:**
- On success: Sends `call_rejected` notification to the caller
- On error: Sends `call_error` message to the client

---

#### 3. End Call

End an active call.

**Message Type:** `call.end`

**Payload:**
```json
{
  "callId": "string",
  "durationSeconds": "integer (optional)"
}
```

**Example:**
```json
{
  "type": "call.end",
  "payload": {
    "callId": "call-uuid-123",
    "durationSeconds": 125
  }
}
```

**Note:** If `durationSeconds` is not provided, the server will calculate it automatically based on the call's start time.

**Server Response:**
- On success: Sends `call_ended` notification to the other participant
- On error: Sends `call_error` message to the client

---

### Server to Client Messages

These are messages sent from the server to clients to notify them of call events.

#### 1. Call Invitation

Sent to the callee when someone initiates a call.

**Message Type:** `call_invitation`

**Structure:**
```json
{
  "type": "call_invitation",
  "data": {
    "callId": "string",
    "channelId": "string",
    "callerId": "string",
    "callerName": "string",
    "callerAvatar": "string (nullable)",
    "callType": "AUDIO | VIDEO"
  },
  "timestamp": "datetime"
}
```

**Example:**
```json
{
  "type": "call_invitation",
  "data": {
    "callId": "call-uuid-123",
    "channelId": "agora-channel-456",
    "callerId": "user-789",
    "callerName": "John Doe",
    "callerAvatar": "https://example.com/avatar.jpg",
    "callType": "VIDEO"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

#### 2. Call Accepted

Sent to the caller when the callee accepts the call.

**Message Type:** `call_accepted`

**Structure:**
```json
{
  "type": "call_accepted",
  "data": {
    "callId": "string",
    "acceptedBy": "string (user ID)"
  },
  "timestamp": "datetime"
}
```

**Example:**
```json
{
  "type": "call_accepted",
  "data": {
    "callId": "call-uuid-123",
    "acceptedBy": "user-456"
  },
  "timestamp": "2024-01-15T10:30:15Z"
}
```

---

#### 3. Call Rejected

Sent to the caller when the callee rejects the call.

**Message Type:** `call_rejected`

**Structure:**
```json
{
  "type": "call_rejected",
  "data": {
    "callId": "string",
    "rejectedBy": "string (user ID)",
    "reason": "string (busy | declined | unavailable)"
  },
  "timestamp": "datetime"
}
```

**Example:**
```json
{
  "type": "call_rejected",
  "data": {
    "callId": "call-uuid-123",
    "rejectedBy": "user-456",
    "reason": "busy"
  },
  "timestamp": "2024-01-15T10:30:10Z"
}
```

---

#### 4. Call Ended

Sent to the other participant when someone ends the call.

**Message Type:** `call_ended`

**Structure:**
```json
{
  "type": "call_ended",
  "data": {
    "callId": "string",
    "endedBy": "string (user ID)",
    "durationSeconds": "integer"
  },
  "timestamp": "datetime"
}
```

**Example:**
```json
{
  "type": "call_ended",
  "data": {
    "callId": "call-uuid-123",
    "endedBy": "user-789",
    "durationSeconds": 125
  },
  "timestamp": "2024-01-15T10:32:05Z"
}
```

---

#### 5. Call Timeout

Sent to both participants when a call is not answered within 60 seconds.

**Message Type:** `call_timeout`

**Structure:**
```json
{
  "type": "call_timeout",
  "data": {
    "callId": "string"
  },
  "timestamp": "datetime"
}
```

**Example:**
```json
{
  "type": "call_timeout",
  "data": {
    "callId": "call-uuid-123"
  },
  "timestamp": "2024-01-15T10:31:00Z"
}
```

**Note:** When a timeout occurs, the call status is automatically updated to `MISSED` and call history entries are created for both participants.

---

#### 6. Call Quality Warning

Sent to a participant when network quality issues are detected.

**Message Type:** `call_quality_warning`

**Structure:**
```json
{
  "type": "call_quality_warning",
  "data": {
    "callId": "string",
    "quality": "string (POOR | BAD | VERY_BAD)"
  },
  "timestamp": "datetime"
}
```

**Example:**
```json
{
  "type": "call_quality_warning",
  "data": {
    "callId": "call-uuid-123",
    "quality": "POOR"
  },
  "timestamp": "2024-01-15T10:31:30Z"
}
```

---

#### 7. Call Error

Sent to the client when a call operation fails.

**Message Type:** `call_error`

**Structure:**
```json
{
  "type": "call_error",
  "payload": {
    "callId": "string",
    "errorType": "string",
    "message": "string"
  }
}
```

**Example:**
```json
{
  "type": "call_error",
  "payload": {
    "callId": "call-uuid-123",
    "errorType": "unauthorized",
    "message": "User is not a participant of this call"
  }
}
```

**Error Types:**
- `call_not_found`: The specified call ID does not exist
- `unauthorized`: User is not authorized to perform the action
- `invalid_status`: Call is not in the correct status for the requested action
- `parse_error`: Failed to parse the incoming message
- `service_error`: Unexpected server error

---

## Message Types Reference

### Client to Server (Incoming)

| Type | Purpose | Payload |
|------|---------|---------|
| `call.accept` | Accept incoming call | `{ callId }` |
| `call.reject` | Reject incoming call | `{ callId, reason }` |
| `call.end` | End active call | `{ callId, durationSeconds? }` |

### Server to Client (Outgoing)

| Type | Purpose | Data Structure |
|------|---------|----------------|
| `call_invitation` | Notify callee of incoming call | `{ callId, channelId, callerId, callerName, callerAvatar, callType }` |
| `call_accepted` | Notify caller of acceptance | `{ callId, acceptedBy }` |
| `call_rejected` | Notify caller of rejection | `{ callId, rejectedBy, reason }` |
| `call_ended` | Notify participant of call end | `{ callId, endedBy, durationSeconds }` |
| `call_timeout` | Notify both participants of timeout | `{ callId }` |
| `call_quality_warning` | Warn about network quality | `{ callId, quality }` |
| `call_error` | Report error to client | `{ callId, errorType, message }` |

---

## Error Handling

### REST API Errors

REST API errors follow the standard HTTP status code conventions:

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

Common status codes:
- `400 BAD_REQUEST`: Invalid request data or invalid state transition
- `401 UNAUTHORIZED`: Missing or invalid authentication token
- `403 FORBIDDEN`: User not authorized for the requested action
- `404 NOT_FOUND`: Call or resource not found
- `429 TOO_MANY_REQUESTS`: Rate limit exceeded
- `500 INTERNAL_SERVER_ERROR`: Unexpected server error

### WebSocket Errors

WebSocket errors are sent as `call_error` messages and do **not** disconnect the WebSocket connection. The client can continue using the connection after receiving an error.

**Error Message Structure:**
```json
{
  "type": "call_error",
  "payload": {
    "callId": "string",
    "errorType": "string",
    "message": "string"
  }
}
```

**Error Types:**

| Error Type | Description | Common Causes |
|------------|-------------|---------------|
| `call_not_found` | Call does not exist | Invalid call ID, call already deleted |
| `unauthorized` | User not authorized | User is not a participant, wrong user attempting action |
| `invalid_status` | Invalid state transition | Accepting already accepted call, ending non-active call |
| `parse_error` | Message parsing failed | Malformed JSON, missing required fields |
| `service_error` | Unexpected server error | Database error, internal service failure |

### Error Handling Best Practices

1. **Always handle `call_error` messages** in your WebSocket client
2. **Display user-friendly error messages** based on `errorType`
3. **Log detailed error information** for debugging
4. **Retry failed operations** with exponential backoff for transient errors
5. **Validate input** on the client side before sending to reduce errors
6. **Keep WebSocket connection alive** after errors - reconnection is not necessary

---

## Examples

### Example 1: Complete Call Flow (WebSocket)

**Step 1: Caller initiates call via REST API**
```http
POST /api/v1/calls/initiate
Authorization: Bearer <caller-token>
Content-Type: application/json

{
  "calleeId": "user-456",
  "callType": "VIDEO",
  "channelId": "agora-channel-789"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Call initiated successfully",
  "data": {
    "id": "call-uuid-123",
    "callerId": "user-789",
    "calleeId": "user-456",
    "callType": "VIDEO",
    "status": "RINGING",
    "channelId": "agora-channel-789"
  }
}
```

**Step 2: Callee receives invitation via WebSocket**
```json
{
  "type": "call_invitation",
  "data": {
    "callId": "call-uuid-123",
    "channelId": "agora-channel-789",
    "callerId": "user-789",
    "callerName": "John Doe",
    "callerAvatar": "https://example.com/avatar.jpg",
    "callType": "VIDEO"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Step 3: Callee accepts via WebSocket**
```json
{
  "type": "call.accept",
  "payload": {
    "callId": "call-uuid-123"
  }
}
```

**Step 4: Caller receives acceptance notification**
```json
{
  "type": "call_accepted",
  "data": {
    "callId": "call-uuid-123",
    "acceptedBy": "user-456"
  },
  "timestamp": "2024-01-15T10:30:15Z"
}
```

**Step 5: Caller ends call via WebSocket**
```json
{
  "type": "call.end",
  "payload": {
    "callId": "call-uuid-123",
    "durationSeconds": 125
  }
}
```

**Step 6: Callee receives end notification**
```json
{
  "type": "call_ended",
  "data": {
    "callId": "call-uuid-123",
    "endedBy": "user-789",
    "durationSeconds": 125
  },
  "timestamp": "2024-01-15T10:32:05Z"
}
```

---

### Example 2: Call Rejection Flow

**Step 1: Callee receives invitation**
```json
{
  "type": "call_invitation",
  "data": {
    "callId": "call-uuid-456",
    "channelId": "agora-channel-789",
    "callerId": "user-123",
    "callerName": "Jane Smith",
    "callerAvatar": null,
    "callType": "AUDIO"
  },
  "timestamp": "2024-01-15T11:00:00Z"
}
```

**Step 2: Callee rejects via WebSocket**
```json
{
  "type": "call.reject",
  "payload": {
    "callId": "call-uuid-456",
    "reason": "busy"
  }
}
```

**Step 3: Caller receives rejection notification**
```json
{
  "type": "call_rejected",
  "data": {
    "callId": "call-uuid-456",
    "rejectedBy": "user-456",
    "reason": "busy"
  },
  "timestamp": "2024-01-15T11:00:05Z"
}
```

---

### Example 3: Call Timeout

**Step 1: Call initiated but not answered**

After 60 seconds of no response, both participants receive:

```json
{
  "type": "call_timeout",
  "data": {
    "callId": "call-uuid-789"
  },
  "timestamp": "2024-01-15T11:01:00Z"
}
```

The call status is automatically updated to `MISSED` and call history entries are created.

---

### Example 4: Error Handling

**Scenario: User tries to accept a call they're not part of**

**Client sends:**
```json
{
  "type": "call.accept",
  "payload": {
    "callId": "call-uuid-999"
  }
}
```

**Server responds:**
```json
{
  "type": "call_error",
  "payload": {
    "callId": "call-uuid-999",
    "errorType": "unauthorized",
    "message": "User is not the callee of this call"
  }
}
```

**Note:** The WebSocket connection remains open and functional after the error.

---

## Implementation Notes

### Backward Compatibility

Both REST API and WebSocket approaches are fully supported:

- **REST API**: Traditional HTTP endpoints remain available for all call operations
- **WebSocket**: Recommended for production use due to lower latency and real-time bidirectional communication

Clients can mix both approaches (e.g., initiate via REST, accept/reject/end via WebSocket).

### Authentication

- **REST API**: Use `Authorization: Bearer <token>` header
- **WebSocket**: Provide token as query parameter: `?token=<token>`

The same JWT token is used for both REST and WebSocket authentication.

### Connection Management

- WebSocket connection is shared between chat and call features
- Connection URL: `/ws/chat`
- Single connection per user session
- Automatic reconnection recommended for production clients

### Message Ordering

- Messages are processed in the order they are received
- Call state transitions are validated server-side
- Invalid state transitions result in `call_error` messages

### Performance Considerations

- WebSocket messages have lower latency than REST API calls
- Call invitation delivery is near-instantaneous via WebSocket
- REST API endpoints are rate-limited to prevent abuse
- WebSocket connections are not rate-limited but monitored for abuse

---

## Related Documentation

- [Chat WebSocket API](./API_DOCUMENTATION.md#4-chat-websocket-api-v1chat) - For chat messaging and typing indicators
- [User Status API](./API_DOCUMENTATION.md#3-user-status-api-v1usersstatus) - For online/offline status
- [Authentication API](./AUTH_API_DOCUMENTATION.md) - For JWT token management

---

---

## Quick Start

**Muốn test nhanh?** Xem [QUICK_START.md](./QUICK_START.md)

**Cần setup users?** Chạy:
```bash
./chattrix-api/.spec/setup-test-users.sh
```

---

**Last Updated:** 2025-11-21
**API Version:** v1
