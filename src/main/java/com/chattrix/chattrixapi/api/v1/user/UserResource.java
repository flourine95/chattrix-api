package com.chattrix.chattrixapi.api.v1.user;

import com.chattrix.chattrixapi.model.User;
import com.chattrix.chattrixapi.request.UserCreateRequest;
import com.chattrix.chattrixapi.response.UserResponse;
import com.chattrix.chattrixapi.service.UserService;
import com.chattrix.chattrixapi.validation.UserCreateValidator;
import jakarta.data.page.Page;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Map;

@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @POST
    @Transactional
    public Response store(@Valid UserCreateRequest request, @Context UriInfo uriInfo) {
        User created = userService.create(request);
        URI uri = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(uri).entity(UserResponse.fromEntity(created)).build();
    }

    @GET
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("id") String sort,
            @QueryParam("asc") @DefaultValue("true") boolean asc,
            @QueryParam("q") String keyword,
            @QueryParam("status") String status
    ) {
        Page<User> pageResult;
        if (keyword != null && !keyword.isBlank()) {
            pageResult = userService.search(keyword, page, size, sort, asc);
        } else if (status != null && !status.isBlank()) {
            pageResult = userService.filterByStatus(status, page, size, sort, asc);
        } else {
            pageResult = userService.getAll(page, size, sort, asc);
        }
            return Response.ok(Map.of(
                "content", pageResult.content(),
                "page", page,
                "size", size,
                "totalElements", pageResult.totalElements(),
                "totalPages", pageResult.totalPages()
        )).build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return userService.getUserById(id)
                .map(user -> Response.ok(UserResponse.fromEntity(user)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, UserCreateRequest request) {
        return userService.getUserById(id).map(existing -> {
            existing.setUsername(request.getUsername());
            existing.setEmail(request.getEmail());
            existing.setFullName(request.getFullName());
            existing.setAvatarUrl(request.getAvatarUrl());
            if (request.getPassword() != null) {
                existing.setPassword("{bcrypt}" + request.getPassword());
            }
            User updated = userService.update(existing);
            return Response.ok(UserResponse.fromEntity(updated)).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        userService.delete(id);
        return Response.noContent().build();
    }
}
