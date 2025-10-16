package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.UserResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * DTO for outgoing messages broadcast to clients via WebSocket
 * Contains additional information like sender details
 */
@Getter
@Setter
public class OutgoingMessageDto {
    private Long id;
    private Long conversationId;
    private UserResponse sender;
    private String content;
    private Instant createdAt;
}

