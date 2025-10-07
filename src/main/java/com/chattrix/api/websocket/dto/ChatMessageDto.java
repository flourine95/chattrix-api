package com.chattrix.api.websocket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ChatMessageDto {
    private UUID conversationId;
    private String content;
}


