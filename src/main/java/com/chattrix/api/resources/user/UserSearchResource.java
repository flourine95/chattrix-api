package com.chattrix.api.resources.user;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.user.UserSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/users/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class UserSearchResource {

    @Inject
    private UserSearchService userSearchService;
    @Inject
    private UserContext userContext;

    @GET
    public Response searchUsers(
            @QueryParam("query") @DefaultValue("") String query,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        var result = userSearchService.searchUsersWithCursor(
                userContext.getCurrentUserId(),
                query.trim(),
                cursor,
                limit
        );

        return Response.ok(ApiResponse.success(result, "Users found successfully")).build();
    }
}