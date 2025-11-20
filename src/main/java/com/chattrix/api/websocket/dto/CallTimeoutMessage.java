package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallTimeoutMessage {
    private String type = "call_timeout";
    private CallTimeoutData data;
    private Instant timestamp;
    
    public CallTimeoutMessage(CallTimeoutData data) {
        this.type = "call_timeout";
        this.data = data;
        this.timestamp = Instant.now();
    }
}
