package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateConversationRequest;
import com.chattrix.api.requests.ReorderPinRequest;
import com.chattrix.api.requests.UpdateConversationRequest;
import com.chattrix.api.requests.UpdateGroupPermissionsRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import com.chattrix.api.services.conversation.GroupPermissionsService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationResource {

    @Inject
    private ConversationService conversationService;
    @Inject
    private UserContext userContext;
    @Inject
    private GroupPermissionsService groupPermissionsService;

    @POST
    public Response createConversation(@Valid CreateConversationRequest request) {
        var res = conversationService.createConversation(userContext.getCurrentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(res, "Conversation created successfully")).build();
    }

    @GET
    public Response getConversations(
            @QueryParam("filter") @DefaultValue("all") String filter,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        var result = conversationService.getConversations(userContext.getCurrentUserId(), filter, cursor, limit);
        return Response.ok(ApiResponse.success(result, "Conversations retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}")
    public Response getConversation(@PathParam("conversationId") Long conversationId) {
        var res = conversationService.getConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(res, "Conversation retrieved successfully")).build();
    }

    @PUT
    @Path("/{conversationId}")
    public Response updateConversation(
            @PathParam("conversationId") Long conversationId,
            @Valid UpdateConversationRequest request) {
        var res = conversationService.updateConversation(userContext.getCurrentUserId(), conversationId, request);
        return Response.ok(ApiResponse.success(res, "Conversation updated successfully")).build();
    }

    @DELETE
    @Path("/{conversationId}")
    public Response deleteConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.archiveConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation archived successfully")).build();
    }

    // Archive/Unarchive
    @POST
    @Path("/{conversationId}/archive")
    public Response archiveConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.archiveConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation archived successfully")).build();
    }

    @POST
    @Path("/{conversationId}/unarchive")
    public Response unarchiveConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.unarchiveConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation unarchived successfully")).build();
    }

    // Mute/Unmute
    @POST
    @Path("/{conversationId}/mute")
    public Response muteConversation(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("duration") Long durationMinutes) {
        conversationService.muteConversation(userContext.getCurrentUserId(), conversationId, durationMinutes);
        return Response.ok(ApiResponse.success(null, "Conversation muted successfully")).build();
    }

    @POST
    @Path("/{conversationId}/unmute")
    public Response unmuteConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.unmuteConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation unmuted successfully")).build();
    }

    // Pin/Unpin
    @POST
    @Path("/{conversationId}/pin")
    public Response pinConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.pinConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation pinned successfully")).build();
    }

    @POST
    @Path("/{conversationId}/unpin")
    public Response unpinConversation(@PathParam("conversationId") Long conversationId) {
        conversationService.unpinConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation unpinned successfully")).build();
    }

    @POST
    @Path("/{conversationId}/reorder")
    public Response reorderPinnedConversation(
            @PathParam("conversationId") Long conversationId,
            @Valid ReorderPinRequest request) {
        conversationService.reorderPinnedConversation(
                userContext.getCurrentUserId(), 
                conversationId, 
                request.getNewPinOrder()
        );
        return Response.ok(ApiResponse.success(null, "Conversation reordered successfully")).build();
    }

    // Group Permissions
    @GET
    @Path("/{conversationId}/permissions")
    public Response getGroupPermissions(@PathParam("conversationId") Long conversationId) {
        var res = groupPermissionsService.getGroupPermissions(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(res, "Permissions retrieved successfully")).build();
    }

    @PUT
    @Path("/{conversationId}/permissions")
    public Response updateGroupPermissions(
            @PathParam("conversationId") Long conversationId,
            @Valid UpdateGroupPermissionsRequest request) {
        var res = groupPermissionsService.updateGroupPermissions(
                userContext.getCurrentUserId(), 
                conversationId, 
                request
        );
        return Response.ok(ApiResponse.success(res, "Permissions updated successfully")).build();
    }
}