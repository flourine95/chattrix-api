# Design Document - Agora Video/Audio Call API

## Overview

This design document outlines the implementation of backend APIs to support real-time video and audio calling functionality using Agora SDK in the Chattrix application. The system will provide secure token generation, call signaling, call history management, quality monitoring, and real-time notifications through WebSocket.

The implementation follows the existing layered architecture pattern (Resources → Services → Repositories → Entities) and integrates with the current authentication and WebSocket infrastructure.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Applications                     │
│                   (Flutter UI, Web, Mobile)                  │
└────────────┬────────────────────────────────┬───────────────┘
             │                                │
             │ REST API                       │ WebSocket
             │                                │
┌────────────▼────────────────────────────────▼───────────────┐
│                    API Gateway Layer                         │
│              (JAX-RS Resources + Filters)                    │
├──────────────────────────────────────────────────────────────┤
│                   Service Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Token Service│  │ Call Service │  │ Quality Svc  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├──────────────────────────────────────────────────────────────┤
│                 Repository Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Call Repo    │  │ History Repo │  │ Quality Repo │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├──────────────────────────────────────────────────────────────┤
│                   Data Layer                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Call Entity  │  │ History Ent  │  │ Quality Ent  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────┬─────────────────────────────────────────────────┘
             │
             ▼
    ┌────────────────┐
    │   PostgreSQL   │
    └────────────────┘

External Services:
┌────────────────┐
│  Agora RTC SDK │  (Token Generation)
└────────────────┘
```

### Call Flow State Machine

```
                    ┌──────────────┐
                    │  INITIATING  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
              ┌─────┤   RINGING    ├─────┐
              │     └──────┬───────┘     │
              │            │             │
              │     ┌──────▼───────┐    │
              │     │  CONNECTING  │    │
              │     └──────┬───────┘    │
              │            │             │
              │     ┌──────▼───────┐    │
              │     │  CONNECTED   │    │
              │     └──────┬───────┘    │
              │            │             │
              │     ┌──────▼───────┐    │
              │     │DISCONNECTING │    │
              │     └──────┬───────┘    │
              │            │             │
              │     ┌──────▼───────┐    │
              └────►│    ENDED     │◄───┘
                    └──────────────┘
                           ▲
                    ┌──────┴───────┐
                    │              │
              ┌─────┴─────┐  ┌─────┴─────┐
              │  REJECTED │  │  MISSED   │
              └───────────┘  └───────────┘
