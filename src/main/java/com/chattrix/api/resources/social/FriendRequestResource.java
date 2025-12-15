package com.chattrix.api.resources.social;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.SendFriendRequestRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.social.FriendRequestService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/friend-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class FriendRequestResource {

    @Inject
    private FriendRequestService friendRequestService;
    @Inject
    private UserContext userContext;

    @POST
    public Response sendFriendRequest(@Valid SendFriendRequestRequest request) {
        var response = friendRequestService.sendFriendRequest(userContext.getCurrentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Friend request sent successfully")).build();
    }

    @POST
    @Path("/{requestId}/accept")
    public Response acceptFriendRequest(@PathParam("requestId") Long requestId) {
        var response = friendRequestService.acceptFriendRequest(userContext.getCurrentUserId(), requestId);
        return Response.ok(ApiResponse.success(response, "Friend request accepted successfully")).build();
    }

    @POST
    @Path("/{requestId}/reject")
    public Response rejectFriendRequest(@PathParam("requestId") Long requestId) {
        friendRequestService.rejectFriendRequest(userContext.getCurrentUserId(), requestId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{requestId}")
    public Response cancelFriendRequest(@PathParam("requestId") Long requestId) {
        friendRequestService.cancelFriendRequest(userContext.getCurrentUserId(), requestId);
        return Response.noContent().build();
    }

    @GET
    @Path("/received")
    public Response getPendingRequestsReceived() {
        var requests = friendRequestService.getPendingRequestsReceived(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(requests, "Pending requests retrieved successfully")).build();
    }

    @GET
    @Path("/sent")
    public Response getPendingRequestsSent() {
        var requests = friendRequestService.getPendingRequestsSent(userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(requests, "Sent requests retrieved successfully")).build();
    }
}