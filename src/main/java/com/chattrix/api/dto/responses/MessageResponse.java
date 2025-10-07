package com.chattrix.api.dto.responses;

import com.chattrix.api.entities.Message;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class MessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private String type;
    private Instant sentAt;
    private Instant createdAt;

    public static MessageResponse fromEntity(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversation().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setContent(message.getContent());
        response.setType(message.getType().name());
        response.setSentAt(message.getSentAt());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
