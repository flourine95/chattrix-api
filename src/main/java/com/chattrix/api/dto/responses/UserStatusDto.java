package com.chattrix.api.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusDto {
    private UUID userId;
    private boolean isOnline;
    private int activeSessionCount;

}