```

## Components and Interfaces

### 1. REST Resources (API Endpoints)

#### AgoraTokenResource
**Path:** `/api/v1/agora/token`

**Responsibilities:**
- Generate Agora RTC tokens for channel access
- Refresh expired tokens
- Validate token generation requests

**Endpoints:**
- `POST /generate` - Generate new RTC token
- `POST /refresh` - Refresh existing token

#### CallResource
**Path:** `/api/v1/calls`

**Responsibilities:**
- Initiate new calls
- Accept/reject incoming calls
- End active calls
- Update call status
- Retrieve call details

**Endpoints:**
- `POST /initiate` - Initiate a new call
- `POST /{callId}/accept` - Accept incoming call
- `POST /{callId}/reject` - Reject incoming call
- `POST /{callId}/end` - End active call
- `PATCH /{callId}/status` - Update call status
- `GET /{callId}` - Get call details

#### CallHistoryResource
**Path:** `/api/v1/calls/history`

**Responsibilities:**
- Retrieve user's call history
- Filter and paginate call records
- Delete call history entries

**Endpoints:**
- `GET /` - Get paginated call history
- `DELETE /{callId}` - Delete call history entry

#### CallQualityResource
**Path:** `/api/v1/calls/{callId}/quality`

**Responsibilities:**
- Receive quality metrics from clients
- Store quality data for analysis
- Trigger quality alerts

**Endpoints:**
- `POST /` - Report call quality metrics

#### CallStatisticsResource
**Path:** `/api/v1/calls/statistics`

**Responsibilities:**
- Calculate call statistics
- Aggregate data by period

**Endpoints:**
- `GET /` - Get call statistics

### 2. Service Layer

#### AgoraTokenService
**Responsibilities:**
- Generate Agora RTC tokens using Agora SDK
- Convert user IDs to numeric UIDs
- Validate token parameters
- Log token generation events

**Key Methods:**
```java
TokenResponse generateToken(String channelId, String userId, String role, int expirationSeconds)
TokenResponse refreshToken(String channelId, String userId, String oldToken)
int generateUidFromUserId(String userId)
```

#### CallService
**Responsibilities:**
- Manage call lifecycle (initiate, accept, reject, end)
- Validate call state transitions
- Check user availability
- Send WebSocket notifications
- Handle call timeouts
- Create call history entries

**Key Methods:**
```java
CallResponse initiateCall(String callerId, InitiateCallRequest request)
CallResponse acceptCall(String callId, String userId)
CallResponse rejectCall(String callId, String userId, String reason)
CallResponse endCall(String callId, String userId, int durationSeconds)
CallResponse updateCallStatus(String callId, String userId, String status)
CallDetails getCallDetails(String callId, String userId)
```

#### CallHistoryService
**Responsibilities:**
- Retrieve call history with pagination
- Filter by call type and status
- Delete call history entries
- Create history entries for completed calls

**Key Methods:**
```java
PaginatedResponse<CallHistoryResponse> getCallHistory(String userId, CallHistoryFilter filter)
void deleteCallHistory(String userId, String callId)
void createHistoryEntry(CallHistoryEntry entry)
```

#### CallQualityService
**Responsibilities:**
- Store quality metrics
- Calculate average quality
- Send quality warnings
- Cache quality data

**Key Methods:**
```java
void reportQuality(String callId, String userId, QualityMetrics metrics)
QualityStatistics getCallQualityStats(String callId)
```

#### CallStatisticsService
**Responsibilities:**
- Calculate call statistics
- Aggregate by period and type

**Key Methods:**
```java
CallStatistics getStatistics(String userId, String period)
```

#### WebSocketNotificationService
**Responsibilities:**
- Send real-time notifications to users
- Handle call invitation, acceptance, rejection, end events

**Key Methods:**
```java
void sendCallInvitation(String userId, CallInvitationData data)
void sendCallAccepted(String userId, String callId)
void sendCallRejected(String userId, String callId, String reason)
void sendCallEnded(String userId, String callId, int duration)
void sendCallTimeout(String userId, String callId)
void sendQualityWarning(String userId, String callId, String quality)
```

### 3. Repository Layer

#### CallRepository
**Responsibilities:**
- CRUD operations for Call entities
- Query calls by status, participants
- Update call status

**Key Methods:**
```java
Call save(Call call)
Optional<Call> findById(String callId)
Optional<Call> findActiveCallByUserId(String userId)
List<Call> findByChannelId(String channelId)
void updateStatus(String callId, CallStatus status)
```

#### CallHistoryRepository
**Responsibilities:**
- Store and retrieve call history
- Paginated queries with filters
- Delete history entries

**Key Methods:**
```java
CallHistory save(CallHistory history)
Page<CallHistory> findByUserId(String userId, Pageable pageable, CallHistoryFilter filter)
void deleteByCallIdAndUserId(String callId, String userId)
```

#### CallQualityMetricsRepository
**Responsibilities:**
- Store quality metrics
- Calculate averages
- Query by call ID

**Key Methods:**
```java
CallQualityMetrics save(CallQualityMetrics metrics)
List<CallQualityMetrics> findByCallId(String callId)
QualityStatistics calculateAverageQuality(String callId)
```

### 4. Configuration

#### AgoraConfig
**Responsibilities:**
- Load Agora configuration from environment
- Provide App ID and App Certificate
- Configure token expiration settings

**Properties:**
```java
String appId
String appCertificate
int defaultTokenExpiration
int maxTokenExpiration
```

#### CallConfig
**Responsibilities:**
- Configure call timeout settings
- Set max call duration

**Properties:**
```java
int callTimeoutSeconds
int maxCallDurationSeconds
```

## Data Models

### Entities

#### Call Entity
```java
@Entity
@Table(name = "calls", indexes = {
    @Index(name = "idx_calls_caller_id", columnList = "caller_id"),
    @Index(name = "idx_calls_callee_id", columnList = "callee_id"),
    @Index(name = "idx_calls_status", columnList = "status"),
    @Index(name = "idx_calls_channel_id", columnList = "channel_id"),
    @Index(name = "idx_calls_start_time", columnList = "start_time")
})
public class Call {
    @Id
    private String id;  // UUID
    
    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;
    
    @Column(name = "caller_id", nullable = false)
    private String callerId;
    
    @Column(name = "callee_id", nullable = false)
    private String calleeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 10)
    private CallType callType;  // AUDIO, VIDEO
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;  // INITIATING, RINGING, CONNECTING, CONNECTED, etc.
    
    @Column(name = "start_time")
    private Instant startTime;
    
    @Column(name = "end_time")
    private Instant endTime;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

#### CallHistory Entity
```java
@Entity
@Table(name = "call_history", indexes = {
    @Index(name = "idx_call_history_user_id", columnList = "user_id"),
    @Index(name = "idx_call_history_timestamp", columnList = "timestamp DESC")
})
public class CallHistory {
    @Id
    private String id;  // UUID
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "call_id", nullable = false)
    private String callId;
    
    @Column(name = "remote_user_id", nullable = false)
    private String remoteUserId;
    
    @Column(name = "remote_user_name", nullable = false)
    private String remoteUserName;
    
    @Column(name = "remote_user_avatar")
    private String remoteUserAvatar;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private CallType callType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallHistoryStatus status;  // COMPLETED, MISSED, REJECTED, FAILED
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallDirection direction;  // INCOMING, OUTGOING
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
```

