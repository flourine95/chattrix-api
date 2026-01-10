package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.BulkCancelResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.security.UserContext;
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
    public Response updateMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid com.chattrix.api.requests.UpdateMessageRequest request) {
        var response = messageService.updateMessage(
                userContext.getCurrentUserId(), conversationId, messageId, request);
        return Response.ok(ApiResponse.success(response, "Message updated successfully")).build();
    }

    @GET
    @Path("/{messageId}/history")
    public Response getEditHistory(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        var history = messageService.getEditHistory(userContext.getCurrentUserId(), conversationId, messageId);
        return Response.ok(ApiResponse.success(history, "Edit history retrieved successfully")).build();
    }

    // Forward message endpoint moved to MessageForwardResource
    // Use /v1/messages/{messageId}/forward instead

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

    // ============================================================================
    // POLLS
    // ============================================================================

    @POST
    @Path("/poll")
    public Response createPoll(
            @PathParam("conversationId") Long conversationId,
            @Valid CreatePollRequest request) {
        var response = messageService.createPoll(userContext.getCurrentUserId(), conversationId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Poll created successfully")).build();
    }

    @POST
    @Path("/{messageId}/poll/vote")
    public Response votePoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid VotePollRequest request) {
        var response = messageService.votePoll(userContext.getCurrentUserId(), conversationId, messageId, request);
        return Response.ok(ApiResponse.success(response, "Vote recorded successfully")).build();
    }

    // ============================================================================
    // EVENTS
    // ============================================================================

    @POST
    @Path("/event")
    public Response createEvent(
            @PathParam("conversationId") Long conversationId,
            @Valid CreateEventRequest request) {
        var response = messageService.createEvent(userContext.getCurrentUserId(), conversationId, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Event created successfully")).build();
    }

    @POST
    @Path("/{messageId}/event/rsvp")
    public Response respondToEvent(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid EventRsvpRequest request) {
        var response = messageService.respondToEvent(userContext.getCurrentUserId(), conversationId, messageId, request);
        return Response.ok(ApiResponse.success(response, "RSVP recorded successfully")).build();
    }

    // ============================================================================
    // FORWARD MESSAGE
    // ============================================================================

    @POST
    @Path("/{messageId}/forward")
    public Response forwardMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId,
            @Valid ForwardMessageRequest request) {
        var responses = messageService.forwardMessage(userContext.getCurrentUserId(), conversationId, messageId, request.conversationIds);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(responses, "Message forwarded successfully")).build();
    }
}