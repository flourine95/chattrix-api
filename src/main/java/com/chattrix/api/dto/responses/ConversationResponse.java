package com.chattrix.api.dto.responses;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ConversationResponse {
    private UUID id;
    private String type;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ParticipantResponse> participants;
    private MessageResponse lastMessage;

    @Getter
    @Setter
    public static class ParticipantResponse {
        private UUID userId;
        private String username;
        private String role;

        public static ParticipantResponse fromEntity(ConversationParticipant participant) {
            ParticipantResponse response = new ParticipantResponse();
            response.setUserId(participant.getUser().getId());
            response.setUsername(participant.getUser().getUsername());
            response.setRole(participant.getRole().name());
            return response;
        }
    }

    @Getter
    @Setter
    public static class MessageResponse {
        private UUID id;
        private String content;
        private String senderUsername;
        private Instant sentAt;
        private String type;
    }

    public static ConversationResponse fromEntity(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setType(conversation.getType().name());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getParticipants() != null) {
            response.setParticipants(
                conversation.getParticipants().stream()
                    .map(ParticipantResponse::fromEntity)
                    .toList()
            );
        }

        return response;
    }
}
