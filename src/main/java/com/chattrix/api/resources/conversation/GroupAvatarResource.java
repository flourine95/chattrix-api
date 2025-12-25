package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.UpdateGroupAvatarRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.GroupAvatarService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/avatar")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class GroupAvatarResource {

    @Inject
    private GroupAvatarService avatarService;

    @Inject
    private UserContext userContext;

    /**
     * Update group avatar (Cloudinary URL)
     * PUT /v1/conversations/{conversationId}/avatar
     */
    @PUT
    public Response updateAvatar(
            @PathParam("conversationId") Long conversationId,
            @Valid UpdateGroupAvatarRequest request) {
        Long userId = userContext.getCurrentUserId();
        ConversationResponse conversation = avatarService.updateAvatar(userId, conversationId, request);
        return Response.ok(ApiResponse.success(conversation, "Group avatar updated successfully")).build();
    }

    /**
     * Delete group avatar
     * DELETE /v1/conversations/{conversationId}/avatar
     */
    @DELETE
    public Response deleteAvatar(@PathParam("conversationId") Long conversationId) {
        Long userId = userContext.getCurrentUserId();
        ConversationResponse conversation = avatarService.deleteAvatar(userId, conversationId);
        return Response.ok(ApiResponse.success(conversation, "Group avatar deleted successfully")).build();
    }
}

