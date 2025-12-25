package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGroupAvatarRequest {
    
    @NotBlank(message = "Avatar URL is required")
    private String avatarUrl;
}

