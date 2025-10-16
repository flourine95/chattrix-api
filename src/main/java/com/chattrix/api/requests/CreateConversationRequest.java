package com.chattrix.api.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateConversationRequest {
    private String type;
    private String name;
    private List<Long> participantIds;
}
