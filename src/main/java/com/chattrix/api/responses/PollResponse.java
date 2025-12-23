package com.chattrix.api.responses;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollResponse {
    private Long id;
    private String question;
    private Long conversationId;
    private UserResponse creator;
    private Boolean allowMultipleVotes;
    private LocalDateTime expiresAt;
    private Boolean isClosed;
    private Boolean isExpired;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Integer totalVoters;
    private List<PollOptionResponse> options;
    private List<Long> currentUserVotedOptionIds;
}
