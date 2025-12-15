package com.chattrix.api.resources.conversation;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.conversation.TypingIndicatorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/typing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class TypingIndicatorResource {

    @Inject
    private TypingIndicatorService typingIndicatorService;
    @Inject
    private UserContext userContext;

    @POST
    @Path("/start")
    public Response startTyping(@PathParam("conversationId") Long conversationId) {
        typingIndicatorService.startTyping(conversationId, userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(null, "Started typing")).build();
    }

    @POST
    @Path("/stop")
    public Response stopTyping(@PathParam("conversationId") Long conversationId) {
        typingIndicatorService.stopTyping(conversationId, userContext.getCurrentUserId());
        return Response.ok(ApiResponse.success(null, "Stopped typing")).build();
    }

    @GET
    @Path("/status") // Đổi path vì ID conversation đã ở trên URL gốc rồi
    public Response getTypingStatus(
            @PathParam("conversationId") Long conversationId,
            @QueryParam("excludeUserId") Long excludeUserId) {
        var typingUserIds = typingIndicatorService.getTypingUsersInConversation(conversationId, excludeUserId);
        return Response.ok(ApiResponse.success(typingUserIds, "Typing status retrieved")).build();
    }
}