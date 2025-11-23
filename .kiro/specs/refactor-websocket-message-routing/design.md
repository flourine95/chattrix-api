# Design Document

## Overview

This design refactors the WebSocket message handling architecture from a monolithic switch-based approach to a clean, extensible handler-based pattern. The current `ChatServerEndpoint` contains over 600 lines of code with all message handling logic embedded in a single class. This refactor will:

1. Extract each message type into its own handler class
2. Implement a registry pattern for handler discovery and routing
3. Use CDI for automatic handler registration
4. Separate concerns by organizing handlers into logical packages
5. Make the system easily extensible without modifying existing code

## Architecture

### Current Architecture (Before)
```
ChatServerEndpoint
├── onMessage(Session, WebSocketMessage)
│   ├── switch (message.getType())
│   │   ├── case "chat.message" → processChatMessage() [100+ lines]
│   │   ├── case "typing.start" → processTypingStart() [50+ lines]
│   │   ├── case "typing.stop" → processTypingStop() [50+ lines]
│   │   ├── case "heartbeat" → processHeartbeat() [20+ lines]
│   │   ├── case "call.accept" → processCallAccept() [60+ lines]
│   │   ├── case "call.reject" → processCallReject() [60+ lines]
│   │   └── case "call.end" → processCallEnd() [60+ lines]
│   └── All business logic mixed in endpoint
```

### New Architecture (After)
```
ChatServerEndpoint
├── @Inject MessageHandlerRegistry
├── onMessage(Session, WebSocketMessage)
│   ├── registry.getHandler(messageType)
│   └── handler.handle(session, userId, payload)

MessageHandlerRegistry
├── Map<String, MessageHandler> handlers
├── @PostConstruct discoverHandlers()
└── getHandler(String type): Optional<MessageHandler>

MessageHandler (Interface)
├── handle(Session, Long, Object)
└── getMessageType(): String

Handlers (Implementations)
├── chat/
│   └── ChatMessageHandler
├── typing/
│   ├── TypingStartHandler
│   └── TypingStopHandler
├── call/
│   ├── CallAcceptHandler
│   ├── CallRejectHandler
│   └── CallEndHandler
└── system/
    └── HeartbeatHandler
```


## Components and Interfaces

### 1. MessageHandler Interface

**Purpose**: Common interface for all message handlers

```java
package com.chattrix.api.websocket.handlers;

import jakarta.websocket.Session;

/**
 * Interface for WebSocket message handlers.
 * Each message type should have its own implementation.
 */
public interface MessageHandler {
    
    /**
     * Handle the incoming WebSocket message
     * 
     * @param session The WebSocket session
     * @param userId The authenticated user ID
     * @param payload The message payload (can be Map or specific DTO)
     */
    void handle(Session session, Long userId, Object payload);
    
    /**
     * Get the message type this handler processes
     * 
     * @return The message type (e.g., "chat.message", "call.accept")
     */
    String getMessageType();
}
```

### 2. MessageHandlerRegistry

**Purpose**: Discover and route messages to appropriate handlers

```java
package com.chattrix.api.websocket.handlers;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for WebSocket message handlers.
 * Automatically discovers all MessageHandler implementations via CDI.
 */
@ApplicationScoped
public class MessageHandlerRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(MessageHandlerRegistry.class.getName());
    
    @Inject
    private Instance<MessageHandler> handlers;
    
    private Map<String, MessageHandler> handlerMap;
    
    @PostConstruct
    public void init() {
        handlerMap = new HashMap<>();
        
        // Discover all MessageHandler implementations
        for (MessageHandler handler : handlers) {
            String messageType = handler.getMessageType();
            handlerMap.put(messageType, handler);
            LOGGER.log(Level.INFO, "Registered handler for message type: {0} -> {1}", 
                new Object[]{messageType, handler.getClass().getSimpleName()});
        }
        
        LOGGER.log(Level.INFO, "Registered {0} message handlers", handlerMap.size());
    }
    
    /**
     * Get the handler for a specific message type
     * 
     * @param messageType The message type
     * @return Optional containing the handler, or empty if not found
     */
    public Optional<MessageHandler> getHandler(String messageType) {
        return Optional.ofNullable(handlerMap.get(messageType));
    }
}
```

### 3. Refactored ChatServerEndpoint

**Purpose**: Simplified endpoint that delegates to handlers

