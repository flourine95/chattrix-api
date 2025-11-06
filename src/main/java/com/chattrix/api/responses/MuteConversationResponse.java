package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MuteConversationResponse {
    private Boolean isMuted;
    private Instant mutedUntil;
}

