package com.chattrix.api.resources;

import com.chattrix.api.dto.responses.ApiResponse;
import com.chattrix.api.dto.responses.UserDto;
import com.chattrix.api.dto.responses.UserStatusDto;
import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.services.UserStatusService;
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

            ApiResponse<List<UserDto>> response = ApiResponse.success(userDtos, "Online users retrieved successfully");
            return Response.ok(response).build();
        } catch (Exception e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Error retrieving online users", "INTERNAL_SERVER_ERROR");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
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

            ApiResponse<List<UserDto>> response = ApiResponse.success(userDtos, "Online users in conversation retrieved successfully");
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Invalid conversation ID format", "INVALID_FORMAT");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        } catch (Exception e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Error retrieving online users", "INTERNAL_SERVER_ERROR");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
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

            UserStatusDto statusDto = new UserStatusDto(userId, isOnline, sessionCount);
            ApiResponse<UserStatusDto> response = ApiResponse.success(statusDto, "User status retrieved successfully");
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Invalid user ID format", "INVALID_FORMAT");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        } catch (Exception e) {
            ApiResponse<Void> errorResponse = ApiResponse.error("Error retrieving user status", "INTERNAL_SERVER_ERROR");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }

}
