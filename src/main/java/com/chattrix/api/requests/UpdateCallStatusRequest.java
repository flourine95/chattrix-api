package com.chattrix.api.requests;

import com.chattrix.api.entities.CallStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating call status.
 * Validates: Requirements 8.1
 */
@Getter
@Setter
public class UpdateCallStatusRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotNull(message = "Status cannot be null")
    private CallStatus status;
}
