package com.chattrix.api.resources;

import com.chattrix.api.dto.responses.ApiResponse;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.services.TypingIndicatorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Set;
import java.util.UUID;

@Path("v1/typing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TypingIndicatorResource {

    @Inject
    private TypingIndicatorService typingIndicatorService;

    @Inject
    private ConversationRepository conversationRepository;

    /**
     * Test endpoint: Simulate user starts typing
     */
    @POST
    @Path("/start")
    public Response startTyping(@QueryParam("userId") UUID userId,
                               @QueryParam("conversationId") UUID conversationId) {
        if (userId == null || conversationId == null) {
            ApiResponse<Void> errorResponse = ApiResponse.error("userId and conversationId are required", "VALIDATION_ERROR");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        // Validate conversation exists
        if (conversationRepository.findById(conversationId).isEmpty()) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Conversation not found", "NOT_FOUND");
            return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
        }

        typingIndicatorService.startTyping(conversationId, userId);

        ApiResponse<String> response = ApiResponse.success(
            "User " + userId + " started typing in conversation " + conversationId,
            "Started typing successfully"
        );
        return Response.ok(response).build();
    }

    /**
     * Test endpoint: Simulate user stops typing
     */
    @POST
    @Path("/stop")
    public Response stopTyping(@QueryParam("userId") UUID userId,
                              @QueryParam("conversationId") UUID conversationId) {
        if (userId == null || conversationId == null) {
            ApiResponse<Void> errorResponse = ApiResponse.error("userId and conversationId are required", "VALIDATION_ERROR");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        typingIndicatorService.stopTyping(conversationId, userId);

        ApiResponse<String> response = ApiResponse.success(
            "User " + userId + " stopped typing in conversation " + conversationId,
            "Stopped typing successfully"
        );
        return Response.ok(response).build();
    }

    /**
     * Test endpoint: Get current typing users in conversation
     */
    @GET
    @Path("/status/{conversationId}")
    public Response getTypingStatus(@PathParam("conversationId") UUID conversationId,
                                   @QueryParam("excludeUserId") UUID excludeUserId) {
        if (conversationId == null) {
            ApiResponse<Void> errorResponse = ApiResponse.error("conversationId is required", "VALIDATION_ERROR");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        Set<UUID> typingUserIds = typingIndicatorService.getTypingUsersInConversation(conversationId, excludeUserId);

        TypingStatusResponse statusResponse = new TypingStatusResponse(conversationId, typingUserIds);
        ApiResponse<TypingStatusResponse> response = ApiResponse.success(statusResponse, "Typing status retrieved successfully");
        return Response.ok(response).build();
    }

    /**
     * Test endpoint: Check if specific user is typing
     */
    @GET
    @Path("/check")
    public Response checkUserTyping(@QueryParam("userId") UUID userId,
                                   @QueryParam("conversationId") UUID conversationId) {
        if (userId == null || conversationId == null) {
            ApiResponse<Void> errorResponse = ApiResponse.error("userId and conversationId are required", "VALIDATION_ERROR");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        boolean isTyping = typingIndicatorService.isUserTyping(conversationId, userId);

        UserTypingStatus userStatus = new UserTypingStatus(userId, conversationId, isTyping);
        ApiResponse<UserTypingStatus> response = ApiResponse.success(userStatus, "Typing status checked successfully");
        return Response.ok(response).build();
    }

    /**
     * Test endpoint: Clear all typing indicators for a user
     */
    @DELETE
    @Path("/clear/{userId}")
    public Response clearUserTyping(@PathParam("userId") UUID userId) {
        if (userId == null) {
            ApiResponse<Void> errorResponse = ApiResponse.error("userId is required", "VALIDATION_ERROR");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }

        typingIndicatorService.removeUserFromAllConversations(userId);

        ApiResponse<String> response = ApiResponse.success(
            "Cleared all typing indicators for user " + userId,
            "Typing indicators cleared successfully"
        );
        return Response.ok(response).build();
    }

    // Response DTOs for testing
    public static class TypingStatusResponse {
        public UUID conversationId;
        public Set<UUID> typingUserIds;
        public int count;

        public TypingStatusResponse(UUID conversationId, Set<UUID> typingUserIds) {
            this.conversationId = conversationId;
            this.typingUserIds = typingUserIds;
            this.count = typingUserIds.size();
        }
    }

    public static class UserTypingStatus {
        public UUID userId;
        public UUID conversationId;
        public boolean isTyping;

        public UserTypingStatus(UUID userId, UUID conversationId, boolean isTyping) {
            this.userId = userId;
            this.conversationId = conversationId;
            this.isTyping = isTyping;
        }
    }
}
