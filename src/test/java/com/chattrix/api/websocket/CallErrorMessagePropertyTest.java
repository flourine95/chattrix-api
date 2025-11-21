package com.chattrix.api.websocket;

import com.chattrix.api.websocket.dto.CallErrorDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for call error messages
 * Feature: websocket-call-integration
 */
public class CallErrorMessagePropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 19: Error messages contain required fields
     * Validates: Requirements 8.3
     */
    @Property(tries = 100)
    void errorMessagesContainRequiredFields(
            @ForAll("callErrorDto") CallErrorDto errorDto
    ) {
        // Verify the DTO itself contains required fields
        assertNotNull(errorDto.getErrorType(), "Error type should not be null");
        assertNotNull(errorDto.getMessage(), "Error message should not be null");
        assertFalse(errorDto.getErrorType().trim().isEmpty(), "Error type should not be empty");
        assertFalse(errorDto.getMessage().trim().isEmpty(), "Error message should not be empty");
        
        // Verify when wrapped in WebSocketMessage and serialized
        WebSocketMessage<CallErrorDto> wsMessage = new WebSocketMessage<>("call_error", errorDto);
        
        try {
            String json = objectMapper.writeValueAsString(wsMessage);
            
            // Verify JSON contains required fields
            assertTrue(json.contains("\"errorType\""), "Serialized message should contain errorType field");
            assertTrue(json.contains("\"message\""), "Serialized message should contain message field");
            
            // Verify we can deserialize and get the same values back
            @SuppressWarnings("unchecked")
            WebSocketMessage<Object> deserialized = objectMapper.readValue(json, WebSocketMessage.class);
            assertNotNull(deserialized.getPayload(), "Deserialized payload should not be null");
            
            // Convert payload to CallErrorDto
            CallErrorDto deserializedError = objectMapper.convertValue(deserialized.getPayload(), CallErrorDto.class);
            assertNotNull(deserializedError.getErrorType(), "Deserialized error type should not be null");
            assertNotNull(deserializedError.getMessage(), "Deserialized message should not be null");
            assertEquals(errorDto.getErrorType(), deserializedError.getErrorType(), "Error type should match after round-trip");
            assertEquals(errorDto.getMessage(), deserializedError.getMessage(), "Message should match after round-trip");
            
        } catch (Exception e) {
            fail("Should be able to serialize and deserialize error message: " + e.getMessage());
        }
    }

    // Arbitraries (generators) for test data

    @Provide
    Arbitrary<CallErrorDto> callErrorDto() {
        Arbitrary<String> callIds = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(5)
                .ofMaxLength(50)
                .injectNull(0.1); // 10% chance of null callId (some errors may not have callId)
        
        Arbitrary<String> errorTypes = Arbitraries.of(
                "call_not_found", 
                "unauthorized", 
                "invalid_status",
                "service_error",
                "parse_error",
                "call_timeout",
                "invalid_request"
        );
        
        // Ensure messages are not empty by using alphanumeric with minimum length
        Arbitrary<String> messages = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '.', ',', '!', '?', '-', '_')
                .ofMinLength(10)
                .ofMaxLength(100)
                .filter(s -> !s.trim().isEmpty()); // Ensure non-empty after trimming
        
        return Combinators.combine(callIds, errorTypes, messages)
                .as(CallErrorDto::new);
    }
}
