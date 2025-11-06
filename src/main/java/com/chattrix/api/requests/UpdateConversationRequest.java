package com.chattrix.api.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConversationRequest {
    private String name;
    private String avatarUrl;
}

