# Implementation Plan - Agora Video/Audio Call API

## Task List

- [x] 1. Set up project dependencies and configuration





  - Add Agora RTC SDK dependency to pom.xml
  - Add JUnit-Quickcheck for property-based testing
  - Add Flyway for database migrations
  - Create configuration classes for Agora and Call settings
  - Add environment variables to application.properties
  - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [x] 2. Create database schema and entities




  - [x] 2.1 Create Flyway migration for calls table


    - Write V1__create_calls_table.sql with indexes
    - _Requirements: 13.1_
  
  - [x] 2.2 Create Flyway migration for call_history table


    - Write V2__create_call_history_table.sql with indexes
    - _Requirements: 13.2_
  
  - [x] 2.3 Create Flyway migration for call_quality_metrics table


    - Write V3__create_call_quality_metrics_table.sql with indexes and foreign key
    - _Requirements: 13.3, 13.4_
  
  - [x] 2.4 Create Call entity with Lombok annotations


    - Define all fields, enums (CallType, CallStatus)
    - Add JPA annotations and indexes
    - Add @PrePersist and @PreUpdate lifecycle methods
    - _Requirements: 1.2, 8.1, 13.5_
  
  - [x] 2.5 Create CallHistory entity


    - Define all fields, enums (CallHistoryStatus, CallDirection)
    - Add JPA annotations and indexes
    - _Requirements: 5.4, 6.1_
  
  - [x] 2.6 Create CallQualityMetrics entity


    - Define all fields, enum (NetworkQuality)
    - Add JPA annotations and foreign key relationship
    - _Requirements: 7.1, 13.4_
  
  - [ ]* 2.7 Write property test for cascade delete
    - **Property 30: Cascade Delete Quality Metrics**
    - **Validates: Requirements 13.4**
  
  - [ ]* 2.8 Write property test for UTC timestamp consistency
    - **Property 31: UTC Timestamp Consistency**
    - **Validates: Requirements 13.5**

- [x] 3. Create DTOs and mappers




  - [x] 3.1 Create request DTOs





    - GenerateTokenRequest with validation annotations
    - RefreshTokenRequest
    - InitiateCallRequest
    - AcceptCallRequest, RejectCallRequest, EndCallRequest
    - UpdateCallStatusRequest
    - ReportQualityRequest
    - _Requirements: 1.1, 2.2, 2.3, 3.1, 4.1, 5.1, 7.1, 7.2, 7.3_
  
  - [x] 3.2 Create response DTOs


    - TokenResponse
    - CallResponse
    - CallHistoryResponse
    - CallStatisticsResponse
    - _Requirements: 3.1, 1.2, 6.1, 15.1_
  
  - [x] 3.3 Create WebSocket message DTOs


    - CallInvitationMessage and CallInvitationData
    - CallAcceptedMessage, CallRejectedMessage, CallEndedMessage
    - CallTimeoutMessage, CallQualityWarningMessage
    - _Requirements: 1.3, 2.2, 2.3, 5.3, 7.4_
  
  - [x] 3.4 Create MapStruct mappers


    - CallMapper (entity ↔ response DTO)
    - CallHistoryMapper
    - Use @Mapper(componentModel = "cdi")
    - _Requirements: 1.2, 6.1_

- [x] 4. Implement repositories





  - [x] 4.1 Create CallRepository


    - Implement save, findById, findActiveCallByUserId
    - Implement findByChannelId, updateStatus
    - _Requirements: 1.2, 1.5, 2.5_
  
  - [x] 4.2 Create CallHistoryRepository


    - Implement save, findByUserId with pagination
    - Implement filtering by call type and status
    - Implement deleteByCallIdAndUserId
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  
  - [x] 4.3 Create CallQualityMetricsRepository


    - Implement save, findByCallId
    - Implement calculateAverageQuality
    - _Requirements: 7.1, 5.5_
  
  - [ ]* 4.4 Write unit tests for repositories
    - Test CRUD operations with H2 database
    - Test pagination and filtering
    - Test cascade delete
    - _Requirements: 6.2, 6.3, 6.4, 13.4_

