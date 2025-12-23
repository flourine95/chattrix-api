package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MentionedUserResponse {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("userId")
    private Long userId; // Same as id, for frontend compatibility
    
    private String fullName;
    private String username;
    
    // Ensure both id and userId have the same value
    public void setId(Long id) {
        this.id = id;
        this.userId = id;
    }
}