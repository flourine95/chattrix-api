package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.ConversationSettings;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.mappers.ConversationMapper;
import com.chattrix.api.mappers.UserMapper;
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
    private UserMapper userMapper;
    @Inject
    private ConversationMapper conversationMapper;
    @Inject
    private ConversationParticipantRepository participantRepository;
    @Inject
    private ConversationSettingsRepository settingsRepository;

    @Transactional
    public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw new BadRequestException("At least one participant is required");
        }

        if ("DIRECT".equals(request.getType())) {
            long count = request.getParticipantIds().stream().filter(id -> !id.equals(currentUserId)).count();
            if (count != 1) throw new BadRequestException("DIRECT conversation must have exactly 1 other participant.");
        }

        if ("GROUP".equals(request.getType())) {
            long count = request.getParticipantIds().stream().filter(id -> !id.equals(currentUserId)).count();
            if (count < 1) throw new BadRequestException("GROUP conversation must have at least 1 other participant");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation = Conversation.builder()
                .name(request.getName())
                .type("GROUP".equals(request.getType()) ? Conversation.ConversationType.GROUP : Conversation.ConversationType.DIRECT)
                .build();

        Set<ConversationParticipant> participants = new HashSet<>();

        participants.add(ConversationParticipant.builder()
                .user(currentUser)
                .conversation(conversation)
                .role(ConversationParticipant.Role.ADMIN)
                .build());

        for (Long participantId : request.getParticipantIds()) {
            if (!participantId.equals(currentUserId)) {
                User participant = userRepository.findById(participantId)
                        .orElseThrow(() -> new BadRequestException("Participant not found: " + participantId));

                participants.add(ConversationParticipant.builder()
                        .user(participant)
                        .conversation(conversation)
                        .role(ConversationParticipant.Role.MEMBER)
                        .build());
            }
        }

        conversation.setParticipants(participants);
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }

    public List<ConversationResponse> getConversations(Long userId, String filter) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);

        // Apply filter
        if (filter != null) {
            switch (filter.toLowerCase()) {
                case "unread":
                    // Filter conversations with unread messages
                    conversations = conversations.stream()
                            .filter(conv -> conv.getParticipants().stream()
                                    .anyMatch(p -> p.getUser().getId().equals(userId) && p.getUnreadCount() > 0))
                            .toList();
                    break;
                case "group":
                    // Filter only GROUP conversations
                    conversations = conversations.stream()
                            .filter(conv -> conv.getType() == Conversation.ConversationType.GROUP)
                            .toList();
                    break;
                case "all":
                default:
                    // Return all conversations (no filtering needed)
                    break;
            }
        }

        return conversationMapper.toResponseList(conversations);
    }

    public ConversationResponse getConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        validateParticipant(conversation, userId);
        return conversationMapper.toResponse(conversation);
    }

    public List<ConversationMemberResponse> getConversationMembers(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        validateParticipant(conversation, userId);

        List<User> users = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .toList();
        return userMapper.toConversationMemberResponseList(users);
    }


    @Transactional
    public ConversationResponse updateConversation(Long userId, Long conversationId, UpdateConversationRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateGroupAdmin(conversationId, userId);

        if (request.getName() != null) conversation.setName(request.getName());
        if (request.getAvatarUrl() != null) conversation.setAvatarUrl(request.getAvatarUrl());

        conversationRepository.save(conversation);
        return conversationMapper.toResponse(conversation);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participantRepository.delete(participant);
    }

    @Transactional
    public void leaveConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Cannot leave a direct conversation");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this conversation"));

        participantRepository.delete(participant);
    }

    @Transactional
    public AddMembersResponse addMembers(Long userId, Long conversationId, AddMembersRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateGroupAdmin(conversationId, userId);

        List<AddMembersResponse.AddedMember> addedMembers = new ArrayList<>();

        for (Long newUserId : request.getUserIds()) {
            if (participantRepository.isUserParticipant(conversationId, newUserId)) continue;

            User newUser = userRepository.findById(newUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + newUserId));

            ConversationParticipant newParticipant = ConversationParticipant.builder()
                    .user(newUser)
                    .conversation(conversation)
                    .role(ConversationParticipant.Role.MEMBER)
                    .build();
            participantRepository.save(newParticipant);

            addedMembers.add(AddMembersResponse.AddedMember.builder()
                    .userId(newUser.getId())
                    .username(newUser.getUsername())
                    .fullName(newUser.getFullName())
                    .role("MEMBER")
                    .joinedAt(newParticipant.getJoinedAt())
                    .build());
        }

        return AddMembersResponse.builder()
                .conversationId(conversationId)
                .addedMembers(addedMembers)
                .build();
    }

    @Transactional
    public void removeMember(Long userId, Long conversationId, Long memberUserId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateGroupAdmin(conversationId, userId);

        if (userId.equals(memberUserId)) {
            throw new BadRequestException("Use leave endpoint to remove yourself from the conversation");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        participantRepository.delete(participant);
    }

    @Transactional
    public ConversationResponse.ParticipantResponse updateMemberRole(Long userId, Long conversationId, Long memberUserId, UpdateMemberRoleRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        validateGroupAdmin(conversationId, userId);

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        participant.setRole(ConversationParticipant.Role.valueOf(request.getRole()));
        participantRepository.save(participant);

        return ConversationResponse.ParticipantResponse.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .role(participant.getRole().name())
                .build();
    }


    public ConversationSettingsResponse getConversationSettings(Long userId, Long conversationId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        return ConversationSettingsResponse.builder()
                .conversationId(conversationId)
                .isMuted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .isBlocked(settings.isBlocked())
                .notificationsEnabled(settings.isNotificationsEnabled())
                .customNickname(settings.getCustomNickname())
                .theme(settings.getTheme())
                .build();
    }

    @Transactional
    public ConversationSettingsResponse updateConversationSettings(Long userId, Long conversationId, UpdateConversationSettingsRequest request) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        if (request.getNotificationsEnabled() != null)
            settings.setNotificationsEnabled(request.getNotificationsEnabled());
        if (request.getCustomNickname() != null) settings.setCustomNickname(request.getCustomNickname());
        if (request.getTheme() != null) settings.setTheme(request.getTheme());

        settingsRepository.save(settings);

        return ConversationSettingsResponse.builder()
                .conversationId(conversationId)
                .isMuted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .isBlocked(settings.isBlocked())
                .notificationsEnabled(settings.isNotificationsEnabled())
                .customNickname(settings.getCustomNickname())
                .theme(settings.getTheme())
                .build();
    }

    @Transactional
    public MuteConversationResponse muteConversation(Long userId, Long conversationId, MuteConversationRequest request) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        Integer duration = request.getDuration();
        if (duration == null || duration == 0) {
            settings.setMuted(false);
            settings.setMutedUntil(null);
        } else if (duration == -1) {
            settings.setMuted(true);
            settings.setMutedUntil(null);
        } else {
            settings.setMuted(true);
            settings.setMutedUntil(Instant.now().plusSeconds(duration));
        }
        settingsRepository.save(settings);

        return MuteConversationResponse.builder()
                .isMuted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .build();
    }

    @Transactional
    public BlockUserResponse blockUser(Long userId, Long conversationId) {
        validateDirectConversation(conversationId);
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("Access denied");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        settings.setBlocked(true);
        settings.setBlockedAt(Instant.now());
        settingsRepository.save(settings);

        return BlockUserResponse.builder()
                .isBlocked(true)
                .blockedAt(settings.getBlockedAt())
                .build();
    }

    @Transactional
    public BlockUserResponse unblockUser(Long userId, Long conversationId) {
        validateDirectConversation(conversationId);
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("Access denied");
        }

        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conversationId)
                .orElseGet(() -> createDefaultSettings(userId, conversationId));

        settings.setBlocked(false);
        settings.setBlockedAt(null);
        settingsRepository.save(settings);

        return BlockUserResponse.builder()
                .isBlocked(false)
                .blockedAt(null)
                .build();
    }

    private ConversationSettings createDefaultSettings(Long userId, Long conversationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        ConversationSettings settings = ConversationSettings.builder()
                .user(user)
                .conversation(conversation)
                .build();
        return settingsRepository.save(settings);
    }

    private void validateParticipant(Conversation conversation, Long userId) {
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (!isParticipant) throw new BadRequestException("You do not have access to this conversation");
    }

    private void validateGroupAdmin(Long conversationId, Long userId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("Access denied");
        }
        Conversation c = conversationRepository.findById(conversationId).orElseThrow();
        if (c.getType() != Conversation.ConversationType.GROUP) {
            throw new BadRequestException("Only group conversations support this action");
        }
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw new BadRequestException("Only admins can perform this action");
        }
    }

    private void validateDirectConversation(Long conversationId) {
        Conversation c = conversationRepository.findById(conversationId).orElseThrow();
        if (c.getType() != Conversation.ConversationType.DIRECT) {
            throw new BadRequestException("Action only available for direct conversations");
        }
    }
}