package com.chattrix.api.websocket.dto;

import com.chattrix.api.entities.CallType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallInvitationDto {
    private String callId;
    private String channelId;
    private Long callerId;
    private String callerName;
    private String callerAvatar;
    private CallType callType;
}