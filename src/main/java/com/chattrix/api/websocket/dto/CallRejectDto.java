package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallRejectDto {
    private String callId;
    private String reason; // "busy", "declined", "unavailable"
}
