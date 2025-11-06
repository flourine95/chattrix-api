package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class BlockUserResponse {
    private Boolean isBlocked;
    private Instant blockedAt;
}

