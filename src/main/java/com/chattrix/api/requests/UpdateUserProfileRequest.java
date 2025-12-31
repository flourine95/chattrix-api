package com.chattrix.api.requests;

import com.chattrix.api.enums.Gender;
import com.chattrix.api.enums.ProfileVisibility;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private Instant dateOfBirth;

    private Gender gender;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    private ProfileVisibility profileVisibility;

    private String avatarUrl;
}

