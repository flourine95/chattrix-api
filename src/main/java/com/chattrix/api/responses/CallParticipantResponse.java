package com.chattrix.api.responses;

import com.chattrix.api.entities.ParticipantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CallParticipantResponse {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private ParticipantStatus status;
    private Instant joinedAt;
    private Instant leftAt;
}
