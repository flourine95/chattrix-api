package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.GlobalSearchResultResponse;
import com.chattrix.api.responses.MediaSearchResponse;
import com.chattrix.api.responses.MessageContextResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.MessageSearchService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@Path("/v1/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageSearchResource {

    @Inject
    private MessageSearchService searchService;

    @Inject
    private UserContext userContext;

    /**
     * Global search - Search messages across all conversations
     */
    @GET
    @Path("/messages")
    @Secured
    public Response globalSearch(
            @QueryParam("query") @NotBlank(message = "Search query is required") String query,
            @QueryParam("type") String type,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Long userId = userContext.getCurrentUserId();
        CursorPaginatedResponse<GlobalSearchResultResponse> results = searchService.globalSearch(userId, query, type, cursor, limit);

        return Response.ok(ApiResponse.success(results, "Search completed successfully")).build();
    }

    /**
     * Get message context - Get messages around a specific message
     */
    @GET
    @Path("/messages/{messageId}/context")
    @Secured
    public Response getMessageContext(
            @PathParam("messageId") Long messageId,
            @QueryParam("conversationId") Long conversationId,
            @QueryParam("contextSize") @DefaultValue("10") int contextSize) {

        Long userId = userContext.getCurrentUserId();
        MessageContextResponse context = searchService.getMessageContext(userId, conversationId, messageId, contextSize);

        return Response.ok(ApiResponse.success(context, "Message context retrieved successfully")).build();
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
    @Path("/conversations/{conversationId}/media")
    @Secured
    public Response searchMedia(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("type") String type,
            @QueryParam("startDate") String startDate,
            @QueryParam("endDate") String endDate,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Long userId = userContext.getCurrentUserId();
        
        MediaSearchResponse results = searchService.searchMedia(
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
    @Path("/conversations/{conversationId}/media/statistics")
    @Secured
    public Response getMediaStatistics(@PathParam("conversationId") Long conversationId) {
        Long userId = userContext.getCurrentUserId();
        
        com.chattrix.api.responses.MediaStatisticsResponse statistics = 
            searchService.getMediaStatistics(userId, conversationId);

        return Response.ok(ApiResponse.success(statistics, "Media statistics retrieved successfully")).build();
    }
}
