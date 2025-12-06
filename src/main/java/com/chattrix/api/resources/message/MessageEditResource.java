package com.chattrix.api.resources.message;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.EditMessageRequest;
import com.chattrix.api.requests.ForwardMessageRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.MessageEditHistoryResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.message.MessageEditService;
import com.chattrix.api.services.message.MessageForwardService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageEditResource {

    @Inject
    private MessageEditService messageEditService;

    @Inject
    private MessageForwardService messageForwardService;

    @PUT
    @Path("/{messageId}/edit")
    public Response editMessage(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId,
            @Valid EditMessageRequest request) {
        User currentUser = getCurrentUser(securityContext);
        MessageResponse response = messageEditService.editMessage(currentUser.getId(), messageId, request);
        return Response.ok(ApiResponse.success(response, "Message edited successfully")).build();
    }

    @DELETE
    @Path("/{messageId}")
    public Response deleteMessage(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId) {
        User currentUser = getCurrentUser(securityContext);
        messageEditService.deleteMessage(currentUser.getId(), messageId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{messageId}/edit-history")
    public Response getEditHistory(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId) {
        User currentUser = getCurrentUser(securityContext);
        List<MessageEditHistoryResponse> history = messageEditService.getEditHistory(currentUser.getId(), messageId);
        return Response.ok(ApiResponse.success(history, "Edit history retrieved successfully")).build();
    }

    @POST
    @Path("/forward")
    public Response forwardMessage(
            @Context SecurityContext securityContext,
            @Valid ForwardMessageRequest request) {
        User currentUser = getCurrentUser(securityContext);
        List<MessageResponse> responses = messageForwardService.forwardMessage(currentUser.getId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(responses, "Message forwarded successfully"))
                .build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