- [x] 5. Implement Agora token service





  - [x] 5.1 Create AgoraConfig class

    - Load appId, appCertificate from environment
    - Configure token expiration settings
    - _Requirements: 14.1, 14.2, 14.3_
  
  - [x] 5.2 Implement AgoraTokenService


    - Implement generateToken method using Agora SDK
    - Implement generateUidFromUserId using MD5 hash
    - Implement validateExpiration to clamp bounds
    - Add audit logging for token generation
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [ ]* 5.3 Write property test for UID consistency
    - **Property 7: Token Generation Consistency**
    - **Validates: Requirements 3.2**
  
  - [ ]* 5.4 Write property test for expiration bounds
    - **Property 8: Token Expiration Bounds**
    - **Validates: Requirements 3.3**
  
  - [ ]* 5.5 Write property test for token generation audit
    - **Property 9: Token Generation Audit**
    - **Validates: Requirements 3.4**
  
  - [x] 5.6 Implement token refresh logic

    - Implement refreshToken method
    - Validate active call exists
    - Preserve channel and role parameters
    - Add audit logging for refresh
    - _Requirements: 4.1, 4.2, 4.4_
  
  - [ ]* 5.7 Write property test for token refresh validation
    - **Property 10: Token Refresh Validation**
    - **Validates: Requirements 4.1**
  
  - [ ]* 5.8 Write property test for parameter preservation
    - **Property 11: Token Refresh Parameter Preservation**
    - **Validates: Requirements 4.2**
  
  - [ ]* 5.9 Write unit tests for error cases
    - Test TOKEN_GENERATION_FAILED error
    - Test NO_ACTIVE_CALL error
    - Test TOKEN_REFRESH_FAILED error
    - _Requirements: 3.5, 4.3, 4.5_

- [x] 6. Implement WebSocket notification service




  - [x] 6.1 Create WebSocketNotificationService

    - Inject ChatSessionService
    - Implement sendCallInvitation method
    - Implement sendCallAccepted, sendCallRejected, sendCallEnded
    - Implement sendCallTimeout, sendQualityWarning
    - _Requirements: 1.3, 2.2, 2.3, 5.3, 7.4, 11.1, 11.2, 11.3, 11.4_
  
  - [ ]* 6.2 Write unit tests for WebSocket notifications
    - Mock ChatSessionService
    - Verify correct message format
    - Verify messages sent to correct users
    - _Requirements: 1.3, 2.2, 2.3, 5.3_

- [x] 7. Implement call timeout scheduler




  - [x] 7.1 Create CallTimeoutScheduler


    - Use ScheduledExecutorService
    - Implement scheduleTimeout method
    - Implement handleCallTimeout to update status and notify
    - _Requirements: 1.4_
  
  - [ ]* 7.2 Write integration test for call timeout
    - Test 60-second timeout behavior
    - Verify status updated to MISSED
    - Verify both participants notified
    - _Requirements: 1.4, 11.5_

- [x] 8. Implement call service








  - [x] 8.1 Create CallService class


    - Inject CallRepository, UserRepository, ContactRepository
    - Inject WebSocketNotificationService, CallTimeoutScheduler
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [x] 8.2 Implement initiateCall method

    - Validate callee exists and is a contact
    - Check caller and callee not already in calls
    - Create call record with INITIATING status
    - Send WebSocket invitation to callee
    - Update status to RINGING
    - Schedule 60-second timeout
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ]* 8.3 Write property test for callee validation
    - **Property 1: Callee Validation**
    - **Validates: Requirements 1.1**
  
  - [ ]* 8.4 Write property test for call record creation
    - **Property 2: Call Record Creation**
    - **Validates: Requirements 1.2**
  
  - [ ]* 8.5 Write property test for call invitation notification
    - **Property 3: Call Invitation Notification**
    - **Validates: Requirements 1.3, 11.1**
  
  - [ ]* 8.6 Write property test for concurrent call prevention
    - **Property 4: Concurrent Call Prevention**
    - **Validates: Requirements 1.5, 2.5**
  
  - [x] 8.7 Implement acceptCall method

    - Verify user is callee
    - Check call status is RINGING
    - Check call not timed out
    - Update status to CONNECTING
    - Send WebSocket notification to caller
    - _Requirements: 2.2, 2.4_
  
  - [ ]* 8.8 Write property test for call acceptance
    - **Property 5: Call Acceptance Status Transition**
    - **Validates: Requirements 2.2, 11.2**
  
  - [x] 8.9 Implement rejectCall method

    - Verify user is callee
    - Check call status is RINGING or INITIATING
    - Update status to REJECTED
    - Store rejection reason
    - Send WebSocket notification to caller
    - _Requirements: 2.3_
  
  - [ ]* 8.10 Write property test for call rejection
    - **Property 6: Call Rejection Status Transition**
    - **Validates: Requirements 2.3, 11.3**
  
  - [x] 8.11 Implement endCall method

    - Verify user is participant
    - Check call not already ended
    - Calculate duration if not provided
    - Update status to ENDED
    - Send WebSocket notification to other participant
    - Create call history entries for both participants
    - Calculate average quality metrics
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [ ]* 8.12 Write property test for call end status update
    - **Property 13: Call End Status Update**
    - **Validates: Requirements 5.1**
  
  - [ ]* 8.13 Write property test for duration calculation
    - **Property 14: Call Duration Calculation**
    - **Validates: Requirements 5.2**
  
  - [ ]* 8.14 Write property test for call end notification
    - **Property 15: Call End Notification**
    - **Validates: Requirements 5.3, 11.4**
  
  - [ ]* 8.15 Write property test for call history creation
    - **Property 16: Call History Creation**
    - **Validates: Requirements 5.4**
  
  - [ ]* 8.16 Write property test for quality metrics aggregation
    - **Property 17: Call Quality Metrics Aggregation**
    - **Validates: Requirements 5.5**
  
  - [x] 8.17 Implement updateCallStatus method

    - Verify user is participant
    - Validate status transition using state machine
    - Update status in database
    - Record start_time when status becomes CONNECTED
    - Notify other participant
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [ ]* 8.18 Write property test for state machine validation
    - **Property 26: Call Status State Machine**
    - **Validates: Requirements 8.1**
  
  - [x] 8.19 Implement getCallDetails method

    - Verify user is participant
    - Return call details with participant info
    - _Requirements: 9.2_
  
  - [ ]* 8.20 Write unit tests for error cases
    - Test USER_NOT_FOUND, NOT_CONTACTS, USER_BUSY
    - Test CALL_NOT_FOUND, CALL_TIMEOUT, CALL_ALREADY_ENDED
    - Test INVALID_CALL_STATUS, INVALID_STATUS_TRANSITION
    - _Requirements: 1.5, 2.4, 2.5, 8.5, 12.2_

