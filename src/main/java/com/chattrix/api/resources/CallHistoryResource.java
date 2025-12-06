package com.chattrix.api.resources;

import com.chattrix.api.entities.CallHistoryStatus;
import com.chattrix.api.entities.CallType;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CallHistoryResponse;
import com.chattrix.api.responses.PaginatedResponse;
import com.chattrix.api.services.CallHistoryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/calls/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CallHistoryResource {

    @Inject
    private CallHistoryService callHistoryService;

    @GET
    public Response getCallHistory(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size,
                                    @QueryParam("callType") String callTypeStr,
                                    @QueryParam("status") String statusStr,
                                    @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        // Parse optional filters
        CallType callType = null;
        if (callTypeStr != null && !callTypeStr.isEmpty()) {
            try {
                callType = CallType.valueOf(callTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid call type: " + callTypeStr);
            }
        }
        
        CallHistoryStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = CallHistoryStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + statusStr);
            }
        }
        
        PaginatedResponse<CallHistoryResponse> response = callHistoryService.getCallHistory(
            userId,
            page,
            size,
            callType,
            status
        );
        
        return Response.ok(ApiResponse.success(response, "Call history retrieved successfully")).build();
    }

    @DELETE
    @Path("/{callId}")
    public Response deleteCallHistory(@PathParam("callId") String callId,
                                       @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        callHistoryService.deleteCallHistory(userId, callId);
        
        return Response.ok(ApiResponse.success(null, "Call history deleted successfully")).build();
    }
}
