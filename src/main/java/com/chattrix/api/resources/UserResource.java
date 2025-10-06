package com.chattrix.api.resources;

import com.chattrix.api.dto.responses.UserDto;
import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.repositories.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Secured
public class UserResource {

    @Inject
    private UserRepository userRepository;

    @GET
    @Path("/me")
    public Response getAuthenticatedUser(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found but was authenticated"));

        return Response.ok(UserDto.fromUser(user)).build();
    }
}

