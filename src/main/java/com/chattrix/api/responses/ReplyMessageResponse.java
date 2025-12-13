package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ReplyMessageResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String content;
    private String type;
    private String mediaUrl;
    private Instant createdAt;
}

