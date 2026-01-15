package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.UpdatePollRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.PollService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/polls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class PollResource {

    @Inject
    private PollService pollService;
    
    @Inject
    private UserContext userContext;

    /**
     * List all polls in conversation with optional filters
     * 
     * @param conversationId Conversation ID
     * @param status Filter by status: "active", "closed", "all" (default: "all")
     * @param cursor Cursor for pagination
     * @param limit Page size (default: 20, max: 100)
     * @return List of polls
     */
    @GET
    public Response listPolls(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("status") @DefaultValue("all") String status,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        var result = pollService.listPolls(
                userContext.getCurrentUserId(), 
                conversationId, 
                status, 
                cursor, 
                limit
        );
        return Response.ok(ApiResponse.success(result, "Polls retrieved successfully")).build();
    }

    /**
     * Get poll detail by message ID
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the poll
     * @return Poll detail with full information
     */
    @GET
    @Path("/{messageId}")
    public Response getPoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        
        var poll = pollService.getPoll(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId
        );
        return Response.ok(ApiResponse.success(poll, "Poll retrieved successfully")).build();
    }

    /**
     * Update poll (close poll, change question/options if no votes yet)
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the poll
     * @param request Update request
     * @return Updated poll
     */
    @PUT
    @Path("/{messageId}")
    public Response updatePoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid UpdatePollRequest request) {
        
        var poll = pollService.updatePoll(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId, 
                request
        );
        return Response.ok(ApiResponse.success(poll, "Poll updated successfully")).build();
    }

    /**
     * Delete poll (only creator or admin can delete)
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the poll
     * @return Success response
     */
    @DELETE
    @Path("/{messageId}")
    public Response deletePoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        
        pollService.deletePoll(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId
        );
        return Response.ok(ApiResponse.success(null, "Poll deleted successfully")).build();
    }

    /**
     * Close poll manually (stop accepting votes)
     * 
     * @param conversationId Conversation ID
     * @param messageId Message ID containing the poll
     * @return Updated poll
     */
    @POST
    @Path("/{messageId}/close")
    public Response closePoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        
        var poll = pollService.closePoll(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId
        );
        return Response.ok(ApiResponse.success(poll, "Poll closed successfully")).build();
    }
}
