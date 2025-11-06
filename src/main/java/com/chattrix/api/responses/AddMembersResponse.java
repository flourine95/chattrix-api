package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class AddMembersResponse {
    private Long conversationId;
    private List<AddedMember> addedMembers;

    @Getter
    @Setter
    public static class AddedMember {
        private Long userId;
        private String username;
        private String fullName;
        private String role;
        private Instant joinedAt;
    }
}

