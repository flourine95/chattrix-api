package com.chattrix.api.resources.user;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.responses.UserStatusResponse;
import com.chattrix.api.services.cache.OnlineStatusCache;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Set;

@Path("/v1/users/status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class UserStatusResource {

    @Inject
    private OnlineStatusCache onlineStatusCache;
    
    @Inject
    private UserRepository userRepository;

    @Inject
    private UserMapper userMapper;

    @GET
    @Path("/online")
    public Response getOnlineUsers(@Context SecurityContext securityContext) {
        Set<Long> onlineUserIds = onlineStatusCache.getOnlineUserIds();
        List<User> onlineUsers = userRepository.findByIds(List.copyOf(onlineUserIds));
        List<UserResponse> userDtos = userMapper.toResponseList(onlineUsers);
        return Response.ok(ApiResponse.success(userDtos, "Online users retrieved successfully")).build();
    }

    @GET
    @Path("/online/conversation/{conversationId}")
    public Response getOnlineUsersInConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        // Get all participants in conversation
        List<User> participants = userRepository.findAll().stream()
                .filter(user -> user.getConversationParticipants().stream()
                        .anyMatch(cp -> cp.getConversation().getId().equals(conversationId)))
                .toList();
        
        // Filter to only online users
        Set<Long> onlineUserIds = onlineStatusCache.getOnlineUserIds();
        List<User> onlineUsers = participants.stream()
                .filter(user -> onlineUserIds.contains(user.getId()))
                .toList();
        
        List<UserResponse> userDtos = userMapper.toResponseList(onlineUsers);
        return Response.ok(ApiResponse.success(userDtos, "Online users in conversation retrieved successfully")).build();
    }

    @GET
    @Path("/{userId}")
    public Response getUserStatus(@Context SecurityContext securityContext, @PathParam("userId") Long userId) {
        boolean isOnline = onlineStatusCache.isOnline(userId);
        int sessionCount = isOnline ? 1 : 0;  // Simplified: online = 1 session

        UserStatusResponse statusDto = new UserStatusResponse(userId, isOnline, sessionCount);
        return Response.ok(ApiResponse.success(statusDto, "User status retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}
