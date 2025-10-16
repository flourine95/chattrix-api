package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic wrapper for all WebSocket messages
 *
 * @param <T> The payload type
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    private String type;
    private T payload;
}