#### CallQualityMetrics Entity
```java
@Entity
@Table(name = "call_quality_metrics", indexes = {
    @Index(name = "idx_quality_call_id", columnList = "call_id"),
    @Index(name = "idx_quality_recorded_at", columnList = "recorded_at")
})
public class CallQualityMetrics {
    @Id
    private String id;  // UUID
    
    @Column(name = "call_id", nullable = false)
    private String callId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "network_quality", length = 20)
    private NetworkQuality networkQuality;  // EXCELLENT, GOOD, POOR, BAD, VERY_BAD, UNKNOWN
    
    @Column(name = "packet_loss_rate")
    private Double packetLossRate;  // 0.0 to 1.0
    
    @Column(name = "round_trip_time")
    private Integer roundTripTime;  // milliseconds
    
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
```

### Enums

```java
public enum CallType {
    AUDIO, VIDEO
}

public enum CallStatus {
    INITIATING,
    RINGING,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ENDED,
    MISSED,
    REJECTED,
    FAILED
}

public enum CallHistoryStatus {
    COMPLETED,
    MISSED,
    REJECTED,
    FAILED
}

public enum CallDirection {
    INCOMING,
    OUTGOING
}

public enum NetworkQuality {
    EXCELLENT,
    GOOD,
    POOR,
    BAD,
    VERY_BAD,
    UNKNOWN
}

public enum AgoraRole {
    PUBLISHER(1),
    SUBSCRIBER(2);
    
    private final int value;
    
    AgoraRole(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}
```

### Request DTOs

#### GenerateTokenRequest
```java
public class GenerateTokenRequest {
    @NotBlank
    @Size(max = 64)
    private String channelId;
    
    @NotBlank
    private String userId;
    
    @NotBlank
    @Pattern(regexp = "publisher|subscriber")
    private String role;
    
    @Min(60)
    @Max(86400)
    private Integer expirationSeconds = 3600;
}
```

#### RefreshTokenRequest
```java
public class RefreshTokenRequest {
    @NotBlank
    private String channelId;
    
    @NotBlank
    private String userId;
    
    @NotBlank
    private String oldToken;
}
```

#### InitiateCallRequest
```java
public class InitiateCallRequest {
    @NotBlank
    private String callId;  // UUID generated by client
    
    @NotBlank
    private String calleeId;
    
    @NotNull
    private CallType callType;
    
    @NotBlank
    @Size(max = 64)
    private String channelId;
}
```

#### AcceptCallRequest
```java
public class AcceptCallRequest {
    @NotBlank
    private String userId;
}
```

#### RejectCallRequest
```java
public class RejectCallRequest {
    @NotBlank
    private String userId;
    
    @NotBlank
    @Pattern(regexp = "busy|declined|unavailable")
    private String reason;
}
```

#### EndCallRequest
```java
public class EndCallRequest {
    @NotBlank
    private String userId;
    
    @NotBlank
    @Pattern(regexp = "caller|callee")
    private String endedBy;
    
    @Min(0)
    private Integer durationSeconds;
}
```

#### UpdateCallStatusRequest
```java
public class UpdateCallStatusRequest {
    @NotBlank
    private String userId;
    
    @NotNull
    private CallStatus status;
}
```

#### ReportQualityRequest
```java
public class ReportQualityRequest {
    @NotBlank
    private String userId;
    
    @NotNull
    private NetworkQuality networkQuality;
    
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double packetLossRate;
    
    @Min(0)
    private Integer roundTripTime;
    
    private Instant timestamp;
}
```

### Response DTOs

#### TokenResponse
```java
public class TokenResponse {
    private String token;
    private String channelId;
    private Integer uid;
    private Instant expiresAt;
}
```

#### CallResponse
```java
public class CallResponse {
    private String callId;
    private String channelId;
    private String callerId;
    private String callerName;
    private String callerAvatar;
    private String calleeId;
    private String calleeName;
    private String calleeAvatar;
    private CallType callType;
    private CallStatus status;
    private Instant createdAt;
}
```

#### CallHistoryResponse
```java
public class CallHistoryResponse {
    private String id;
    private String callId;
    private String remoteUserId;
    private String remoteUserName;
    private String remoteUserAvatar;
    private CallType callType;
    private CallHistoryStatus status;
    private CallDirection direction;
    private Instant timestamp;
    private Integer durationSeconds;
}
```

#### CallStatisticsResponse
```java
public class CallStatisticsResponse {
    private Integer totalCalls;
    private Integer totalDurationMinutes;
    private Map<CallType, Integer> callsByType;
    private Map<CallHistoryStatus, Integer> callsByStatus;
    private Integer averageCallDuration;
}
```

### WebSocket Message DTOs

#### CallInvitationMessage
```java
public class CallInvitationMessage {
    private String type = "call_invitation";
    private CallInvitationData data;
    private Instant timestamp;
}

public class CallInvitationData {
    private String callId;
    private String channelId;
    private String callerId;
    private String callerName;
    private String callerAvatar;
    private CallType callType;
}
```

#### CallAcceptedMessage
```java
public class CallAcceptedMessage {
    private String type = "call_accepted";
    private CallAcceptedData data;
    private Instant timestamp;
}

public class CallAcceptedData {
    private String callId;
    private String acceptedBy;
}
```

#### CallRejectedMessage
```java
public class CallRejectedMessage {
    private String type = "call_rejected";
    private CallRejectedData data;
    private Instant timestamp;
}

public class CallRejectedData {
    private String callId;
    private String rejectedBy;
    private String reason;
}
```

