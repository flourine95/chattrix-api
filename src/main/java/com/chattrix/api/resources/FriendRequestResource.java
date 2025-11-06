package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.SendFriendRequestRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.FriendRequestResponse;
import com.chattrix.api.services.FriendRequestService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/friend-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class FriendRequestResource {

    @Inject
    private FriendRequestService friendRequestService;

    @POST
    @Path("/send")
    public Response sendFriendRequest(
            @Context SecurityContext securityContext,
            @Valid SendFriendRequestRequest request) {
        User currentUser = getCurrentUser(securityContext);
        FriendRequestResponse response = friendRequestService.sendFriendRequest(currentUser.getId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Friend request sent successfully"))
                .build();
    }

    @POST
    @Path("/{requestId}/accept")
    public Response acceptFriendRequest(
            @Context SecurityContext securityContext,
            @PathParam("requestId") Long requestId) {
        User currentUser = getCurrentUser(securityContext);
        FriendRequestResponse response = friendRequestService.acceptFriendRequest(currentUser.getId(), requestId);
        return Response.ok(ApiResponse.success(response, "Friend request accepted successfully")).build();
    }

    @POST
    @Path("/{requestId}/reject")
    public Response rejectFriendRequest(
            @Context SecurityContext securityContext,
            @PathParam("requestId") Long requestId) {
        User currentUser = getCurrentUser(securityContext);
        friendRequestService.rejectFriendRequest(currentUser.getId(), requestId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{requestId}/cancel")
    public Response cancelFriendRequest(
            @Context SecurityContext securityContext,
            @PathParam("requestId") Long requestId) {
        User currentUser = getCurrentUser(securityContext);
        friendRequestService.cancelFriendRequest(currentUser.getId(), requestId);
        return Response.noContent().build();
    }

    @GET
    @Path("/received")
    public Response getPendingRequestsReceived(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        List<FriendRequestResponse> requests = friendRequestService.getPendingRequestsReceived(currentUser.getId());
        return Response.ok(ApiResponse.success(requests, "Pending requests retrieved successfully")).build();
    }

    @GET
    @Path("/sent")
    public Response getPendingRequestsSent(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        List<FriendRequestResponse> requests = friendRequestService.getPendingRequestsSent(currentUser.getId());
        return Response.ok(ApiResponse.success(requests, "Sent requests retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

