package com.chattrix.api.resources;

import com.chattrix.api.entities.User;
import com.chattrix.api.filters.Secured;
import com.chattrix.api.filters.UserPrincipal;
import com.chattrix.api.requests.CreateConversationRequest;
import com.chattrix.api.responses.ApiResponse;
import com.chattrix.api.responses.ConversationMemberResponse;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.services.ConversationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ConversationResource {

    @Inject
    private ConversationService conversationService;

    @POST
    public Response createConversation(@Context SecurityContext securityContext, @Valid CreateConversationRequest request) {
        User currentUser = getCurrentUser(securityContext);
        ConversationResponse conversation = conversationService.createConversation(currentUser.getId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(conversation, "Conversation created successfully"))
                .build();
    }

    @GET
    public Response getConversations(@Context SecurityContext securityContext) {
        User currentUser = getCurrentUser(securityContext);
        List<ConversationResponse> conversations = conversationService.getConversations(currentUser.getId());
        return Response.ok(ApiResponse.success(conversations, "Conversations retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}")
    public Response getConversation(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        ConversationResponse conversation = conversationService.getConversation(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(conversation, "Conversation retrieved successfully")).build();
    }

    @GET
    @Path("/{conversationId}/members")
    public Response getConversationMembers(@Context SecurityContext securityContext, @PathParam("conversationId") Long conversationId) {
        User currentUser = getCurrentUser(securityContext);
        List<ConversationMemberResponse> members = conversationService.getConversationMembers(currentUser.getId(), conversationId);
        return Response.ok(ApiResponse.success(members, "Conversation members retrieved successfully")).build();
    }

    private User getCurrentUser(SecurityContext securityContext) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        return userPrincipal.user();
    }
}
