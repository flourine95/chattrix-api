package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.InviteLinkInfoResponse;
import com.chattrix.api.responses.JoinViaInviteResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.InviteLinkService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Path("/v1/invite")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class InviteJoinResource {

    @Inject
    private InviteLinkService inviteLinkService;

    @Inject
    private UserContext userContext;

    /**
     * Get invite link info (public - no auth required)
     */
    @GET
    @Path("/{token}")
    public Response getInviteLinkInfo(@PathParam("token") String token) {
        log.info("Getting invite link info for token: {}", token);
        InviteLinkInfoResponse response = inviteLinkService.getInviteLinkInfo(token);

        return Response.ok(ApiResponse.success(response, "Invite link info retrieved successfully")).build();
    }

    /**
     * Join group via invite link
     */
    @POST
    @Path("/{token}/join")
    @Secured
    public Response joinViaInviteLink(@PathParam("token") String token) {
        Long userId = userContext.getCurrentUserId();
        log.info("User {} joining via invite token: {}", userId, token);
        JoinViaInviteResponse response = inviteLinkService.joinViaInviteLink(userId, token);

        return Response.ok(ApiResponse.success(response, "Joined group successfully")).build();
    }
}
