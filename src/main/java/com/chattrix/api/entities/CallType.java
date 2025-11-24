package com.chattrix.api.entities;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the type of call.
 */
public enum CallType {
    /**
     * Audio-only call
     */
    AUDIO,

    /**
     * Video call with audio
     */
    VIDEO;

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
