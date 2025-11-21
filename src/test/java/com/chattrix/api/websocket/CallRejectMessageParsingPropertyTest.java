package com.chattrix.api.websocket;

import com.chattrix.api.websocket.dto.CallRejectDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for call reject message parsing
 * Feature: websocket-call-integration
 * 
 * These tests verify that call reject messages can be parsed correctly
 * and all fields are extracted properly.
 */
public class CallRejectMessageParsingPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 5: Message parsing extracts all fields
     * Validates: Requirements 3.2
     * 
     * This property tests that for any valid call.reject message,
     * the WebSocket endpoint should successfully extract both callId and reason from the payload.
     */
    @Property(tries = 100)
    void messageParsingExtractsAllFields(
            @ForAll("callId") String callId,
            @ForAll("rejectionReason") String reason
    ) {
        // Create a CallRejectDto with both fields
        CallRejectDto callRejectDto = new CallRejectDto();
        callRejectDto.setCallId(callId);
        callRejectDto.setReason(reason);
        
        // Wrap it in a WebSocketMessage
        WebSocketMessage<CallRejectDto> message = new WebSocketMessage<>("call.reject", callRejectDto);
        
        // Simulate the parsing logic from processCallReject
        CallRejectDto parsedDto = objectMapper.convertValue(message.getPayload(), CallRejectDto.class);
        
        // Verify the property: both fields should be extracted successfully
        assertNotNull(parsedDto, "Parsed DTO should not be null");
        assertNotNull(parsedDto.getCallId(), "CallId should be extracted");
        assertNotNull(parsedDto.getReason(), "Reason should be extracted");
        
        assertEquals(callId, parsedDto.getCallId(), 
            "Extracted callId should match the original callId");
        assertEquals(reason, parsedDto.getReason(), 
            "Extracted reason should match the original reason");
    }

    // Arbitraries (generators) for test data

    @Provide
    Arbitrary<String> callId() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> rejectionReason() {
        return Arbitraries.of("busy", "declined", "unavailable");
    }
}
