package com.chattrix.api.resources.user;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.mappers.UserProfileMapper;
import com.chattrix.api.requests.UpdateUserProfileRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.UserProfileResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.user.UserProfileService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class UserProfileResource {

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private UserProfileMapper userProfileMapper;

    @Inject
    private UserContext userContext;

    @GET
    @Path("/me")
    public Response getMyProfile() {
        User user = userProfileService.getUserProfile(userContext.getCurrentUserId());
        UserProfileResponse response = userProfileMapper.toProfileResponse(user);
        return Response.ok(ApiResponse.success(response, "Profile retrieved successfully")).build();
    }

    @PUT
    @Path("/me")
    public Response updateMyProfile(@Valid UpdateUserProfileRequest request) {
        User updatedUser = userProfileService.updateUserProfile(userContext.getCurrentUserId(), request);
        UserProfileResponse response = userProfileMapper.toProfileResponse(updatedUser);
        return Response.ok(ApiResponse.success(response, "Profile updated successfully")).build();
    }

    @GET
    @Path("/{userId}")
    public Response getUserProfile(@PathParam("userId") Long userId) {
        User user = userProfileService.getUserProfile(userId);
        UserProfileResponse response = userProfileMapper.toProfileResponse(user);
        return Response.ok(ApiResponse.success(response, "User profile retrieved successfully")).build();
    }

    @GET
    @Path("/username/{username}")
    public Response getUserProfileByUsername(@PathParam("username") String username) {
        User user = userProfileService.getUserByUsername(username);
        UserProfileResponse response = userProfileMapper.toProfileResponse(user);
        return Response.ok(ApiResponse.success(response, "User profile retrieved successfully")).build();
    }
}

