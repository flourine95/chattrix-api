package com.chattrix.api.responses;

import com.chattrix.api.entities.User;
import com.chattrix.api.enums.Gender;
import com.chattrix.api.enums.ProfileVisibility;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private Instant lastSeen;
    private Instant createdAt;
    private Instant updatedAt;
}