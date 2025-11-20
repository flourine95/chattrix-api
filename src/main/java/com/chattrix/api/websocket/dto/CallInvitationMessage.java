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
public class CallInvitationMessage {
    private String type = "call_invitation";
    private CallInvitationData data;
    private Instant timestamp;
    
    public CallInvitationMessage(CallInvitationData data) {
        this.type = "call_invitation";
        this.data = data;
        this.timestamp = Instant.now();
    }
}
