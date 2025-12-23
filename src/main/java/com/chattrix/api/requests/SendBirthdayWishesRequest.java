package com.chattrix.api.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendBirthdayWishesRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotEmpty(message = "At least one conversation is required")
    private List<Long> conversationIds;
    
    private String customMessage; // Optional custom message, nếu null sẽ dùng template
}
