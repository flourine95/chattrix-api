package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusResponse {
    private Long userId;
    @JsonProperty("isOnline")
    private boolean isOnline;
    private int activeSessionCount;
}
