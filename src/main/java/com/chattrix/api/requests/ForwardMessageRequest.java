package com.chattrix.api.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ForwardMessageRequest {
    @NotNull(message = "Message ID is required")
    public Long messageId;

    @NotEmpty(message = "At least one conversation ID is required")
    public List<Long> conversationIds;
}

