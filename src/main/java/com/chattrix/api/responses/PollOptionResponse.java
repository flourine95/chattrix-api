package com.chattrix.api.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionResponse {
    private Long id;
    private String text;
    private Integer voteCount;
    private List<Long> voterIds;  // Empty if anonymous
    private Boolean hasVoted;     // Whether current user voted for this option
}
