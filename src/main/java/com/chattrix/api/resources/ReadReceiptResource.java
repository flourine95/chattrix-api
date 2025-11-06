package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ReadReceiptResponse;
import com.chattrix.api.services.ReadReceiptService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ReadReceiptResource {

    @Inject
    private ReadReceiptService readReceiptService;

    @POST
    @Path("/messages/{messageId}/mark-read")
    public Response markAsRead(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId) {
        User currentUser = getCurrentUser(securityContext);
        readReceiptService.markAsRead(currentUser.getId(), messageId);
        return Response.noContent().build();
    }

    @POST
    @Path("/conversations/{conversationId}/mark-read")
    public Response markConversationAsRead(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @QueryParam("lastMessageId") Long lastMessageId) {
        User currentUser = getCurrentUser(securityContext);
        readReceiptService.markConversationAsRead(currentUser.getId(), conversationId, lastMessageId);
        return Response.noContent().build();
    }

    @GET
    @Path("/messages/{messageId}/read-receipts")
    public Response getReadReceipts(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId) {
        User currentUser = getCurrentUser(securityContext);
        List<ReadReceiptResponse> receipts = readReceiptService.getReadReceipts(currentUser.getId(), messageId);
        return Response.ok(ApiResponse.success(receipts, "Read receipts retrieved successfully")).build();
    }

    @GET
    @Path("/unread-count")
    public Response getUnreadCount(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        Long count = readReceiptService.getUnreadCount(currentUser.getId());
        return Response.ok(ApiResponse.success(Map.of("unreadCount", count), "Unread count retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

