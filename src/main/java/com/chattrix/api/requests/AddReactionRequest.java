package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;

public record AddReactionRequest(
        @NotBlank(message = "Emoji is required")
        String emoji
) {
}

