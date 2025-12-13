package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.requests.EditMessageRequest;
import com.chattrix.api.requests.ForwardMessageRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.MessageEditService;
import com.chattrix.api.services.message.MessageForwardService;
import com.chattrix.api.services.message.MessageService;
import com.chattrix.api.services.message.PinnedMessageService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageResource {

    @Inject private MessageService messageService;
    @Inject private MessageEditService messageEditService;
    @Inject private MessageForwardService messageForwardService;
    @Inject private PinnedMessageService pinnedMessageService;
    @Inject private UserContext userContext;

    @GET
    public Response getMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size,
            @QueryParam("sort") @DefaultValue("DESC") String sort) {

        var messages = messageService.getMessages(
                userContext.getCurrentUserId(), conversationId, page, size, sort
        );
        return Response.ok(ApiResponse.success(messages, "Messages retrieved successfully")).build();
    }

    @POST
    public Response sendMessage(
            @PathParam("conversationId") Long conversationId,
            @Valid ChatMessageRequest request) {
        var message = messageService.sendMessage(userContext.getCurrentUserId(), conversationId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(message, "Message sent successfully")).build();
    }

    @GET
    @Path("/{messageId}")
    public Response getMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        var message = messageService.getMessage(userContext.getCurrentUserId(), conversationId, messageId);
        return Response.ok(ApiResponse.success(message, "Message retrieved successfully")).build();
    }

    @DELETE
    @Path("/{messageId}")
    public Response deleteMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        messageService.deleteMessage(userContext.getCurrentUserId(), conversationId, messageId);
        return Response.ok(ApiResponse.success(null, "Message deleted successfully")).build();
    }

    @PUT
    @Path("/{messageId}")
    public Response editMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid EditMessageRequest request) {

        var response = messageEditService.editMessage(
                userContext.getCurrentUserId(), conversationId, messageId, request);
        return Response.ok(ApiResponse.success(response, "Message edited successfully")).build();
    }

    @GET
    @Path("/{messageId}/history")
    public Response getEditHistory(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        var history = messageEditService.getEditHistory(userContext.getCurrentUserId(), conversationId, messageId);
        return Response.ok(ApiResponse.success(history, "Edit history retrieved")).build();
    }

    @POST
    @Path("/forward")
    public Response forwardMessage(
            @PathParam("conversationId") Long conversationId,
            @Valid ForwardMessageRequest request) {
        var responses = messageForwardService.forwardMessage(userContext.getCurrentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(responses, "Message forwarded successfully")).build();
    }

    @GET
    @Path("/pinned")
    public Response getPinnedMessages(@PathParam("conversationId") Long conversationId) {
        var pinnedMessages = pinnedMessageService.getPinnedMessages(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(pinnedMessages, "Pinned messages retrieved")).build();
    }

    @POST
    @Path("/{messageId}/pin")
    public Response pinMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        var response = pinnedMessageService.pinMessage(userContext.getCurrentUserId(), conversationId, messageId);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Message pinned successfully")).build();
    }

    @DELETE
    @Path("/{messageId}/pin")
    public Response unpinMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        pinnedMessageService.unpinMessage(userContext.getCurrentUserId(), conversationId, messageId);
        return Response.ok(ApiResponse.success(null, "Message unpinned successfully")).build();
    }
}