package com.chattrix.api.requests;

import com.chattrix.api.entities.CallStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCallStatusRequest {

    @NotNull(message = "Status cannot be null")
    private CallStatus status;
}
