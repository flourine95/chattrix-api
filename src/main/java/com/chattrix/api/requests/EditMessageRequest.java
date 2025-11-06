package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;

public record EditMessageRequest(
        @NotBlank(message = "Content is required")
        String content
) {
}

