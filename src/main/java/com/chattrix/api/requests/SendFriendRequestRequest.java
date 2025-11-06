package com.chattrix.api.requests;

import jakarta.validation.constraints.NotNull;

public class SendFriendRequestRequest {
    @NotNull(message = "Receiver user ID is required")
    public Long receiverUserId;

    public String nickname;
}

