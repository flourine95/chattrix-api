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
    private Long messageId;
    private String title;
    private String description;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private List<Long> goingUserIds;
    private List<Long> maybeUserIds;
    private List<Long> notGoingUserIds;
    private Integer goingCount;
    private Integer maybeCount;
    private Integer notGoingCount;
    private String currentUserStatus;  // "GOING", "MAYBE", "NOT_GOING", or null
    private Boolean isPast;
    private Long createdBy;
    private String createdByUsername;
    private Instant createdAt;
}
