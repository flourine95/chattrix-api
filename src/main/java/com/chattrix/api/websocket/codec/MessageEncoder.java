package com.chattrix.api.websocket.codec;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
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
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String encode(WebSocketMessage<?> message) throws EncodeException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new EncodeException(message, "Failed to encode WebSocket message", e);
        }
    }
}