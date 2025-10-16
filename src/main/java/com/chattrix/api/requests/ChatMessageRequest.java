package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
        @NotBlank(message = "Content is required")
        String content
) {
}
