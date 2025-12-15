package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.AddMembersRequest;
import com.chattrix.api.requests.UpdateMemberRoleRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationMemberResource {

    @Inject
    private ConversationService conversationService;
    @Inject
    private UserContext userContext;

    @GET
    public Response getConversationMembers(@PathParam("conversationId") Long conversationId) {
        var members = conversationService.getConversationMembers(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(members, "Members retrieved successfully")).build();
    }

    @POST
    public Response addMembers(
            @PathParam("conversationId") Long conversationId,
            @Valid AddMembersRequest request) {
        var response = conversationService.addMembers(userContext.getCurrentUserId(), conversationId, request);
        return Response.ok(ApiResponse.success(response, "Members added successfully")).build();
    }

    @DELETE
    @Path("/{userId}")
    public Response removeMember(
            @PathParam("conversationId") Long conversationId,
            @PathParam("userId") Long userId) {
        conversationService.removeMember(userContext.getCurrentUserId(), conversationId, userId);
        return Response.ok(ApiResponse.success(null, "Member removed successfully")).build();
    }

    @POST
    @Path("/leave")
    public Response leaveConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.leaveConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Left conversation successfully")).build();
    }

    @PUT
    @Path("/{userId}/role")
    public Response updateMemberRole(
            @PathParam("conversationId") Long conversationId,
            @PathParam("userId") Long userId,
            @Valid UpdateMemberRoleRequest request) {
        var participant = conversationService.updateMemberRole(
                userContext.getCurrentUserId(), conversationId, userId, request
        );
        return Response.ok(ApiResponse.success(participant, "Member role updated successfully")).build();
    }
}