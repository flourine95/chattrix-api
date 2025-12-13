package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
public class ConversationResponse {
    private Long id;
    private String type;
    private String name;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;

    private List<ParticipantResponse> participants;

    private MessageResponse lastMessage;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantResponse {
        private Long userId;
        private String username;
        private String role;
    }

    @Data
    public static class MessageResponse {
        private Long id;
        private String content;
        private Long senderId;
        private String senderUsername;
        private Instant sentAt;
        private String type;
    }
}