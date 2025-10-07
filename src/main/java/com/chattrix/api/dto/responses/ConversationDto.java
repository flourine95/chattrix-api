package com.chattrix.api.dto.responses;

import com.chattrix.api.entities.Conversation;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ConversationDto {
    private UUID id;
    private String name;
    private Conversation.ConversationType type;
    private List<UserDto> participants;
    private Instant createdAt;

    public static ConversationDto fromEntity(Conversation conversation) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setName(conversation.getName());
        dto.setType(conversation.getType());
        dto.setCreatedAt(conversation.getCreatedAt());
        if (conversation.getParticipants() != null) {
            dto.setParticipants(conversation.getParticipants().stream()
                    .map(participant -> UserDto.fromUser(participant.getUser()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}


