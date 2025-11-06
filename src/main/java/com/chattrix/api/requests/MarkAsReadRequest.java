package com.chattrix.api.requests;

import jakarta.validation.constraints.NotNull;

public class MarkAsReadRequest {
    @NotNull(message = "Message ID is required")
    public Long messageId;
}

