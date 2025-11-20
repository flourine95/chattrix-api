package com.chattrix.api.resources;

import com.chattrix.api.filters.CallRateLimited;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.AcceptCallRequest;
import com.chattrix.api.requests.EndCallRequest;
import com.chattrix.api.requests.InitiateCallRequest;
import com.chattrix.api.requests.RejectCallRequest;
import com.chattrix.api.requests.UpdateCallStatusRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CallResponse;
import com.chattrix.api.services.CallService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/calls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CallResource {

    @Inject
    private CallService callService;

    @POST
    @Path("/initiate")
    @CallRateLimited(operation = CallRateLimited.OperationType.CALL_INITIATION)
    public Response initiateCall(@Valid InitiateCallRequest request,
                                  @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String callerId = String.valueOf(userPrincipal.getUserId());
        
        CallResponse callResponse = callService.initiateCall(callerId, request);
        
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(callResponse, "Call initiated successfully"))
                .build();
    }

    @POST
    @Path("/{callId}/accept")
    public Response acceptCall(@PathParam("callId") String callId,
                                @Valid AcceptCallRequest request,
                                @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        CallResponse callResponse = callService.acceptCall(callId, userId);
        
        return Response.ok(ApiResponse.success(callResponse, "Call accepted successfully")).build();
    }

    @POST
    @Path("/{callId}/reject")
    public Response rejectCall(@PathParam("callId") String callId,
                                @Valid RejectCallRequest request,
                                @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        // Set the user ID from the authenticated user
        request.setUserId(userId);
        
        CallResponse callResponse = callService.rejectCall(callId, request);
        
        return Response.ok(ApiResponse.success(callResponse, "Call rejected successfully")).build();
    }

    @POST
    @Path("/{callId}/end")
    public Response endCall(@PathParam("callId") String callId,
                            @Valid EndCallRequest request,
                            @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        // Set the user ID from the authenticated user
        request.setUserId(userId);
        
        CallResponse callResponse = callService.endCall(callId, request);
        
        return Response.ok(ApiResponse.success(callResponse, "Call ended successfully")).build();
    }

    @PATCH
    @Path("/{callId}/status")
    public Response updateCallStatus(@PathParam("callId") String callId,
                                      @Valid UpdateCallStatusRequest request,
                                      @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        // Set the user ID from the authenticated user
        request.setUserId(userId);
        
        CallResponse callResponse = callService.updateCallStatus(callId, request);
        
        return Response.ok(ApiResponse.success(callResponse, "Call status updated successfully")).build();
    }

    @GET
    @Path("/{callId}")
    public Response getCallDetails(@PathParam("callId") String callId,
                                    @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        CallResponse callResponse = callService.getCallDetails(callId, userId);
        
        return Response.ok(ApiResponse.success(callResponse, "Call details retrieved successfully")).build();
    }
}
