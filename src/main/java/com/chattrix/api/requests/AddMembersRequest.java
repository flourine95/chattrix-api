package com.chattrix.api.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AddMembersRequest {
    @NotEmpty(message = "User IDs are required")
    private List<Long> userIds;
}