#### CallEndedMessage
```java
public class CallEndedMessage {
    private String type = "call_ended";
    private CallEndedData data;
    private Instant timestamp;
}

public class CallEndedData {
    private String callId;
    private String endedBy;
    private Integer durationSeconds;
}
```

#### CallTimeoutMessage
```java
public class CallTimeoutMessage {
    private String type = "call_timeout";
    private CallTimeoutData data;
    private Instant timestamp;
}

public class CallTimeoutData {
    private String callId;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Callee Validation
*For any* call initiation request, the system should only accept the request if the callee exists and is a contact of the caller
**Validates: Requirements 1.1**

### Property 2: Call Record Creation
*For any* successful call initiation, the system should create a call record with a unique channel ID and status "INITIATING"
**Validates: Requirements 1.2**

### Property 3: Call Invitation Notification
*For any* call initiation, the system should send a WebSocket "call_invitation" event to the callee with complete caller information
**Validates: Requirements 1.3, 11.1**

### Property 4: Concurrent Call Prevention
*For any* user who has an active call (status in INITIATING, RINGING, CONNECTING, CONNECTED), attempting to initiate or receive another call should be rejected with status 409
**Validates: Requirements 1.5, 2.5**

### Property 5: Call Acceptance Status Transition
*For any* call in RINGING status, when accepted by the callee, the status should transition to CONNECTING and a "call_accepted" WebSocket event should be sent to the caller
**Validates: Requirements 2.2, 11.2**

### Property 6: Call Rejection Status Transition
*For any* call in RINGING or INITIATING status, when rejected by the callee, the status should transition to REJECTED and a "call_rejected" WebSocket event with reason should be sent to the caller
**Validates: Requirements 2.3, 11.3**

### Property 7: Token Generation Consistency
*For any* user ID, the system should always generate the same numeric UID, ensuring consistency across multiple token generations
**Validates: Requirements 3.2**

### Property 8: Token Expiration Bounds
*For any* token generation request, the expiration time should be clamped between 60 and 86400 seconds regardless of the requested value
**Validates: Requirements 3.3**

### Property 9: Token Generation Audit
*For any* successful token generation, the system should log the event with user ID, channel ID, UID, and expiration time
**Validates: Requirements 3.4**

### Property 10: Token Refresh Validation
*For any* token refresh request, the system should only succeed if the user has an active call (status in CONNECTING or CONNECTED) in the specified channel
**Validates: Requirements 4.1**

### Property 11: Token Refresh Parameter Preservation
*For any* successful token refresh, the new token should maintain the same channel ID and role as the original token
**Validates: Requirements 4.2**

### Property 12: Token Refresh Audit
*For any* successful token refresh, the system should log the event with user ID, channel ID, and partial old token information
**Validates: Requirements 4.4**

### Property 13: Call End Status Update
*For any* active call (status CONNECTED), when ended by a participant, the status should transition to ENDED and the end_time should be recorded
**Validates: Requirements 5.1**

### Property 14: Call Duration Calculation
*For any* ended call with both start_time and end_time, the duration_seconds should equal (end_time - start_time) in seconds
**Validates: Requirements 5.2**

### Property 15: Call End Notification
*For any* call ending, the system should send a "call_ended" WebSocket event to the other participant with the call ID and duration
**Validates: Requirements 5.3, 11.4**

### Property 16: Call History Creation
*For any* ended call, the system should create exactly two call history entries—one for the caller (direction OUTGOING) and one for the callee (direction INCOMING)
**Validates: Requirements 5.4**

### Property 17: Call Quality Metrics Aggregation
*For any* ended call that has quality metrics, the system should calculate and store the average network quality and packet loss rate
**Validates: Requirements 5.5**

### Property 18: Call History Authorization
*For any* user requesting call history, the system should return only call records where the user is either the caller or callee
**Validates: Requirements 6.1, 9.4**

### Property 19: Call History Pagination
*For any* call history request, the page size should be capped at 100 and the response should include correct pagination metadata (total, totalPages, hasNextPage, hasPrevPage)
**Validates: Requirements 6.2**

### Property 20: Call History Type Filtering
*For any* call history request with callType filter (audio/video), all returned records should match the specified call type
**Validates: Requirements 6.3**

### Property 21: Call History Status Filtering
*For any* call history request with status filter (completed/missed/rejected), all returned records should match the specified status
**Validates: Requirements 6.4**

### Property 22: Call History Ordering
*For any* call history response, the records should be ordered by timestamp in descending order (newest first)
**Validates: Requirements 6.5**

### Property 23: Quality Metrics Validation
*For any* quality metrics report, the system should validate that networkQuality is a valid enum value, packetLossRate is between 0 and 1, and roundTripTime is non-negative
**Validates: Requirements 7.1, 7.2, 7.3**

### Property 24: Poor Quality Warning
*For any* quality metrics report with networkQuality in (POOR, BAD, VERY_BAD), the system should send a "call_quality_warning" WebSocket event to the other participant
**Validates: Requirements 7.4**

### Property 25: Quality Metrics Caching
*For any* quality metrics report, the system should cache the latest metrics in Redis with a 5-minute TTL using key format "call:{callId}:quality:{userId}"
**Validates: Requirements 7.5**

### Property 26: Call Status State Machine
*For any* call status update, the transition should be valid according to the state machine: RINGING→CONNECTING, CONNECTING→(CONNECTED|DISCONNECTING), CONNECTED→DISCONNECTING
**Validates: Requirements 8.1**

### Property 27: Authentication Requirement
*For any* API endpoint call without a valid JWT token, the system should return status 401 with an authentication error
**Validates: Requirements 9.1**

### Property 28: Authorization Enforcement
*For any* API call where a user attempts to access or modify another user's data, the system should return status 403 with error "UNAUTHORIZED_ACCESS"
**Validates: Requirements 9.2, 9.3**

### Property 29: Error Response Structure
*For any* error response, the JSON should contain fields: success (false), error.code, error.message, and requestId
**Validates: Requirements 12.1, 12.5**

### Property 30: Cascade Delete Quality Metrics
*For any* call deletion, all associated quality metrics records should also be deleted
**Validates: Requirements 13.4**

### Property 31: UTC Timestamp Consistency
*For any* timestamp stored in the database (createdAt, updatedAt, startTime, endTime, recordedAt), the timezone should be UTC
**Validates: Requirements 13.5**

### Property 32: Statistics Total Calls Accuracy
*For any* statistics request for a given period, the totalCalls should equal the count of all calls within that period for the user
**Validates: Requirements 15.1**

### Property 33: Statistics Type Aggregation
*For any* statistics response, the sum of callsByType values (audio + video) should equal totalCalls
**Validates: Requirements 15.2**

### Property 34: Statistics Status Aggregation
*For any* statistics response, the sum of callsByStatus values (completed + missed + rejected + failed) should equal totalCalls
**Validates: Requirements 15.3**

### Property 35: Statistics Duration Calculation
*For any* statistics response, totalDurationMinutes should equal the sum of all call durations divided by 60
**Validates: Requirements 15.4**

### Property 36: Statistics Average Duration
*For any* statistics response with totalCalls > 0, averageCallDuration should equal totalDurationMinutes / totalCalls
**Validates: Requirements 15.5**

## Error Handling

### Error Response Format

All error responses follow a consistent structure:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message"
  },
  "requestId": "uuid"
}
```

