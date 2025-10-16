package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for typing indicator response broadcast to clients via WebSocket
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorResponseDto {
    private Long conversationId;
    private List<TypingUserDto> typingUsers;
}

