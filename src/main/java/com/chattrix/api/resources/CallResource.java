package com.chattrix.api.resources;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.services.CallService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Path("/v1/calls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@NoArgsConstructor(force = true)
public class CallResource {

    private final CallService callService;

    private Long getUserId(SecurityContext ctx) {
        return ((UserPrincipal) ctx.getUserPrincipal()).getUserId();
    }

    @POST
    @Path("/initiate")
    public Response initiateCall(@Valid InitiateCallRequest request, @Context SecurityContext ctx) {
        var response = callService.initiateCall(getUserId(ctx), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Call initiated"))
                .build();
    }

    @POST
    @Path("/{callId}/accept")
    public Response acceptCall(@PathParam("callId") String callId, @Context SecurityContext ctx) {
        var response = callService.acceptCall(callId, getUserId(ctx));
        return Response.ok(ApiResponse.success(response, "Call accepted")).build();
    }

    @POST
    @Path("/{callId}/reject")
    public Response rejectCall(@PathParam("callId") String callId,
                               @Valid RejectCallRequest request,
                               @Context SecurityContext ctx) {
        var response = callService.rejectCall(callId, getUserId(ctx), request);
        return Response.ok(ApiResponse.success(response, "Call rejected")).build();
    }

    @POST
    @Path("/{callId}/end")
    public Response endCall(@PathParam("callId") String callId,
                            @Valid EndCallRequest request,
                            @Context SecurityContext ctx) {
        var response = callService.endCall(callId, getUserId(ctx), request);
        return Response.ok(ApiResponse.success(response, "Call ended")).build();
    }
}