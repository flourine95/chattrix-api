package com.chattrix.api.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ConversationUpdateDto {
    private Long conversationId;
    private Instant updatedAt;
    private LastMessageDto lastMessage;

    @Getter
    @Setter
    public static class LastMessageDto {
        private Long id;
        private String content;
        private Long senderId;
        private String senderUsername;
        private Instant sentAt;
        private String type;
    }
}

