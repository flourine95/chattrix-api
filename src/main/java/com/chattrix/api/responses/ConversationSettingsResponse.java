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
    private Boolean muted;
    private Instant mutedUntil;
    private Boolean blocked;
    private Boolean notificationsEnabled;
    private String customNickname;
    private String theme;
    private Boolean pinned;
    private Integer pinOrder;
    private Boolean archived;
    private Boolean hidden;
}