package com.chattrix.api.websocket.handlers;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for WebSocket message handlers.
 * <p>
 * This registry automatically discovers all {@link MessageHandler} implementations
 * via CDI and provides a lookup mechanism to route incoming messages to the
 * appropriate handler based on message type.
 * </p>
 * 
 * <h2>Handler Discovery</h2>
 * <p>
 * During application startup, the {@link #init()} method is called automatically
 * by CDI. This method discovers all beans that implement {@link MessageHandler}
 * and registers them in an internal map, keyed by their message type.
 * </p>
 * 
 * <h2>Handler Lookup</h2>
 * <p>
 * The {@link #getHandler(String)} method provides O(1) lookup of handlers by
 * message type. If no handler is registered for a given type, an empty
 * {@link Optional} is returned.
 * </p>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Inject
 * private MessageHandlerRegistry registry;
 * 
 * public void onMessage(Session session, WebSocketMessage<?> message) {
 *     Long userId = (Long) session.getUserProperties().get("userId");
 *     String messageType = message.getType();
 *     
 *     registry.getHandler(messageType).ifPresentOrElse(
 *         handler -> handler.handle(session, userId, message.getPayload()),
 *         () -> logger.warning("No handler for: " + messageType)
 *     );
 * }
 * }</pre>
 * 
 * <h2>Extensibility</h2>
 * <p>
 * To add support for a new message type, simply create a new class that
 * implements {@link MessageHandler}, annotate it with {@code @ApplicationScoped},
 * and it will be automatically discovered and registered. No modifications to
 * this registry or the endpoint are required.
 * </p>
 * 
 * @see MessageHandler
 * @since 1.0
 */
@ApplicationScoped
public class MessageHandlerRegistry {
    
    private static final Logger LOGGER = Logger.getLogger(MessageHandlerRegistry.class.getName());
    
    /**
     * CDI-injected instance of all MessageHandler implementations.
     * <p>
     * CDI automatically discovers all beans that implement the MessageHandler
     * interface and makes them available through this Instance object.
     * </p>
     * <p>
     * Package-private for testing purposes.
     * </p>
     */
    @Inject
    Instance<MessageHandler> handlers;
    
    /**
     * Internal map storing the registered handlers, keyed by message type.
     * <p>
     * This map is populated during the {@link #init()} phase and provides
     * fast O(1) lookup of handlers by message type.
     * </p>
     */
    private Map<String, MessageHandler> handlerMap;
    
    /**
     * Initialize the registry by discovering and registering all handlers.
     * <p>
     * This method is automatically called by CDI after the bean is constructed
     * and all dependencies are injected. It iterates through all discovered
     * {@link MessageHandler} implementations and registers them in the internal
     * map.
     * </p>
     * 
     * <h3>Registration Process</h3>
     * <ol>
     *   <li>Create an empty handler map</li>
     *   <li>Iterate through all MessageHandler beans discovered by CDI</li>
     *   <li>For each handler, call {@link MessageHandler#getMessageType()}</li>
     *   <li>Register the handler in the map with its message type as the key</li>
     *   <li>Log the registration for debugging purposes</li>
     * </ol>
     * 
     * <h3>Logging</h3>
     * <p>
     * Each handler registration is logged at INFO level with the format:
     * {@code "Registered handler for message type: {type} -> {handlerClass}"}
     * </p>
     * <p>
     * After all handlers are registered, a summary is logged:
     * {@code "Registered {count} message handlers"}
     * </p>
     * 
     * @throws IllegalStateException if two handlers attempt to register for the same message type
     */
    @PostConstruct
    public void init() {
        handlerMap = new HashMap<>();
        
        LOGGER.log(Level.INFO, "Initializing MessageHandlerRegistry...");
        
        // Discover all MessageHandler implementations via CDI
        for (MessageHandler handler : handlers) {
            String messageType = handler.getMessageType();
            
            // Check for duplicate registrations
            if (handlerMap.containsKey(messageType)) {
                MessageHandler existingHandler = handlerMap.get(messageType);
                String errorMessage = String.format(
                    "Duplicate handler registration for message type '%s': %s and %s",
                    messageType,
                    existingHandler.getClass().getSimpleName(),
                    handler.getClass().getSimpleName()
                );
                LOGGER.log(Level.SEVERE, errorMessage);
                throw new IllegalStateException(errorMessage);
            }
            
            // Register the handler
            handlerMap.put(messageType, handler);
            
            LOGGER.log(Level.INFO, "Registered handler for message type: {0} -> {1}", 
                new Object[]{messageType, handler.getClass().getSimpleName()});
        }
        
        LOGGER.log(Level.INFO, "Registered {0} message handler(s)", handlerMap.size());
    }
    
    /**
     * Get the handler for a specific message type.
     * <p>
     * This method provides fast O(1) lookup of handlers by message type.
     * If no handler is registered for the given type, an empty {@link Optional}
     * is returned.
     * </p>
     * 
     * <h3>Usage Pattern</h3>
     * <pre>{@code
     * registry.getHandler(messageType).ifPresentOrElse(
     *     handler -> {
     *         // Handler found, delegate processing
     *         handler.handle(session, userId, payload);
     *     },
     *     () -> {
     *         // No handler found, log warning
     *         logger.warning("Unknown message type: " + messageType);
     *     }
     * );
     * }</pre>
     * 
     * @param messageType The message type to look up (e.g., "chat.message", "call.accept").
     *                    Must not be null.
     * @return An {@link Optional} containing the handler if one is registered for
     *         the given type, or {@link Optional#empty()} if no handler is found.
     * @throws NullPointerException if messageType is null
     */
    public Optional<MessageHandler> getHandler(String messageType) {
        if (messageType == null) {
            LOGGER.log(Level.WARNING, "Attempted to look up handler with null message type");
            return Optional.empty();
        }
        
        return Optional.ofNullable(handlerMap.get(messageType));
    }
    
    /**
     * Get the total number of registered handlers.
     * <p>
     * This method is primarily useful for testing and debugging purposes.
     * </p>
     * 
     * @return The number of handlers currently registered in the registry
     */
    public int getHandlerCount() {
        return handlerMap.size();
    }
    
    /**
     * Check if a handler is registered for the given message type.
     * <p>
     * This is a convenience method equivalent to:
     * {@code getHandler(messageType).isPresent()}
     * </p>
     * 
     * @param messageType The message type to check
     * @return {@code true} if a handler is registered for this type, {@code false} otherwise
     */
    public boolean hasHandler(String messageType) {
        return messageType != null && handlerMap.containsKey(messageType);
    }
}
