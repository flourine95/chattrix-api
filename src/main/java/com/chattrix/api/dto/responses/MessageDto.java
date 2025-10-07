package com.chattrix.api.dto.responses;

import com.chattrix.api.entities.Message;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class MessageDto {
    private UUID id;
    private UUID conversationId;
    private UserDto sender;
    private String content;
    private String type;
    private Instant sentAt;

    public static MessageDto fromEntity(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSender(UserDto.fromUser(message.getSender()));
        dto.setContent(message.getContent());
        dto.setType(message.getType().name());
        dto.setSentAt(message.getSentAt());
        return dto;
    }
}
