package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.GlobalSearchResultResponse;
import com.chattrix.api.responses.MessageContextResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.MessageSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
            @QueryParam("query") String query,
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
}
