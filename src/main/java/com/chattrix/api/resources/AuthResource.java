package com.chattrix.api.resources;

import com.chattrix.api.dto.requests.LoginRequest;
import com.chattrix.api.dto.requests.RegisterRequest;
import com.chattrix.api.dto.responses.AuthResponse;
import com.chattrix.api.services.AuthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthService authService;

    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        authService.register(request);
        return Response.status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return Response.ok(authResponse).build();
    }
}