```java
package com.chattrix.api.websocket;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.chattrix.api.websocket.handlers.MessageHandler;
import com.chattrix.api.websocket.handlers.MessageHandlerRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@ServerEndpoint(value = "/ws/chat",
        configurator = CdiAwareConfigurator.class,
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class)
public class ChatServerEndpoint {
    
    private static final Logger LOGGER = Logger.getLogger(ChatServerEndpoint.class.getName());
    
    @Inject
    private TokenService tokenService;
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private ChatSessionService chatSessionService;
    
    @Inject
    private UserStatusService userStatusService;
    
    @Inject
    private MessageHandlerRegistry handlerRegistry;
    
    @OnOpen
    public void onOpen(Session session) throws IOException {
        // Authentication logic remains the same
        String token = getTokenFromQuery(session);
        if (token == null || !tokenService.validateToken(token)) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            return;
        }
        
        Long userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not found"));
            return;
        }
        
        session.getUserProperties().put("userId", user.getId());
        chatSessionService.addSession(user.getId(), session);
        userStatusService.setUserOnline(user.getId());
        
        LOGGER.log(Level.INFO, "User connected: {0}", user.getUsername());
    }
    
    @OnMessage
    @Transactional
    public void onMessage(Session session, WebSocketMessage<?> message) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) {
            LOGGER.warning("Received message from unauthenticated session");
            return;
        }
        
        // Update last seen
        userStatusService.updateLastSeen(userId);
        
        String messageType = message.getType();
        LOGGER.log(Level.FINE, "Received message type: {0} from user: {1}", 
            new Object[]{messageType, userId});
        
        // Look up handler
        handlerRegistry.getHandler(messageType).ifPresentOrElse(
            handler -> {
                try {
                    LOGGER.log(Level.FINE, "Delegating to handler: {0}", 
                        handler.getClass().getSimpleName());
                    handler.handle(session, userId, message.getPayload());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error in handler for message type: " + messageType, e);
                }
            },
            () -> LOGGER.log(Level.WARNING, "No handler found for message type: {0}", messageType)
        );
    }
    
    @OnClose
    public void onClose(Session session) {
        // Cleanup logic remains the same
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId, session);
            userStatusService.setUserOffline(userId);
            LOGGER.log(Level.INFO, "User disconnected: {0}", userId);
        }
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        Long userId = (Long) session.getUserProperties().get("userId");
        LOGGER.log(Level.SEVERE, "WebSocket error for user " + userId, throwable);
    }
    
    private String getTokenFromQuery(Session session) {
        // Token extraction logic remains the same
        Map<String, List<String>> params = session.getRequestParameterMap();
        return params.containsKey("token") ? params.get("token").get(0) : null;
    }
}
```


### 4. Example Handler Implementations

#### ChatMessageHandler

```java
package com.chattrix.api.websocket.handlers.chat;

import com.chattrix.api.websocket.handlers.MessageHandler;
import com.chattrix.api.websocket.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class ChatMessageHandler implements MessageHandler {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private MessageRepository messageRepository;
    
    @Inject
    private ChatSessionService chatSessionService;
    
    @Inject
    private WebSocketMapper webSocketMapper;
    
    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            // Convert payload to DTO
            ChatMessageDto dto = objectMapper.convertValue(payload, ChatMessageDto.class);
            
            // Process chat message (extracted from original processChatMessage method)
            User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
            
            Conversation conversation = conversationRepository
                .findByIdWithParticipants(dto.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
            
            // Validate sender is participant
            boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
            if (!isParticipant) {
                throw new IllegalArgumentException("Sender is not a participant");
            }
            
            // Create and save message
            Message newMessage = new Message();
            newMessage.setContent(dto.getContent());
            newMessage.setSender(sender);
            newMessage.setConversation(conversation);
            // ... set other fields
            
            messageRepository.save(newMessage);
            
            // Broadcast to participants
            OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(newMessage);
            WebSocketMessage<OutgoingMessageDto> wsMessage = 
                new WebSocketMessage<>("chat.message", outgoingDto);
            
            conversation.getParticipants().forEach(participant -> {
                chatSessionService.sendMessageToUser(
                    participant.getUser().getId(), 
                    wsMessage
                );
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing chat message", e);
            // Send error response to client
        }
    }
    
    @Override
    public String getMessageType() {
        return "chat.message";
    }
}
```

#### CallAcceptHandler

