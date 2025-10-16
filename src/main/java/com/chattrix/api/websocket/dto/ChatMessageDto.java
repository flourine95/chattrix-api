package com.chattrix.api.websocket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    private Long conversationId;
    private String content;
}
