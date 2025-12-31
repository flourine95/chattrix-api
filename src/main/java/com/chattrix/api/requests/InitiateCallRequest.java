package com.chattrix.api.requests;

import com.chattrix.api.enums.CallType;
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
    @NotNull(message = "Conversation ID is required")
    private Long conversationId;

    @NotNull(message = "Call type is required")
    private CallType callType;
}