```java
package com.chattrix.api.websocket.handlers.call;

import com.chattrix.api.websocket.handlers.MessageHandler;
import com.chattrix.api.websocket.dto.CallAcceptDto;
import com.chattrix.api.websocket.dto.CallErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

@ApplicationScoped
public class CallAcceptHandler implements MessageHandler {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    private CallService callService;
    
    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            CallAcceptDto dto = objectMapper.convertValue(payload, CallAcceptDto.class);
            
            if (dto.getCallId() == null || dto.getCallId().trim().isEmpty()) {
                sendError(session, null, "invalid_request", "Call ID is required");
                return;
            }
            
            callService.acceptCallViaWebSocket(dto.getCallId(), String.valueOf(userId));
            
        } catch (ResourceNotFoundException e) {
            sendError(session, extractCallId(payload), "call_not_found", e.getMessage());
        } catch (UnauthorizedException e) {
            sendError(session, extractCallId(payload), "unauthorized", e.getMessage());
        } catch (BadRequestException e) {
            sendError(session, extractCallId(payload), "invalid_status", e.getMessage());
        } catch (Exception e) {
            sendError(session, extractCallId(payload), "service_error", "Unexpected error");
        }
    }
    
    @Override
    public String getMessageType() {
        return "call.accept";
    }
    
    private void sendError(Session session, String callId, String errorType, String message) {
        try {
            CallErrorDto errorDto = new CallErrorDto();
            errorDto.setCallId(callId);
            errorDto.setErrorType(errorType);
            errorDto.setMessage(message);
            
            WebSocketMessage<CallErrorDto> wsMessage = 
                new WebSocketMessage<>("call_error", errorDto);
            session.getBasicRemote().sendObject(wsMessage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send error message", e);
        }
    }
    
    private String extractCallId(Object payload) {
        try {
            if (payload instanceof Map) {
                Object callId = ((Map<?, ?>) payload).get("callId");
                return callId != null ? callId.toString() : null;
            }
            CallAcceptDto dto = objectMapper.convertValue(payload, CallAcceptDto.class);
            return dto.getCallId();
        } catch (Exception e) {
            return null;
        }
    }
}
```

#### HeartbeatHandler

```java
package com.chattrix.api.websocket.handlers.system;

import com.chattrix.api.websocket.handlers.MessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class HeartbeatHandler implements MessageHandler {
    
    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            // Send acknowledgment
            WebSocketMessage<Map<String, Object>> ackMessage = 
                new WebSocketMessage<>("heartbeat.ack", Map.of(
                    "userId", userId.toString(),
                    "timestamp", Instant.now().toString()
                ));
            
            session.getBasicRemote().sendObject(ackMessage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error sending heartbeat ack", e);
        }
    }
    
    @Override
    public String getMessageType() {
        return "heartbeat";
    }
}
```


## Data Models

No new data models are required. All existing DTOs remain unchanged:
- `WebSocketMessage<T>`
- `ChatMessageDto`
- `CallAcceptDto`, `CallRejectDto`, `CallEndDto`
- `TypingIndicatorDto`
- Error DTOs

## Package Structure

