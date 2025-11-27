package com.chattrix.api.requests;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AgoraTokenRequest {

    @NotBlank(message = "Channel ID cannot be blank")
    @Size(max = 64, message = "Channel ID must not exceed 64 characters")
    private String channelId;

    @NotBlank(message = "Role cannot be blank")
    @Pattern(regexp = "publisher|subscriber", message = "Role must be either 'publisher' or 'subscriber'")
    private String role;

    @Min(value = 60, message = "Expiration must be at least 60 seconds")
    @Max(value = 86400, message = "Expiration must not exceed 86400 seconds (24 hours)")
    private Integer expirationSeconds = 3600;
}