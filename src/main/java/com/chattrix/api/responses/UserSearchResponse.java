package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserSearchResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private boolean online;
    private Instant lastSeen;
    private boolean contact;
    private boolean hasConversation;
    private Long conversationId;
}

