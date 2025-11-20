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
public class CallAcceptedMessage {
    private String type = "call_accepted";
    private CallAcceptedData data;
    private Instant timestamp;
    
    public CallAcceptedMessage(CallAcceptedData data) {
        this.type = "call_accepted";
        this.data = data;
        this.timestamp = Instant.now();
    }
}
