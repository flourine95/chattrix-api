package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.*;
import com.chattrix.api.services.ConversationService;
import com.chattrix.api.services.MessageService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationResource {

    @Inject
    private ConversationService conversationService;

    @Inject
    private MessageService messageService;

    @POST
    public Response createConversation(@Context SecurityContext securityContext, @Valid CreateConversationRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ConversationResponse conversation = conversationService.createConversation(currentUser.getId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(conversation, "Conversation created successfully"))
                .build();
    }

    @GET
    public Response getConversations(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        List<ConversationResponse> conversations = conversationService.getConversations(currentUser.getId());
        return Response.ok(ApiResponse.success(conversations, "Conversations retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}")
    public Response getConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationResponse conversation = conversationService.getConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(conversation, "Conversation retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}/members")
    public Response getConversationMembers(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        List<ConversationMemberResponse> members = conversationService.getConversationMembers(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(members, "Conversation members retrieved successfully")).build();
    }

    // ==================== CHAT INFO ENDPOINTS ====================

    @PUT
    @Path("/{conversationId}")
    public Response updateConversation(@Context SecurityContext securityContext,
                                      @PathParam("conversationId") Long conversationId,
                                      @Valid UpdateConversationRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ConversationResponse conversation = conversationService.updateConversation(currentUser.getId(), conversationId, request);
        return Response.ok(ApiResponse.success(conversation, "Conversation updated successfully")).build();
    }

    @DELETE
    @Path("/{conversationId}")
    public Response deleteConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        conversationService.deleteConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Conversation deleted successfully")).build();
    }

    @POST
    @Path("/{conversationId}/leave")
    public Response leaveConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        conversationService.leaveConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(null, "Left conversation successfully")).build();
    }

    @POST
    @Path("/{conversationId}/members")
    public Response addMembers(@Context SecurityContext securityContext,
                               @PathParam("conversationId") Long conversationId,
                               @Valid AddMembersRequest request) {
        User currentUser = getCurrentUser(securityContext);
        AddMembersResponse response = conversationService.addMembers(currentUser.getId(), conversationId, request);
        return Response.ok(ApiResponse.success(response, "Members added successfully")).build();
    }

    @DELETE
    @Path("/{conversationId}/members/{userId}")
    public Response removeMember(@Context SecurityContext securityContext,
                                 @PathParam("conversationId") Long conversationId,
                                 @PathParam("userId") Long userId) {
        User currentUser = getCurrentUser(securityContext);
        conversationService.removeMember(currentUser.getId(), conversationId, userId);
        return Response.ok(ApiResponse.success(null, "Member removed successfully")).build();
    }

    @PUT
    @Path("/{conversationId}/members/{userId}/role")
    public Response updateMemberRole(@Context SecurityContext securityContext,
                                    @PathParam("conversationId") Long conversationId,
                                    @PathParam("userId") Long userId,
                                    @Valid UpdateMemberRoleRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ConversationResponse.ParticipantResponse participant = conversationService.updateMemberRole(currentUser.getId(), conversationId, userId, request);
        return Response.ok(ApiResponse.success(participant, "Member role updated successfully")).build();
    }

    @GET
    @Path("/{conversationId}/settings")
    public Response getConversationSettings(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettingsResponse settings = conversationService.getConversationSettings(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(settings, "Settings retrieved successfully")).build();
    }

    @PUT
    @Path("/{conversationId}/settings")
    public Response updateConversationSettings(@Context SecurityContext securityContext,
                                              @PathParam("conversationId") Long conversationId,
                                              @Valid UpdateConversationSettingsRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ConversationSettingsResponse settings = conversationService.updateConversationSettings(currentUser.getId(), conversationId, request);
        return Response.ok(ApiResponse.success(settings, "Settings updated successfully")).build();
    }

    @POST
    @Path("/{conversationId}/mute")
    public Response muteConversation(@Context SecurityContext securityContext,
                                    @PathParam("conversationId") Long conversationId,
                                    @Valid MuteConversationRequest request) {
        User currentUser = getCurrentUser(securityContext);
        MuteConversationResponse response = conversationService.muteConversation(currentUser.getId(), conversationId, request);
        return Response.ok(ApiResponse.success(response, "Conversation mute status updated successfully")).build();
    }

    @POST
    @Path("/{conversationId}/block")
    public Response blockUser(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        BlockUserResponse response = conversationService.blockUser(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(response, "User blocked successfully")).build();
    }

    @POST
    @Path("/{conversationId}/unblock")
    public Response unblockUser(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        BlockUserResponse response = conversationService.unblockUser(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(response, "User unblocked successfully")).build();
    }

    @GET
    @Path("/{conversationId}/messages/search")
    public Response searchMessages(@Context SecurityContext securityContext,
                                  @PathParam("conversationId") Long conversationId,
                                  @QueryParam("query") String query,
                                  @QueryParam("type") String type,
                                  @QueryParam("senderId") Long senderId,
                                  @QueryParam("page") @DefaultValue("0") int page,
                                  @QueryParam("size") @DefaultValue("20") int size,
                                  @QueryParam("sort") @DefaultValue("DESC") String sort) {
        User currentUser = getCurrentUser(securityContext);
        Map<String, Object> result = messageService.searchMessages(currentUser.getId(), conversationId, query, type, senderId, page, size, sort);
        return Response.ok(ApiResponse.success(result, "Messages searched successfully")).build();
    }

    @GET
    @Path("/{conversationId}/media")
    public Response getMediaFiles(@Context SecurityContext securityContext,
                                 @PathParam("conversationId") Long conversationId,
                                 @QueryParam("type") String type,
                                 @QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("20") int size) {
        User currentUser = getCurrentUser(securityContext);
        Map<String, Object> result = messageService.getMediaFiles(currentUser.getId(), conversationId, type, page, size);
        return Response.ok(ApiResponse.success(result, "Media files retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}
