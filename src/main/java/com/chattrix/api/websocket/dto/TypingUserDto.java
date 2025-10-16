package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for user typing information
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingUserDto {
    private Long userId;
    private String username;
    private String fullName;
}

