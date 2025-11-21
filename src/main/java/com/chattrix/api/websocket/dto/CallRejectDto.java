package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for rejecting a call via WebSocket
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallRejectDto {
    private String callId;
    private String reason; // "busy", "declined", "unavailable"
}