```
com.chattrix.api.websocket/
├── ChatServerEndpoint.java
├── CdiAwareConfigurator.java
├── codec/
│   ├── MessageEncoder.java
│   └── MessageDecoder.java
├── dto/
│   ├── WebSocketMessage.java
│   ├── ChatMessageDto.java
│   ├── CallAcceptDto.java
│   └── ... (other DTOs)
└── handlers/
    ├── MessageHandler.java (interface)
    ├── MessageHandlerRegistry.java
    ├── chat/
    │   └── ChatMessageHandler.java
    ├── typing/
    │   ├── TypingStartHandler.java
    │   └── TypingStopHandler.java
    ├── call/
    │   ├── CallAcceptHandler.java
    │   ├── CallRejectHandler.java
    │   └── CallEndHandler.java
    └── system/
        └── HeartbeatHandler.java
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptence Criteria Testing Prework:

1.1 WHEN implementing message handlers THEN each handler SHALL be a separate class
  Thoughts: This is about code organization, not runtime behavior
  Testable: no

1.2 WHEN creating a handler THEN the handler SHALL implement MessageHandler interface
  Thoughts: This is enforced by the compiler
  Testable: no

1.3 WHEN the interface is defined THEN it SHALL have a handle() method
  Thoughts: This is about interface design, enforced by compiler
  Testable: no

1.4 WHEN a handler is created THEN it SHALL be annotated with @ApplicationScoped
  Thoughts: This is about CDI configuration, not runtime behavior
  Testable: no

1.5 WHEN a handler processes a message THEN it SHALL handle its own exceptions
  Thoughts: This is about error handling within handlers. We can test that exceptions don't propagate
  Testable: yes - property

2.1 WHEN creating the registry THEN it SHALL be a CDI-managed service
  Thoughts: This is about CDI configuration
  Testable: no

2.2 WHEN the registry initializes THEN it SHALL discover all MessageHandler implementations
  Thoughts: This is about the registry discovering handlers. We can test that all handlers are registered
  Testable: yes - property

2.3 WHEN looking up a handler THEN the registry SHALL return the handler for the given message type
  Thoughts: For any message type that has a handler, the registry should return that handler
  Testable: yes - property

2.4 WHEN a message type has no handler THEN the registry SHALL return an empty Optional
  Thoughts: For any message type without a handler, the registry should return empty
  Testable: yes - property

2.5 WHEN handlers are registered THEN each handler SHALL declare which message type it handles
  Thoughts: This is enforced by the interface
  Testable: no

3.1 WHEN a message is received THEN the endpoint SHALL look up the appropriate handler
  Thoughts: For any message, the endpoint should query the registry
  Testable: yes - property

3.2 WHEN a handler is found THEN the endpoint SHALL delegate processing to that handler
  Thoughts: For any message with a registered handler, that handler's handle() method should be called
  Testable: yes - property

3.3 WHEN no handler is found THEN the endpoint SHALL log a warning
  Thoughts: This is about logging behavior, difficult to test
  Testable: no

3.4 WHEN delegating to a handler THEN the endpoint SHALL pass Session, userId, and payload
  Thoughts: For any handler invocation, all three parameters should be passed
  Testable: yes - property

3.5 WHEN a handler throws an exception THEN the endpoint SHALL catch it
  Thoughts: For any exception thrown by a handler, the endpoint should catch it and continue
  Testable: yes - property

4.1-4.5: Package organization
  Thoughts: These are about code organization, not runtime behavior
  Testable: no

5.1-5.5: Dependency injection
  Thoughts: These are about CDI configuration
  Testable: no

6.1-6.5: Testability
  Thoughts: These are about test design, not runtime behavior
  Testable: no

7.1-7.5: Separation of concerns
  Thoughts: These are about architecture, not specific runtime behavior
  Testable: no

8.1-8.5: Open/Closed Principle
  Thoughts: These are about extensibility, not runtime behavior
  Testable: no

9.1-9.5: Error handling consistency
  Thoughts: These are about patterns, not specific behavior
  Testable: no

10.1-10.5: Logging
  Thoughts: Logging is difficult to test in unit tests
  Testable: no

### Property Reflection

After reviewing the prework, we have identified the following testable properties:
- Property 1 (1.5): Handlers handle their own exceptions
- Property 2 (2.2): Registry discovers all handlers
- Property 3 (2.3): Registry returns correct handler for message type
- Property 4 (2.4): Registry returns empty for unknown message type
- Property 5 (3.1, 3.2): Endpoint delegates to correct handler
- Property 6 (3.4): Handler receives all required parameters
- Property 7 (3.5): Endpoint catches handler exceptions

Properties 5 and 6 can be combined - if the endpoint delegates to the correct handler, it must be passing the parameters.
Property 1 and 7 are related - both about exception handling.

We'll focus on the core properties that validate the registry and routing behavior.

### Property 1: Registry discovers all handler implementations
*For any* MessageHandler implementation in the classpath, the registry should discover and register it during initialization.
**Validates: Requirements 2.2**

### Property 2: Registry returns correct handler for registered type
*For any* message type that has a registered handler, calling getHandler() should return that handler.
**Validates: Requirements 2.3**

### Property 3: Registry returns empty for unregistered type
*For any* message type that does not have a registered handler, calling getHandler() should return Optional.empty().
**Validates: Requirements 2.4**

### Property 4: Endpoint delegates to handler when found
*For any* message with a registered handler, the endpoint should invoke that handler's handle() method with the correct parameters.
**Validates: Requirements 3.1, 3.2, 3.4**

### Property 5: Endpoint continues processing after handler exception
*For any* exception thrown by a handler, the endpoint should catch it, log it, and remain ready to process subsequent messages.
**Validates: Requirements 1.5, 3.5**


## Error Handling

### Handler-Level Error Handling

Each handler is responsible for its own error handling:

```java
@Override
public void handle(Session session, Long userId, Object payload) {
    try {
        // Process message
    } catch (SpecificException e) {
        // Handle specific error
        sendError(session, "specific_error", e.getMessage());
    } catch (Exception e) {
        // Handle generic error
        sendError(session, "service_error", "Unexpected error");
        LOGGER.log(Level.SEVERE, "Error in handler", e);
    }
}
```

### Endpoint-Level Error Handling

The endpoint catches any exceptions that escape handlers:

```java
handlerRegistry.getHandler(messageType).ifPresentOrElse(
    handler -> {
        try {
            handler.handle(session, userId, message.getPayload());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Uncaught exception in handler", e);
            // Connection remains open
        }
    },
    () -> LOGGER.log(Level.WARNING, "No handler for: {0}", messageType)
);
```

### Error Response Pattern

Common error response builder (can be a utility class):

```java
public class WebSocketErrorHelper {
    
