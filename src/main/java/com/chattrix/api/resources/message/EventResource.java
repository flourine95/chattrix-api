package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.UpdateEventRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.EventService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
     * List all events in conversation with optional filters
     * 
     * @param conversationId Conversation ID
     * @param status Filter by status: "upcoming", "past", "all" (default: "all")
     * @param cursor Cursor for pagination
     * @param limit Page size (default: 20, max: 100)
     * @return List of events
     */
    @GET
    public Response listEvents(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("status") @DefaultValue("all") String status,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        var result = eventService.listEvents(
                userContext.getCurrentUserId(), 
                conversationId, 
                status, 
                cursor, 
                limit
        );
        return Response.ok(ApiResponse.success(result, "Events retrieved successfully")).build();
    }

    /**
     * Get event detail by message ID
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the event
     * @return Event detail with full information
     */
    @GET
    @Path("/{messageId}")
    public Response getEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        
        var event = eventService.getEvent(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId
        );
        return Response.ok(ApiResponse.success(event, "Event retrieved successfully")).build();
    }

    /**
     * Update event details
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the event
     * @param request Update request
     * @return Updated event
     */
    @PUT
    @Path("/{messageId}")
    public Response updateEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid UpdateEventRequest request) {
        
        var event = eventService.updateEvent(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId, 
                request
        );
        return Response.ok(ApiResponse.success(event, "Event updated successfully")).build();
    }

    /**
     * Delete event (only creator or admin can delete)
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the event
     * @return Success response
     */
    @DELETE
    @Path("/{messageId}")
    public Response deleteEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        
        eventService.deleteEvent(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId
        );
        return Response.ok(ApiResponse.success(null, "Event deleted successfully")).build();
    }

    /**
     * Get list of users who RSVP'd to event
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the event
     * @param status Filter by RSVP status: "going", "maybe", "not_going", "all" (default: "all")
     * @return List of users with their RSVP status
     */
    @GET
    @Path("/{messageId}/rsvps")
    public Response getEventRsvps(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @QueryParam("status") @DefaultValue("all") String status) {
        
        var rsvps = eventService.getEventRsvps(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId,
                status
        );
        return Response.ok(ApiResponse.success(rsvps, "Event RSVPs retrieved successfully")).build();
    }
}
