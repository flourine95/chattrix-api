package com.chattrix.api.resources;

import com.chattrix.api.dto.responses.MessageResponse;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/conversations/{conversationId}/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MessageResource {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private TokenService tokenService;

    @GET
    public Response getMessages(
        @Context HttpHeaders headers,
        @PathParam("conversationId") UUID conversationId,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size) {

        // Authenticate user
        String token = extractTokenFromHeaders(headers);
        if (token == null || !tokenService.validateToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = tokenService.getUsernameFromToken(token);
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
            .orElseThrow(() -> new WebApplicationException("Conversation not found", Response.Status.NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
            .anyMatch(p -> p.getUser().getId().equals(currentUser.getId()));

        if (!isParticipant) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Get messages (need to add pagination to repository)
        List<Message> messages = messageRepository.findByConversationId(conversationId);
        List<MessageResponse> responses = messages.stream()
            .map(MessageResponse::fromEntity)
            .toList();

        return Response.ok(responses).build();
    }

    @GET
    @Path("/{messageId}")
    public Response getMessage(
        @Context HttpHeaders headers,
        @PathParam("conversationId") UUID conversationId,
        @PathParam("messageId") UUID messageId) {

        // Authenticate user
        String token = extractTokenFromHeaders(headers);
        if (token == null || !tokenService.validateToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = tokenService.getUsernameFromToken(token);
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
            .orElseThrow(() -> new WebApplicationException("Conversation not found", Response.Status.NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
            .anyMatch(p -> p.getUser().getId().equals(currentUser.getId()));

        if (!isParticipant) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // Get specific message
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new WebApplicationException("Message not found", Response.Status.NOT_FOUND));

        // Verify message belongs to this conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        MessageResponse response = MessageResponse.fromEntity(message);
        return Response.ok(response).build();
    }

    private String extractTokenFromHeaders(HttpHeaders headers) {
        String authHeader = headers.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
