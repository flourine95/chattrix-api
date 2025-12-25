package com.chattrix.api.responses;

import lombok.Data;

import java.time.Instant;

@Data
public class MessageDetailResponse {
    private Long id;
    private Long conversationId;
    private UserResponse sender;
    private String content;
    private String type;
    private Instant sentAt;
    
    // Poll reference
    private Long pollId;
    private PollResponse poll;

    // Event reference
    private Long eventId;
    private EventResponse event;
}
