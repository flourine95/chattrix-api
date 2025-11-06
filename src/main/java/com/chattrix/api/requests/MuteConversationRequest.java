package com.chattrix.api.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MuteConversationRequest {
    /**
     * Duration in seconds to mute the conversation.
     * - null or 0: unmute
     * - positive number: mute for that duration (e.g., 3600 = 1 hour)
     * - -1: mute indefinitely
     */
    private Integer duration;
}

