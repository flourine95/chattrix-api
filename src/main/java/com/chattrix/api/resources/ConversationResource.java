package com.chattrix.api.resources;

import com.chattrix.api.dto.requests.CreateConversationRequest;
import com.chattrix.api.dto.responses.ConversationResponse;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.TokenService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Path("/v1/conversations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConversationResource {

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private TokenService tokenService;

    @POST
    @Transactional
    public Response createConversation(@Context HttpHeaders headers, CreateConversationRequest request) {
        // Get current user from JWT token
        String token = extractTokenFromHeaders(headers);
        if (token == null || !tokenService.validateToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = tokenService.getUsernameFromToken(token);
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        // Validate request
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("At least one participant is required")
                .build();
        }

        // Create conversation
        Conversation conversation = new Conversation();
        conversation.setName(request.getName());
        conversation.setType("GROUP".equals(request.getType()) ? Conversation.ConversationType.GROUP : Conversation.ConversationType.DIRECT);

        // Add participants
        Set<ConversationParticipant> participants = new HashSet<>();

        // Add current user as admin/member
        ConversationParticipant currentUserParticipant = new ConversationParticipant();
        currentUserParticipant.setUser(currentUser);
        currentUserParticipant.setConversation(conversation);
        currentUserParticipant.setRole(ConversationParticipant.Role.ADMIN);
        participants.add(currentUserParticipant);

        // Add other participants
        for (UUID participantId : request.getParticipantIds()) {
            if (!participantId.equals(currentUser.getId())) { // Don't add current user twice
                User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new WebApplicationException("Participant not found: " + participantId, Response.Status.BAD_REQUEST));

                ConversationParticipant conversationParticipant = new ConversationParticipant();
                conversationParticipant.setUser(participant);
                conversationParticipant.setConversation(conversation);
                conversationParticipant.setRole(ConversationParticipant.Role.MEMBER);
                participants.add(conversationParticipant);
            }
        }

        conversation.setParticipants(participants);
        conversationRepository.save(conversation);

        ConversationResponse response = ConversationResponse.fromEntity(conversation);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public Response getConversations(@Context HttpHeaders headers) {
        // Get current user from JWT token
        String token = extractTokenFromHeaders(headers);
        if (token == null || !tokenService.validateToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = tokenService.getUsernameFromToken(token);
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        List<Conversation> conversations = conversationRepository.findByUserId(currentUser.getId());
        List<ConversationResponse> responses = conversations.stream()
            .map(ConversationResponse::fromEntity)
            .toList();

        return Response.ok(responses).build();
    }

    @GET
    @Path("/{conversationId}")
    public Response getConversation(@Context HttpHeaders headers, @PathParam("conversationId") UUID conversationId) {
        // Get current user from JWT token
        String token = extractTokenFromHeaders(headers);
        if (token == null || !tokenService.validateToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = tokenService.getUsernameFromToken(token);
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
            .orElseThrow(() -> new WebApplicationException("Conversation not found", Response.Status.NOT_FOUND));

        // Check if user is participant
        boolean isParticipant = conversation.getParticipants().stream()
            .anyMatch(p -> p.getUser().getId().equals(currentUser.getId()));

        if (!isParticipant) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ConversationResponse response = ConversationResponse.fromEntity(conversation);
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
