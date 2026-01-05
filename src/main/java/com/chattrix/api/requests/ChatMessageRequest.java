package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record ChatMessageRequest(
        @NotBlank(message = "Content is required")
        String content,
        String type,
        Map<String, Object> metadata,  // All metadata fields: mediaUrl, fileName, latitude, etc.
        Long replyToMessageId,
        List<Long> mentions
) {
}
