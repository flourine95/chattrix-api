package com.chattrix.api.websocket.handlers;

import jakarta.enterprise.inject.Instance;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageHandlerRegistry.
 * <p>
 * These tests verify that the registry correctly discovers handlers,
 * registers them by message type, and provides proper lookup functionality.
 * </p>
 */
class MessageHandlerRegistryTest {
    
    private MessageHandlerRegistry registry;
    
    // Simple mock Instance implementation for testing
    private static class MockInstance implements Instance<MessageHandler> {
        private final List<MessageHandler> handlers;
        
        MockInstance(List<MessageHandler> handlers) {
            this.handlers = handlers;
        }
        
        @Override
        public Iterator<MessageHandler> iterator() {
            return handlers.iterator();
        }
        
        @Override
        public MessageHandler get() {
            return handlers.isEmpty() ? null : handlers.get(0);
        }
        
        @Override
        public Instance<MessageHandler> select(java.lang.annotation.Annotation... qualifiers) {
            return this;
        }
        
        @Override
        public <U extends MessageHandler> Instance<U> select(Class<U> subtype, java.lang.annotation.Annotation... qualifiers) {
            return null;
        }
        
        @Override
        public <U extends MessageHandler> Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> subtype, java.lang.annotation.Annotation... qualifiers) {
            return null;
        }
        
        @Override
        public boolean isUnsatisfied() {
            return handlers.isEmpty();
        }
        
        @Override
        public boolean isAmbiguous() {
            return handlers.size() > 1;
        }
        
        @Override
        public void destroy(MessageHandler instance) {
        }
        
        @Override
        public Instance.Handle<MessageHandler> getHandle() {
            return null;
        }
        
        @Override
        public Iterable<? extends Instance.Handle<MessageHandler>> handles() {
            return null;
        }
    }
    
    // Test handler implementations
    private static class TestChatHandler implements MessageHandler {
        @Override
        public void handle(Session session, Long userId, Object payload) {
            // Test implementation
        }
        
        @Override
        public String getMessageType() {
            return "chat.message";
        }
    }
    
    private static class TestCallHandler implements MessageHandler {
        @Override
        public void handle(Session session, Long userId, Object payload) {
            // Test implementation
        }
        
        @Override
        public String getMessageType() {
            return "call.accept";
        }
    }
    
    private static class TestHeartbeatHandler implements MessageHandler {
        @Override
        public void handle(Session session, Long userId, Object payload) {
            // Test implementation
        }
        
        @Override
        public String getMessageType() {
            return "heartbeat";
        }
    }
    
    @BeforeEach
    void setUp() {
        registry = new MessageHandlerRegistry();
    }
    
    @Test
    void shouldDiscoverAndRegisterAllHandlers() {
        // Given: Multiple handler implementations
        List<MessageHandler> handlers = List.of(
            new TestChatHandler(),
            new TestCallHandler(),
            new TestHeartbeatHandler()
        );
        
        // Set up the mock Instance
        registry.handlers = new MockInstance(handlers);
        
        // When: Registry initializes
        registry.init();
        
        // Then: All handlers should be registered
        assertEquals(3, registry.getHandlerCount(), "Should register all 3 handlers");
        assertTrue(registry.hasHandler("chat.message"), "Should have chat handler");
        assertTrue(registry.hasHandler("call.accept"), "Should have call handler");
        assertTrue(registry.hasHandler("heartbeat"), "Should have heartbeat handler");
    }
    
    @Test
    void shouldReturnCorrectHandlerForRegisteredType() {
        // Given: A registry with registered handlers
        List<MessageHandler> handlers = List.of(
            new TestChatHandler(),
            new TestCallHandler()
        );
        registry.handlers = new MockInstance(handlers);
        registry.init();
        
        // When: Looking up a registered message type
        Optional<MessageHandler> handler = registry.getHandler("chat.message");
        
        // Then: Should return the correct handler
        assertTrue(handler.isPresent(), "Handler should be present");
        assertEquals("chat.message", handler.get().getMessageType(), 
            "Should return handler for chat.message");
    }
    
    @Test
    void shouldReturnEmptyForUnregisteredType() {
        // Given: A registry with some registered handlers
        List<MessageHandler> handlers = List.of(new TestChatHandler());
        registry.handlers = new MockInstance(handlers);
        registry.init();
        
        // When: Looking up an unregistered message type
        Optional<MessageHandler> handler = registry.getHandler("unknown.type");
        
        // Then: Should return empty Optional
        assertTrue(handler.isEmpty(), "Should return empty for unknown type");
    }
    
    @Test
    void shouldReturnEmptyForNullMessageType() {
        // Given: A registry with registered handlers
        List<MessageHandler> handlers = List.of(new TestChatHandler());
        registry.handlers = new MockInstance(handlers);
        registry.init();
        
        // When: Looking up with null message type
        Optional<MessageHandler> handler = registry.getHandler(null);
        
        // Then: Should return empty Optional
        assertTrue(handler.isEmpty(), "Should return empty for null type");
    }
    
    @Test
    void shouldThrowExceptionForDuplicateHandlers() {
        // Given: Two handlers with the same message type
        MessageHandler handler1 = new TestChatHandler();
        MessageHandler handler2 = new MessageHandler() {
            @Override
            public void handle(Session session, Long userId, Object payload) {}
            
            @Override
            public String getMessageType() {
                return "chat.message"; // Duplicate!
            }
        };
        
        List<MessageHandler> handlers = List.of(handler1, handler2);
        registry.handlers = new MockInstance(handlers);
        
        // When/Then: Should throw IllegalStateException
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> registry.init(),
            "Should throw exception for duplicate handlers"
        );
        
        assertTrue(exception.getMessage().contains("Duplicate handler registration"),
            "Exception message should mention duplicate registration");
    }
    
    @Test
    void shouldHandleEmptyHandlerList() {
        // Given: No handlers available
        List<MessageHandler> handlers = List.of();
        registry.handlers = new MockInstance(handlers);
        
        // When: Registry initializes
        registry.init();
        
        // Then: Should have zero handlers
        assertEquals(0, registry.getHandlerCount(), "Should have no handlers");
        assertFalse(registry.hasHandler("any.type"), "Should not have any handlers");
    }
    
    @Test
    void shouldReturnCorrectHandlerCount() {
        // Given: Multiple handlers
        List<MessageHandler> handlers = List.of(
            new TestChatHandler(),
            new TestCallHandler(),
            new TestHeartbeatHandler()
        );
        registry.handlers = new MockInstance(handlers);
        registry.init();
        
        // When/Then: Handler count should match
        assertEquals(3, registry.getHandlerCount());
    }
    
    @Test
    void shouldCorrectlyCheckHandlerExistence() {
        // Given: A registry with specific handlers
        List<MessageHandler> handlers = List.of(
            new TestChatHandler(),
            new TestCallHandler()
        );
        registry.handlers = new MockInstance(handlers);
        registry.init();
        
        // When/Then: hasHandler should work correctly
        assertTrue(registry.hasHandler("chat.message"));
        assertTrue(registry.hasHandler("call.accept"));
        assertFalse(registry.hasHandler("heartbeat"));
        assertFalse(registry.hasHandler("unknown.type"));
        assertFalse(registry.hasHandler(null));
    }
}
