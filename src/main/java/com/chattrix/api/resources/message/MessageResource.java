package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.BulkCancelScheduledMessagesRequest;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.requests.EditMessageRequest;
import com.chattrix.api.requests.ForwardMessageRequest;
import com.chattrix.api.requests.ScheduleMessageRequest;
import com.chattrix.api.requests.UpdateScheduledMessageRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.BulkCancelResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.MessageEditService;
import com.chattrix.api.services.message.MessageForwardService;
import com.chattrix.api.services.message.MessageService;
import com.chattrix.api.services.message.ScheduledMessageService;
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

    @Inject
    private MessageService messageService;
    @Inject
    private MessageEditService messageEditService;
    @Inject
    private MessageForwardService messageForwardService;
    @Inject
    private ScheduledMessageService scheduledMessageService;
    @Inject
    private UserContext userContext;

    @GET
    public Response getMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("sort") @DefaultValue("DESC") String sort) {

        var messages = messageService.getMessages(
                userContext.getCurrentUserId(), conversationId, cursor, limit, sort
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

    // Pinned messages endpoints moved to PinnedMessageResource
    // Use /v1/conversations/{conversationId}/messages/pinned instead
    // Use /v1/conversations/{conversationId}/messages/{messageId}/pin instead

    // ============================================================================
    // SCHEDULED MESSAGES
    // ============================================================================

    @POST
    @Path("/schedule")
    public Response scheduleMessage(
            @PathParam("conversationId") Long conversationId,
            @Valid ScheduleMessageRequest request) {
        var response = scheduledMessageService.scheduleMessage(
                userContext.getCurrentUserId(), conversationId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Scheduled message created successfully")).build();
    }

    @GET
    @Path("/scheduled")
    public Response getScheduledMessages(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("status") String status,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        Long userId = userContext.getCurrentUserId();
        CursorPaginatedResponse<MessageResponse> result = scheduledMessageService.getScheduledMessages(
                userId, conversationId, status, cursor, limit);
        return Response.ok(ApiResponse.success(result, "Scheduled messages retrieved successfully")).build();
    }

    @GET
    @Path("/scheduled/{scheduledMessageId}")
    public Response getScheduledMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("scheduledMessageId") Long scheduledMessageId) {
        Long userId = userContext.getCurrentUserId();
        MessageResponse response = scheduledMessageService.getScheduledMessage(
                userId, conversationId, scheduledMessageId);
        return Response.ok(ApiResponse.success(response, "Scheduled message retrieved successfully")).build();
    }

    @PUT
    @Path("/scheduled/{scheduledMessageId}")
    public Response updateScheduledMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("scheduledMessageId") Long scheduledMessageId,
            @Valid UpdateScheduledMessageRequest request) {
        Long userId = userContext.getCurrentUserId();
        MessageResponse response = scheduledMessageService.updateScheduledMessage(
                userId, conversationId, scheduledMessageId, request);
        return Response.ok(ApiResponse.success(response, "Scheduled message updated successfully")).build();
    }

    @DELETE
    @Path("/scheduled/{scheduledMessageId}")
    public Response cancelScheduledMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("scheduledMessageId") Long scheduledMessageId) {
        Long userId = userContext.getCurrentUserId();
        scheduledMessageService.cancelScheduledMessage(userId, conversationId, scheduledMessageId);
        return Response.ok(ApiResponse.success(null, "Scheduled message cancelled successfully")).build();
    }

    @DELETE
    @Path("/scheduled/bulk")
    public Response bulkCancelScheduledMessages(
            @PathParam("conversationId") Long conversationId,
            @Valid BulkCancelScheduledMessagesRequest request) {
        Long userId = userContext.getCurrentUserId();
        BulkCancelResponse response = scheduledMessageService.bulkCancelScheduledMessages(
                userId, conversationId, request.scheduledMessageIds());
        return Response.ok(ApiResponse.success(response, "Scheduled messages cancelled successfully")).build();
    }
}