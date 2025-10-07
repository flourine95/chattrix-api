package com.chattrix.api.websocket.dto;

import com.chattrix.api.dto.responses.UserDto;
import com.chattrix.api.entities.Message;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OutgoingMessageDto {
    private UUID id;
    private UUID conversationId;
    private UserDto sender;
    private String content;
    private Instant createdAt;

    public static OutgoingMessageDto fromEntity(Message message) {
        OutgoingMessageDto dto = new OutgoingMessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSender(UserDto.fromUser(message.getSender()));
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
