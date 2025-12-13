package com.chattrix.api.responses;

import com.chattrix.api.entities.Gender;
import com.chattrix.api.entities.ProfileVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    @JsonProperty("isEmailVerified")
    private boolean isEmailVerified;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private Gender gender;
    private Instant dateOfBirth;
    private String location;
    private ProfileVisibility profileVisibility;
    @JsonProperty("isOnline")
    private boolean isOnline;
    private Instant lastSeen;
    private Instant createdAt;
    private Instant updatedAt;
}