package com.chattrix.api.requests;

import java.time.Instant;

public record UpdateScheduledMessageRequest(
        String content,
        Instant scheduledTime,
        String mediaUrl,
        String thumbnailUrl,
        String fileName
) {
}
