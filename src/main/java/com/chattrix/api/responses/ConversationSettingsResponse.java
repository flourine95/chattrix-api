package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSettingsResponse {
    private Long conversationId;
    private Boolean isMuted;
    private Instant mutedUntil;
    private Boolean isBlocked;
    private Boolean notificationsEnabled;
    private String customNickname;
    private String theme;
}