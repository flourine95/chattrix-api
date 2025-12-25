package com.chattrix.api.resources.user;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.SendBirthdayWishesRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.BirthdayUserResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.birthday.BirthdayService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/v1/birthdays")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BirthdayResource {

    @Inject
    BirthdayService birthdayService;

    @Inject
    UserContext userContext;

    /**
     * Get users whose birthday is today (from contacts and conversations)
     * GET /v1/birthdays/today
     */
    @GET
    @Path("/today")
    @Secured
    public Response getUsersWithBirthdayToday() {
        Long currentUserId = userContext.getCurrentUserId();
        List<BirthdayUserResponse> users = birthdayService.getUsersWithBirthdayToday(currentUserId);
        return Response.ok(ApiResponse.success(users, "Users with birthday today retrieved successfully")).build();
    }

    /**
     * Get users whose birthday is within the next N days (from contacts and conversations)
     * GET /v1/birthdays/upcoming?days=7
     */
    @GET
    @Path("/upcoming")
    @Secured
    public Response getUsersWithUpcomingBirthdays(
            @QueryParam("days") @DefaultValue("7") int days) {

        if (days < 1 || days > 365) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("BAD_REQUEST", "Days must be between 1 and 365", null))
                    .build();
        }

        Long currentUserId = userContext.getCurrentUserId();
        List<BirthdayUserResponse> users = birthdayService.getUsersWithUpcomingBirthdays(days, currentUserId);
        return Response.ok(ApiResponse.success(users, "Upcoming birthdays retrieved successfully")).build();
    }

    /**
     * Get birthdays in a specific conversation
     * GET /v1/birthdays/conversation/{conversationId}?days=30
     */
    @GET
    @Path("/conversation/{conversationId}")
    @Secured
    public Response getBirthdaysInConversation(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("days") @DefaultValue("30") int days) {

        if (days < 1 || days > 365) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("BAD_REQUEST", "Days must be between 1 and 365", null))
                    .build();
        }

        List<BirthdayUserResponse> users = birthdayService.getBirthdaysInConversation(conversationId, days);
        return Response.ok(ApiResponse.success(users, "Birthdays in conversation retrieved successfully")).build();
    }

    /**
     * Send birthday wishes to a user in specified conversations
     * POST /v1/birthdays/send-wishes
     * 
     * Request body:
     * {
     *   "userId": 123,
     *   "conversationIds": [1, 2, 3],
     *   "customMessage": "Happy birthday!" (optional)
     * }
     */
    @POST
    @Path("/send-wishes")
    @Secured
    public Response sendBirthdayWishes(@Valid SendBirthdayWishesRequest request) {
        Long senderId = userContext.getCurrentUserId();
        
        birthdayService.sendBirthdayWishes(request, senderId);
        
        Map<String, Object> result = Map.of(
            "userId", request.getUserId(),
            "conversationCount", request.getConversationIds().size()
        );
        
        return Response.ok(ApiResponse.success(result, "Birthday wishes sent successfully")).build();
    }

    /**
     * DEMO/TEST ONLY: Manually trigger birthday check
     * DELETE THIS IN PRODUCTION!
     * 
     * GET /v1/birthdays/trigger-check
     */
    @GET
    @Path("/trigger-check")
    @Secured
    public Response triggerBirthdayCheck() {
        try {
            birthdayService.checkAndSendBirthdayWishes();
            Map<String, String> result = Map.of("status", "triggered");
            return Response.ok(ApiResponse.success(result, "Birthday check triggered successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("INTERNAL_ERROR", "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * DEBUG ONLY: Check timezone handling for a specific user
     * GET /v1/birthdays/debug/{userId}
     */
    @GET
    @Path("/debug/{userId}")
    @Secured
    public Response debugUserBirthday(@PathParam("userId") Long userId) {
        Map<String, Object> debugInfo = birthdayService.debugUserBirthday(userId);
        return Response.ok(ApiResponse.success(debugInfo, "Debug information retrieved successfully")).build();
    }
}
