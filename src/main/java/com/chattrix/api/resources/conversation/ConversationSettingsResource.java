package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.MuteMemberRequest;
import com.chattrix.api.requests.UpdateConversationSettingsRequest;
import com.chattrix.api.requests.UpdateGroupPermissionsRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import com.chattrix.api.services.conversation.ConversationSettingsService;
import com.chattrix.api.services.conversation.GroupPermissionsService;
import com.chattrix.api.services.conversation.MemberMuteService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationSettingsResource {

    @Inject
    private ConversationSettingsService settingsService;
    @Inject
    private ConversationService conversationService;
    @Inject
    private MemberMuteService memberMuteService;
    @Inject
    private GroupPermissionsService permissionsService;
    @Inject
    private UserContext userContext;

    @GET
    public Response getSettings(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.getSettings(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Settings retrieved successfully")).build();
    }

    @PUT
    public Response updateSettings(
            @PathParam("conversationId") Long conversationId,
            @Valid UpdateConversationSettingsRequest request) {
        var settings = conversationService.updateConversationSettings(
                userContext.getCurrentUserId(), conversationId, request
        );
        return Response.ok(ApiResponse.success(settings, "Settings updated successfully")).build();
    }

    // --- ACTIONS ---

    @POST
    @Path("/mute") // Gộp logic toggle hoặc tách unmute tùy service, ở đây giữ tách
    public Response muteConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.muteConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation muted")).build();
    }

    @POST
    @Path("/unmute")
    public Response unmuteConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.unmuteConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unmuted")).build();
    }

    @POST
    @Path("/pin")
    public Response pinConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.pinConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation pinned")).build();
    }

    @POST
    @Path("/unpin")
    public Response unpinConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.unpinConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unpinned")).build();
    }

    @POST
    @Path("/archive")
    public Response archiveConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.archiveConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation archived")).build();
    }

    @POST
    @Path("/unarchive")
    public Response unarchiveConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.unarchiveConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unarchived")).build();
    }

    @POST
    @Path("/hide")
    public Response hideConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.hideConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation hidden")).build();
    }

    @POST
    @Path("/unhide")
    public Response unhideConversation(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.unhideConversation(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unhidden")).build();
    }

    @POST
    @Path("/block")
    public Response blockUser(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.blockUser(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "User blocked")).build();
    }

    @POST
    @Path("/unblock")
    public Response unblockUser(@PathParam("conversationId") Long conversationId) {
        var settings = settingsService.unblockUser(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "User unblocked")).build();
    }

    @POST
    @Path("/members/{userId}/mute")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response muteMember(
            @PathParam("conversationId") Long conversationId,
            @PathParam("userId") Long userId,
            MuteMemberRequest request) {
        var response = memberMuteService.muteMember(
                userContext.getCurrentUserId(), conversationId, userId, request
        );
        return Response.ok(ApiResponse.success(response, "Member muted successfully")).build();
    }

    @POST
    @Path("/members/{userId}/unmute")
    public Response unmuteMember(
            @PathParam("conversationId") Long conversationId,
            @PathParam("userId") Long userId) {
        var response = memberMuteService.unmuteMember(
                userContext.getCurrentUserId(), conversationId, userId
        );
        return Response.ok(ApiResponse.success(response, "Member unmuted successfully")).build();
    }

    @GET
    @Path("/permissions")
    public Response getGroupPermissions(@PathParam("conversationId") Long conversationId) {
        var permissions = permissionsService.getGroupPermissions(
                userContext.getCurrentUserId(), conversationId
        );
        return Response.ok(ApiResponse.success(permissions, "Permissions retrieved successfully")).build();
    }

    @PUT
    @Path("/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateGroupPermissions(
            @PathParam("conversationId") Long conversationId,
            UpdateGroupPermissionsRequest request) {
        var permissions = permissionsService.updateGroupPermissions(
                userContext.getCurrentUserId(), conversationId, request
        );
        return Response.ok(ApiResponse.success(permissions, "Permissions updated successfully")).build();
    }
}