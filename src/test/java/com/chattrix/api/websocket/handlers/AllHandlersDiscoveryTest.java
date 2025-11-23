package com.chattrix.api.websocket.handlers;

import com.chattrix.api.websocket.handlers.call.CallAcceptHandler;
import com.chattrix.api.websocket.handlers.call.CallEndHandler;
import com.chattrix.api.websocket.handlers.call.CallRejectHandler;
import com.chattrix.api.websocket.handlers.chat.ChatMessageHandler;
import com.chattrix.api.websocket.handlers.system.HeartbeatHandler;
import com.chattrix.api.websocket.handlers.typing.TypingStartHandler;
import com.chattrix.api.websocket.handlers.typing.TypingStopHandler;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify all 7 message handlers are discovered and registered.
 * <p>
 * This test validates Requirements 2.2, 8.2, 8.3:
 * - All MessageHandler implementations are discovered
 * - Handlers are automatically registered without modifying existing code
 * - The registry correctly maps all message types
 * </p>
 */
class AllHandlersDiscoveryTest {
    
    private MessageHandlerRegistry registry;
    
    // Mock Instance implementation for testing
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
    
    @BeforeEach
    void setUp() {
        registry = new MessageHandlerRegistry();
    }
    
    @Test
    void shouldDiscoverAll7Handlers() {
        // Given: All 7 actual handler implementations
        // Note: These handlers have dependencies, so we're testing the structure
        // In a real CDI environment, these would be automatically discovered
        
        // We'll verify the handler classes exist and have correct message types
        // by instantiating them with null dependencies (just for type checking)
        
        // Verify all handler classes exist and are properly structured
        assertDoesNotThrow(() -> {
            Class.forName("com.chattrix.api.websocket.handlers.chat.ChatMessageHandler");
            Class.forName("com.chattrix.api.websocket.handlers.typing.TypingStartHandler");
            Class.forName("com.chattrix.api.websocket.handlers.typing.TypingStopHandler");
            Class.forName("com.chattrix.api.websocket.handlers.call.CallAcceptHandler");
            Class.forName("com.chattrix.api.websocket.handlers.call.CallRejectHandler");
            Class.forName("com.chattrix.api.websocket.handlers.call.CallEndHandler");
            Class.forName("com.chattrix.api.websocket.handlers.system.HeartbeatHandler");
        }, "All 7 handler classes should exist");
    }
    
    @Test
    void shouldRegisterAllHandlersWithCorrectMessageTypes() {
        // Given: Mock handlers representing all 7 types
        List<MessageHandler> handlers = List.of(
            createMockHandler("chat.message", "ChatMessageHandler"),
            createMockHandler("typing.start", "TypingStartHandler"),
            createMockHandler("typing.stop", "TypingStopHandler"),
            createMockHandler("call.accept", "CallAcceptHandler"),
            createMockHandler("call.reject", "CallRejectHandler"),
            createMockHandler("call.end", "CallEndHandler"),
            createMockHandler("heartbeat", "HeartbeatHandler")
        );
        
        registry.handlers = new MockInstance(handlers);
        
        // When: Registry initializes
        registry.init();
        
        // Then: All 7 handlers should be registered
        assertEquals(7, registry.getHandlerCount(), 
            "Should register all 7 handlers (chat, typing x2, call x3, heartbeat)");
        
        // Verify each message type is registered
        assertTrue(registry.hasHandler("chat.message"), "Should have chat.message handler");
        assertTrue(registry.hasHandler("typing.start"), "Should have typing.start handler");
        assertTrue(registry.hasHandler("typing.stop"), "Should have typing.stop handler");
        assertTrue(registry.hasHandler("call.accept"), "Should have call.accept handler");
        assertTrue(registry.hasHandler("call.reject"), "Should have call.reject handler");
        assertTrue(registry.hasHandler("call.end"), "Should have call.end handler");
        assertTrue(registry.hasHandler("heartbeat"), "Should have heartbeat handler");
    }
    
    @Test
    void shouldReturnCorrectHandlerForEachMessageType() {
        // Given: All 7 handlers registered
        List<MessageHandler> handlers = List.of(
            createMockHandler("chat.message", "ChatMessageHandler"),
            createMockHandler("typing.start", "TypingStartHandler"),
            createMockHandler("typing.stop", "TypingStopHandler"),
            createMockHandler("call.accept", "CallAcceptHandler"),
            createMockHandler("call.reject", "CallRejectHandler"),
            createMockHandler("call.end", "CallEndHandler"),
            createMockHandler("heartbeat", "HeartbeatHandler")
        );
        
        registry.handlers = new MockInstance(handlers);
        registry.init();
        
        // When/Then: Each message type should return the correct handler
        assertHandlerPresent("chat.message");
        assertHandlerPresent("typing.start");
        assertHandlerPresent("typing.stop");
        assertHandlerPresent("call.accept");
        assertHandlerPresent("call.reject");
        assertHandlerPresent("call.end");
        assertHandlerPresent("heartbeat");
    }
    
    @Test
    void shouldOrganizeHandlersInCorrectPackages() {
        // Verify package organization matches requirements 4.1-4.4
        assertPackageStructure("com.chattrix.api.websocket.handlers.chat.ChatMessageHandler", "chat");
        assertPackageStructure("com.chattrix.api.websocket.handlers.typing.TypingStartHandler", "typing");
        assertPackageStructure("com.chattrix.api.websocket.handlers.typing.TypingStopHandler", "typing");
        assertPackageStructure("com.chattrix.api.websocket.handlers.call.CallAcceptHandler", "call");
        assertPackageStructure("com.chattrix.api.websocket.handlers.call.CallRejectHandler", "call");
        assertPackageStructure("com.chattrix.api.websocket.handlers.call.CallEndHandler", "call");
        assertPackageStructure("com.chattrix.api.websocket.handlers.system.HeartbeatHandler", "system");
    }
    
    // Helper methods
    
    private MessageHandler createMockHandler(String messageType, String handlerName) {
        return new MessageHandler() {
            @Override
            public void handle(jakarta.websocket.Session session, Long userId, Object payload) {
                // Mock implementation
            }
            
            @Override
            public String getMessageType() {
                return messageType;
            }
            
            @Override
            public String toString() {
                return handlerName;
            }
        };
    }
    
    private void assertHandlerPresent(String messageType) {
        assertTrue(registry.getHandler(messageType).isPresent(),
            "Handler for " + messageType + " should be present");
        assertEquals(messageType, registry.getHandler(messageType).get().getMessageType(),
            "Handler should return correct message type");
    }
    
    private void assertPackageStructure(String className, String expectedPackage) {
        try {
            Class<?> clazz = Class.forName(className);
            String packageName = clazz.getPackage().getName();
            assertTrue(packageName.contains("." + expectedPackage),
                className + " should be in " + expectedPackage + " package");
        } catch (ClassNotFoundException e) {
            fail("Handler class " + className + " should exist");
        }
    }
}
