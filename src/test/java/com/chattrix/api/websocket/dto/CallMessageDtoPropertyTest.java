package com.chattrix.api.websocket.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for WebSocket call message DTOs
 * Feature: websocket-call-integration
 */
public class CallMessageDtoPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 16: Wrapper contains type field
     * Validates: Requirements 7.2
     */
    @Property(tries = 100)
    void wrapperContainsTypeField(
            @ForAll("messageType") String type,
            @ForAll("callAcceptDto") CallAcceptDto payload
    ) {
        WebSocketMessage<CallAcceptDto> message = new WebSocketMessage<>(type, payload);
        
        assertNotNull(message.getType(), "Type field should not be null");
        assertEquals(type, message.getType(), "Type field should match the provided type");
    }

    /**
     * Property 17: Wrapper contains payload field
     * Validates: Requirements 7.3
     */
    @Property(tries = 100)
    void wrapperContainsPayloadField(
            @ForAll("messageType") String type,
            @ForAll("callAcceptDto") CallAcceptDto payload
    ) {
        WebSocketMessage<CallAcceptDto> message = new WebSocketMessage<>(type, payload);
        
        assertNotNull(message.getPayload(), "Payload field should not be null");
        assertEquals(payload, message.getPayload(), "Payload field should match the provided payload");
    }

    /**
     * Property 15: All call messages use wrapper format
     * Validates: Requirements 7.1
     */
    @Property(tries = 100)
    void allCallMessagesUseWrapperFormat(
            @ForAll("callAcceptDto") CallAcceptDto acceptDto,
            @ForAll("callRejectDto") CallRejectDto rejectDto,
            @ForAll("callEndDto") CallEndDto endDto,
            @ForAll("callErrorDto") CallErrorDto errorDto
    ) throws Exception {
        // Test CallAcceptDto wrapped in WebSocketMessage
        WebSocketMessage<CallAcceptDto> acceptMessage = new WebSocketMessage<>("call.accept", acceptDto);
        String acceptJson = objectMapper.writeValueAsString(acceptMessage);
        assertNotNull(acceptJson);
        assertTrue(acceptJson.contains("\"type\""));
        assertTrue(acceptJson.contains("\"payload\""));

        // Test CallRejectDto wrapped in WebSocketMessage
        WebSocketMessage<CallRejectDto> rejectMessage = new WebSocketMessage<>("call.reject", rejectDto);
        String rejectJson = objectMapper.writeValueAsString(rejectMessage);
        assertNotNull(rejectJson);
        assertTrue(rejectJson.contains("\"type\""));
        assertTrue(rejectJson.contains("\"payload\""));

        // Test CallEndDto wrapped in WebSocketMessage
        WebSocketMessage<CallEndDto> endMessage = new WebSocketMessage<>("call.end", endDto);
        String endJson = objectMapper.writeValueAsString(endMessage);
        assertNotNull(endJson);
        assertTrue(endJson.contains("\"type\""));
        assertTrue(endJson.contains("\"payload\""));

        // Test CallErrorDto wrapped in WebSocketMessage
        WebSocketMessage<CallErrorDto> errorMessage = new WebSocketMessage<>("call_error", errorDto);
        String errorJson = objectMapper.writeValueAsString(errorMessage);
        assertNotNull(errorJson);
        assertTrue(errorJson.contains("\"type\""));
        assertTrue(errorJson.contains("\"payload\""));
    }

    // Arbitraries (generators) for test data

    @Provide
    Arbitrary<String> messageType() {
        return Arbitraries.of(
                "call.accept",
                "call.reject",
                "call.end",
                "call_invitation",
                "call_accepted",
                "call_rejected",
                "call_ended",
                "call_timeout",
                "call_error"
        );
    }

    @Provide
    Arbitrary<CallAcceptDto> callAcceptDto() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50)
                .map(callId -> new CallAcceptDto(callId));
    }

    @Provide
    Arbitrary<CallRejectDto> callRejectDto() {
        Arbitrary<String> callIds = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50);
        
        Arbitrary<String> reasons = Arbitraries.of("busy", "declined", "unavailable");
        
        return Combinators.combine(callIds, reasons)
                .as(CallRejectDto::new);
    }

    @Provide
    Arbitrary<CallEndDto> callEndDto() {
        Arbitrary<String> callIds = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50);
        
        Arbitrary<Integer> durations = Arbitraries.integers()
                .between(0, 3600)
                .injectNull(0.2); // 20% chance of null duration
        
        return Combinators.combine(callIds, durations)
                .as(CallEndDto::new);
    }

    @Provide
    Arbitrary<CallErrorDto> callErrorDto() {
        Arbitrary<String> callIds = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50);
        
        Arbitrary<String> errorTypes = Arbitraries.of(
                "call_not_found", 
                "unauthorized", 
                "invalid_status",
                "service_error"
        );
        
        Arbitrary<String> messages = Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars(' ', '.', ',')
                .ofMinLength(10)
                .ofMaxLength(100);
        
        return Combinators.combine(callIds, errorTypes, messages)
                .as(CallErrorDto::new);
    }
}