### Error Codes and HTTP Status Mapping

| Error Code | HTTP Status | Description | When to Use |
|------------|-------------|-------------|-------------|
| `TOKEN_GENERATION_FAILED` | 500 | Failed to generate Agora token | Agora SDK error or configuration issue |
| `TOKEN_REFRESH_FAILED` | 500 | Failed to refresh token | Token refresh process error |
| `NO_ACTIVE_CALL` | 400 | No active call found | Token refresh without active call |
| `CALL_NOT_FOUND` | 404 | Call record not found | Invalid call ID |
| `USER_NOT_FOUND` | 404 | User does not exist | Invalid callee ID |
| `NOT_CONTACTS` | 403 | Users are not contacts | Attempting to call non-contact |
| `USER_BUSY` | 409 | User already in a call | Concurrent call attempt |
| `CALL_TIMEOUT` | 400 | Call has timed out | Accepting expired call |
| `CALL_ALREADY_ENDED` | 400 | Call has already ended | Operating on ended call |
| `INVALID_CALL_STATUS` | 400 | Invalid call status | Wrong status for operation |
| `INVALID_STATUS_TRANSITION` | 400 | Invalid state transition | Violating state machine |
| `INVALID_CALL_TYPE` | 400 | Invalid call type | Call type not audio/video |
| `INVALID_CHANNEL_ID` | 400 | Invalid channel ID format | Channel ID validation failure |
| `INVALID_ROLE` | 400 | Invalid Agora role | Role not publisher/subscriber |
| `INVALID_EXPIRATION` | 400 | Invalid expiration time | Expiration out of bounds |
| `INVALID_QUALITY` | 400 | Invalid network quality | Quality enum validation failure |
| `INVALID_PACKET_LOSS` | 400 | Invalid packet loss rate | Packet loss not in 0-1 range |
| `INVALID_RTT` | 400 | Invalid round trip time | Negative RTT value |
| `INVALID_REASON` | 400 | Invalid rejection reason | Reason not in allowed values |
| `UNAUTHORIZED_ACCESS` | 403 | Access denied | Cross-user access attempt |
| `AUTHENTICATION_FAILED` | 401 | Authentication failed | Invalid/missing JWT token |
| `RATE_LIMIT_EXCEEDED` | 429 | Rate limit exceeded | Too many requests |
| `VALIDATION_ERROR` | 400 | Request validation failed | Bean validation failure |
| `INTERNAL_SERVER_ERROR` | 500 | Internal server error | Unexpected server error |

### Exception Handling Strategy

1. **Business Logic Exceptions**: Custom exceptions thrown by services
   - `BusinessException` - Base class for business logic errors
   - `ResourceNotFoundException` - Entity not found
   - `UnauthorizedException` - Authentication/authorization failure
   - `BadRequestException` - Invalid request data
   - `ConflictException` - Resource conflict (e.g., user busy)

