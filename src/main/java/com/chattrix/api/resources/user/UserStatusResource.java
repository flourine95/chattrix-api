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
    
    @Inject
    private com.chattrix.api.repositories.ConversationParticipantRepository participantRepository;

    @GET
    @Path("/online")
    public Response getOnlineUsers(@Context SecurityContext securityContext) {
        Set<Long> onlineUserIds = onlineStatusCache.getOnlineUserIds();
        List<UserResponse> userDtos = userRepository.findByIdsAsDTO(onlineUserIds);
        return Response.ok(ApiResponse.success(userDtos, "Online users retrieved successfully")).build();
    }

    @GET
    @Path("/online/conversation/{conversationId}")
    public Response getOnlineUsersInConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        // Get all participant user IDs in conversation
        List<Long> participantUserIds = participantRepository.findByConversationId(conversationId)
                .stream()
                .map(cp -> cp.getUser().getId())
                .toList();
        
        // Filter to only online users
        Set<Long> onlineUserIds = onlineStatusCache.getOnlineUserIds();
        Set<Long> onlineParticipantIds = participantUserIds.stream()
                .filter(onlineUserIds::contains)
                .collect(java.util.stream.Collectors.toSet());
        
        // Fetch user DTOs for online participants
        List<UserResponse> userDtos = userRepository.findByIdsAsDTO(onlineParticipantIds);
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
