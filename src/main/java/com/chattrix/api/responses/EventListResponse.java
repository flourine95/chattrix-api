package com.chattrix.api.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventListResponse {
    private List<EventResponse> events;
    private Integer totalCount;
    private Integer upcomingCount;
    private Integer pastCount;
}
