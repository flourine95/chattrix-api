package com.chattrix.api.websocket.codec;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

public class MessageEncoder implements Encoder.Text<WebSocketMessage<?>> {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // Register JavaTimeModule for Java 8 date/time support (Instant, LocalDateTime, etc.)
        objectMapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings, not timestamps
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String encode(WebSocketMessage<?> message) throws EncodeException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new EncodeException(message, "Failed to encode to JSON", e);
        }
    }
}
