package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteLinkResponse {
    private Long id;
    private String token;
    private Long conversationId;
    private Long createdBy;
    private String createdByUsername;
    private Instant createdAt;
    private Instant expiresAt;
    private Integer maxUses;
    private Integer currentUses;
    private Boolean revoked;
    private Instant revokedAt;
    private Long revokedBy;
    private Boolean valid;
}
