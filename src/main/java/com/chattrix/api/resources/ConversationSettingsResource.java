package com.chattrix.api.resources;

import com.chattrix.api.entities.ConversationSettings;
import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.services.ConversationSettingsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/conversations/{conversationId}/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationSettingsResource {

    @Inject
    private ConversationSettingsService settingsService;

    @GET
    public Response getSettings(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.getSettings(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Settings retrieved successfully")).build();
    }

    @POST
    @Path("/hide")
    public Response hideConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.hideConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation hidden successfully")).build();
    }

    @POST
    @Path("/unhide")
    public Response unhideConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.unhideConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unhidden successfully")).build();
    }

    @POST
    @Path("/archive")
    public Response archiveConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.archiveConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation archived successfully")).build();
    }

    @POST
    @Path("/unarchive")
    public Response unarchiveConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.unarchiveConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unarchived successfully")).build();
    }

    @POST
    @Path("/pin")
    public Response pinConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.pinConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation pinned successfully")).build();
    }

    @POST
    @Path("/unpin")
    public Response unpinConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.unpinConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unpinned successfully")).build();
    }

    @POST
    @Path("/mute")
    public Response muteConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.muteConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation muted successfully")).build();
    }

    @POST
    @Path("/unmute")
    public Response unmuteConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettings settings = settingsService.unmuteConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Conversation unmuted successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

