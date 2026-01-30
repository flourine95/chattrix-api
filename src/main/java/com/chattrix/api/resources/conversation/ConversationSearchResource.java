package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.MessageService;
import com.chattrix.api.services.message.MessageSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationSearchResource {

    @Inject
    private MessageService messageService;
    
    @Inject
    private MessageSearchService messageSearchService;
    
    @Inject
    private UserContext userContext;

    /**
     * Search messages in a specific conversation
     */
    @GET
    @Path("/messages")
    public Response searchMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("query") String query,
            @QueryParam("type") String type,
            @QueryParam("senderId") Long senderId,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("sort") @DefaultValue("DESC") String sort) {

        var result = messageService.searchMessages(
                userContext.getCurrentUserId(), conversationId, query, type, senderId, cursor, limit, sort
        );
        return Response.ok(ApiResponse.success(result, "Messages searched successfully")).build();
    }

    /**
     * Search media files in a conversation
     * 
     * @param conversationId Conversation ID
     * @param type Media type filter (IMAGE, VIDEO, AUDIO, FILE, LINK) - can be comma-separated for multiple types
     * @param startDate Filter by start date (ISO-8601 format, e.g., 2026-01-01T00:00:00Z)
     * @param endDate Filter by end date (ISO-8601 format, e.g., 2026-01-31T23:59:59Z)
     * @param cursor Cursor for pagination
     * @param limit Number of items per page (default: 20, max: 100)
     * @return Media messages with statistics and pagination info
     */
    @GET
    @Path("/media")
    public Response searchMedia(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("type") String type,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Long userId = userContext.getCurrentUserId();
        
        var results = messageSearchService.searchMedia(
            userId, conversationId, type, startDate, endDate, cursor, limit);

        return Response.ok(ApiResponse.success(results, "Media search completed successfully")).build();
    }
    
    /**
     * Get media statistics for a conversation
     * 
     * @param conversationId Conversation ID
     * @return Statistics about media files (counts by type)
     */
    @GET
    @Path("/media/statistics")
    public Response getMediaStatistics(@PathParam("conversationId") Long conversationId) {
        Long userId = userContext.getCurrentUserId();
        
        var statistics = messageSearchService.getMediaStatistics(userId, conversationId);

        return Response.ok(ApiResponse.success(statistics, "Media statistics retrieved successfully")).build();
    }
}
