# Implementation Plan

- [ ] 1. Add call.invitation handler to ChatServerEndpoint
  - Add new case "call.invitation" in the onMessage() switch statement
  - Create processCallInvitation(Session session, Long userId, WebSocketMessage<?> message) method
  - Log the invalid operation attempt with user ID at WARNING level
  - Call sendCallError() with errorType "invalid_operation" and descriptive message
  - Ensure the method doesn't close the WebSocket connection
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 1.1 Write property test for call invitation rejection
  - **Property 1: Client-sent call invitations are rejected with error**
  - **Validates: Requirements 2.1, 2.2, 2.5**

- [ ] 2. Verify existing REST API call initiation flow
  - Review CallService.initiateCall() method
  - Confirm it creates call with INITIATING status
  - Confirm it sends WebSocket invitation via WebSocketNotificationService
  - Confirm it updates status to RINGING
  - Confirm it schedules timeout
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ]* 2.1 Write integration test for complete call flow
  - Test REST API initiate → server sends invitation → callee receives
  - Verify call record is created correctly
  - Verify WebSocket message is sent to callee
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 3. Update API documentation
  - Document that call initiation MUST use POST /api/v1/calls/initiate
  - Document that call.invitation is NOT a valid client-to-server message
  - Document the error response format for invalid_operation
  - Add examples of correct call flow
  - Add examples of invalid flow and error response
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
