package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateConversationRequest;
import com.chattrix.api.requests.UpdateConversationRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationResource {

    @Inject
    private ConversationService conversationService;
    @Inject
    private UserContext userContext;

    @POST
    public Response createConversation(@Valid CreateConversationRequest request) {
        var res = conversationService.createConversation(userContext.getCurrentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(res, "Conversation created successfully")).build();
    }

    @GET
    public Response getConversations(
            @QueryParam("filter") @DefaultValue("all") String filter,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        var result = conversationService.getConversationsWithCursor(userContext.getCurrentUserId(), filter, cursor, limit);
        return Response.ok(ApiResponse.success(result, "Conversations retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}")
    public Response getConversation(@PathParam("conversationId") Long conversationId) {
        var res = conversationService.getConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(res, "Conversation retrieved successfully")).build();
    }

    @PUT
    @Path("/{conversationId}")
    public Response updateConversation(
            @PathParam("conversationId") Long conversationId,
            @Valid UpdateConversationRequest request) {
        var res = conversationService.updateConversation(userContext.getCurrentUserId(), conversationId, request);
        return Response.ok(ApiResponse.success(res, "Conversation updated successfully")).build();
    }

    @DELETE
    @Path("/{conversationId}")
    public Response deleteConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.deleteConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation deleted successfully")).build();
    }
}