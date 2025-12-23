package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.BulkCancelScheduledMessagesRequest;
import com.chattrix.api.requests.UpdateScheduledMessageRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.BulkCancelResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.ScheduledMessageService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/v1/messages/scheduled")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ScheduledMessageResource {

    @Inject
    private ScheduledMessageService scheduledMessageService;

    @Inject
    private UserContext userContext;

    @GET
    public Response getScheduledMessages(
            @QueryParam("conversationId") Long conversationId,
            @QueryParam("status") @DefaultValue("PENDING") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        Long userId = userContext.getCurrentUserId();
        Map<String, Object> result = scheduledMessageService.getScheduledMessages(
                userId, conversationId, status, page, size);

        return Response.ok(ApiResponse.success(result, "Scheduled messages retrieved successfully"))
                .build();
    }

    @GET
    @Path("/{scheduledMessageId}")
    public Response getScheduledMessage(@PathParam("scheduledMessageId") Long scheduledMessageId) {
        Long userId = userContext.getCurrentUserId();
        MessageResponse response = scheduledMessageService.getScheduledMessage(userId, scheduledMessageId);

        return Response.ok(ApiResponse.success(response, "Scheduled message retrieved successfully"))
                .build();
    }

    @PUT
    @Path("/{scheduledMessageId}")
    public Response updateScheduledMessage(
            @PathParam("scheduledMessageId") Long scheduledMessageId,
            @Valid UpdateScheduledMessageRequest request
    ) {
        Long userId = userContext.getCurrentUserId();
        MessageResponse response = scheduledMessageService.updateScheduledMessage(
                userId, scheduledMessageId, request);

        return Response.ok(ApiResponse.success(response, "Scheduled message updated successfully"))
                .build();
    }

    @DELETE
    @Path("/{scheduledMessageId}")
    public Response cancelScheduledMessage(@PathParam("scheduledMessageId") Long scheduledMessageId) {
        Long userId = userContext.getCurrentUserId();
        scheduledMessageService.cancelScheduledMessage(userId, scheduledMessageId);

        return Response.ok(ApiResponse.success(null, "Scheduled message cancelled successfully"))
                .build();
    }

    @DELETE
    @Path("/bulk")
    public Response bulkCancelScheduledMessages(@Valid BulkCancelScheduledMessagesRequest request) {
        Long userId = userContext.getCurrentUserId();
        BulkCancelResponse response = scheduledMessageService.bulkCancelScheduledMessages(
                userId, request.scheduledMessageIds());

        return Response.ok(ApiResponse.success(response, "Scheduled messages cancelled successfully"))
                .build();
    }
}
