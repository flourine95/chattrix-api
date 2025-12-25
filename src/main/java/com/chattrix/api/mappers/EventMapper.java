package com.chattrix.api.mappers;

import com.chattrix.api.entities.Event;
import com.chattrix.api.entities.EventRsvp;
import com.chattrix.api.responses.EventResponse;
import com.chattrix.api.responses.EventRsvpResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = {UserMapper.class})
public interface EventMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "goingCount", ignore = true)
    @Mapping(target = "maybeCount", ignore = true)
    @Mapping(target = "notGoingCount", ignore = true)
    @Mapping(target = "currentUserRsvpStatus", ignore = true)
    @Mapping(target = "rsvps", ignore = true)
    EventResponse toResponse(Event event);

    @Mapping(target = "status", source = "status")
    EventRsvpResponse toRsvpResponse(EventRsvp rsvp);
}

