package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class ContactResponse {
    public Long id;
    public Long contactUserId;
    public String username;
    public String fullName;
    public String avatarUrl;
    public String nickname;
    public boolean isFavorite;
    public boolean isOnline;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Instant lastSeen;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Instant createdAt;
}
