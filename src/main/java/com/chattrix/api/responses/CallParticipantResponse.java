package com.chattrix.api.responses;

import com.chattrix.api.enums.CallParticipantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CallParticipantResponse {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private CallParticipantStatus status;
    private Instant joinedAt;
    private Instant leftAt;
}
