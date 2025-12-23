package com.chattrix.api.resources.poll;

import com.chattrix.api.filters.RateLimited;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreatePollRequest;
import com.chattrix.api.requests.VotePollRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.PaginatedResponse;
import com.chattrix.api.responses.PollResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.poll.PollService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/polls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PollResource {
    @Inject
    private PollService pollService;

    @Inject
    private UserContext userContext;

    @POST
    @Path("/conversation/{conversationId}")
    @Secured
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

    @POST
    @Path("/{pollId}/vote")
    @Secured
    @RateLimited
    public Response vote(
            @PathParam("pollId") Long pollId,
            @Valid VotePollRequest request
    ) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.vote(pollId, request, userId);
        return Response.ok(ApiResponse.success(poll, "Vote recorded successfully")).build();
    }

    @DELETE
    @Path("/{pollId}/vote")
    @Secured
    @RateLimited
    public Response removeVote(@PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.removeVote(pollId, userId);
        return Response.ok(ApiResponse.success(poll, "Vote removed successfully")).build();
    }

    @GET
    @Path("/{pollId}")
    @Secured
    public Response getPoll(@PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.getPoll(pollId, userId);
        return Response.ok(ApiResponse.success(poll, "Poll retrieved successfully")).build();
    }

    @GET
    @Path("/conversation/{conversationId}")
    @Secured
    public Response getConversationPolls(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        Long userId = userContext.getCurrentUserId();
        PaginatedResponse<PollResponse> polls = pollService.getConversationPolls(conversationId, userId, page, size);
        return Response.ok(ApiResponse.success(polls, "Polls retrieved successfully")).build();
    }

    @POST
    @Path("/{pollId}/close")
    @Secured
    @RateLimited
    public Response closePoll(@PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        PollResponse poll = pollService.closePoll(pollId, userId);
        return Response.ok(ApiResponse.success(poll, "Poll closed successfully")).build();
    }

    @DELETE
    @Path("/{pollId}")
    @Secured
    @RateLimited
    public Response deletePoll(@PathParam("pollId") Long pollId) {
        Long userId = userContext.getCurrentUserId();
        pollService.deletePoll(pollId, userId);
        return Response.ok(ApiResponse.success("Poll deleted successfully")).build();
    }
}
