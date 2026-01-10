package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.ConversationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCreatedDto {
    private ConversationResponse conversation;
    private Long createdBy;
    private String createdByUsername;
}
