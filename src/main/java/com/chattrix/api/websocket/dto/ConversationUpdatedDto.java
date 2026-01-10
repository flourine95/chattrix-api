package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdatedDto {
    private Long conversationId;
    private String name;
    private String avatarUrl;
    private String description;
    private Long updatedBy;
    private String updatedByUsername;
    private Instant updatedAt;
}
