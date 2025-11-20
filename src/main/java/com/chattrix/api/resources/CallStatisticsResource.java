package com.chattrix.api.resources;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CallStatisticsResponse;
import com.chattrix.api.services.CallStatisticsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/calls/statistics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class CallStatisticsResource {

    @Inject
    private CallStatisticsService callStatisticsService;

    @GET
    public Response getStatistics(@QueryParam("period") @DefaultValue("week") String period,
                                   @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());
        
        CallStatisticsResponse statistics = callStatisticsService.getStatistics(userId, period);
        
        return Response.ok(ApiResponse.success(statistics, "Statistics retrieved successfully")).build();
    }
}
