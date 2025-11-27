package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    private String type;
    private T payload;
    private long timestamp;

    public WebSocketMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
}