package com.chattrix.api.requests;

import com.chattrix.api.entities.CallType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateCallRequest {
    @NotNull(message = "Callee ID is required")
    private Long calleeId;

    @NotNull(message = "Call type is required")
    private CallType callType;
}