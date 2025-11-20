package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for accepting an incoming call.
 * Validates: Requirements 2.2
 */
@Getter
@Setter
public class AcceptCallRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
}