- [x] 9. Checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement call history service





  - [x] 10.1 Create CallHistoryService class


    - Inject CallHistoryRepository, UserRepository
    - _Requirements: 6.1_
  
  - [x] 10.2 Implement getCallHistory method

    - Validate pagination parameters (max 100)
    - Build filters for call type and status
    - Query with pagination and ordering
    - Return paginated response with metadata
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  
  - [ ]* 10.3 Write property test for authorization
    - **Property 18: Call History Authorization**
    - **Validates: Requirements 6.1, 9.4**
  
  - [ ]* 10.4 Write property test for pagination
    - **Property 19: Call History Pagination**
    - **Validates: Requirements 6.2**
  
  - [ ]* 10.5 Write property test for type filtering
    - **Property 20: Call History Type Filtering**
    - **Validates: Requirements 6.3**
  
  - [ ]* 10.6 Write property test for status filtering
    - **Property 21: Call History Status Filtering**
    - **Validates: Requirements 6.4**
  
  - [ ]* 10.7 Write property test for ordering
    - **Property 22: Call History Ordering**
    - **Validates: Requirements 6.5**
  
  - [x] 10.8 Implement deleteCallHistory method

    - Verify user owns the history entry
    - Delete from database
    - _Requirements: 9.2_
  
  - [x] 10.9 Implement createHistoryEntry helper method

    - Get remote user info
    - Create history entry with all fields
    - Handle upsert for duplicate entries
    - _Requirements: 5.4_

- [x] 11. Implement call quality service





  - [x] 11.1 Create CallQualityService class


    - Inject CallQualityMetricsRepository, CallRepository
    - Inject WebSocketNotificationService
    - _Requirements: 7.1_
  
  - [x] 11.2 Implement reportQuality method

    - Verify user is participant
    - Validate quality metrics (enum, packet loss, RTT)
    - Store metrics in database
    - Send warning if quality is poor
    - Cache metrics in Redis
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ]* 11.3 Write property test for quality validation
    - **Property 23: Quality Metrics Validation**
    - **Validates: Requirements 7.1, 7.2, 7.3**
  
  - [ ]* 11.4 Write property test for poor quality warning
    - **Property 24: Poor Quality Warning**
    - **Validates: Requirements 7.4**
  
  - [ ]* 11.5 Write property test for quality caching
    - **Property 25: Quality Metrics Caching**
    - **Validates: Requirements 7.5**
  
  - [x] 11.6 Implement getCallQualityStats method

    - Calculate average quality and packet loss
    - Return statistics
    - _Requirements: 5.5_

- [x] 12. Implement call statistics service





  - [x] 12.1 Create CallStatisticsService class


    - Inject CallHistoryRepository
    - _Requirements: 15.1_
  
  - [x] 12.2 Implement getStatistics method


    - Parse period parameter (day/week/month/year)
    - Calculate date range
    - Query call history for period
    - Aggregate by type and status
    - Calculate total duration and average
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_
  
  - [ ]* 12.3 Write property test for total calls accuracy
    - **Property 32: Statistics Total Calls Accuracy**
    - **Validates: Requirements 15.1**
  
  - [ ]* 12.4 Write property test for type aggregation
    - **Property 33: Statistics Type Aggregation**
    - **Validates: Requirements 15.2**
  
  - [ ]* 12.5 Write property test for status aggregation
    - **Property 34: Statistics Status Aggregation**
    - **Validates: Requirements 15.3**
  
  - [ ]* 12.6 Write property test for duration calculation
    - **Property 35: Statistics Duration Calculation**
    - **Validates: Requirements 15.4**
  
  - [ ]* 12.7 Write property test for average duration
    - **Property 36: Statistics Average Duration**
    - **Validates: Requirements 15.5**

