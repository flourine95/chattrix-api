package com.chattrix.api.requests;

import com.chattrix.api.entities.NetworkQuality;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Request DTO for reporting call quality metrics.
 * Validates: Requirements 7.1, 7.2, 7.3
 */
@Getter
@Setter
public class ReportQualityRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotNull(message = "Network quality cannot be null")
    private NetworkQuality networkQuality;
    
    @DecimalMin(value = "0.0", message = "Packet loss rate must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Packet loss rate must not exceed 1.0")
    private Double packetLossRate;
    
    @Min(value = 0, message = "Round trip time must be non-negative")
    private Integer roundTripTime;
    
    private Instant timestamp;
}
