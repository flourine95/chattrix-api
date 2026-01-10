package com.chattrix.api.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderPinRequest {
    
    @NotNull(message = "New pin order is required")
    @Min(value = 1, message = "Pin order must be at least 1")
    private Integer newPinOrder;
}
