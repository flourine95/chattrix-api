package com.chattrix.api.websocket.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

/**
 * Generic encoder that can encode any object to JSON.
 * This encoder supports all message types including CallInvitationMessage, CallAcceptedMessage, etc.
 */
public class GenericMessageEncoder implements Encoder.Text<Object> {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // Register JavaTimeModule for Java 8 date/time support (Instant, LocalDateTime, etc.)
        objectMapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings, not timestamps
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String encode(Object message) throws EncodeException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new EncodeException(message, "Failed to encode to JSON", e);
        }
    }
}
