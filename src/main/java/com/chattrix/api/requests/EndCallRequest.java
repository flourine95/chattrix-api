package com.chattrix.api.requests;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndCallRequest {

    @Pattern(
            regexp = "hangup|network error|device error|timeout",
            message = "Reason must be one of: hangup, network error, device error, timeout"
    )
    @Builder.Default
    private String reason = "hangup";
}