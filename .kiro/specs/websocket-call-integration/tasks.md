# Implementation Plan

- [x] 1. Create WebSocket call message DTOs
  - Create CallAcceptDto, CallRejectDto, CallEndDto, and CallErrorDto classes in websocket/dto package
  - Add Lombok annotations (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor)
  - Add validation annotations where appropriate
  - _Requirements: 7.1, 7.2, 7.3, 8.3_

- [x] 1.1 Write property test for DTO serialization
  - **Property 15: All call messages use wrapper format**
  - **Validates: Requirements 7.1**

- [x] 1.2 Write property test for wrapper structure
  - **Property 16: Wrapper contains type field**
  - **Property 17: Wrapper contains payload field**
  - **Validates: Requirements 7.2, 7.3**

- [x] 2. Add CallService methods for WebSocket operations
  - Create acceptCallViaWebSocket(String callId, String userId) method
  - Create rejectCallViaWebSocket(String callId, String userId, String reason) method
  - Create endCallViaWebSocket(String callId, String userId, Integer durationSeconds) method
  - Reuse existing validation and business logic from REST methods
  - _Requirements: 2.3, 2.4, 2.5, 3.3, 3.4, 3.5, 4.3, 4.4, 4.5, 4.6_

- [x] 2.1 Write property test for callee authorization on accept
  - **Property 2: Only the callee can accept a call**
  - **Validates: Requirements 2.4**

- [x] 2.2 Write property test for callee authorization on reject
  - **Property 6: Only the callee can reject a call**
  - **Validates: Requirements 3.4**

- [x] 2.3 Write property test for participant authorization on end
  - **Property 9: Only participants can end a call**
  - **Validates: Requirements 4.4**

- [x] 2.4 Write property test for duration calculation
  - **Property 10: Duration is calculated when not provided**
  - **Validates: Requirements 4.6**

- [x] 3. Inject CallService into ChatServerEndpoint
  - Add @Inject CallService callService field to ChatServerEndpoint
  - Verify CDI injection works correctly
  - _Requirements: 10.2_

- [x] 4. Implement processCallAccept handler in ChatServerEndpoint
  - Add processCallAccept(Session session, Long userId, WebSocketMessage<?> message) method
  - Parse CallAcceptDto from message payload using ObjectMapper
  - Extract callId from DTO
  - Invoke callService.acceptCallViaWebSocket()
  - Handle exceptions and send error messages
  - Log the operation
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 8.2, 8.4, 10.3, 10.4_

- [x] 4.1 Write property test for accept notification
  - **Property 3: Call acceptance triggers notification to caller**
  - **Validates: Requirements 2.6**

- [x] 4.2 Write property test for accept notification fields
  - **Property 4: Accepted notification contains required fields**
  - **Validates: Requirements 2.7**

- [x] 5. Implement processCallReject handler in ChatServerEndpoint
  - Add processCallReject(Session session, Long userId, WebSocketMessage<?> message) method
  - Parse CallRejectDto from message payload using ObjectMapper
  - Extract callId and reason from DTO
  - Invoke callService.rejectCallViaWebSocket()
  - Handle exceptions and send error messages
  - Log the operation
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 8.2, 8.4, 10.3, 10.4_

- [x] 5.1 Write property test for message parsing
  - **Property 5: Message parsing extracts all fields**
  - **Validates: Requirements 3.2**

- [x] 5.2 Write property test for reject notification
  - **Property 7: Call rejection triggers notification to caller**
  - **Validates: Requirements 3.6**

- [x] 5.3 Write property test for reject notification fields
  - **Property 8: Rejected notification contains required fields**
  - **Validates: Requirements 3.7**

- [x] 6. Implement processCallEnd handler in ChatServerEndpoint
  - Add processCallEnd(Session session, Long userId, WebSocketMessage<?> message) method
  - Parse CallEndDto from message payload using ObjectMapper
  - Extract callId and durationSeconds from DTO
  - Invoke callService.endCallViaWebSocket()
  - Handle exceptions and send error messages
  - Log the operation
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 8.2, 8.4, 10.3, 10.4_

- [x] 6.1 Write property test for end notification
  - **Property 11: Call ending triggers notification to other participant**
  - **Validates: Requirements 4.7**

- [x] 6.2 Write property test for end notification fields
  - **Property 12: Ended notification contains required fields**
  - **Validates: Requirements 4.8**

