package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for rejecting an incoming call.
 * Validates: Requirements 2.3
 */
@Getter
@Setter
public class RejectCallRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotBlank(message = "Reason cannot be blank")
    @Pattern(regexp = "busy|declined|unavailable", message = "Reason must be one of: busy, declined, unavailable")
    private String reason;
}
