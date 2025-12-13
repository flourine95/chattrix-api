package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateConversationRequest;
import com.chattrix.api.requests.UpdateConversationRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import com.chattrix.api.services.message.MessageService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationResource {

    @Inject
    private ConversationService conversationService;
    @Inject
    private MessageService messageService;
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
            @QueryParam("filter") @DefaultValue("all") String filter) {
        var list = conversationService.getConversations(userContext.getCurrentUserId(), filter);
        return Response.ok(ApiResponse.success(list, "Conversations retrieved successfully")).build();
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

    @GET
    @Path("/{conversationId}/messages/search")
    public Response searchMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("query") String query,
            @QueryParam("type") String type,
            @QueryParam("senderId") Long senderId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("DESC") String sort) {

        Map<String, Object> result = messageService.searchMessages(
                userContext.getCurrentUserId(), conversationId, query, type, senderId, page, size, sort
        );
        return Response.ok(ApiResponse.success(result, "Messages searched successfully")).build();
    }

    @GET
    @Path("/{conversationId}/media")
    public Response getMediaFiles(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("type") String type,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        Map<String, Object> result = messageService.getMediaFiles(
                userContext.getCurrentUserId(), conversationId, type, page, size
        );
        return Response.ok(ApiResponse.success(result, "Media files retrieved successfully")).build();
    }
}