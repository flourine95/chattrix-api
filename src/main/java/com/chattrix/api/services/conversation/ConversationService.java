package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.ConversationSettings;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.ConversationMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.*;
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
    @Inject
    private MessageReadReceiptRepository readReceiptRepository;

    @Transactional
    public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw BusinessException.badRequest("At least one participant is required", "BAD_REQUEST");
        }

        if ("DIRECT".equals(request.getType())) {
            long count = request.getParticipantIds().stream().filter(id -> !id.equals(currentUserId)).count();
            if (count != 1)
                throw BusinessException.badRequest("DIRECT conversation must have exactly 1 other participant.", "BAD_REQUEST");
        }

        if ("GROUP".equals(request.getType())) {
            long count = request.getParticipantIds().stream().filter(id -> !id.equals(currentUserId)).count();
            if (count < 1)
                throw BusinessException.badRequest("GROUP conversation must have at least 1 other participant", "BAD_REQUEST");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

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
                        .orElseThrow(() -> BusinessException.badRequest("Participant not found: " + participantId, "BAD_REQUEST"));

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

    public PaginatedResponse<ConversationResponse> getConversations(Long userId, String filter, int page, int size) {
        List<Conversation> allConversations = conversationRepository.findByUserId(userId);

        // Apply filter
        if (filter != null) {
            switch (filter.toLowerCase()) {
                case "unread":
                    // Filter conversations with unread messages
                    allConversations = allConversations.stream()
                            .filter(conv -> conv.getParticipants().stream()
                                    .anyMatch(p -> p.getUser().getId().equals(userId) && p.getUnreadCount() > 0))
                            .toList();
                    break;
                case "group":
                    // Filter only GROUP conversations
                    allConversations = allConversations.stream()
                            .filter(conv -> conv.getType() == Conversation.ConversationType.GROUP)
                            .toList();
                    break;
                case "all":
                default:
                    // Return all conversations (no filtering needed)
                    break;
            }
        }

        // Calculate pagination
        long totalElements = allConversations.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allConversations.size());

        // Get page of conversations
        List<Conversation> pagedConversations = allConversations.subList(startIndex, endIndex);

        // Map to response and populate unreadCount for current user
        List<ConversationResponse> responses = conversationMapper.toResponseList(pagedConversations);
        for (int i = 0; i < pagedConversations.size(); i++) {
            Conversation conv = pagedConversations.get(i);
            ConversationResponse response = responses.get(i);

            // Find current user's participant to get unreadCount
            conv.getParticipants().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> response.setUnreadCount(p.getUnreadCount()));

            // Populate readBy for lastMessage if exists
            if (response.getLastMessage() != null && conv.getLastMessage() != null) {
                var receipts = readReceiptRepository.findByMessageId(conv.getLastMessage().getId());
                response.getLastMessage().setReadCount((long) receipts.size());

                List<ConversationResponse.ReadReceiptInfo> readByList = receipts.stream()
                        .map(receipt -> ConversationResponse.ReadReceiptInfo.builder()
                                .userId(receipt.getUser().getId())
                                .username(receipt.getUser().getUsername())
                                .fullName(receipt.getUser().getFullName())
                                .avatarUrl(receipt.getUser().getAvatarUrl())
                                .readAt(receipt.getReadAt())
                                .build())
                        .toList();
                response.getLastMessage().setReadBy(readByList);
            }
        }

        PaginatedResponse<ConversationResponse> result = new PaginatedResponse<>();
        result.setData(responses);
        result.setPage(page);
        result.setSize(size);
        result.setTotal(totalElements);
        result.setTotalPages(totalPages);
        result.setHasNextPage(page < totalPages - 1);
        result.setHasPrevPage(page > 0);

        return result;
    }

    public ConversationResponse getConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));
        validateParticipant(conversation, userId);
        return conversationMapper.toResponse(conversation);
    }

    public List<ConversationMemberResponse> getConversationMembers(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));
        validateParticipant(conversation, userId);

        List<User> users = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .toList();
        return userMapper.toConversationMemberResponseList(users);
    }


    @Transactional
    public ConversationResponse updateConversation(Long userId, Long conversationId, UpdateConversationRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);

        if (request.getName() != null) conversation.setName(request.getName());
        if (request.getAvatarUrl() != null) conversation.setAvatarUrl(request.getAvatarUrl());

        conversationRepository.save(conversation);
        return conversationMapper.toResponse(conversation);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Participant not found", "RESOURCE_NOT_FOUND"));

        participantRepository.delete(participant);
    }

    @Transactional
    public void leaveConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw BusinessException.badRequest("Cannot leave a direct conversation", "BAD_REQUEST");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.badRequest("You are not a member of this conversation", "BAD_REQUEST"));

        participantRepository.delete(participant);
    }

    @Transactional
    public AddMembersResponse addMembers(Long userId, Long conversationId, AddMembersRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);

        List<AddMembersResponse.AddedMember> addedMembers = new ArrayList<>();

        for (Long newUserId : request.getUserIds()) {
            if (participantRepository.isUserParticipant(conversationId, newUserId)) continue;

            User newUser = userRepository.findById(newUserId)
                    .orElseThrow(() -> BusinessException.notFound("User not found: " + newUserId, "RESOURCE_NOT_FOUND"));

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
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);

        if (userId.equals(memberUserId)) {
            throw BusinessException.badRequest("Use leave endpoint to remove yourself from the conversation", "BAD_REQUEST");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> BusinessException.notFound("Member not found", "RESOURCE_NOT_FOUND"));

        participantRepository.delete(participant);
    }

    @Transactional
    public ConversationResponse.ParticipantResponse updateMemberRole(Long userId, Long conversationId, Long memberUserId, UpdateMemberRoleRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> BusinessException.notFound("Member not found", "RESOURCE_NOT_FOUND"));

        participant.setRole(ConversationParticipant.Role.valueOf(request.getRole()));
        participantRepository.save(participant);

        return ConversationResponse.ParticipantResponse.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .fullName(participant.getUser().getFullName())
                .avatarUrl(participant.getUser().getAvatarUrl())
                .role(participant.getRole().name())
                .build();
    }


    public ConversationSettingsResponse getConversationSettings(Long userId, Long conversationId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
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
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
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
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
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
            throw BusinessException.badRequest("Access denied", "BAD_REQUEST");
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
            throw BusinessException.badRequest("Access denied", "BAD_REQUEST");
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
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        ConversationSettings settings = ConversationSettings.builder()
                .user(user)
                .conversation(conversation)
                .build();
        return settingsRepository.save(settings);
    }

    private void validateParticipant(Conversation conversation, Long userId) {
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (!isParticipant)
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
    }

    private void validateGroupAdmin(Long conversationId, Long userId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("Access denied", "BAD_REQUEST");
        }
        Conversation c = conversationRepository.findById(conversationId).orElseThrow();
        if (c.getType() != Conversation.ConversationType.GROUP) {
            throw BusinessException.badRequest("Only group conversations support this action", "BAD_REQUEST");
        }
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw BusinessException.badRequest("Only admins can perform this action", "BAD_REQUEST");
        }
    }

    private void validateDirectConversation(Long conversationId) {
        Conversation c = conversationRepository.findById(conversationId).orElseThrow();
        if (c.getType() != Conversation.ConversationType.DIRECT) {
            throw BusinessException.badRequest("Action only available for direct conversations", "BAD_REQUEST");
        }
    }
}




