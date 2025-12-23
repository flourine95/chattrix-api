package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleMessageRequest(
        @NotBlank(message = "Content is required")
        String content,

        String type, // TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT

        @NotNull(message = "Scheduled time is required")
        Instant scheduledTime,

        String mediaUrl,
        String thumbnailUrl,
        String fileName,
        Long fileSize,
        Integer duration,
        Long replyToMessageId
) {
}
