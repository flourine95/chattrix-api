package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteLinkInfoResponse {
    private String token;
    private Long groupId;
    private String groupName;
    private String groupAvatar;
    private Integer memberCount;
    private Boolean valid;
    private Instant expiresAt;
    private Long createdBy;
    private String createdByUsername;
    private String createdByFullName;
}