2. **Validation Exceptions**: Bean Validation (JSR 380)
   - Automatically handled by `ConstraintViolationExceptionMapper`
   - Returns 400 with detailed validation errors

3. **JAX-RS Exceptions**: WebApplicationException and subclasses
   - Mapped to appropriate HTTP status codes
   - Wrapped in standard error response format

4. **Unexpected Exceptions**: Runtime exceptions
   - Logged with full stack trace
   - Returned as 500 Internal Server Error
   - Include request ID for debugging

### Logging Strategy

All errors should be logged with appropriate levels:

- **ERROR**: Server errors (500), authentication failures, unexpected exceptions
- **WARN**: Business logic errors (400, 403, 404, 409), rate limiting
- **INFO**: Successful operations, state transitions
- **DEBUG**: Detailed operation flow, parameter values

Log format should include:
- Timestamp
- Log level
- Request ID
- User ID (if authenticated)
- Operation name
- Error message
- Stack trace (for errors)

## Testing Strategy

### Unit Testing

Unit tests will verify individual components in isolation using JUnit 5 and Mockito.

**Test Coverage:**

1. **Service Layer Tests**
   - Mock repositories and external dependencies
   - Test business logic and validation
   - Test error handling and exception throwing
   - Test state transitions
   - Verify method calls on mocked dependencies

2. **Repository Layer Tests**
   - Use in-memory H2 database for testing
   - Test CRUD operations
   - Test custom queries
   - Test pagination and filtering

3. **Mapper Tests**
   - Test entity to DTO mapping
   - Test DTO to entity mapping
   - Test null handling
   - Test collection mapping

4. **Validation Tests**
   - Test Bean Validation annotations
   - Test custom validators
   - Test validation error messages

**Example Unit Test Structure:**

```java
@ExtendWith(MockitoExtension.class)
class CallServiceTest {
    
    @Mock
    private CallRepository callRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private WebSocketNotificationService notificationService;
    
    @InjectMocks
    private CallService callService;
    
    @Test
    void initiateCall_WithValidData_ShouldCreateCall() {
        // Arrange
        InitiateCallRequest request = createValidRequest();
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(callRepository.save(any())).thenReturn(new Call());
        
        // Act
        CallResponse response = callService.initiateCall("caller123", request);
        
        // Assert
        assertNotNull(response);
        verify(callRepository).save(any(Call.class));
        verify(notificationService).sendCallInvitation(any(), any());
    }
    
    @Test
    void initiateCall_WithNonContact_ShouldThrowException() {
        // Arrange
        InitiateCallRequest request = createValidRequest();
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));
        when(contactRepository.areContacts(any(), any())).thenReturn(false);
        
        // Act & Assert
        assertThrows(ForbiddenException.class, 
            () -> callService.initiateCall("caller123", request));
    }
}
```

### Property-Based Testing

Property-based tests will verify universal properties using **JUnit-Quickcheck** library.

**Library Setup:**

```xml
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-core</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-generators</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```

**Property Test Configuration:**
- Each property test should run a minimum of 100 iterations
- Use custom generators for domain objects
- Tag each test with the property number from design document

**Example Property Test:**

```java
@RunWith(JUnitQuickcheck.class)
public class CallServicePropertyTest {
    
    /**
     * Feature: agora-video-audio-call-api, Property 7: Token Generation Consistency
     * For any user ID, the system should always generate the same numeric UID
     */
    @Property(trials = 100)
    public void tokenGeneration_SameUserId_ProducesSameUid(
        @From(UserIdGenerator.class) String userId) {
        
        // Arrange
        AgoraTokenService service = new AgoraTokenService();
        
        // Act
        int uid1 = service.generateUidFromUserId(userId);
        int uid2 = service.generateUidFromUserId(userId);
        
        // Assert
        assertEquals(uid1, uid2, 
            "Same user ID should always produce the same UID");
    }
    
    /**
     * Feature: agora-video-audio-call-api, Property 8: Token Expiration Bounds
     * For any token generation request, expiration should be clamped between 60 and 86400
     */
    @Property(trials = 100)
    public void tokenGeneration_AnyExpiration_ClampedToBounds(
        @InRange(minInt = -1000, maxInt = 100000) int requestedExpiration) {
        
        // Arrange
        AgoraTokenService service = new AgoraTokenService();
        GenerateTokenRequest request = new GenerateTokenRequest();
        request.setExpirationSeconds(requestedExpiration);
        
        // Act
        int actualExpiration = service.validateExpiration(requestedExpiration);
        
        // Assert
        assertTrue(actualExpiration >= 60 && actualExpiration <= 86400,
            "Expiration should be clamped between 60 and 86400 seconds");
    }
    
    /**
     * Feature: agora-video-audio-call-api, Property 14: Call Duration Calculation
     * For any ended call, duration should equal end_time - start_time
     */
    @Property(trials = 100)
    public void callEnd_WithStartAndEndTime_CalculatesCorrectDuration(
        @From(InstantGenerator.class) Instant startTime,
        @InRange(minInt = 1, maxInt = 7200) int durationSeconds) {
        
        // Arrange
        Instant endTime = startTime.plusSeconds(durationSeconds);
        Call call = new Call();
        call.setStartTime(startTime);
        call.setEndTime(endTime);
        
        // Act
        int calculatedDuration = callService.calculateDuration(call);
        
        // Assert
        assertEquals(durationSeconds, calculatedDuration,
            "Duration should equal end_time - start_time in seconds");
    }
}
```

