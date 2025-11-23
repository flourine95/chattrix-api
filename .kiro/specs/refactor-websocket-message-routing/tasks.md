# Implementation Plan

- [x] 1. Create MessageHandler interface and package structure





  - Create com.chattrix.api.websocket.handlers package
  - Create MessageHandler interface with handle() and getMessageType() methods
  - Create sub-packages: chat/, typing/, call/, system/
  - Add JavaDoc documentation to interface
  - _Requirements: 1.1, 1.2, 1.3, 4.1, 4.2, 4.3, 4.4_

- [x] 2. Create MessageHandlerRegistry





  - Create MessageHandlerRegistry class in handlers package
  - Add @ApplicationScoped annotation
  - Inject Instance<MessageHandler> for CDI discovery
  - Implement @PostConstruct init() method to discover handlers
  - Implement getHandler(String messageType) method returning Optional
  - Add logging for handler registration
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 10.1, 10.2_

- [ ]* 2.1 Write property test for handler discovery
  - **Property 1: Registry discovers all handler implementations**
  - **Validates: Requirements 2.2**

- [ ]* 2.2 Write property test for handler lookup
  - **Property 2: Registry returns correct handler for registered type**
  - **Property 3: Registry returns empty for unregistered type**
  - **Validates: Requirements 2.3, 2.4**

- [x] 3. Create HeartbeatHandler (simplest handler first)




  - Create HeartbeatHandler class in system package
  - Implement MessageHandler interface
  - Add @ApplicationScoped annotation
  - Implement handle() method to send heartbeat acknowledgment
  - Implement getMessageType() to return "heartbeat"
  - _Requirements: 1.1, 1.2, 1.4, 4.5, 5.1_

- [ ]* 3.1 Write unit test for HeartbeatHandler
  - Test that handler sends acknowledgment message
  - Test that handler handles errors gracefully
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 4. Create TypingStartHandler





  - Create TypingStartHandler class in typing package
  - Implement MessageHandler interface
  - Inject TypingIndicatorService, ConversationRepository, WebSocketMapper
  - Implement handle() method (extract from processTypingStart)
  - Implement getMessageType() to return "typing.start"
  - Add error handling
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.4, 5.2, 5.3, 5.4, 5.5_

- [x] 5. Create TypingStopHandler





  - Create TypingStopHandler class in typing package
  - Implement MessageHandler interface
  - Inject TypingIndicatorService, ConversationRepository, WebSocketMapper
  - Implement handle() method (extract from processTypingStop)
  - Implement getMessageType() to return "typing.stop"
  - Add error handling
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.4, 5.2, 5.3, 5.4, 5.5_
- [x] 6. Create CallAcceptHandler




- [ ] 6. Create CallAcceptHandler

  - Create CallAcceptHandler class in call package
  - Implement MessageHandler interface
  - Inject CallService
  - Implement handle() method (extract from processCallAccept)
  - Implement getMessageType() to return "call.accept"
  - Add error handling with sendError helper
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.3, 5.1, 5.2, 9.1, 9.2, 9.3_

- [ ]* 6.1 Write unit test for CallAcceptHandler
  - Test successful call acceptance
  - Test error cases (not found, unauthorized, invalid status)
  - Test error response format
  - _Requirements: 6.1, 6.2, 6.3, 6.4_


- [x] 7. Create CallRejectHandler



  - Create CallRejectHandler class in call package
  - Implement MessageHandler interface
  - Inject CallService
  - Implement handle() method (extract from processCallReject)
  - Implement getMessageType() to return "call.reject"
  - Add error handling with sendError helper
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.3, 5.1, 5.2, 9.1, 9.2, 9.3_

- [x] 8. Create CallEndHandler





  - Create CallEndHandler class in call package
  - Implement MessageHandler interface
  - Inject CallService
  - Implement handle() method (extract from processCallEnd)
  - Implement getMessageType() to return "call.end"
  - Add error handling with sendError helper
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.3, 5.1, 5.2, 9.1, 9.2, 9.3_
- [x] 9. Create ChatMessageHandler




- [ ] 9. Create ChatMessageHandler

  - Create ChatMessageHandler class in chat package
  - Implement MessageHandler interface
  - Inject UserRepository, ConversationRepository, MessageRepository, ChatSessionService, WebSocketMapper
  - Implement handle() method (extract from processChatMessage)
  - Implement getMessageType() to return "chat.message"
  - Add error handling
  - _Requirements: 1.1, 1.2, 1.4, 1.5, 4.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 9.1 Write unit test for ChatMessageHandler
  - Test successful message processing
  - Test validation errors (not participant, invalid conversation)
  - Test mention handling
  - Test reply handling
  - _Requirements: 6.1, 6.2, 6.3, 6.4_
-

- [x] 10. Refactor ChatServerEndpoint to use registry




  - Inject MessageHandlerRegistry into ChatServerEndpoint
  - Replace switch statement in onMessage() with registry lookup
  - Delegate to handler.handle() when handler is found
  - Log warning when no handler is found
  - Add try-catch around handler invocation
  - Keep onOpen, onClose, onError methods unchanged
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 10.3, 10.4, 10.5_

- [ ]* 10.1 Write property test for endpoint delegation
  - **Property 4: Endpoint delegates to handler when found**
  - **Validates: Requirements 3.1, 3.2, 3.4**

- [ ]* 10.2 Write property test for exception handling
  - **Property 5: Endpoint continues processing after handler exception**
  - **Validates: Requirements 1.5, 3.5**

- [x] 11. Remove old handler methods from ChatServerEndpoint





  - Remove processChatMessage() method
  - Remove processTypingStart() method
  - Remove processTypingStop() method
  - Remove processHeartbeat() method
  - Remove processCallAccept() method
  - Remove processCallReject() method
  - Remove processCallEnd() method
  - Remove sendCallError() helper method
  - Remove extractCallIdFromPayload() helper method
  - Clean up unused imports
  - _Requirements: 7.1, 7.2, 7.3_

- [x] 12. Create WebSocketErrorHelper utility (optional)




  - Create WebSocketErrorHelper class with static sendError() method
  - Use in call handlers for consistent error responses
  - Add JavaDoc documentation
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [x] 13. Verify all handlers are discovered and registered





  - Start application and check logs for handler registration
  - Verify all 7 handlers are registered (chat, typing x2, call x3, heartbeat)
  - Test each message type to ensure routing works
  - _Requirements: 2.2, 8.2, 8.3_

- [ ] 14. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 15. Write integration tests for complete flow
  - Test chat message flow end-to-end
  - Test call accept flow end-to-end
  - Test typing indicator flow end-to-end
  - Test error handling for unknown message types
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 16. Update documentation
  - Document the handler pattern in code comments
  - Add examples of creating new handlers
  - Document the registry discovery mechanism
  - Update architecture diagrams if they exist
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 17. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
