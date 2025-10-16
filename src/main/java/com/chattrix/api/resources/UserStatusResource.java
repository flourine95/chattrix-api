package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.responses.UserStatusResponse;
import com.chattrix.api.services.UserStatusService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/users/status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class UserStatusResource {

    @Inject
    private UserStatusService userStatusService;

    @Inject
    private UserMapper userMapper;

    @GET
    @Path("/online")
    public Response getOnlineUsers(@Context SecurityContext securityContext) {
        List<User> onlineUsers = userStatusService.getOnlineUsers();
        List<UserResponse> userDtos = userMapper.toResponseList(onlineUsers);
        return Response.ok(ApiResponse.success(userDtos, "Online users retrieved successfully")).build();
    }

    @GET
    @Path("/online/conversation/{conversationId}")
    public Response getOnlineUsersInConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        List<User> onlineUsers = userStatusService.getOnlineUsersInConversation(conversationId);
        List<UserResponse> userDtos = userMapper.toResponseList(onlineUsers);
        return Response.ok(ApiResponse.success(userDtos, "Online users in conversation retrieved successfully")).build();
    }

    @GET
    @Path("/{userId}")
    public Response getUserStatus(@Context SecurityContext securityContext, @PathParam("userId") Long userId) {
        boolean isOnline = userStatusService.isUserOnline(userId);
        int sessionCount = userStatusService.getActiveSessionCount(userId);

        UserStatusResponse statusDto = new UserStatusResponse(userId, isOnline, sessionCount);
        return Response.ok(ApiResponse.success(statusDto, "User status retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}
