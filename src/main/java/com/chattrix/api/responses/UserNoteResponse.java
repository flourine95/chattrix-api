package com.chattrix.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserNoteResponse {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String noteText;
    private String musicUrl;
    private String musicTitle;
    private String emoji;
    private Instant createdAt;
    private Instant expiresAt;
    private Long replyCount;  // Tổng số tin nhắn reply vào note này
}

