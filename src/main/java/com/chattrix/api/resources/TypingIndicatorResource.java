package com.chattrix.api.resources;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.services.TypingIndicatorService;
import com.chattrix.api.websocket.dto.TypingIndicatorDto;
import com.chattrix.api.websocket.dto.TypingIndicatorResponseDto;
import com.chattrix.api.websocket.dto.TypingUserDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("userId and conversationId are required")
                    .build();
        }

        // Validate conversation exists
        if (conversationRepository.findById(conversationId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Conversation not found")
                    .build();
        }

        typingIndicatorService.startTyping(conversationId, userId);

        return Response.ok()
                .entity("User " + userId + " started typing in conversation " + conversationId)
                .build();
    }

    /**
     * Test endpoint: Simulate user stops typing
     */
    @POST
    @Path("/stop")
    public Response stopTyping(@QueryParam("userId") UUID userId,
                              @QueryParam("conversationId") UUID conversationId) {
        if (userId == null || conversationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("userId and conversationId are required")
                    .build();
        }

        typingIndicatorService.stopTyping(conversationId, userId);

        return Response.ok()
                .entity("User " + userId + " stopped typing in conversation " + conversationId)
                .build();
    }

    /**
     * Test endpoint: Get current typing users in conversation
     */
    @GET
    @Path("/status/{conversationId}")
    public Response getTypingStatus(@PathParam("conversationId") UUID conversationId,
                                   @QueryParam("excludeUserId") UUID excludeUserId) {
        if (conversationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("conversationId is required")
                    .build();
        }

        Set<UUID> typingUserIds = typingIndicatorService.getTypingUsersInConversation(conversationId, excludeUserId);

        // For testing purposes, just return the user IDs
        return Response.ok()
                .entity(new TypingStatusResponse(conversationId, typingUserIds))
                .build();
    }

    /**
     * Test endpoint: Check if specific user is typing
     */
    @GET
    @Path("/check")
    public Response checkUserTyping(@QueryParam("userId") UUID userId,
                                   @QueryParam("conversationId") UUID conversationId) {
        if (userId == null || conversationId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("userId and conversationId are required")
                    .build();
        }

        boolean isTyping = typingIndicatorService.isUserTyping(conversationId, userId);

        return Response.ok()
                .entity(new UserTypingStatus(userId, conversationId, isTyping))
                .build();
    }

    /**
     * Test endpoint: Clear all typing indicators for a user
     */
    @DELETE
    @Path("/clear/{userId}")
    public Response clearUserTyping(@PathParam("userId") UUID userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("userId is required")
                    .build();
        }

        typingIndicatorService.removeUserFromAllConversations(userId);

        return Response.ok()
                .entity("Cleared all typing indicators for user " + userId)
                .build();
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
