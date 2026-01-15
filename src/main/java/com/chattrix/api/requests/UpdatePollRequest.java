package com.chattrix.api.requests;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePollRequest {
    
    @Size(min = 1, max = 500, message = "Question must be between 1 and 500 characters")
    private String question;

    @Size(min = 2, max = 10, message = "Poll must have between 2 and 10 options")
    private List<@Size(min = 1, max = 200) String> options;

    private Boolean allowMultipleVotes;

    private Instant expiresAt;

    private Boolean closed;  // Manually close poll
}
