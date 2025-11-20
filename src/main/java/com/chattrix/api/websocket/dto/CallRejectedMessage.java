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
public class CallRejectedMessage {
    private String type = "call_rejected";
    private CallRejectedData data;
    private Instant timestamp;
    
    public CallRejectedMessage(CallRejectedData data) {
        this.type = "call_rejected";
        this.data = data;
        this.timestamp = Instant.now();
    }
}
