package com.chattrix.api.responses;

import com.chattrix.api.enums.CallStatus;
import com.chattrix.api.enums.CallType;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CallResponse {
    private String id;
    private String channelId;
    private Long conversationId;

    private Long callerId;
    private String callerName;
    private String callerAvatar;

    private List<CallParticipantResponse> participants;

    private CallType callType;
    private CallStatus status;
    private Instant createdAt;
    private Integer durationSeconds;
}
