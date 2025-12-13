package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMembersResponse {
    private Long conversationId;
    private List<AddedMember> addedMembers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddedMember {
        private Long userId;
        private String username;
        private String fullName;
        private String role;
        private Instant joinedAt;
    }
}