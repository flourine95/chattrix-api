package com.chattrix.api.resources.user;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.SendBirthdayWishesRequest;
import com.chattrix.api.responses.BirthdayUserResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.birthday.BirthdayService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/v1/birthdays")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BirthdayResource {

    @Inject
    BirthdayService birthdayService;

    @Inject
    UserContext userContext;

    /**
     * Get users whose birthday is today
     * GET /v1/birthdays/today
     */
    @GET
    @Path("/today")
    @Secured
    public Response getUsersWithBirthdayToday() {
        List<BirthdayUserResponse> users = birthdayService.getUsersWithBirthdayToday();
        return Response.ok(users).build();
    }

    /**
     * Get users whose birthday is within the next N days (default 7 days)
     * GET /v1/birthdays/upcoming?days=7
     */
    @GET
    @Path("/upcoming")
    @Secured
    public Response getUsersWithUpcomingBirthdays(
            @QueryParam("days") @DefaultValue("7") int days) {
        
        if (days < 1 || days > 365) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Days must be between 1 and 365")
                    .build();
        }

        List<BirthdayUserResponse> users = birthdayService.getUsersWithUpcomingBirthdays(days);
        return Response.ok(users).build();
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
        
        return Response.ok()
                .entity("Birthday wishes sent successfully")
                .build();
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
            return Response.ok()
                    .entity("Birthday check triggered successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error: " + e.getMessage())
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
        return Response.ok(birthdayService.debugUserBirthday(userId)).build();
    }
}
