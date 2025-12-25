package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationMemberResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;
    private boolean online;
}

