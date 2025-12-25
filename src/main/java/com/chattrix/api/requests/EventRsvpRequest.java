package com.chattrix.api.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRsvpRequest {

    @NotNull(message = "Status is required")
    @Pattern(regexp = "GOING|MAYBE|NOT_GOING", message = "Status must be GOING, MAYBE, or NOT_GOING")
    private String status;
}

