package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserNoteRequest(
        @NotBlank(message = "Note text is required")
        @Size(max = 60, message = "Note text must not exceed 60 characters")
        String noteText,

        String musicUrl,

        @Size(max = 100, message = "Music title must not exceed 100 characters")
        String musicTitle,

        @Size(max = 10, message = "Emoji must not exceed 10 characters")
        String emoji
) {
}

