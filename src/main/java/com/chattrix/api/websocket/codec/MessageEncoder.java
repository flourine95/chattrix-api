package com.chattrix.api.websocket.codec;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<WebSocketMessage> {

    private static final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public String encode(WebSocketMessage webSocketMessage) {
        return jsonb.toJson(webSocketMessage);
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
