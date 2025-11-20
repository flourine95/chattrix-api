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
public class CallEndedMessage {
    private String type = "call_ended";
    private CallEndedData data;
    private Instant timestamp;
    
    public CallEndedMessage(CallEndedData data) {
        this.type = "call_ended";
        this.data = data;
        this.timestamp = Instant.now();
    }
}
