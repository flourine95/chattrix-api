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

// Sử dụng Root path hỗn hợp để cover cả 2 trường hợp
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageReadResource {

    @Inject private ReadReceiptService readReceiptService;
    @Inject private UserContext userContext;

    // 1. Mark specific message as read
    @POST
    @Path("/messages/{messageId}/read")
    public Response markAsRead(@PathParam("messageId") Long messageId) {
        readReceiptService.markAsRead(userContext.getCurrentUserId(), messageId);
        return Response.noContent().build();
    }

    // 2. Get who read the message
    @GET
    @Path("/messages/{messageId}/receipts")
    public Response getReadReceipts(@PathParam("messageId") Long messageId) {
        var receipts = readReceiptService.getReadReceipts(userContext.getCurrentUserId(), messageId);
        return Response.ok(ApiResponse.success(receipts, "Read receipts retrieved successfully")).build();
    }

    // 3. Mark conversation as read (Sync action)
    // Đặt ở đây dù path là /conversations/... để gom nhóm logic "Read"
    @POST
    @Path("/conversations/{conversationId}/mark-read")
    public Response markConversationAsRead(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("lastMessageId") Long lastMessageId) {
        readReceiptService.markConversationAsRead(userContext.getCurrentUserId(), conversationId, lastMessageId);
        return Response.noContent().build();
    }

    // 4. Global Unread Count (Có thể chuyển sang UserResource nếu muốn)
    @GET
    @Path("/messages/unread-count")
    public Response getUnreadCount() {
        Long count = readReceiptService.getUnreadCount(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(Map.of("unreadCount", count), "Unread count retrieved")).build();
    }
}