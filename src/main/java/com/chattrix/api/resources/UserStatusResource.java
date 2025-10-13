package com.chattrix.api.resources;

import com.chattrix.api.dto.responses.UserDto;
import com.chattrix.api.entities.User;
import com.chattrix.api.services.UserStatusService;
import com.chattrix.api.filters.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/v1/users/status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserStatusResource {

    @Inject
    private UserStatusService userStatusService;

    @GET
    @Path("/online")
    @Secured
    public Response getOnlineUsers() {
        try {
            List<User> onlineUsers = userStatusService.getOnlineUsers();
            List<UserDto> userDtos = onlineUsers.stream()
                    .map(UserDto::fromUser)
                    .collect(Collectors.toList());

            return Response.ok(userDtos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving online users")
                    .build();
        }
    }

    @GET
    @Path("/online/conversation/{conversationId}")
    @Secured
    public Response getOnlineUsersInConversation(@PathParam("conversationId") String conversationIdStr) {
        try {
            UUID conversationId = UUID.fromString(conversationIdStr);
            List<User> onlineUsers = userStatusService.getOnlineUsersInConversation(conversationId);
            List<UserDto> userDtos = onlineUsers.stream()
                    .map(UserDto::fromUser)
                    .collect(Collectors.toList());

            return Response.ok(userDtos).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid conversation ID format")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving online users for conversation")
                    .build();
        }
    }

    @GET
    @Path("/{userId}")
    @Secured
    public Response getUserStatus(@PathParam("userId") String userIdStr) {
        try {
            UUID userId = UUID.fromString(userIdStr);
            boolean isOnline = userStatusService.isUserOnline(userId);
            int sessionCount = userStatusService.getActiveSessionCount(userId);

            return Response.ok(new UserStatusDto(userId, isOnline, sessionCount)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid user ID format")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving user status")
                    .build();
        }
    }

    // Inner DTO class for user status response
    public static class UserStatusDto {
        private UUID userId;
        private boolean isOnline;
        private int activeSessionCount;

        public UserStatusDto() {}

        public UserStatusDto(UUID userId, boolean isOnline, int activeSessionCount) {
            this.userId = userId;
            this.isOnline = isOnline;
            this.activeSessionCount = activeSessionCount;
        }

        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public boolean isOnline() { return isOnline; }
        public void setOnline(boolean online) { isOnline = online; }

        public int getActiveSessionCount() { return activeSessionCount; }
        public void setActiveSessionCount(int activeSessionCount) { this.activeSessionCount = activeSessionCount; }
    }
}
