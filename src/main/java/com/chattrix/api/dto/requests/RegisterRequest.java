package com.chattrix.api.dto.requests;

import com.chattrix.api.validations.UniqueUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Pattern(
            regexp = "^(?![_.])(?!.*[_.]{2})(?=.*[a-zA-Z])[a-zA-Z0-9._]+(?<![_.])$",
            message = "Username must contain at least one letter, and cannot start or end with '.' or '_'"
    )
    @UniqueUsername
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Display name cannot be blank")
    @Size(min = 1, max = 50, message = "Display name must be between 1 and 50 characters")
    private String displayName;
}