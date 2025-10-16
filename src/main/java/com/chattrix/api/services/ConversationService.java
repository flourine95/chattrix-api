package com.chattrix.api.services;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.CreateConversationRequest;
import com.chattrix.api.responses.ConversationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ConversationService {

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    @Transactional
    public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
        // Validate request
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create conversation
        Conversation conversation = new Conversation();
        conversation.setName(request.getName());
        conversation.setType("GROUP".equals(request.getType()) ?
                Conversation.ConversationType.GROUP : Conversation.ConversationType.DIRECT);

        // Add participants
        Set<ConversationParticipant> participants = new HashSet<>();

        // Add current user as admin
        ConversationParticipant currentUserParticipant = new ConversationParticipant();
        currentUserParticipant.setUser(currentUser);
        currentUserParticipant.setConversation(conversation);
        currentUserParticipant.setRole(ConversationParticipant.Role.ADMIN);
        participants.add(currentUserParticipant);

        // Add other participants
        for (Long participantId : request.getParticipantIds()) {
            if (!participantId.equals(currentUserId)) {
                User participant = userRepository.findById(participantId)
                        .orElseThrow(() -> new BadRequestException("Participant not found: " + participantId));

                ConversationParticipant conversationParticipant = new ConversationParticipant();
                conversationParticipant.setUser(participant);
                conversationParticipant.setConversation(conversation);
                conversationParticipant.setRole(ConversationParticipant.Role.MEMBER);
                participants.add(conversationParticipant);
            }
        }

        conversation.setParticipants(participants);
        conversationRepository.save(conversation);

        return ConversationResponse.fromEntity(conversation);
    }

    public List<ConversationResponse> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        return conversations.stream()
                .map(ConversationResponse::fromEntity)
                .toList();
    }

    public ConversationResponse getConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Check if user is participant
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        return ConversationResponse.fromEntity(conversation);
    }
}

