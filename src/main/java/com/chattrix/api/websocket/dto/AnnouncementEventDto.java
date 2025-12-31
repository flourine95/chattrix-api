package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.MessageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementEventDto {
    private MessageResponse announcement;
    private Long conversationId;
}
