package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.PinnedMessageResponse;
import com.chattrix.api.services.PinnedMessageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/conversations/{conversationId}/pinned-messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class PinnedMessageResource {

    @Inject
    private PinnedMessageService pinnedMessageService;

    @POST
    @Path("/{messageId}/pin")
    public Response pinMessage(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        User currentUser = getCurrentUser(securityContext);
        PinnedMessageResponse response = pinnedMessageService.pinMessage(currentUser.getId(), conversationId, messageId);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Message pinned successfully"))
                .build();
    }

    @DELETE
    @Path("/{messageId}/unpin")
    public Response unpinMessage(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        User currentUser = getCurrentUser(securityContext);
        pinnedMessageService.unpinMessage(currentUser.getId(), conversationId, messageId);
        return Response.noContent().build();
    }

    @GET
    public Response getPinnedMessages(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        List<PinnedMessageResponse> pinnedMessages = pinnedMessageService.getPinnedMessages(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(pinnedMessages, "Pinned messages retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

