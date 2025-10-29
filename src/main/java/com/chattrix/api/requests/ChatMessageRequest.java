package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChatMessageRequest(
        @NotBlank(message = "Content is required")
        String content,
        String type,
        String mediaUrl,
        String thumbnailUrl,
        String fileName,
        Long fileSize,
        Integer duration,
        Double latitude,
        Double longitude,
        String locationName,
        Long replyToMessageId,
        List<Long> mentions
) {
}
