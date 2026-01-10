package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationPermissionsUpdatedDto {
    private Long conversationId;
    private Map<String, String> permissions;
    private Long updatedBy;
    private String updatedByUsername;
    private Instant timestamp;
}
