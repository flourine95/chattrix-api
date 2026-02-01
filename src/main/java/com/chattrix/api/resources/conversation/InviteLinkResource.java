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

    @GET
    @Path("/{conversationId}/invite-links")
    public Response getInviteLinks(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("status") String status,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        
        Long userId = userContext.getCurrentUserId();
        log.info("User {} getting invite links for conversation {} (status: {}, cursor: {}, limit: {})", 
                userId, conversationId, status, cursor, limit);
        
        var response = inviteLinkService.getInviteLinks(userId, conversationId, status, cursor, limit);

        return Response.ok(ApiResponse.success(response, "Invite links retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}/invite-link/{token}")
    public Response getInviteLinkDetail(
            @PathParam("conversationId") Long conversationId,
            @PathParam("token") String token) {
        
        Long userId = userContext.getCurrentUserId();
        log.info("User {} getting invite link detail {} for conversation {}", userId, token, conversationId);
        var response = inviteLinkService.getInviteLinkDetail(userId, conversationId, token);

        return Response.ok(ApiResponse.success(response, "Invite link detail retrieved successfully")).build();
    }

    @DELETE
    @Path("/{conversationId}/invite-link/{token}")
    public Response revokeInviteLink(
            @PathParam("conversationId") Long conversationId,
            @PathParam("token") String token) {
        
        Long userId = userContext.getCurrentUserId();
        log.info("User {} revoking invite link {} for conversation {}", userId, token, conversationId);
        inviteLinkService.revokeInviteLink(userId, conversationId, token);

        return Response.ok(ApiResponse.success(null, "Invite link revoked successfully")).build();
    }
}
