package com.chattrix.api.resources.user;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/v1/users/{userId}/mutual-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MutualGroupsResource {

    @Inject
    private ConversationService conversationService;

    @Inject
    private UserContext userContext;

    /**
     * Get mutual groups between current user and another user
     * GET /v1/users/{userId}/mutual-groups
     */
    @GET
    public Response getMutualGroups(@PathParam("userId") Long otherUserId) {
        Long currentUserId = userContext.getCurrentUserId();
        
        List<ConversationResponse> mutualGroups = conversationService.getMutualGroups(currentUserId, otherUserId);
        
        return Response.ok(ApiResponse.success(mutualGroups, "Mutual groups retrieved successfully")).build();
    }
}

