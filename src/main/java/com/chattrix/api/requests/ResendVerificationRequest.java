package com.chattrix.api.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;
}
