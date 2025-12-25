package com.chattrix.api.requests;

import lombok.Data;

@Data
public class MuteMemberRequest {
    private Integer duration; // Duration in seconds, null or -1 for permanent mute
}
