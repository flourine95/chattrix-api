package com.chattrix.api.resources.call;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.EndCallRequest;
import com.chattrix.api.requests.InitiateCallRequest;
import com.chattrix.api.requests.RejectCallRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.call.CallService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/calls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CallResource {

    @Inject
    private CallService callService;

    @Inject
    private UserContext userContext;

    @POST
    @Path("/initiate")
    public Response initiateCall(@Valid InitiateCallRequest request) {
        var response = callService.initiateCall(userContext.getCurrentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Call initiated"))
                .build();
    }

    @POST
    @Path("/{callId}/accept")
    public Response acceptCall(@PathParam("callId") String callId) {
        var response = callService.acceptCall(callId, userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(response, "Call accepted")).build();
    }

    @POST
    @Path("/{callId}/join")
    public Response joinCall(@PathParam("callId") String callId) {
        var response = callService.acceptCall(callId, userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(response, "Joined call")).build();
    }

    @GET
    @Path("/active/{conversationId}")
    public Response getActiveCall(@PathParam("conversationId") Long conversationId) {
        var response = callService.getActiveCall(conversationId);
        return Response.ok(ApiResponse.success(response, "Active call retrieved")).build();
    }

    @POST
    @Path("/{callId}/reject")
    public Response rejectCall(@PathParam("callId") String callId,
                               @Valid RejectCallRequest request) {
        var response = callService.rejectCall(callId, userContext.getCurrentUserId(), request);
        return Response.ok(ApiResponse.success(response, "Call rejected")).build();
    }

    @POST
    @Path("/{callId}/end")
    public Response endCall(@PathParam("callId") String callId,
                            @Valid EndCallRequest request) {
        var response = callService.endCall(callId, userContext.getCurrentUserId(), request);
        return Response.ok(ApiResponse.success(response, "Call ended")).build();
    }

    @GET
    @Path("/history")
    public Response getCallHistory(
            @QueryParam("limit") @DefaultValue("50") int limit,
            @QueryParam("status") String status) {
        var response = callService.getCallHistory(userContext.getCurrentUserId(), limit, status);
        return Response.ok(ApiResponse.success(response, "Call history retrieved")).build();
    }
}
