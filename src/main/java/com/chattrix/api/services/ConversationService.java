package com.chattrix.api.services;

import com.chattrix.api.dto.requests.CreateConversationRequest;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConversationService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Transactional
    public Conversation createConversation(CreateConversationRequest request, UUID creatorId) {
        // Add creator to the list of participants if not already present
        Set<UUID> participantIds = new HashSet<>(request.getParticipantIds());
        participantIds.add(creatorId);

        if (participantIds.size() < 2) {
            throw new BadRequestException("A conversation needs at least two participants.");
        }

        // Fetch all user entities
        List<User> participants = userRepository.findByIds(participantIds);
        if (participants.size() != participantIds.size()) {
            throw new BadRequestException("One or more participant IDs are invalid.");
        }

        // Create Conversation
        Conversation conversation = new Conversation();
        conversation.setType(participants.size() > 2 ? Conversation.ConversationType.GROUP : Conversation.ConversationType.DIRECT);

        // Set name for group chat
        if (conversation.getType() == Conversation.ConversationType.GROUP) {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new BadRequestException("Group conversations must have a name.");
            }
            conversation.setName(request.getName());
        }

        // Create ConversationParticipants
        Set<ConversationParticipant> conversationParticipants = participants.stream().map(user -> {
            ConversationParticipant participant = new ConversationParticipant();
            participant.setUser(user);
            participant.setConversation(conversation);
            // For now, everyone is a member. Creator could be an admin in the future.
            participant.setRole(ConversationParticipant.Role.MEMBER);
            return participant;
        }).collect(Collectors.toSet());

        conversation.setParticipants(conversationParticipants);

        return conversationRepository.save(conversation);
    }

    public Conversation getConversationById(UUID conversationId) {
        return conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found."));
    }

    public List<Conversation> getUserConversations(UUID userId) {
        return conversationRepository.findByUserId(userId);
    }
}
