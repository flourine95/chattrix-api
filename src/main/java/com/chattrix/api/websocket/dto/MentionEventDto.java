package com.chattrix.api.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MentionEventDto {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private Long mentionedUserId;
    private Instant createdAt;
}

