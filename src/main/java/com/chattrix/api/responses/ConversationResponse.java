package com.chattrix.api.responses;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ConversationResponse {
    private Long id;
    private String type;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ParticipantResponse> participants;
    private MessageResponse lastMessage;

    public static ConversationResponse fromEntity(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setType(conversation.getType().name());
        response.setName(conversation.getName());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getParticipants() != null) {
            response.setParticipants(
                    conversation.getParticipants().stream()
                            .map(ParticipantResponse::fromEntity)
                            .toList()
            );
        }

        // Map lastMessage if exists
        if (conversation.getLastMessage() != null) {
            response.setLastMessage(MessageResponse.fromEntity(conversation.getLastMessage()));
        }

        return response;
    }

    @Getter
    @Setter
    public static class ParticipantResponse {
        private Long userId;
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
        private Long id;
        private String content;
        private Long senderId;
        private String senderUsername;
        private Instant sentAt;
        private String type;

        public static MessageResponse fromEntity(com.chattrix.api.entities.Message message) {
            if (message == null) return null;

            MessageResponse response = new MessageResponse();
            response.setId(message.getId());
            response.setContent(message.getContent());
            response.setSenderId(message.getSender().getId());
            response.setSenderUsername(message.getSender().getUsername());
            response.setSentAt(message.getSentAt());
            response.setType(message.getType().name());
            return response;
        }
    }
}
