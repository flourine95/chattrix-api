package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationMemberResponse {
    private Long id;
    private String name;
    private String username;
    private String avatarUrl;
    private String status;
}

