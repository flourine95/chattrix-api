package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusResponse {
    private Long userId;
    private boolean isOnline;
    private int activeSessionCount;
}
