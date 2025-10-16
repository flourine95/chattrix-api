package com.chattrix.api.requests;

import jakarta.validation.constraints.NotNull;

public class AddContactRequest {
    @NotNull(message = "Contact user ID is required")
    public Long contactUserId;

    public String nickname;
}
