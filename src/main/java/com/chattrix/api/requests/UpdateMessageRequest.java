package com.chattrix.api.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMessageRequest {
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 2000, message = "Content must be less than 2000 characters")
    private String content;
}
