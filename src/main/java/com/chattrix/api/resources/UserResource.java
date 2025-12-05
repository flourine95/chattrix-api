package com.chattrix.api.resources;

import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.services.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("v1/users")
public class UserResource {

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllUsers() {
        List<UserResponse> users = userService.findAllUsers();

        return Response.ok(ApiResponse.success(users, "Get users successful")).build();
    }
}