package com.chattrix.api.resources.event;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateEventRequest;
import com.chattrix.api.requests.EventRsvpRequest;
import com.chattrix.api.requests.UpdateEventRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.EventResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.event.EventService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/v1/conversations/{conversationId}/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class EventResource {

    @Inject
    private EventService eventService;

    @Inject
    private UserContext userContext;

    /**
     * Get all events for a conversation
     * GET /v1/conversations/{conversationId}/events
     */
    @GET
    public Response getEvents(@PathParam("conversationId") Long conversationId) {
        Long userId = userContext.getCurrentUserId();
        List<EventResponse> events = eventService.getEvents(userId, conversationId);
        return Response.ok(ApiResponse.success(events, "Events retrieved successfully")).build();
    }

    /**
     * Get a specific event by ID
     * GET /v1/conversations/{conversationId}/events/{eventId}
     */
    @GET
    @Path("/{eventId}")
    public Response getEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("eventId") Long eventId) {
        Long userId = userContext.getCurrentUserId();
        EventResponse event = eventService.getEvent(userId, conversationId, eventId);
        return Response.ok(ApiResponse.success(event, "Event retrieved successfully")).build();
    }

    /**
     * Create a new event
     * POST /v1/conversations/{conversationId}/events
     */
    @POST
    public Response createEvent(
            @PathParam("conversationId") Long conversationId,
            @Valid CreateEventRequest request) {
        Long userId = userContext.getCurrentUserId();
        EventResponse event = eventService.createEvent(userId, conversationId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(event, "Event created successfully"))
                .build();
    }

    /**
     * Update an event
     * PUT /v1/conversations/{conversationId}/events/{eventId}
     */
    @PUT
    @Path("/{eventId}")
    public Response updateEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("eventId") Long eventId,
            @Valid UpdateEventRequest request) {
        Long userId = userContext.getCurrentUserId();
        EventResponse event = eventService.updateEvent(userId, conversationId, eventId, request);
        return Response.ok(ApiResponse.success(event, "Event updated successfully")).build();
    }

    /**
     * Delete an event
     * DELETE /v1/conversations/{conversationId}/events/{eventId}
     */
    @DELETE
    @Path("/{eventId}")
    public Response deleteEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("eventId") Long eventId) {
        Long userId = userContext.getCurrentUserId();
        eventService.deleteEvent(userId, conversationId, eventId);
        return Response.ok(ApiResponse.success(null, "Event deleted successfully")).build();
    }

    /**
     * RSVP to an event
     * POST /v1/conversations/{conversationId}/events/{eventId}/rsvp
     */
    @POST
    @Path("/{eventId}/rsvp")
    public Response rsvpToEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("eventId") Long eventId,
            @Valid EventRsvpRequest request) {
        Long userId = userContext.getCurrentUserId();
        EventResponse event = eventService.rsvpToEvent(userId, conversationId, eventId, request);
        return Response.ok(ApiResponse.success(event, "RSVP updated successfully")).build();
    }
}

