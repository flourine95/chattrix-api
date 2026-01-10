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
public class ConversationRoleUpdatedDto {
    private Long conversationId;
    private Long userId;
    private String username;
    private String fullName;
    private String oldRole;
    private String newRole;
    private Long updatedBy;
    private String updatedByUsername;
    private Instant timestamp;
}
