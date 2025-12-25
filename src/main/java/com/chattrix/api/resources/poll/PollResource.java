package com.chattrix.api.resources.poll;

import com.chattrix.api.filters.RateLimited;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreatePollRequest;
import com.chattrix.api.requests.VotePollRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.PollResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.poll.PollService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/polls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class PollResource {
    @Inject
    private PollService pollService;

    @Inject
    private UserContext userContext;

    @POST
    @RateLimited
    public Response createPoll(
            @PathParam("conversationId") Long conversationId,
            @Valid CreatePollRequest request
    ) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.createPoll(conversationId, request, userId);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(poll, "Poll created successfully"))
                .build();
    }

    @GET
    public Response getConversationPolls(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit
    ) {
        Long userId = userContext.getCurrentUserId();
        CursorPaginatedResponse<PollResponse> polls = pollService.getConversationPolls(conversationId, userId, cursor, limit);
        return Response.ok(ApiResponse.success(polls, "Polls retrieved successfully")).build();
    }

    @GET
    @Path("/{pollId}")
    public Response getPoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.getPoll(conversationId, pollId, userId);
        return Response.ok(ApiResponse.success(poll, "Poll retrieved successfully")).build();
    }

    @POST
    @Path("/{pollId}/vote")
    @RateLimited
    public Response vote(
            @PathParam("conversationId") Long conversationId,
            @PathParam("pollId") Long pollId,
            @Valid VotePollRequest request
    ) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.vote(conversationId, pollId, request, userId);
        return Response.ok(ApiResponse.success(poll, "Vote recorded successfully")).build();
    }

    @DELETE
    @Path("/{pollId}/vote")
    @RateLimited
    public Response removeVote(
            @PathParam("conversationId") Long conversationId,
            @PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.removeVote(conversationId, pollId, userId);
        return Response.ok(ApiResponse.success(poll, "Vote removed successfully")).build();
    }

    @POST
    @Path("/{pollId}/close")
    @RateLimited
    public Response closePoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.closePoll(conversationId, pollId, userId);
        return Response.ok(ApiResponse.success(poll, "Poll closed successfully")).build();
    }

    @DELETE
    @Path("/{pollId}")
    @RateLimited
    public Response deletePoll(
            @PathParam("conversationId") Long conversationId,
            @PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        pollService.deletePoll(conversationId, pollId, userId);
        return Response.ok(ApiResponse.success("Poll deleted successfully")).build();
    }
}
