package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;
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
    private Instant expiresAt;
    private Boolean closed;
    private Boolean expired;
    private Boolean active;
    private Instant createdAt;
    private Integer totalVoters;
    private List<PollOptionResponse> options;
    private List<Long> currentUserVotedOptionIds;
}