- [x] 13. Implement REST resources




  - [x] 13.1 Create AgoraTokenResource


    - Add @Path("/v1/agora/token"), @Secured
    - Inject AgoraTokenService
    - Implement POST /generate endpoint
    - Implement POST /refresh endpoint
    - Add validation and error handling
    - _Requirements: 3.1, 4.1, 9.1_
  
  - [x] 13.2 Create CallResource


    - Add @Path("/v1/calls"), @Secured
    - Inject CallService
    - Implement POST /initiate endpoint
    - Implement POST /{callId}/accept endpoint
    - Implement POST /{callId}/reject endpoint
    - Implement POST /{callId}/end endpoint
    - Implement PATCH /{callId}/status endpoint
    - Implement GET /{callId} endpoint
    - Add validation and error handling
    - _Requirements: 1.1, 2.2, 2.3, 5.1, 8.1, 9.1_
  
  - [x] 13.3 Create CallHistoryResource


    - Add @Path("/v1/calls/history"), @Secured
    - Inject CallHistoryService
    - Implement GET / endpoint with query parameters
    - Implement DELETE /{callId} endpoint
    - Add validation and error handling
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 9.1_
  
  - [x] 13.4 Create CallQualityResource


    - Add @Path("/v1/calls/{callId}/quality"), @Secured
    - Inject CallQualityService
    - Implement POST / endpoint
    - Add validation and error handling
    - _Requirements: 7.1, 9.1_
  
  - [x] 13.5 Create CallStatisticsResource


    - Add @Path("/v1/calls/statistics"), @Secured
    - Inject CallStatisticsService
    - Implement GET / endpoint with period parameter
    - Add validation and error handling
    - _Requirements: 15.1, 9.1_
  
  - [ ]* 13.6 Write property test for authentication requirement
    - **Property 27: Authentication Requirement**
    - **Validates: Requirements 9.1**
  
  - [ ]* 13.7 Write property test for authorization enforcement
    - **Property 28: Authorization Enforcement**
    - **Validates: Requirements 9.2, 9.3**

- [x] 14. Implement error handling






  - [x] 14.1 Create custom exception classes

    - CallNotFoundException extends ResourceNotFoundException
    - UserBusyException extends ConflictException
    - CallTimeoutException extends BadRequestException
    - InvalidCallStatusException extends BadRequestException
    - InvalidStatusTransitionException extends BadRequestException
    - _Requirements: 12.2, 12.3_
  
  - [x] 14.2 Create exception mappers


    - Map custom exceptions to appropriate HTTP status codes
    - Return structured error responses with requestId
    - Include error codes from design document
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [ ]* 14.3 Write property test for error response structure
    - **Property 29: Error Response Structure**
    - **Validates: Requirements 12.1, 12.5**
  
  - [ ]* 14.4 Write unit tests for exception mappers
    - Test all error codes and status mappings
    - Verify error response format
    - Verify requestId included
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 15. Implement rate limiting




  - [x] 15.1 Create RateLimitService


    - Use Guava RateLimiter
    - Implement tryAcquire method
    - Configure limits per operation
    - _Requirements: 10.1, 10.2, 10.3_
  
  - [x] 15.2 Create RateLimitFilter


    - Apply to token generation endpoint (10/min)
    - Apply to call initiation endpoint (5/min)
    - Apply to call history endpoint (20/min)
    - Return 429 with Retry-After header
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [ ]* 15.3 Write integration tests for rate limiting
    - Test token generation rate limit
    - Test call initiation rate limit
    - Test call history rate limit
    - Verify 429 response and Retry-After header
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 16. Final checkpoint - Ensure all tests pass




  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 17. Write integration tests for complete call flows
  - Test successful call flow (initiate → accept → connect → end)
  - Test call rejection flow
  - Test call timeout flow
  - Test token generation and refresh flow
  - Test call history and statistics flow
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.2, 2.3, 5.1, 5.2, 5.3, 5.4_

- [ ]* 18. Performance testing
  - Load test token generation endpoint (target: 100 req/s)
  - Load test call initiation endpoint (target: 50 req/s)
  - Test concurrent call handling (target: 1000 concurrent calls)
  - Test WebSocket scalability (target: 10000 concurrent connections)
