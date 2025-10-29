package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Simplified message response for reply context
 * Contains only essential fields to avoid deep nesting
 */
@Getter
@Setter
public class ReplyMessageResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderName;
    private String content;
    private String type;
    private String mediaUrl;
    private Instant createdAt;
}

