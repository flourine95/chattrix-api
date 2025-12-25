package com.chattrix.api.websocket.dto;

import com.chattrix.api.responses.EventResponse;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEventDto {
    private String type; // EVENT_CREATED, EVENT_UPDATED, EVENT_DELETED, EVENT_RSVP_UPDATED
    private EventResponse event;
}

