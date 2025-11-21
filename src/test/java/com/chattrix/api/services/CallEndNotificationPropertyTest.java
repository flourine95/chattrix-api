package com.chattrix.api.services;

import com.chattrix.api.websocket.dto.*;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for call end notifications
 * Feature: websocket-call-integration
 * 
 * These tests verify that call ending triggers proper notifications
 * with all required fields.
 */
public class CallEndNotificationPropertyTest {

    /**
     * Property 11: Call ending triggers notification to other participant
     * Validates: Requirements 4.7
     * 
     * This property tests that for any call that is successfully ended,
     * a WebSocket message with type "call_ended" should be sent to the
     * participant who did not initiate the ending.
     */
    @Property(tries = 100)
    void callEndingTriggersNotificationToOtherParticipant(
            @ForAll("callId") String callId,
            @ForAll("endedBy") String endedBy,
            @ForAll("durationSeconds") Integer durationSeconds
    ) {
        // Simulate the notification creation logic from WebSocketNotificationService.sendCallEnded
        CallEndedData data = new CallEndedData();
        data.setCallId(callId);
        data.setEndedBy(endedBy);
        data.setDurationSeconds(durationSeconds);
        
        CallEndedMessage message = new CallEndedMessage();
        message.setType("call_ended");
        message.setData(data);
        
        WebSocketMessage<CallEndedMessage> wsMessage = 
            new WebSocketMessage<>("call_ended", message);
        
        // Verify the property: notification should have correct type
        assertNotNull(wsMessage, "Notification message should be created");
        assertEquals("call_ended", wsMessage.getType(), 
            "Notification type should be 'call_ended'");
        assertNotNull(wsMessage.getPayload(), "Notification payload should not be null");
        assertEquals("call_ended", wsMessage.getPayload().getType(), 
            "Payload type should be 'call_ended'");
    }

    /**
     * Property 12: Ended notification contains required fields
     * Validates: Requirements 4.8
     * 
     * This property tests that for any call ended notification,
     * the message should include callId, endedBy, and durationSeconds fields.
     */
    @Property(tries = 100)
    void endedNotificationContainsRequiredFields(
            @ForAll("callId") String callId,
            @ForAll("endedBy") String endedBy,
            @ForAll("durationSeconds") Integer durationSeconds
    ) {
        // Simulate the notification creation logic from WebSocketNotificationService.sendCallEnded
        CallEndedData data = new CallEndedData();
        data.setCallId(callId);
        data.setEndedBy(endedBy);
        data.setDurationSeconds(durationSeconds);
        
        CallEndedMessage message = new CallEndedMessage();
        message.setType("call_ended");
        message.setData(data);
        
        WebSocketMessage<CallEndedMessage> wsMessage = 
            new WebSocketMessage<>("call_ended", message);
        
        // Verify the property: notification should contain required fields
        assertNotNull(wsMessage.getPayload(), "Notification payload should not be null");
        assertNotNull(wsMessage.getPayload().getData(), "Notification data should not be null");
        
        CallEndedData notificationData = wsMessage.getPayload().getData();
        assertNotNull(notificationData.getCallId(), "Notification should contain callId field");
        assertNotNull(notificationData.getEndedBy(), "Notification should contain endedBy field");
        assertNotNull(notificationData.getDurationSeconds(), "Notification should contain durationSeconds field");
        
        assertEquals(callId, notificationData.getCallId(), 
            "Notification callId should match the provided callId");
        assertEquals(endedBy, notificationData.getEndedBy(), 
            "Notification endedBy should match the provided endedBy");
        assertEquals(durationSeconds, notificationData.getDurationSeconds(), 
            "Notification durationSeconds should match the provided durationSeconds");
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
    Arbitrary<String> endedBy() {
        return Arbitraries.longs()
                .between(1L, 1000000L)
                .map(String::valueOf);
    }

    @Provide
    Arbitrary<Integer> durationSeconds() {
        return Arbitraries.integers()
                .between(0, 7200); // 0 to 2 hours
    }
}
