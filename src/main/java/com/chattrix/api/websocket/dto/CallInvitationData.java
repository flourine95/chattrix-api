package com.chattrix.api.websocket.dto;

import com.chattrix.api.entities.CallType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallInvitationData {
    private String callId;
    private String channelId;
    private String callerId;
    private String callerName;
    private String callerAvatar;
    private CallType callType;
}
