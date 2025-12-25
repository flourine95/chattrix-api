package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.requests.CreateAnnouncementRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.AnnouncementService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/v1/conversations/{conversationId}/announcements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnnouncementResource {

    @Inject
    private AnnouncementService announcementService;

    @Inject
    private UserContext userContext;

    /**
     * Create announcement (admin only)
     */
    @POST
    @Secured
    public Response createAnnouncement(
            @PathParam("conversationId") Long conversationId,
            @Valid CreateAnnouncementRequest request) {

        Long userId = userContext.getCurrentUserId();
        MessageResponse response = announcementService.createAnnouncement(userId, conversationId, request);

        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(response, "Announcement created successfully"))
                .build();
    }

    /**
     * Get all announcements with cursor-based pagination
     */
    @GET
    @Secured
    public Response getAnnouncements(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Long userId = userContext.getCurrentUserId();
        CursorPaginatedResponse<MessageResponse> announcements = announcementService.getAnnouncements(userId, conversationId, cursor, limit);

        return Response.ok(ApiResponse.success(announcements, "Announcements retrieved successfully")).build();
    }

    /**
     * Delete announcement (admin only)
     */
    @DELETE
    @Path("/{messageId}")
    @Secured
    public Response deleteAnnouncement(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {

        Long userId = userContext.getCurrentUserId();
        announcementService.deleteAnnouncement(userId, conversationId, messageId);

        Map<String, Object> result = Map.of(
            "conversationId", conversationId,
            "messageId", messageId
        );

        return Response.ok(ApiResponse.success(result, "Announcement deleted successfully")).build();
    }
}
