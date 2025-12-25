package com.chattrix.api.requests;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInviteLinkRequest {

    @Min(value = 60, message = "Expiration must be at least 60 seconds")
    private Integer expiresIn; // seconds, null = never expires

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses; // null = unlimited
}
