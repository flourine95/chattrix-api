package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.ReadReceiptService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Read Receipt endpoints
 */
@Path("/v1/read-receipts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ReadReceiptResource {

    @Inject
    private ReadReceiptService readReceiptService;
    
    @Inject
    private UserContext userContext;

    /**
     * Mark message as read
     * POST /v1/read-receipts/messages/{messageId}
     * 
     * TODO: Implement individual message read receipts
     * For now, use mark conversation as read instead
     */
    @POST
    @Path("/messages/{messageId}")
    @Secured
    public Response markMessageAsRead(@PathParam("messageId") Long messageId) {
        log.warn("Individual message read receipt not implemented yet. Use mark conversation as read instead.");
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(Map.of(
                    "success", false,
                    "message", "Individual message read receipts not implemented. Use POST /v1/read-receipts/conversations/{conversationId} instead",
                    "code", "NOT_IMPLEMENTED"
                ))
                .build();
    }

    /**
     * Mark conversation as read
     * POST /v1/read-receipts/conversations/{conversationId}
     * 
     * Marks all messages in conversation as read and resets unread count
     */
    @POST
    @Path("/conversations/{conversationId}")
    @Secured
    public Response markConversationAsRead(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("lastMessageId") Long lastMessageId) {
        
        Long userId = userContext.getCurrentUserId();
        readReceiptService.markConversationAsRead(userId, conversationId, lastMessageId);
        
        return Response.noContent().build();
    }

    /**
     * Get global unread count
     * GET /v1/read-receipts/unread-count
     * 
     * Returns total unread message count across all conversations
     */
    @GET
    @Path("/unread-count")
    @Secured
    public Response getUnreadCount() {
        Long userId = userContext.getCurrentUserId();
        int unreadCount = readReceiptService.getTotalUnreadCount(userId);
        
        return Response.ok(Map.of(
            "success", true,
            "data", Map.of("unreadCount", unreadCount)
        )).build();
    }
}
