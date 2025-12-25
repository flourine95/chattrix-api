package com.chattrix.api.requests;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePollRequest {
    @NotBlank(message = "Question is required")
    @Size(min = 1, max = 500, message = "Question must be between 1 and 500 characters")
    private String question;

    @NotNull(message = "Options are required")
    @Size(min = 2, max = 10, message = "Poll must have between 2 and 10 options")
    private List<@NotBlank @Size(min = 1, max = 200) String> options;

    @NotNull(message = "allowMultipleVotes is required")
    private Boolean allowMultipleVotes;

    private Instant expiresAt;
}
