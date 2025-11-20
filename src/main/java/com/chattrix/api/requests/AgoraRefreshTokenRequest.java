package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for refreshing an Agora RTC token.
 * Validates: Requirements 4.1
 */
@Getter
@Setter
public class AgoraRefreshTokenRequest {
    
    @NotBlank(message = "Channel ID cannot be blank")
    private String channelId;
    
    // User ID is set by the backend from JWT token, not validated from client request
    private String userId;
    
    @NotBlank(message = "Old token cannot be blank")
    private String oldToken;
}
