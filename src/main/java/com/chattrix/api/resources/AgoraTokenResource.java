package com.chattrix.api.resources;

import com.chattrix.api.filters.CallRateLimited;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.AgoraRefreshTokenRequest;
import com.chattrix.api.requests.GenerateTokenRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.TokenResponse;
import com.chattrix.api.services.AgoraTokenService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/agora/token")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class AgoraTokenResource {

    @Inject
    private AgoraTokenService agoraTokenService;

    @POST
    @Path("/generate")
    @CallRateLimited(operation = CallRateLimited.OperationType.TOKEN_GENERATION)
    public Response generateToken(@Valid GenerateTokenRequest request,
                                  @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());

        // Set the user ID from the authenticated user
        request.setUserId(userId);

        TokenResponse tokenResponse = agoraTokenService.generateToken(request);

        return Response.ok(ApiResponse.success(tokenResponse, "Token generated successfully")).build();
    }

    @POST
    @Path("/refresh")
    @CallRateLimited(operation = CallRateLimited.OperationType.TOKEN_GENERATION)
    public Response refreshToken(@Valid AgoraRefreshTokenRequest request,
                                 @Context SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        String userId = String.valueOf(userPrincipal.getUserId());

        // Set the user ID from the authenticated user
        request.setUserId(userId);

        TokenResponse tokenResponse = agoraTokenService.refreshToken(request);

        return Response.ok(ApiResponse.success(tokenResponse, "Token refreshed successfully")).build();
    }
}

