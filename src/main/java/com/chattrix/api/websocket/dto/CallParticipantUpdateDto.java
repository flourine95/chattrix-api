package com.chattrix.api.websocket.dto;

import com.chattrix.api.enums.CallParticipantStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallParticipantUpdateDto {
    private String callId;
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private CallParticipantStatus status; // JOINED, LEFT, REJECTED, etc.
}
