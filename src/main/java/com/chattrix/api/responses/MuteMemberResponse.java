package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MuteMemberResponse {
    private Long userId;
    private String username;
    private String fullName;
    private boolean muted;
    private Instant mutedUntil;
    private Instant mutedAt;
    private Long mutedBy;
}
