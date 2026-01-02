package com.chattrix.api.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ForwardMessageRequest {
    @NotEmpty(message = "At least one conversation ID is required")
    public List<Long> conversationIds;
}

