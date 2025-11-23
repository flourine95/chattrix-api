package com.chattrix.api.websocket.handlers;

import jakarta.websocket.Session;

/**
 * Interface for WebSocket message handlers.
 * <p>
 * Each message type in the WebSocket communication protocol should have its own
 * implementation of this interface. Handlers are responsible for processing
 * incoming messages of a specific type and performing the appropriate business logic.
 * </p>
 * 
 * <h2>Handler Lifecycle</h2>
 * <p>
 * Handlers are CDI-managed beans (typically {@code @ApplicationScoped}) that are
 * automatically discovered and registered by the {@link MessageHandlerRegistry}
 * during application startup.
 * </p>
 * 
 * <h2>Implementation Guidelines</h2>
 * <ul>
 *   <li>Each handler should be annotated with {@code @ApplicationScoped}</li>
 *   <li>Handlers should inject required services via CDI {@code @Inject}</li>
 *   <li>Handlers are responsible for their own error handling</li>
 *   <li>Handlers should not throw exceptions to the endpoint</li>
 *   <li>Handlers should send error responses directly to the client when needed</li>
 * </ul>
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class ChatMessageHandler implements MessageHandler {
 *     
 *     @Inject
 *     private MessageRepository messageRepository;
 *     
 *     @Override
 *     public void handle(Session session, Long userId, Object payload) {
 *         try {
 *             // Convert payload to DTO
 *             ChatMessageDto dto = objectMapper.convertValue(payload, ChatMessageDto.class);
 *             
 *             // Process the message
 *             // ... business logic ...
 *             
 *         } catch (Exception e) {
 *             // Handle errors and send error response
 *             sendError(session, "error_type", e.getMessage());
 *         }
 *     }
 *     
 *     @Override
 *     public String getMessageType() {
 *         return "chat.message";
 *     }
 * }
 * }</pre>
 * 
 * @see MessageHandlerRegistry
 * @since 1.0
 */
public interface MessageHandler {
    
    /**
     * Handle an incoming WebSocket message.
     * <p>
     * This method is called by the {@code ChatServerEndpoint} when a message
     * of the appropriate type is received. The handler is responsible for:
     * </p>
     * <ul>
     *   <li>Converting the payload to the appropriate DTO type</li>
     *   <li>Validating the message content</li>
     *   <li>Executing the business logic</li>
     *   <li>Sending responses or broadcasting to other users</li>
     *   <li>Handling any errors that occur during processing</li>
     * </ul>
     * 
     * <p>
     * <strong>Important:</strong> This method should NOT throw exceptions.
     * All exceptions should be caught and handled within the handler,
     * typically by sending an error response to the client.
     * </p>
     * 
     * @param session The WebSocket session for the connection. Use this to send
     *                responses back to the client or access session properties.
     * @param userId  The authenticated user ID extracted from the session.
     *                This is guaranteed to be non-null as authentication is
     *                performed during the {@code onOpen} phase.
     * @param payload The message payload. This is typically a {@code Map<String, Object>}
     *                that should be converted to the appropriate DTO using an
     *                {@code ObjectMapper}. The structure depends on the message type.
     * 
     * @throws IllegalArgumentException if the payload cannot be converted to the expected type
     *                                  (should be caught and handled within the implementation)
     */
    void handle(Session session, Long userId, Object payload);
    
    /**
     * Get the message type that this handler processes.
     * <p>
     * This value is used by the {@link MessageHandlerRegistry} to map incoming
     * messages to the appropriate handler. The message type should match the
     * {@code type} field in the {@code WebSocketMessage} DTO.
     * </p>
     * 
     * <h3>Message Type Conventions</h3>
     * <ul>
     *   <li>Chat messages: {@code "chat.message"}</li>
     *   <li>Typing indicators: {@code "typing.start"}, {@code "typing.stop"}</li>
     *   <li>Call events: {@code "call.accept"}, {@code "call.reject"}, {@code "call.end"}</li>
     *   <li>System messages: {@code "heartbeat"}</li>
     * </ul>
     * 
     * @return The message type string (e.g., "chat.message", "call.accept").
     *         Must be non-null and should be unique across all handlers.
     */
    String getMessageType();
}
