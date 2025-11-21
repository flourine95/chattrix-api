package com.chattrix.api.websocket;

import com.chattrix.api.entities.NetworkQuality;
import com.chattrix.api.websocket.dto.CallQualityWarningData;
import com.chattrix.api.websocket.dto.CallQualityWarningMessage;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import net.jqwik.api.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for call quality warning messages
 * Feature: websocket-call-integration
 */
public class CallQualityWarningPropertyTest {

    private static final List<String> VALID_QUALITY_LEVELS = Arrays.asList("POOR", "BAD", "VERY_BAD");

    /**
     * Property 14: Quality level is valid enum value
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void qualityLevelIsValidEnumValue(
            @ForAll("callQualityWarningData") CallQualityWarningData warningData
    ) {
        // Verify the quality field is one of the valid enum values
        assertNotNull(warningData.getQuality(), "Quality should not be null");
        assertTrue(
            VALID_QUALITY_LEVELS.contains(warningData.getQuality()),
            "Quality must be one of POOR, BAD, or VERY_BAD, but was: " + warningData.getQuality()
        );
        
        // Verify the quality can be parsed as a NetworkQuality enum
        assertDoesNotThrow(
            () -> NetworkQuality.valueOf(warningData.getQuality()),
            "Quality should be a valid NetworkQuality enum value"
        );
        
        NetworkQuality quality = NetworkQuality.valueOf(warningData.getQuality());
        assertTrue(
            quality == NetworkQuality.POOR || quality == NetworkQuality.BAD || quality == NetworkQuality.VERY_BAD,
            "Quality should be POOR, BAD, or VERY_BAD"
        );
    }

    /**
     * Property: Quality warning messages contain required fields and valid quality
     * Validates: Requirements 6.2, 6.3
     */
    @Property(tries = 100)
    void qualityWarningMessagesContainRequiredFields(
            @ForAll("callQualityWarningData") CallQualityWarningData warningData
    ) {
        // Verify the data contains required fields
        assertNotNull(warningData.getCallId(), "Call ID should not be null");
        assertNotNull(warningData.getQuality(), "Quality should not be null");
        assertFalse(warningData.getCallId().trim().isEmpty(), "Call ID should not be empty");
        
        // Verify quality is valid
        assertTrue(
            VALID_QUALITY_LEVELS.contains(warningData.getQuality()),
            "Quality must be one of POOR, BAD, or VERY_BAD"
        );
        
        // Create the message wrapper
        CallQualityWarningMessage message = new CallQualityWarningMessage(warningData);
        assertEquals("call_quality_warning", message.getType(), "Message type should be call_quality_warning");
        assertNotNull(message.getTimestamp(), "Timestamp should not be null");
        assertNotNull(message.getData(), "Message data should not be null");
        
        // Verify data fields are preserved
        assertEquals(warningData.getCallId(), message.getData().getCallId(), 
            "Message should preserve callId");
        assertEquals(warningData.getQuality(), message.getData().getQuality(), 
            "Message should preserve quality");
        
        // Wrap in WebSocketMessage and verify structure
        WebSocketMessage<CallQualityWarningMessage> wsMessage = 
            new WebSocketMessage<>("call_quality_warning", message);
        
        assertNotNull(wsMessage, "WebSocket message should be created");
        assertEquals("call_quality_warning", wsMessage.getType(), 
            "WebSocket message type should be call_quality_warning");
        assertNotNull(wsMessage.getPayload(), "WebSocket message payload should not be null");
        assertNotNull(wsMessage.getPayload().getData(), 
            "WebSocket message payload data should not be null");
        assertEquals(warningData.getCallId(), wsMessage.getPayload().getData().getCallId(),
            "WebSocket message should contain correct callId");
        assertEquals(warningData.getQuality(), wsMessage.getPayload().getData().getQuality(),
            "WebSocket message should contain correct quality");
    }

    // Arbitraries (generators) for test data

    @Provide
    Arbitrary<CallQualityWarningData> callQualityWarningData() {
        Arbitrary<String> callIds = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50);
        
        // Only generate valid quality levels: POOR, BAD, VERY_BAD
        Arbitrary<String> qualityLevels = Arbitraries.of("POOR", "BAD", "VERY_BAD");
        
        return Combinators.combine(callIds, qualityLevels)
                .as(CallQualityWarningData::new);
    }
}
