package com.chattrix.api.responses;

import com.chattrix.api.entities.Gender;
import com.chattrix.api.entities.ProfileVisibility;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private boolean emailVerified;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private Gender gender;
    private Instant dateOfBirth;
    private String location;
    private ProfileVisibility profileVisibility;
    private boolean online;
    private Instant lastSeen;
    private Instant createdAt;
    private Instant updatedAt;
}