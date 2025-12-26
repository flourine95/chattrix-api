package com.chattrix.api.websocket.dto;

import com.chattrix.api.entities.ParticipantStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallParticipantUpdateDto {
    private String callId;
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private ParticipantStatus status; // JOINED, LEFT, REJECTED, etc.
}
