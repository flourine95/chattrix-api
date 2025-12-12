package com.chattrix.api.responses;

import com.chattrix.api.entities.Gender;
import com.chattrix.api.entities.ProfileVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    @JsonProperty("isEmailVerified")
    private boolean isEmailVerified;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private String bio;
    private Instant dateOfBirth;
    private Gender gender;
    private String location;
    private ProfileVisibility profileVisibility;
    @JsonProperty("isOnline")
    private boolean isOnline;
    private Instant lastSeen;
    private Instant createdAt;
}

