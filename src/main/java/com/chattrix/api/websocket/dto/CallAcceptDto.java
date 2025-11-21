package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for accepting a call via WebSocket
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallAcceptDto {
    private String callId;
}
