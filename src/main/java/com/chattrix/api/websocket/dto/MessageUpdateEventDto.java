package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageUpdateEventDto {
    private Long messageId;
    private Long conversationId;
    private String content;
    private Boolean isEdited;
    private Instant updatedAt;
}
