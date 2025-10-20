package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.UserSearchResponse;
import com.chattrix.api.services.UserSearchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/users/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class UserSearchResource {

    @Inject
    private UserSearchService userSearchService;

    /**
     * Tìm kiếm người dùng để tạo đoạn chat
     * 
     * @param query Từ khóa tìm kiếm (username, email, hoặc full name)
     * @param limit Số lượng kết quả tối đa (mặc định: 20)
     * @return Danh sách người dùng phù hợp
     */
    @GET
    public Response searchUsers(
            @Context SecurityContext securityContext,
            @QueryParam("query") @DefaultValue("") String query,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        // Validate query
        if (query == null || query.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Search query cannot be empty", "INVALID_QUERY"))
                    .build();
        }

        // Validate limit
        if (limit < 1 || limit > 50) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Limit must be between 1 and 50", "INVALID_LIMIT"))
                    .build();
        }

        User currentUser = getCurrentUser(securityContext);
        List<UserSearchResponse> users = userSearchService.searchUsers(
                currentUser.getId(), 
                query.trim(), 
                limit
        );

        return Response.ok(ApiResponse.success(users, "Users found successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}

