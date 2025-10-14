package com.chattrix.api.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateConversationRequest {
    private String type;
    private String name;
    private List<UUID> participantIds;
}
