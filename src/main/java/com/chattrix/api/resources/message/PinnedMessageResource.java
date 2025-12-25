package com.chattrix.api.resources.message;

import com.chattrix.api.filters.Secured;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.security.UserContext;
import com.chattrix.api.services.message.PinnedMessageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/conversations/{conversationId}/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class PinnedMessageResource {
    
    @Inject
    private PinnedMessageService pinnedMessageService;
    
    @Inject
    private UserContext userContext;
    
    @POST
    @Path("/{messageId}/pin")
    public Response pinMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        var response = pinnedMessageService.pinMessage(
                userContext.getCurrentUserId(), 
                conversationId, 
                messageId
        );
        return Response.ok(ApiResponse.success(response, "Message pinned successfully")).build();
    }
    
    @DELETE
    @Path("/{messageId}/pin")
    public Response unpinMessage(
            @PathParam("conversationId") Long conversationId,
            @PathParam("messageId") Long messageId) {
        pinnedMessageService.unpinMessage(
                userContext.getCurrentUserId(),
                conversationId,
                messageId
        );
        return Response.ok(ApiResponse.success(null, "Message unpinned successfully")).build();
    }
    
    @GET
    @Path("/pinned")
    public Response getPinnedMessages(@PathParam("conversationId") Long conversationId) {
        var messages = pinnedMessageService.getPinnedMessages(
                userContext.getCurrentUserId(), 
                conversationId
        );
        return Response.ok(ApiResponse.success(messages, "Pinned messages retrieved successfully")).build();
    }
}
