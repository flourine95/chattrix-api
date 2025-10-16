package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private String type;
    private Instant sentAt;
    private Instant createdAt;
}
