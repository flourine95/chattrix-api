package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.UpdateConversationSettingsRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.ConversationService;
import com.chattrix.api.services.conversation.ConversationSettingsService;
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

    @Inject private ConversationSettingsService settingsService;
    @Inject private ConversationService conversationService; // Inject thêm để xử lý Block/Unblock
    @Inject private UserContext userContext;

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
        var response = conversationService.blockUser(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(response, "User blocked successfully")).build();
    }

    @POST
    @Path("/unblock")
    public Response unblockUser(@PathParam("conversationId") Long conversationId) {
        var response = conversationService.unblockUser(userContext.getCurrentUserId(), conversationId);
        return Response.ok(ApiResponse.success(response, "User unblocked successfully")).build();
    }
}