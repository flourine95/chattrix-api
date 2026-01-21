package com.chattrix.api.websocket.codec;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;

public class MessageDecoder implements Decoder.Text<WebSocketMessage<?>> {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public WebSocketMessage<?> decode(String s) throws DecodeException {
        try {
            return objectMapper.readValue(s, WebSocketMessage.class);
        } catch (JsonProcessingException e) {
            // Log the actual message that failed to decode
            System.err.println("Failed to decode WebSocket message: " + s);
            System.err.println("Error: " + e.getMessage());
            throw new DecodeException(s, "Failed to decode to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return (s != null && !s.trim().isEmpty());
    }
}
