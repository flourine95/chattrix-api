package com.chattrix.api.responses;

import com.chattrix.api.entities.CallStatus;
import com.chattrix.api.entities.CallType;
import lombok.Data;

import java.time.Instant;

@Data
public class CallResponse {
    private String id;
    private String channelId;

    private Long callerId;
    private String callerName;
    private String callerAvatar;

    private Long calleeId;
    private String calleeName;
    private String calleeAvatar;

    private CallType callType;
    private CallStatus status;
    private Instant createdAt;
    private Integer durationSeconds;
}