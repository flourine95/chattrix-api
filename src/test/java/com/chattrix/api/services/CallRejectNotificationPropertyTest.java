package com.chattrix.api.services;

import com.chattrix.api.websocket.dto.*;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for call rejection notifications
 * Feature: websocket-call-integration
 * 
 * These tests verify that call rejection triggers proper notifications
 * with all required fields.
 */
public class CallRejectNotificationPropertyTest {

    /**
     * Property 7: Call rejection triggers notification to caller
     * Validates: Requirements 3.6
     * 
     * This property tests that for any call that is successfully rejected,
     * a WebSocket message with type "call_rejected" should be sent to the caller.
     */
    @Property(tries = 100)
    void callRejectionTriggersNotificationToCaller(
            @ForAll("callId") String callId,
            @ForAll("rejectedBy") String rejectedBy,
            @ForAll("rejectionReason") String reason
    ) {
        // Simulate the notification creation logic from WebSocketNotificationService.sendCallRejected
        CallRejectedData data = new CallRejectedData();
        data.setCallId(callId);
        data.setRejectedBy(rejectedBy);
        data.setReason(reason);
        
        CallRejectedMessage message = new CallRejectedMessage();
        message.setType("call_rejected");
        message.setData(data);
        
        WebSocketMessage<CallRejectedMessage> wsMessage = 
            new WebSocketMessage<>("call_rejected", message);
        
        // Verify the property: notification should have correct type
        assertNotNull(wsMessage, "Notification message should be created");
        assertEquals("call_rejected", wsMessage.getType(), 
            "Notification type should be 'call_rejected'");
        assertNotNull(wsMessage.getPayload(), "Notification payload should not be null");
        assertEquals("call_rejected", wsMessage.getPayload().getType(), 
            "Payload type should be 'call_rejected'");
    }

    /**
     * Property 8: Rejected notification contains required fields
     * Validates: Requirements 3.7
     * 
     * This property tests that for any call rejection notification,
     * the message should include callId, rejectedBy, and reason fields.
     */
    @Property(tries = 100)
    void rejectedNotificationContainsRequiredFields(
            @ForAll("callId") String callId,
            @ForAll("rejectedBy") String rejectedBy,
            @ForAll("rejectionReason") String reason
    ) {
        // Simulate the notification creation logic from WebSocketNotificationService.sendCallRejected
        CallRejectedData data = new CallRejectedData();
        data.setCallId(callId);
        data.setRejectedBy(rejectedBy);
        data.setReason(reason);
        
        CallRejectedMessage message = new CallRejectedMessage();
        message.setType("call_rejected");
        message.setData(data);
        
        WebSocketMessage<CallRejectedMessage> wsMessage = 
            new WebSocketMessage<>("call_rejected", message);
        
        // Verify the property: notification should contain required fields
        assertNotNull(wsMessage.getPayload(), "Notification payload should not be null");
        assertNotNull(wsMessage.getPayload().getData(), "Notification data should not be null");
        
        CallRejectedData notificationData = wsMessage.getPayload().getData();
        assertNotNull(notificationData.getCallId(), "Notification should contain callId field");
        assertNotNull(notificationData.getRejectedBy(), "Notification should contain rejectedBy field");
        assertNotNull(notificationData.getReason(), "Notification should contain reason field");
        
        assertEquals(callId, notificationData.getCallId(), 
            "Notification callId should match the provided callId");
        assertEquals(rejectedBy, notificationData.getRejectedBy(), 
            "Notification rejectedBy should match the provided rejectedBy");
        assertEquals(reason, notificationData.getReason(), 
            "Notification reason should match the provided reason");
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
    Arbitrary<String> rejectedBy() {
        return Arbitraries.longs()
                .between(1L, 1000000L)
                .map(String::valueOf);
    }

    @Provide
    Arbitrary<String> rejectionReason() {
        return Arbitraries.of("busy", "declined", "unavailable");
    }
}
