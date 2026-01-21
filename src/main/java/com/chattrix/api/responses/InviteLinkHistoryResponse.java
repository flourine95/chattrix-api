package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteLinkHistoryResponse {
    private Long id;
    private String token;
    private UserBasicResponse createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private Integer maxUses;
    private Integer currentUses;
    private Boolean isActive;
    private Boolean isRevoked;
    private Boolean isExpired;
    private Instant revokedAt;
    private UserBasicResponse revokedBy;
    private String status; // "active", "expired", "revoked", "max_uses_reached"
}
