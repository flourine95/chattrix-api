package com.chattrix.api.resources.invite;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.InviteLinkInfoResponse;
import com.chattrix.api.responses.JoinViaInviteResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.invite.GroupInviteLinkService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Public invite link endpoints (no conversation context required)
 * For conversation-scoped endpoints, see ConversationInviteLinkResource
 */
@Path("/v1/invite-links")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicInviteLinkResource {

    @Inject
    private GroupInviteLinkService inviteLinkService;

    @Inject
    private UserContext userContext;

    /**
     * Get invite link info (public - no auth required)
     */
    @GET
    @Path("/{token}")
    public Response getInviteLinkInfo(@PathParam("token") String token) {
        InviteLinkInfoResponse response = inviteLinkService.getInviteLinkInfo(token);
        return Response.ok(ApiResponse.success(response, "Invite link info retrieved successfully")).build();
    }

    /**
     * Join group via invite link (requires authentication)
     */
    @POST
    @Path("/{token}/join")
    @Secured
    public Response joinViaInviteLink(@PathParam("token") String token) {
        Long userId = userContext.getCurrentUserId();
        JoinViaInviteResponse response = inviteLinkService.joinViaInviteLink(userId, token);

        return Response.ok(ApiResponse.success(response, "Successfully joined conversation via invite link")).build();
    }
}

