package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.MessageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/search")
@Produces(MediaType.APPLICATION_JSON)
@Secured
public class ConversationSearchResource {

    @Inject
    private MessageService messageService;
    @Inject
    private UserContext userContext;

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

    @GET
    @Path("/media")
    public Response getMediaFiles(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("type") String type,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        var result = messageService.getMediaFiles(
                userContext.getCurrentUserId(), conversationId, type, cursor, limit
        );
        return Response.ok(ApiResponse.success(result, "Media files retrieved successfully")).build();
    }
}

