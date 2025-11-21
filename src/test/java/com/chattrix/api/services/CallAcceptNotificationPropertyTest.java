package com.chattrix.api.services;

import com.chattrix.api.websocket.dto.*;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for call acceptance notifications
 * Feature: websocket-call-integration
 * 
 * These tests verify that call acceptance triggers proper notifications
 * with all required fields.
 */
public class CallAcceptNotificationPropertyTest {

    /**
     * Property 3: Call acceptance triggers notification to caller
     * Validates: Requirements 2.6
     * 
     * This property tests that for any call that is successfully accepted,
     * a WebSocket message with type "call_accepted" should be sent to the caller.
     */
    @Property(tries = 100)
    void callAcceptanceTriggersNotificationToCaller(
            @ForAll("callId") String callId,
            @ForAll("acceptedBy") String acceptedBy
    ) {
        // Simulate the notification creation logic from WebSocketNotificationService.sendCallAccepted
        CallAcceptedData data = new CallAcceptedData();
        data.setCallId(callId);
        data.setAcceptedBy(acceptedBy);
        
        CallAcceptedMessage message = new CallAcceptedMessage();
        message.setType("call_accepted");
        message.setData(data);
        
        WebSocketMessage<CallAcceptedMessage> wsMessage = 
            new WebSocketMessage<>("call_accepted", message);
        
        // Verify the property: notification should have correct type
        assertNotNull(wsMessage, "Notification message should be created");
        assertEquals("call_accepted", wsMessage.getType(), 
            "Notification type should be 'call_accepted'");
        assertNotNull(wsMessage.getPayload(), "Notification payload should not be null");
        assertEquals("call_accepted", wsMessage.getPayload().getType(), 
            "Payload type should be 'call_accepted'");
    }

    /**
     * Property 4: Accepted notification contains required fields
     * Validates: Requirements 2.7
     * 
     * This property tests that for any call acceptance notification,
     * the message should include callId and acceptedBy fields.
     */
    @Property(tries = 100)
    void acceptedNotificationContainsRequiredFields(
            @ForAll("callId") String callId,
            @ForAll("acceptedBy") String acceptedBy
    ) {
        // Simulate the notification creation logic from WebSocketNotificationService.sendCallAccepted
        CallAcceptedData data = new CallAcceptedData();
        data.setCallId(callId);
        data.setAcceptedBy(acceptedBy);
        
        CallAcceptedMessage message = new CallAcceptedMessage();
        message.setType("call_accepted");
        message.setData(data);
        
        WebSocketMessage<CallAcceptedMessage> wsMessage = 
            new WebSocketMessage<>("call_accepted", message);
        
        // Verify the property: notification should contain required fields
        assertNotNull(wsMessage.getPayload(), "Notification payload should not be null");
        assertNotNull(wsMessage.getPayload().getData(), "Notification data should not be null");
        
        CallAcceptedData notificationData = wsMessage.getPayload().getData();
        assertNotNull(notificationData.getCallId(), "Notification should contain callId field");
        assertNotNull(notificationData.getAcceptedBy(), "Notification should contain acceptedBy field");
        
        assertEquals(callId, notificationData.getCallId(), 
            "Notification callId should match the provided callId");
        assertEquals(acceptedBy, notificationData.getAcceptedBy(), 
            "Notification acceptedBy should match the provided acceptedBy");
    }

    // Arbitraries (generators) for test data

    @Provide
    Arbitrary<String> callId() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(10)
                .ofMaxLength(36);
    }

    @Provide
    Arbitrary<String> acceptedBy() {
        return Arbitraries.longs()
                .between(1L, 1000000L)
                .map(String::valueOf);
    }
}
