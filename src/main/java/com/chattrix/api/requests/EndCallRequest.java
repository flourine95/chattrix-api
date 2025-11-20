package com.chattrix.api.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for ending an active call.
 * Validates: Requirements 5.1
 */
@Getter
@Setter
public class EndCallRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotBlank(message = "Ended by cannot be blank")
    @Pattern(regexp = "caller|callee", message = "Ended by must be either 'caller' or 'callee'")
    private String endedBy;
    
    @Min(value = 0, message = "Duration must be non-negative")
    private Integer durationSeconds;
}
