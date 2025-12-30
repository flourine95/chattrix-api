package com.chattrix.api.requests;

import com.chattrix.api.validations.UniqueEmail;
import com.chattrix.api.validations.UniqueUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Pattern(
            regexp = "^(?![_.])(?!.*[_.]{2})(?=.*[a-zA-Z])[a-zA-Z0-9._]+(?<![_.])$",
            message = "Username must contain at least one letter, and cannot start or end with '.' or '_'"
    )
    @UniqueUsername
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    @UniqueEmail
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name cannot be blank")
    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;
}