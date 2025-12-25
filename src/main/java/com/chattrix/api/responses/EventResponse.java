package com.chattrix.api.responses;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private Long id;
    private Long conversationId;
    private UserResponse creator;
    private String title;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private Instant createdAt;
    private Instant updatedAt;
    
    // RSVP statistics
    private Integer goingCount;
    private Integer maybeCount;
    private Integer notGoingCount;
    
    // Current user's RSVP status
    private String currentUserRsvpStatus;
    
    // List of RSVPs (optional, for detailed view)
    private List<EventRsvpResponse> rsvps;
}

