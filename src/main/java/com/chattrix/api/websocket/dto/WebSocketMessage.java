package com.chattrix.api.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage<T> {
    private String type;
    private T payload;
    private String timestamp;

    public WebSocketMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = Instant.now().toString();
    }
}