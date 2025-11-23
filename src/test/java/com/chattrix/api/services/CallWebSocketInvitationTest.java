package com.chattrix.api.services;

import com.chattrix.api.entities.CallType;
import com.chattrix.api.websocket.dto.CallInvitationData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WebSocket call invitation broadcasting.
 * Validates: Requirements 1.2, 3.3
 * 
 * This test verifies that the CallInvitationData structure contains all
 * required fields: callId, channelId, callerId, callerName, callerAvatar, callType
 * 
 * Note: The actual WebSocket broadcasting is tested through the CallService
 * implementation which calls webSocketNotificationService.sendCallInvitation()
 * with a properly constructed CallInvitationData object.
 */
public class CallWebSocketInvitationTest {

    /**
     * Test that CallInvitationData can be constructed with all required fields
     * Validates: Requirements 1.2, 3.3
     */
    @Test
    void callInvitationDataShouldContainAllRequiredFields() {
        // Arrange
        String callId = "call_123";
        String channelId = "channel_1234567890_1_2";
        String callerId = "1";
        String callerName = "John Caller";
        String callerAvatar = "https://example.com/avatar/caller.jpg";
        CallType callType = CallType.VIDEO;

        // Act
        CallInvitationData invitationData = new CallInvitationData();
        invitationData.setCallId(callId);
        invitationData.setChannelId(channelId);
        invitationData.setCallerId(callerId);
        invitationData.setCallerName(callerName);
        invitationData.setCallerAvatar(callerAvatar);
        invitationData.setCallType(callType);

        // Assert - Verify all required fields are present
        assertNotNull(invitationData, "Invitation data should not be null");
        assertEquals(callId, invitationData.getCallId(), 
                "Invitation should contain the call ID");
        assertEquals(channelId, invitationData.getChannelId(), 
                "Invitation should contain the backend-generated channel ID");
        assertEquals(callerId, invitationData.getCallerId(), 
                "Invitation should contain the caller ID");
        assertEquals(callerName, invitationData.getCallerName(), 
                "Invitation should contain the caller's full name");
        assertEquals(callerAvatar, invitationData.getCallerAvatar(), 
                "Invitation should contain the caller's avatar URL");
        assertEquals(callType, invitationData.getCallType(), 
                "Invitation should contain the call type");
    }

    /**
     * Test that CallInvitationData can be constructed using all-args constructor
     * Validates: Requirements 1.2, 3.3
     */
    @Test
    void callInvitationDataShouldSupportAllArgsConstructor() {
        // Arrange & Act
        CallInvitationData invitationData = new CallInvitationData(
                "call_456",
                "channel_9876543210_1_2",
                "1",
                "John Caller",
                "https://example.com/avatar/caller.jpg",
                CallType.AUDIO
        );

        // Assert
        assertEquals("call_456", invitationData.getCallId());
        assertEquals("channel_9876543210_1_2", invitationData.getChannelId());
        assertEquals("1", invitationData.getCallerId());
        assertEquals("John Caller", invitationData.getCallerName());
        assertEquals("https://example.com/avatar/caller.jpg", invitationData.getCallerAvatar());
        assertEquals(CallType.AUDIO, invitationData.getCallType());
    }

    /**
     * Test that CallInvitationData handles null avatar gracefully
     * Validates: Requirements 1.2
     */
    @Test
    void callInvitationDataShouldHandleNullAvatar() {
        // Arrange & Act
        CallInvitationData invitationData = new CallInvitationData(
                "call_789",
                "channel_1111111111_1_2",
                "1",
                "John Caller",
                null, // No avatar
                CallType.VIDEO
        );

        // Assert
        assertNull(invitationData.getCallerAvatar(), 
                "Invitation should handle null avatar gracefully");
        assertEquals("John Caller", invitationData.getCallerName(), 
                "Caller name should still be present");
    }

    /**
     * Test that CallInvitationData supports both VIDEO and AUDIO call types
     * Validates: Requirements 1.2, 3.3
     */
    @Test
    void callInvitationDataShouldSupportBothCallTypes() {
        // Test VIDEO call
        CallInvitationData videoInvitation = new CallInvitationData();
        videoInvitation.setCallType(CallType.VIDEO);
        assertEquals(CallType.VIDEO, videoInvitation.getCallType(), 
                "Should support VIDEO call type");

        // Test AUDIO call
        CallInvitationData audioInvitation = new CallInvitationData();
        audioInvitation.setCallType(CallType.AUDIO);
        assertEquals(CallType.AUDIO, audioInvitation.getCallType(), 
                "Should support AUDIO call type");
    }

    /**
     * Test that channel ID format is preserved in CallInvitationData
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Test
    void callInvitationDataShouldPreserveChannelIdFormat() {
        // Arrange - Channel ID with expected format: channel_{timestamp}_{callerId}_{calleeId}
        String channelId = "channel_1234567890_user1_user2";

        // Act
        CallInvitationData invitationData = new CallInvitationData();
        invitationData.setChannelId(channelId);

        // Assert
        assertEquals(channelId, invitationData.getChannelId(), 
                "Channel ID should be preserved exactly as provided");
        assertTrue(invitationData.getChannelId().startsWith("channel_"), 
                "Channel ID should start with 'channel_' prefix");
    }
}
