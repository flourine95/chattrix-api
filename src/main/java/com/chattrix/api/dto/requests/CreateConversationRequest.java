package com.chattrix.api.dto.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateConversationRequest {
    private String type; // "PRIVATE" or "GROUP"
    private String name;
    private List<UUID> participantIds; // List of user IDs to add to conversation
}
