package com.chattrix.api.responses;

import com.chattrix.api.entities.CallStatus;
import com.chattrix.api.entities.CallType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallResponse {
    private String callId;
    private String channelId;
    private String callerId;
    private String callerName;
    private String callerAvatar;
    private String calleeId;
    private String calleeName;
    private String calleeAvatar;
    private CallType callType;
    private CallStatus status;
    private Instant createdAt;
}