    public static void sendError(Session session, String errorType, String message) {
        try {
            ErrorDto errorDto = new ErrorDto();
            errorDto.setErrorType(errorType);
            errorDto.setMessage(message);
            errorDto.setTimestamp(Instant.now());
            
            WebSocketMessage<ErrorDto> wsMessage = 
                new WebSocketMessage<>("error", errorDto);
            
            session.getBasicRemote().sendObject(wsMessage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send error message", e);
        }
    }
}
```

## Testing Strategy

### Unit Testing

**Handler Tests:**
```java
@ExtendWith(MockitoExtension.class)
class ChatMessageHandlerTest {
    
    @Mock
    private Session session;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @InjectMocks
    private ChatMessageHandler handler;
    
    @Test
    void shouldProcessValidChatMessage() {
        // Given
        Long userId = 1L;
        Map<String, Object> payload = Map.of(
            "conversationId", 123L,
            "content", "Hello"
        );
        
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(mockUser));
        when(conversationRepository.findByIdWithParticipants(123L))
            .thenReturn(Optional.of(mockConversation));
        
        // When
        handler.handle(session, userId, payload);
        
        // Then
        verify(messageRepository).save(any(Message.class));
        verify(chatSessionService).sendMessageToUser(anyLong(), any());
    }
    
    @Test
    void shouldHandleUserNotFound() {
        // Given
        Long userId = 1L;
        Map<String, Object> payload = Map.of("conversationId", 123L);
        
        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());
        
        // When
        handler.handle(session, userId, payload);
        
        // Then
        verify(session.getBasicRemote()).sendObject(argThat(msg -> 
            ((WebSocketMessage<?>) msg).getType().equals("error")
        ));
    }
}
```

**Registry Tests:**
```java
class MessageHandlerRegistryTest {
    
    @Test
    void shouldDiscoverAllHandlers() {
        // Given
        MessageHandlerRegistry registry = new MessageHandlerRegistry();
        Instance<MessageHandler> handlers = createMockHandlers(
            new ChatMessageHandler(),
            new CallAcceptHandler(),
            new HeartbeatHandler()
        );
        registry.handlers = handlers;
        
        // When
        registry.init();
        
        // Then
        assertEquals(3, registry.getHandlerCount());
        assertTrue(registry.getHandler("chat.message").isPresent());
        assertTrue(registry.getHandler("call.accept").isPresent());
        assertTrue(registry.getHandler("heartbeat").isPresent());
    }
    
    @Test
    void shouldReturnEmptyForUnknownType() {
        // Given
        MessageHandlerRegistry registry = createRegistryWithHandlers();
        
        // When
        Optional<MessageHandler> handler = registry.getHandler("unknown.type");
        
        // Then
        assertTrue(handler.isEmpty());
    }
}
```

**Endpoint Tests:**
```java
class ChatServerEndpointTest {
    
    @Mock
    private MessageHandlerRegistry registry;
    
    @Mock
    private MessageHandler mockHandler;
    
    @InjectMocks
    private ChatServerEndpoint endpoint;
    
    @Test
    void shouldDelegateToHandler() {
        // Given
        Session session = mock(Session.class);
        when(session.getUserProperties()).thenReturn(Map.of("userId", 1L));
        
        WebSocketMessage<?> message = new WebSocketMessage<>("chat.message", Map.of());
        when(registry.getHandler("chat.message"))
            .thenReturn(Optional.of(mockHandler));
        
        // When
        endpoint.onMessage(session, message);
        
        // Then
        verify(mockHandler).handle(eq(session), eq(1L), any());
    }
    
