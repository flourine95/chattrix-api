package com.chattrix.api.requests;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkCancelScheduledMessagesRequest(
        @NotEmpty(message = "Scheduled message IDs are required")
        List<Long> scheduledMessageIds
) {
}
