package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.services.TypingIndicatorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.Set;

@Path("/v1/typing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class TypingIndicatorResource {

    @Inject
    private TypingIndicatorService typingIndicatorService;

    /**
     * Test endpoint: Simulate user starts typing
     */
    @POST
    @Path("/start")
    public Response startTyping(@Context SecurityContext securityContext, @QueryParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);

        typingIndicatorService.startTyping(conversationId, currentUser.getId());
        return Response.ok(ApiResponse.success(
                "User " + currentUser.getId() + " started typing in conversation " + conversationId,
                "Started typing successfully"
        )).build();
    }

    /**
     * Test endpoint: Simulate user stops typing
     */
    @POST
    @Path("/stop")
    public Response stopTyping(@Context SecurityContext securityContext, @QueryParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);

        typingIndicatorService.stopTyping(conversationId, currentUser.getId());
        return Response.ok(ApiResponse.success(
                "User " + currentUser.getId() + " stopped typing in conversation " + conversationId,
                "Stopped typing successfully"
        )).build();
    }

    /**
     * Test endpoint: Get current typing users in conversation
     */
    @GET
    @Path("/status/{conversationId}")
    public Response getTypingStatus(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @QueryParam("excludeUserId") Long excludeUserId) {

        Set<Long> typingUserIds = typingIndicatorService.getTypingUsersInConversation(conversationId, excludeUserId);
        TypingStatusResponse statusResponse = new TypingStatusResponse(conversationId, typingUserIds);
        return Response.ok(ApiResponse.success(statusResponse, "Typing status retrieved successfully")).build();
    }

    /**
     * Test endpoint: Check if specific user is typing
     */
    @GET
    @Path("/check")
    public Response checkUserTyping(
            @Context SecurityContext securityContext,
            @QueryParam("userId") Long userId,
            @QueryParam("conversationId") Long conversationId) {

        boolean isTyping = typingIndicatorService.isUserTyping(conversationId, userId);
        UserTypingStatus userStatus = new UserTypingStatus(userId, conversationId, isTyping);
        return Response.ok(ApiResponse.success(userStatus, "Typing status checked successfully")).build();
    }

    /**
     * Test endpoint: Clear all typing indicators for a user
     */
    @DELETE
    @Path("/clear")
    public Response clearUserTyping(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);

        typingIndicatorService.removeUserFromAllConversations(currentUser.getId());
        return Response.ok(ApiResponse.success(
                "Cleared all typing indicators for user " + currentUser.getId(),
                "Typing indicators cleared successfully"
        )).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }

    // Response DTOs
    public static class TypingStatusResponse {
        public Long conversationId;
        public Set<Long> typingUserIds;
        public int count;

        public TypingStatusResponse(Long conversationId, Set<Long> typingUserIds) {
            this.conversationId = conversationId;
            this.typingUserIds = typingUserIds;
            this.count = typingUserIds.size();
        }
    }

    public static class UserTypingStatus {
        public Long userId;
        public Long conversationId;
        public boolean isTyping;

        public UserTypingStatus(Long userId, Long conversationId, boolean isTyping) {
            this.userId = userId;
            this.conversationId = conversationId;
            this.isTyping = isTyping;
        }
    }
}
