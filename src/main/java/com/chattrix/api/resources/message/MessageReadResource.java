package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.ReadReceiptService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

// Resource for read receipts and marking messages/conversations as read
@Path("/v1/read-receipts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageReadResource {

    @Inject private ReadReceiptService readReceiptService;
    @Inject private UserContext userContext;

    // 1. Mark specific message as read
    // POST /api/v1/read-receipts/messages/{messageId}
    @POST
    @Path("/messages/{messageId}")
    public Response markAsRead(@PathParam("messageId") Long messageId) {
        readReceiptService.markAsRead(userContext.getCurrentUserId(), messageId);
        return Response.noContent().build();
    }

    // 2. Get who read the message
    // GET /api/v1/read-receipts/messages/{messageId}
    @GET
    @Path("/messages/{messageId}")
    public Response getReadReceipts(@PathParam("messageId") Long messageId) {
        var receipts = readReceiptService.getReadReceipts(userContext.getCurrentUserId(), messageId);
        return Response.ok(ApiResponse.success(receipts, "Read receipts retrieved successfully")).build();
    }

    // 3. Mark conversation as read (Sync action)
    // POST /api/v1/read-receipts/conversations/{conversationId}
    @POST
    @Path("/conversations/{conversationId}")
    public Response markConversationAsRead(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("lastMessageId") Long lastMessageId) {
        readReceiptService.markConversationAsRead(userContext.getCurrentUserId(), conversationId, lastMessageId);
        return Response.noContent().build();
    }

    // 4. Global Unread Count
    // GET /api/v1/read-receipts/unread-count
    @GET
    @Path("/unread-count")
    public Response getUnreadCount() {
        Long count = readReceiptService.getUnreadCount(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(Map.of("unreadCount", count), "Unread count retrieved")).build();
    }
}