package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;

/**
 * Basic user information DTO used for nested user references.
 * MapStruct will automatically use this when mapping User entities in nested contexts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicResponse {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private Instant lastSeen;
}
