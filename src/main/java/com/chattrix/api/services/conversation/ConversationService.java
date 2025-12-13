package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.ConversationSettings;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.ConversationSettingsRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class ConversationService {

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MessageMapper messageMapper;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private ConversationSettingsRepository settingsRepository;

    @Transactional
    public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
        // Validate request
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }

        // Validate DIRECT conversation
        if ("DIRECT".equals(request.getType())) {
            // Remove current user ID if exists in list
            long otherParticipantsCount = request.getParticipantIds().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .count();

            if (otherParticipantsCount != 1) {
                throw new BadRequestException("DIRECT conversation must have exactly 1 other participant. Found: " + otherParticipantsCount);
            }
        }

        // Validate GROUP conversation
        if ("GROUP".equals(request.getType())) {
            long otherParticipantsCount = request.getParticipantIds().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .count();

            if (otherParticipantsCount < 1) {
                throw new BadRequestException("GROUP conversation must have at least 1 other participant");
            }
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

    public List<ConversationMemberResponse> getConversationMembers(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Check if user is participant
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // Get all participant users
        List<User> users = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .toList();

        return messageMapper.toConversationMemberResponseList(users);
    }

    // ==================== NEW CHAT INFO METHODS ====================

    @Transactional
    public ConversationResponse updateConversation(Long userId, Long conversationId, UpdateConversationRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // Only GROUP conversations can be updated
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Only group conversations can be updated");
        }

        // Only ADMIN can update conversation
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can update conversation details");
        }

        // Update fields
        if (request.getName() != null) {
            conversation.setName(request.getName());
        }
        if (request.getAvatarUrl() != null) {
            conversation.setAvatarUrl(request.getAvatarUrl());
        }

        conversationRepository.save(conversation);
        return ConversationResponse.fromEntity(conversation);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // For now, we'll just remove the user from participants (soft delete for the user)
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participantRepository.delete(participant);

        // If no participants left, we could delete the conversation entirely
        long remainingParticipants = participantRepository.countByConversationId(conversationId);
        if (remainingParticipants == 0) {
            // Delete conversation (implementation depends on your requirements)
            // For now, we'll leave it as is
        }
    }

    @Transactional
    public void leaveConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only GROUP conversations can be left
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot leave a direct conversation");
        }

        // Check if user is participant
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this conversation"));

        participantRepository.delete(participant);

        // If no participants left, delete the conversation
        long remainingParticipants = participantRepository.countByConversationId(conversationId);
        if (remainingParticipants == 0) {
            // Delete conversation
        }
    }

    @Transactional
    public AddMembersResponse addMembers(Long userId, Long conversationId, AddMembersRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only GROUP conversations can have members added
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot add members to a direct conversation");
        }

        // Only ADMIN can add members
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can add members");
        }

        List<AddMembersResponse.AddedMember> addedMembers = new ArrayList<>();

        for (Long newUserId : request.getUserIds()) {
            // Check if user already exists
            if (participantRepository.isUserParticipant(conversationId, newUserId)) {
                continue; // Skip if already a member
            }

            User newUser = userRepository.findById(newUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + newUserId));

            ConversationParticipant newParticipant = new ConversationParticipant();
            newParticipant.setUser(newUser);
            newParticipant.setConversation(conversation);
            newParticipant.setRole(ConversationParticipant.Role.MEMBER);
            participantRepository.save(newParticipant);

            AddMembersResponse.AddedMember addedMember = new AddMembersResponse.AddedMember();
            addedMember.setUserId(newUser.getId());
            addedMember.setUsername(newUser.getUsername());
            addedMember.setFullName(newUser.getFullName());
            addedMember.setRole("MEMBER");
            addedMember.setJoinedAt(newParticipant.getJoinedAt());
            addedMembers.add(addedMember);
        }

        AddMembersResponse response = new AddMembersResponse();
        response.setConversationId(conversationId);
        response.setAddedMembers(addedMembers);
        return response;
    }

    @Transactional
    public void removeMember(Long userId, Long conversationId, Long memberUserId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only GROUP conversations can have members removed
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot remove members from a direct conversation");
        }

        // Only ADMIN can remove members
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can remove members");
        }

        // Cannot remove yourself (use leave instead)
        if (userId.equals(memberUserId)) {
            throw new BadRequestException("Use leave endpoint to remove yourself from the conversation");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this conversation"));

        participantRepository.delete(participant);
    }

    @Transactional
    public ConversationResponse.ParticipantResponse updateMemberRole(Long userId, Long conversationId, Long memberUserId, UpdateMemberRoleRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only GROUP conversations have roles
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot update roles in a direct conversation");
        }

        // Only ADMIN can update roles
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can update member roles");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this conversation"));

        ConversationParticipant.Role newRole = ConversationParticipant.Role.valueOf(request.getRole());
        participant.setRole(newRole);
        participantRepository.save(participant);

        ConversationResponse.ParticipantResponse response = new ConversationResponse.ParticipantResponse();
        response.setUserId(participant.getUser().getId());
        response.setUsername(participant.getUser().getUsername());
        response.setRole(newRole.name());
        return response;
    }

    // ==================== CONVERSATION SETTINGS ====================

    public ConversationSettingsResponse getConversationSettings(Long userId, Long conversationId) {
        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        ConversationSettingsResponse response = new ConversationSettingsResponse();
        response.setConversationId(conversationId);
        response.setIsMuted(settings.isMuted());
        response.setMutedUntil(settings.getMutedUntil());
        response.setIsBlocked(settings.isBlocked());
        response.setNotificationsEnabled(settings.isNotificationsEnabled());
        response.setCustomNickname(settings.getCustomNickname());
        response.setTheme(settings.getTheme());
        return response;
    }

    @Transactional
    public ConversationSettingsResponse updateConversationSettings(Long userId, Long conversationId, UpdateConversationSettingsRequest request) {
        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        if (request.getNotificationsEnabled() != null) {
            settings.setNotificationsEnabled(request.getNotificationsEnabled());
        }
        if (request.getCustomNickname() != null) {
            settings.setCustomNickname(request.getCustomNickname());
        }
        if (request.getTheme() != null) {
            settings.setTheme(request.getTheme());
        }

        settingsRepository.save(settings);

        ConversationSettingsResponse response = new ConversationSettingsResponse();
        response.setConversationId(conversationId);
        response.setIsMuted(settings.isMuted());
        response.setMutedUntil(settings.getMutedUntil());
        response.setIsBlocked(settings.isBlocked());
        response.setNotificationsEnabled(settings.isNotificationsEnabled());
        response.setCustomNickname(settings.getCustomNickname());
        response.setTheme(settings.getTheme());
        return response;
    }

    @Transactional
    public MuteConversationResponse muteConversation(Long userId, Long conversationId, MuteConversationRequest request) {
        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        Integer duration = request.getDuration();
        if (duration == null || duration == 0) {
            // Unmute
            settings.setMuted(false);
            settings.setMutedUntil(null);
        } else if (duration == -1) {
            // Mute indefinitely
            settings.setMuted(true);
            settings.setMutedUntil(null);
        } else {
            // Mute for duration
            settings.setMuted(true);
            settings.setMutedUntil(Instant.now().plusSeconds(duration));
        }

        settingsRepository.save(settings);

        MuteConversationResponse response = new MuteConversationResponse();
        response.setIsMuted(settings.isMuted());
        response.setMutedUntil(settings.getMutedUntil());
        return response;
    }

    @Transactional
    public BlockUserResponse blockUser(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only DIRECT conversations can be blocked
        if (conversation.getType() != Conversation.ConversationType.DIRECT) {
            throw new BadRequestException("Can only block users in direct conversations");
        }

        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        settings.setBlocked(true);
        settings.setBlockedAt(Instant.now());
        settingsRepository.save(settings);

        BlockUserResponse response = new BlockUserResponse();
        response.setIsBlocked(true);
        response.setBlockedAt(settings.getBlockedAt());
        return response;
    }

    @Transactional
    public BlockUserResponse unblockUser(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Only DIRECT conversations can be unblocked
        if (conversation.getType() != Conversation.ConversationType.DIRECT) {
            throw new BadRequestException("Can only unblock users in direct conversations");
        }

        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        settings.setBlocked(false);
        settings.setBlockedAt(null);
        settingsRepository.save(settings);

        BlockUserResponse response = new BlockUserResponse();
        response.setIsBlocked(false);
        response.setBlockedAt(null);
        return response;
    }

    private ConversationSettings createDefaultSettings(Long userId, Long conversationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        ConversationSettings settings = new ConversationSettings();
        settings.setUser(user);
        settings.setConversation(conversation);
        settings.setMuted(false);
        settings.setBlocked(false);
        settings.setNotificationsEnabled(true);
        return settingsRepository.save(settings);
    }
}

