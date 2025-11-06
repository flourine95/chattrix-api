package com.chattrix.api.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConversationSettingsRequest {
    private Boolean notificationsEnabled;
    private String customNickname;
    private String theme;
}

