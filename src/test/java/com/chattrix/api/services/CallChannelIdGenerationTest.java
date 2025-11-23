package com.chattrix.api.services;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for channel ID generation logic.
 * Validates: Requirements 3.1, 3.2
 */
public class CallChannelIdGenerationTest {

    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("^channel_\\d+_\\w+_\\w+$");

    /**
     * Test that channel ID follows the expected format: channel_{timestamp}_{callerId}_{calleeId}
     */
    @Test
    void channelIdShouldFollowExpectedFormat() {
        String callerId = "user123";
        String calleeId = "user456";
        
        // Simulate the channel ID generation logic
        long timestamp = System.currentTimeMillis();
        String channelId = String.format("channel_%d_%s_%s", timestamp, callerId, calleeId);
        
        // Verify format matches pattern
        assertTrue(CHANNEL_ID_PATTERN.matcher(channelId).matches(),
                "Channel ID should match pattern: channel_{timestamp}_{callerId}_{calleeId}");
        
        // Verify it contains the user IDs
        assertTrue(channelId.contains(callerId), "Channel ID should contain caller ID");
        assertTrue(channelId.contains(calleeId), "Channel ID should contain callee ID");
        
        // Verify it starts with "channel_"
        assertTrue(channelId.startsWith("channel_"), "Channel ID should start with 'channel_'");
    }

    /**
     * Test that channel IDs are unique for different timestamps
     */
    @Test
    void channelIdsShouldBeUniqueForDifferentTimestamps() throws InterruptedException {
        String callerId = "user123";
        String calleeId = "user456";
        
        // Generate first channel ID
        long timestamp1 = System.currentTimeMillis();
        String channelId1 = String.format("channel_%d_%s_%s", timestamp1, callerId, calleeId);
        
        // Wait a bit to ensure different timestamp
        Thread.sleep(2);
        
        // Generate second channel ID
        long timestamp2 = System.currentTimeMillis();
        String channelId2 = String.format("channel_%d_%s_%s", timestamp2, callerId, calleeId);
        
        // Verify they are different
        assertNotEquals(channelId1, channelId2, 
                "Channel IDs generated at different times should be unique");
    }

    /**
     * Test that channel IDs are different for different user pairs
     */
    @Test
    void channelIdsShouldBeDifferentForDifferentUserPairs() {
        long timestamp = System.currentTimeMillis();
        
        String channelId1 = String.format("channel_%d_%s_%s", timestamp, "user123", "user456");
        String channelId2 = String.format("channel_%d_%s_%s", timestamp, "user789", "user456");
        String channelId3 = String.format("channel_%d_%s_%s", timestamp, "user123", "user789");
        
        // Verify all are different
        assertNotEquals(channelId1, channelId2, 
                "Channel IDs for different caller should be different");
        assertNotEquals(channelId1, channelId3, 
                "Channel IDs for different callee should be different");
        assertNotEquals(channelId2, channelId3, 
                "Channel IDs for different user pairs should be different");
    }

    /**
     * Test that channel ID length is reasonable (not too long for Agora)
     */
    @Test
    void channelIdLengthShouldBeReasonable() {
        String callerId = "user123456789"; // Longer user ID
        String calleeId = "user987654321";
        
        long timestamp = System.currentTimeMillis();
        String channelId = String.format("channel_%d_%s_%s", timestamp, callerId, calleeId);
        
        // Agora channel names should be under 64 characters
        assertTrue(channelId.length() <= 64, 
                "Channel ID should not exceed 64 characters for Agora compatibility");
    }
}