**Custom Generators:**

```java
public class UserIdGenerator extends Generator<String> {
    public UserIdGenerator() {
        super(String.class);
    }
    
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        return "user_" + random.nextInt(1, 10000);
    }
}

public class CallGenerator extends Generator<Call> {
    public CallGenerator() {
        super(Call.class);
    }
    
    @Override
    public Call generate(SourceOfRandomness random, GenerationStatus status) {
        Call call = new Call();
        call.setId(UUID.randomUUID().toString());
        call.setChannelId("channel_" + random.nextInt(1, 1000));
        call.setCallerId("user_" + random.nextInt(1, 1000));
        call.setCalleeId("user_" + random.nextInt(1, 1000));
        call.setCallType(random.choose(Arrays.asList(CallType.values())));
        call.setStatus(random.choose(Arrays.asList(CallStatus.values())));
        call.setCreatedAt(Instant.now());
        return call;
    }
}
```

### Integration Testing

Integration tests will verify end-to-end flows using Arquillian or REST Assured.

**Test Scenarios:**

1. **Complete Call Flow**
   - Initiate call → Accept → Connect → End
   - Verify database state at each step
   - Verify WebSocket notifications

2. **Call Rejection Flow**
   - Initiate call → Reject
   - Verify status and history

3. **Call Timeout Flow**
   - Initiate call → Wait 60s → Verify timeout
   - Verify notifications sent

4. **Token Generation and Refresh**
   - Generate token → Use in call → Refresh → Verify new token

5. **Call History and Statistics**
   - Create multiple calls → Query history → Verify pagination and filtering
   - Calculate statistics → Verify aggregations

### Test Data Management

- Use **Flyway** or **Liquibase** for test database migrations
- Create test data builders for complex objects
- Use **TestContainers** for PostgreSQL integration tests
- Clean up test data after each test

### Performance Testing

- Load test token generation endpoint (target: 100 req/s)
- Load test call initiation endpoint (target: 50 req/s)
- Test concurrent call handling (target: 1000 concurrent calls)
- Test WebSocket scalability (target: 10000 concurrent connections)

## Implementation Notes

### Agora SDK Integration

The Agora RTC SDK for Java will be used for token generation:

```xml
<dependency>
    <groupId>io.agora</groupId>
    <artifactId>authentication</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Token Generation Example:**

```java
import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;

public String generateToken(String channelName, int uid, int expirationSeconds) {
    int timestamp = (int)(System.currentTimeMillis() / 1000);
    int privilegeExpiredTs = timestamp + expirationSeconds;
    
    return RtcTokenBuilder.buildTokenWithUid(
        appId,
        appCertificate,
        channelName,
        uid,
        Role.Role_Publisher,
        privilegeExpiredTs
    );
}
```

### UID Generation Strategy

Convert string user IDs to numeric UIDs using MD5 hash:

```java
public int generateUidFromUserId(String userId) {
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(userId.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        return Math.abs(buffer.getInt());
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("MD5 algorithm not available", e);
    }
}
```

### Call Timeout Implementation

Use **ScheduledExecutorService** for timeout handling:

```java
@ApplicationScoped
public class CallTimeoutScheduler {
    
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(10);
    
    public void scheduleTimeout(String callId, int timeoutSeconds) {
        scheduler.schedule(() -> {
            handleCallTimeout(callId);
        }, timeoutSeconds, TimeUnit.SECONDS);
    }
    
    private void handleCallTimeout(String callId) {
        // Update call status to MISSED
        // Send timeout notifications
    }
}
```

### WebSocket Integration

Integrate with existing WebSocket infrastructure:

```java
@Inject
private ChatSessionService chatSessionService;

public void sendCallInvitation(String userId, CallInvitationData data) {
    CallInvitationMessage message = new CallInvitationMessage();
    message.setData(data);
    message.setTimestamp(Instant.now());
    
    Session session = chatSessionService.getSession(userId);
    if (session != null && session.isOpen()) {
        session.getAsyncRemote().sendObject(message);
    }
}
```

### Database Migration

Use Flyway for database schema management:

**V1__create_calls_table.sql:**

```sql
CREATE TABLE calls (
    id VARCHAR(36) PRIMARY KEY,
    channel_id VARCHAR(64) NOT NULL,
    caller_id VARCHAR(255) NOT NULL,
    callee_id VARCHAR(255) NOT NULL,
    call_type VARCHAR(10) NOT NULL CHECK (call_type IN ('AUDIO', 'VIDEO')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INITIATING', 'RINGING', 'CONNECTING', 'CONNECTED', 'DISCONNECTING', 'ENDED', 'MISSED', 'REJECTED', 'FAILED')),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_seconds INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_calls_caller_id ON calls(caller_id);
CREATE INDEX idx_calls_callee_id ON calls(callee_id);
CREATE INDEX idx_calls_status ON calls(status);
CREATE INDEX idx_calls_channel_id ON calls(channel_id);
CREATE INDEX idx_calls_start_time ON calls(start_time);
```

**V2__create_call_history_table.sql:**

```sql
CREATE TABLE call_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    call_id VARCHAR(36) NOT NULL,
    remote_user_id VARCHAR(255) NOT NULL,
    remote_user_name VARCHAR(100) NOT NULL,
    remote_user_avatar VARCHAR(500),
    call_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('INCOMING', 'OUTGOING')),
    timestamp TIMESTAMP NOT NULL,
    duration_seconds INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, call_id)
);

