package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchUserRequest {
    
    @NotBlank(message = "Search query cannot be blank")
    @Size(min = 1, max = 100, message = "Search query must be between 1 and 100 characters")
    private String query;
}

