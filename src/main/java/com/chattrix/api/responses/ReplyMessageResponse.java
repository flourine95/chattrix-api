package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class ReplyMessageResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String content;
    private String type;
    private Map<String, Object> metadata;
    private Instant createdAt;
}