CREATE INDEX idx_call_history_user_id ON call_history(user_id);
CREATE INDEX idx_call_history_timestamp ON call_history(timestamp DESC);
```

**V3__create_call_quality_metrics_table.sql:**

```sql
CREATE TABLE call_quality_metrics (
    id VARCHAR(36) PRIMARY KEY,
    call_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    network_quality VARCHAR(20),
    packet_loss_rate DECIMAL(5,4),
    round_trip_time INTEGER,
    recorded_at TIMESTAMP NOT NULL,
    FOREIGN KEY (call_id) REFERENCES calls(id) ON DELETE CASCADE
);

CREATE INDEX idx_quality_call_id ON call_quality_metrics(call_id);
CREATE INDEX idx_quality_recorded_at ON call_quality_metrics(recorded_at);
```

### Environment Configuration

**application.properties:**

```properties
# Agora Configuration
agora.app.id=${AGORA_APP_ID}
agora.app.certificate=${AGORA_APP_CERTIFICATE}
agora.token.expiration.default=3600
agora.token.expiration.max=86400

# Call Configuration
call.timeout.seconds=60
call.max.duration.seconds=7200

# Rate Limiting
rate.limit.token.generation=10
rate.limit.call.initiation=5
rate.limit.call.history=20
```

### Rate Limiting Implementation

Use **Guava RateLimiter** or **Bucket4j**:

```java
@ApplicationScoped
public class RateLimitService {
    
    private final LoadingCache<String, RateLimiter> limiters = 
        CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(getPermitsPerMinute(key));
                }
            });
    
    public boolean tryAcquire(String userId, String operation) {
        String key = userId + ":" + operation;
        RateLimiter limiter = limiters.getUnchecked(key);
        return limiter.tryAcquire();
    }
    
    private double getPermitsPerMinute(String key) {
        if (key.contains("token-generation")) return 10.0;
        if (key.contains("call-initiation")) return 5.0;
        if (key.contains("call-history")) return 20.0;
        return 10.0;
    }
}
```

## Security Considerations

1. **Token Security**
   - Never expose Agora App Certificate in logs or responses
   - Validate token requests against active calls
   - Implement token refresh to minimize exposure window

2. **Authorization**
   - Verify user identity on all operations
   - Prevent cross-user data access
   - Validate contact relationships before allowing calls

3. **Input Validation**
   - Sanitize all user inputs
   - Validate channel ID format
   - Enforce expiration bounds

4. **Rate Limiting**
   - Protect against DoS attacks
   - Implement per-user rate limits
   - Return 429 with Retry-After header

5. **Audit Logging**
   - Log all token generations and refreshes
   - Log call state transitions
   - Log authorization failures

## Deployment Considerations

1. **Database**
   - Use connection pooling (HikariCP)
   - Configure appropriate indexes
   - Set up database backups
   - Monitor query performance

2. **WebSocket**
   - Configure WebSocket timeout settings
   - Implement reconnection logic
   - Monitor active connections
   - Scale horizontally with sticky sessions

3. **Monitoring**
   - Track API response times
   - Monitor error rates
   - Alert on high failure rates
   - Track WebSocket connection metrics

4. **Scalability**
   - Use Redis for distributed caching
   - Implement database read replicas
   - Use load balancer for horizontal scaling
   - Consider message queue for async operations

## Dependencies

### Maven Dependencies to Add

```xml
<!-- Agora RTC SDK -->
<dependency>
    <groupId>io.agora</groupId>
    <artifactId>authentication</artifactId>
    <version>2.0.0</version>
</dependency>

<!-- JUnit Quickcheck for Property-Based Testing -->
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-core</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.pholser</groupId>
    <artifactId>junit-quickcheck-generators</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>

<!-- Flyway for Database Migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.0</version>
</dependency>
```

## Summary

This design provides a comprehensive backend API implementation for Agora video/audio calling functionality. The system follows the existing architectural patterns, implements robust error handling, includes comprehensive testing strategies with both unit and property-based tests, and ensures security through authentication, authorization, and rate limiting.

Key implementation priorities:
1. **Phase 1 (MVP)**: Token generation, call signaling, WebSocket events
2. **Phase 2**: Token refresh, call history, status management
3. **Phase 3**: Quality monitoring, statistics, advanced features

The design ensures scalability, maintainability, and correctness through well-defined interfaces, state machines, and testable properties.
