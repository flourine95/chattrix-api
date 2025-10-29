package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MentionedUserResponse {
    private Long id;
    private String name;
    private String username;
}

