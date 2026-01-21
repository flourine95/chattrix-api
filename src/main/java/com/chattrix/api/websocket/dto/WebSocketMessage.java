package com.chattrix.api.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage<T> {
    private String type;
    private T payload;
    private String timestamp; // Changed to String to accept ISO 8601 format from client

    public WebSocketMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = java.time.Instant.now().toString();
    }
}