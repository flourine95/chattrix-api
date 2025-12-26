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
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    private List<ParticipantResponse> participants;

    private MessageResponse lastMessage;

    // Unread count for current user
    private Integer unreadCount;

    // Settings for current user
    private ConversationSettingsResponse settings;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantResponse {
        private Long userId;
        private String username;
        private String fullName;
        private String avatarUrl;
        private String role;
        private Boolean online;
        private Instant lastSeen;
    }

    @Data
    public static class MessageResponse {
        private Long id;
        private String content;
        private Long senderId;
        private String senderUsername;
        private String senderFullName;
        private String senderAvatarUrl;
        private Instant sentAt;
        private String type;
        private Long readCount;
        private List<ReadReceiptInfo> readBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadReceiptInfo {
        private Long userId;
        private String username;
        private String fullName;
        private String avatarUrl;
        private Instant readAt;
    }
}
