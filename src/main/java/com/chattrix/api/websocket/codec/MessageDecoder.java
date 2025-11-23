package com.chattrix.api.websocket.codec;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;

public class MessageDecoder implements Decoder.Text<WebSocketMessage<?>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebSocketMessage<?> decode(String s) throws DecodeException {
        try {
            return objectMapper.readValue(s, WebSocketMessage.class);
        } catch (JsonProcessingException e) {
            throw new DecodeException(s, "Failed to decode to JSON", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return (s != null);
    }
}
