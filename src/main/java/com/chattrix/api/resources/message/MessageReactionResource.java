package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.AddReactionRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.ReactionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/messages/{messageId}/reactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class MessageReactionResource {

    @Inject private ReactionService reactionService;
    @Inject private UserContext userContext;

    @POST
    public Response addReaction(
            @PathParam("messageId") Long messageId,
            @Valid AddReactionRequest request) {
        var response = reactionService.addReaction(userContext.getCurrentUserId(), messageId, request.emoji());
        return Response.ok(ApiResponse.success(response, "Reaction toggled successfully")).build();
    }

    @DELETE
    @Path("/{emoji}")
    public Response removeReaction(
            @PathParam("messageId") Long messageId,
            @PathParam("emoji") String emoji) {
        var response = reactionService.removeReaction(userContext.getCurrentUserId(), messageId, emoji);
        return Response.ok(ApiResponse.success(response, "Reaction removed successfully")).build();
    }

    @GET
    public Response getReactions(@PathParam("messageId") Long messageId) {
        var response = reactionService.getReactions(userContext.getCurrentUserId(), messageId);
        return Response.ok(ApiResponse.success(response, "Reactions retrieved successfully")).build();
    }
}
