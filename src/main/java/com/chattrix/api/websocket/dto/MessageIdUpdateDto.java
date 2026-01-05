package com.chattrix.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for notifying clients when temp message ID is replaced with real DB ID
 * Used in Write-Behind cache pattern
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageIdUpdateDto {
    private Long tempId;        // Temporary ID (negative number)
    private Long realId;        // Real DB ID (positive number)
    private Long conversationId;
}
