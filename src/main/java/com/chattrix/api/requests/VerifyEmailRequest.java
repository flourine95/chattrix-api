package com.chattrix.api.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "OTP cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otp;
}
