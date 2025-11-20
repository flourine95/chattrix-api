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
public class CallQualityWarningMessage {
    private String type = "call_quality_warning";
    private CallQualityWarningData data;
    private Instant timestamp;
    
    public CallQualityWarningMessage(CallQualityWarningData data) {
        this.type = "call_quality_warning";
        this.data = data;
        this.timestamp = Instant.now();
    }
}
