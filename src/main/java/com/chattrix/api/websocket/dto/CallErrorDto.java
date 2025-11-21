package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for call error messages via WebSocket
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallErrorDto {
    private String callId;
    private String errorType; // "call_not_found", "unauthorized", "invalid_status"
    private String message;
}