- [x] 7. Add call message routing to onMessage method
  - Add switch cases for "call.accept", "call.reject", "call.end" in onMessage()
  - Route to appropriate handler methods
  - Ensure last_seen is updated for all call messages
  - _Requirements: 10.1, 10.3, 10.4_

- [x] 7.1 Write property test for last seen update
  - **Property 20: Last seen is updated on call messages**
  - **Validates: Requirements 10.4**

- [x] 8. Implement sendCallError helper method
  - Add sendCallError(Session session, String callId, String errorType, String message) method
  - Create CallErrorDto with provided parameters
  - Wrap in WebSocketMessage with type "call_error"
  - Send to client session
  - Log the error
  - _Requirements: 8.2, 8.3, 8.4, 8.5_

- [x] 8.1 Write property test for error message structure
  - **Property 19: Error messages contain required fields**
  - **Validates: Requirements 8.3**

- [x] 9. Add exception handling to all call handlers
  - Catch ResourceNotFoundException and send "call_not_found" error
  - Catch UnauthorizedException and send "unauthorized" error
  - Catch BadRequestException and send "invalid_status" error
  - Catch generic Exception and send "service_error" error
  - Ensure WebSocket connection stays alive after errors
  - _Requirements: 8.1, 8.2, 8.4, 8.5_

- [x] 10. Verify call invitation messages contain all required fields
  - Review CallInvitationData class
  - Ensure callId, channelId, callerId, callerName, callerAvatar, callType are all set
  - Update CallService.initiateCall() if any fields are missing
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [ ] 10.1 Write property test for invitation message fields
  - **Property 1: Call invitation messages contain all required fields**
  - **Validates: Requirements 1.3**

- [x] 11. Verify timeout notification implementation
  - Review CallTimeoutScheduler to ensure it sends "call_timeout" messages
  - Verify CallTimeoutData includes callId field
  - Ensure timeout updates call status to MISSED
  - Ensure call history entries are created on timeout
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 11.1 Write property test for timeout notification fields
  - **Property 13: Timeout notification contains callId**
  - **Validates: Requirements 5.2**

- [x] 12. Verify quality warning implementation





  - Review CallQualityService to ensure it sends "call_quality_warning" messages
  - Verify CallQualityWarningData includes callId and quality fields
  - Ensure quality field is validated as enum (POOR, BAD, VERY_BAD)
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 12.1 Write property test for quality level validation


  - **Property 14: Quality level is valid enum value**
  - **Validates: Requirements 6.3**

- [x] 13. Update MessageEncoder to handle new call DTOs
  - Verify MessageEncoder can serialize CallAcceptDto, CallRejectDto, CallEndDto, CallErrorDto
  - Test encoding with sample data
  - _Requirements: 7.1, 7.5_

- [x] 14. Update MessageDecoder to handle incoming call messages
  - Verify MessageDecoder can deserialize incoming call messages
  - Test decoding with sample JSON
  - Handle parse errors gracefully
  - _Requirements: 7.4, 7.5, 8.1_

- [ ] 14.1 Write property test for message parsing
  - **Property 18: Incoming messages are parsed correctly**
  - **Validates: Requirements 7.4**

- [x] 15. Add logging for all call WebSocket operations
  - Log when call.accept message is received
  - Log when call.reject message is received
  - Log when call.end message is received
  - Log when errors occur
  - Log when notifications are sent
  - _Requirements: 8.1_

- [x] 16. Checkpoint - Ensure all tests pass





  - Ensure all tests pass, ask the user if questions arise.

- [-] 17. Test WebSocket call flow end-to-end


  - Test initiate call via REST → receive invitation via WebSocket
  - Test accept call via WebSocket → caller receives acceptance
  - Test reject call via WebSocket → caller receives rejection
  - Test end call via WebSocket → other participant receives end notification
  - Test error scenarios (invalid call ID, unauthorized user, invalid status)
  - _Requirements: All_

- [ ] 17.1 Write integration tests for complete call flows
  - Test full accept flow with two WebSocket clients
  - Test full reject flow with two WebSocket clients
  - Test full end flow with two WebSocket clients
  - Test error handling with invalid data

- [x] 18. Update API documentation




  - Document new WebSocket message types for call operations
  - Document message payload structures
  - Document error types and error handling
  - Add examples of call WebSocket messages
  - Note that REST API endpoints remain available for backward compatibility
  - _Requirements: All_

- [ ] 19. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
