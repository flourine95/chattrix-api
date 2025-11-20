package com.chattrix.api.resources;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.ReportQualityRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.services.CallQualityService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/calls/{callId}/quality")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CallQualityResource {

    @Inject
    private CallQualityService callQualityService;

    @POST
    public Response reportQuality(@PathParam("callId") String callId,
                                   @Valid ReportQualityRequest request,
                                   @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        Long userId = userPrincipal.getUserId();
        
        // Set the user ID from the authenticated user
        request.setUserId(String.valueOf(userId));
        
        callQualityService.reportQuality(callId, userId, request);
        
        return Response.ok(ApiResponse.success(null, "Quality metrics reported successfully")).build();
    }
}
