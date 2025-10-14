package com.chattrix.api.resources;

import com.chattrix.api.dto.requests.ChangePasswordRequest;
import com.chattrix.api.dto.requests.LoginRequest;
import com.chattrix.api.dto.requests.RefreshTokenRequest;
import com.chattrix.api.dto.requests.RegisterRequest;
import com.chattrix.api.dto.responses.ApiResponse;
import com.chattrix.api.dto.responses.AuthResponse;
import com.chattrix.api.dto.responses.UserDto;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.services.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthService authService;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        authService.register(request);
        ApiResponse<Void> response = ApiResponse.success(null, "Registration successful");
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        ApiResponse<AuthResponse> response = ApiResponse.success(authResponse, "Login successful");
        return Response.ok(response).build();
    }

    @GET
    @Path("/me")
    @Secured
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        UserDto userDto = authService.getCurrentUser(username);
        ApiResponse<UserDto> response = ApiResponse.success(userDto, "User retrieved successfully");
        return Response.ok(response).build();
    }

    @POST
    @Path("/logout")
    @Secured
    public Response logout(@Context SecurityContext securityContext, @Context jakarta.ws.rs.core.HttpHeaders headers) {
        String username = securityContext.getUserPrincipal().getName();

        // Lấy token từ Authorization header
        String authorizationHeader = headers.getHeaderString("Authorization");
        String token = authorizationHeader.substring("Bearer ".length()).trim();

        authService.logout(username, token);
        ApiResponse<Void> response = ApiResponse.success(null, "Logged out from this device successfully");
        return Response.ok(response).build();
    }

    @POST
    @Path("/logout-all")
    @Secured
    public Response logoutAllDevices(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        authService.logoutAllDevices(username);
        ApiResponse<Void> response = ApiResponse.success(null, "Logged out from all devices successfully");
        return Response.ok(response).build();
    }

    @POST
    @Path("/refresh")
    public Response refreshToken(@Valid RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        ApiResponse<AuthResponse> response = ApiResponse.success(authResponse, "Token refreshed successfully");
        return Response.ok(response).build();
    }

    @PUT
    @Path("/change-password")
    @Secured
    public Response changePassword(@Valid ChangePasswordRequest request, @Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        authService.changePassword(username, request);
        ApiResponse<Void> response = ApiResponse.success(null, "Password changed successfully");
        return Response.ok(response).build();
    }
}
