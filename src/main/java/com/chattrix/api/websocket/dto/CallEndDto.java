package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for ending a call via WebSocket
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallEndDto {
    private String callId;
    private Integer durationSeconds; // optional
}
