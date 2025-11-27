package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectCallRequest {
    @NotBlank(message = "Reason cannot be blank")
    @Pattern(regexp = "busy|declined|unavailable", message = "Reason must be one of: busy, declined, unavailable")
    private String reason;
}
