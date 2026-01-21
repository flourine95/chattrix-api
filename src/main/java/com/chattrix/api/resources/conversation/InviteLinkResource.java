package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateInviteLinkRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.InviteLinkResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.InviteLinkService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@Slf4j
public class InviteLinkResource {

    @Inject
    private InviteLinkService inviteLinkService;

    @Inject
    private UserContext userContext;

    /**
     * Create invite link for a group
     */
    @POST
    @Path("/{conversationId}/invite-link")
    public Response createInviteLink(
            @PathParam("conversationId") Long conversationId,
            @Valid CreateInviteLinkRequest request) {

        Long userId = userContext.getCurrentUserId();
        log.info("User {} creating invite link for conversation {}", userId, conversationId);
        InviteLinkResponse response = inviteLinkService.createInviteLink(userId, conversationId, request);

        return Response.ok(ApiResponse.success(response, "Invite link created successfully")).build();
    }

    /**
     * Get current invite link for a group
     */
    @GET
    @Path("/{conversationId}/invite-link")
    public Response getCurrentInviteLink(@PathParam("conversationId") Long conversationId) {
        Long userId = userContext.getCurrentUserId();
        log.info("User {} getting invite link for conversation {}", userId, conversationId);
        InviteLinkResponse response = inviteLinkService.getCurrentInviteLink(userId, conversationId);

        return Response.ok(ApiResponse.success(response, "Invite link retrieved successfully")).build();
    }

    /**
     * Get invite link history for a group (with cursor pagination)
     */
    @GET
    @Path("/{conversationId}/invite-links")
    public Response getInviteLinkHistory(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        Long userId = userContext.getCurrentUserId();
        log.info("User {} getting invite link history for conversation {} (cursor: {}, limit: {})", 
                userId, conversationId, cursor, limit);
        
        var response = inviteLinkService.getInviteLinkHistory(userId, conversationId, cursor, limit);

        return Response.ok(ApiResponse.success(response, "Invite link history retrieved successfully")).build();
    }

    /**
     * Revoke invite link
     */
    @DELETE
    @Path("/{conversationId}/invite-link")
    public Response revokeInviteLink(@PathParam("conversationId") Long conversationId) {
        Long userId = userContext.getCurrentUserId();
        log.info("User {} revoking invite link for conversation {}", userId, conversationId);
        inviteLinkService.revokeInviteLink(userId, conversationId);

        return Response.ok(ApiResponse.success(null, "Invite link revoked successfully")).build();
    }
}
