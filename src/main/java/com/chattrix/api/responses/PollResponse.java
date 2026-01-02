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
    private Long messageId;
    private String question;
    private List<PollOptionResponse> options;
    private Boolean allowMultiple;
    private Boolean anonymous;
    private Instant closesAt;
    private Boolean isClosed;
    private Integer totalVotes;
    private Long createdBy;
    private String createdByUsername;
    private Instant createdAt;
}
