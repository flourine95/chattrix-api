package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.MessageService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/conversations/{conversationId}/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageResource {

    @Inject
    private MessageService messageService;

    @GET
    public Response getMessages(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size,
            @QueryParam("sort") @DefaultValue("DESC") String sort) {

        User currentUser = getCurrentUser(securityContext);
        List<MessageResponse> messages = messageService.getMessages(currentUser.getId(), conversationId, page, size, sort);
        return Response.ok(ApiResponse.success(messages, "Messages retrieved successfully")).build();
    }

    @POST
    public Response sendMessage(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @Valid ChatMessageRequest request) {

        User currentUser = getCurrentUser(securityContext);
        MessageResponse message = messageService.sendMessage(currentUser.getId(), conversationId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(message, "Message sent successfully"))
                .build();
    }

    @GET
    @Path("/{messageId}")
    public Response getMessage(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {

        User currentUser = getCurrentUser(securityContext);
        MessageResponse message = messageService.getMessage(currentUser.getId(), conversationId, messageId);
        return Response.ok(ApiResponse.success(message, "Message retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}
