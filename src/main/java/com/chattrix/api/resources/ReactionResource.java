package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.AddReactionRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ReactionResponse;
import com.chattrix.api.services.ReactionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/messages/{messageId}/reactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ReactionResource {

    @Inject
    private ReactionService reactionService;

    @POST
    public Response addReaction(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId,
            @Valid AddReactionRequest request) {

        User currentUser = getCurrentUser(securityContext);
        ReactionResponse response = reactionService.addReaction(currentUser.getId(), messageId, request.emoji());
        return Response.ok(ApiResponse.success(response, "Reaction toggled successfully")).build();
    }

    @DELETE
    @Path("/{emoji}")
    public Response removeReaction(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId,
            @PathParam("emoji") String emoji) {

        User currentUser = getCurrentUser(securityContext);
        ReactionResponse response = reactionService.removeReaction(currentUser.getId(), messageId, emoji);
        return Response.ok(ApiResponse.success(response, "Reaction removed successfully")).build();
    }

    @GET
    public Response getReactions(
            @Context SecurityContext securityContext,
            @PathParam("messageId") Long messageId) {

        User currentUser = getCurrentUser(securityContext);
        ReactionResponse response = reactionService.getReactions(currentUser.getId(), messageId);
        return Response.ok(ApiResponse.success(response, "Reactions retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