    @Test
    void shouldContinueAfterHandlerException() {
        // Given
        Session session = mock(Session.class);
        when(session.getUserProperties()).thenReturn(Map.of("userId", 1L));
        
        WebSocketMessage<?> message = new WebSocketMessage<>("chat.message", Map.of());
        when(registry.getHandler("chat.message"))
            .thenReturn(Optional.of(mockHandler));
        doThrow(new RuntimeException("Handler error"))
            .when(mockHandler).handle(any(), any(), any());
        
        // When
        endpoint.onMessage(session, message);
        
        // Then - no exception thrown, endpoint continues
        verify(mockHandler).handle(any(), any(), any());
    }
}
```

### Property-Based Testing

We will use **JUnit 5** with **jqwik** for property-based testing.

```java
@Property
// Feature: refactor-websocket-message-routing, Property 2: Registry returns correct handler
void registryReturnsCorrectHandler(@ForAll("messageTypes") String messageType) {
    // Given a registry with registered handlers
    MessageHandlerRegistry registry = createPopulatedRegistry();
    
    // When looking up a registered message type
    Optional<MessageHandler> handler = registry.getHandler(messageType);
    
    // Then the handler should be present and handle that type
    assertTrue(handler.isPresent());
    assertEquals(messageType, handler.get().getMessageType());
}

@Property
// Feature: refactor-websocket-message-routing, Property 3: Registry returns empty for unknown
void registryReturnsEmptyForUnknown(@ForAll String randomType) {
    // Given a registry with known handlers
    MessageHandlerRegistry registry = createPopulatedRegistry();
    Set<String> knownTypes = Set.of("chat.message", "call.accept", "heartbeat");
    
    // When looking up an unknown type
    Assume.that(!knownTypes.contains(randomType));
    Optional<MessageHandler> handler = registry.getHandler(randomType);
    
    // Then it should return empty
    assertTrue(handler.isEmpty());
}
```

### Integration Testing

Test the complete flow with real handlers:

```java
@QuarkusTest
class WebSocketIntegrationTest {
    
    @Test
    void shouldRouteMessageToCorrectHandler() {
        // Given a WebSocket connection
        WebSocketClient client = connectToWebSocket();
        
        // When sending a chat message
        client.send(new WebSocketMessage<>("chat.message", chatPayload));
        
        // Then the message should be processed and broadcast
        WebSocketMessage<?> response = client.receive();
        assertEquals("chat.message", response.getType());
    }
}
```

## Implementation Notes

### Migration Strategy

1. **Phase 1**: Create handler infrastructure
   - Create MessageHandler interface
   - Create MessageHandlerRegistry
   - Add to ChatServerEndpoint (parallel to existing code)

2. **Phase 2**: Extract handlers one by one
   - Start with simple handlers (heartbeat)
   - Move to complex handlers (chat, call)
   - Test each handler independently

3. **Phase 3**: Remove old code
   - Once all handlers are extracted and tested
   - Remove old switch statement and private methods
   - Clean up imports

### Benefits

1. **Maintainability**: Each handler is small and focused
2. **Testability**: Handlers can be tested in isolation
3. **Extensibility**: New message types = new handler class
4. **Separation of Concerns**: Business logic separated from routing
5. **Open/Closed Principle**: Add features without modifying existing code

### Performance Considerations

- **Handler Lookup**: O(1) HashMap lookup, negligible overhead
- **CDI Injection**: Handlers are singletons, no per-request overhead
- **Memory**: Minimal - one handler instance per type
- **No Breaking Changes**: Message format and behavior remain identical

### Backward Compatibility

- **100% Compatible**: No changes to message format or behavior
- **Client Unaffected**: Clients see no difference
- **Gradual Migration**: Can migrate handlers incrementally
- **Rollback Safe**: Can revert to old code if needed

## Future Enhancements

1. **Handler Interceptors**: Add AOP-style interceptors for cross-cutting concerns
2. **Handler Metrics**: Track handler execution time and error rates
3. **Handler Validation**: Validate payloads before passing to handlers
4. **Handler Composition**: Chain multiple handlers for complex flows
5. **Dynamic Handler Registration**: Register handlers at runtime
