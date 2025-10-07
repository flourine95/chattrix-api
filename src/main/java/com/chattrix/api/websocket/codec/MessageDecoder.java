package com.chattrix.api.websocket.codec;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class MessageDecoder implements Decoder.Text<WebSocketMessage> {

    private static final Jsonb jsonb = JsonbBuilder.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public WebSocketMessage decode(String s) throws DecodeException {
        System.out.println("Received from client: " + s);
        try {
            return objectMapper.readValue(s, WebSocketMessage.class);
        } catch (JsonProcessingException e) {
            throw new DecodeException(s, "Error decoding JSON", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return (s != null);
    }

    @Override
    public void init(EndpointConfig endpointConfig) {
        // Custom initialization logic
    }

    @Override
    public void destroy() {
        // Close resources
    }
}
