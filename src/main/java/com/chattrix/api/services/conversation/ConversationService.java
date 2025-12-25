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
import java.util.Optional;
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
    @Inject
    private com.chattrix.api.services.message.SystemMessageService systemMessageService;
    @Inject
    private com.chattrix.api.services.conversation.GroupPermissionsService groupPermissionsService;
    @Inject
    private ConversationSettingsService conversationSettingsService;

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

    /**
     * Get conversations with cursor-based pagination and filtering.
     */
    public CursorPaginatedResponse<ConversationResponse> getConversationsWithCursor(Long userId, String filter, Long cursor, int limit) {
        // Validate limit
        if (limit < 1) {
            limit = 1;
        }
        if (limit > 100) {
            limit = 100;
        }

        // Fetch conversations with cursor and filter
        List<Conversation> conversations = conversationRepository.findByUserIdWithCursorAndFilter(userId, cursor, limit, filter);

        // Check if there are more items
        boolean hasMore = conversations.size() > limit;
        if (hasMore) {
            conversations = conversations.subList(0, limit);
        }

        // Map to response and populate unreadCount for current user
        List<ConversationResponse> responses = conversationMapper.toResponseList(conversations);
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
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

        // Calculate next cursor
        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
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

    public CursorPaginatedResponse<ConversationMemberResponse> getConversationMembersWithCursor(Long userId, Long conversationId, Long cursor, int limit) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;

        List<ConversationParticipant> participants = participantRepository.findByConversationIdWithCursor(conversationId, cursor, limit);

        boolean hasMore = participants.size() > limit;
        if (hasMore) {
            participants = participants.subList(0, limit);
        }

        List<User> users = participants.stream()
                .map(ConversationParticipant::getUser)
                .toList();
        List<ConversationMemberResponse> responses = userMapper.toConversationMemberResponseList(users);

        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }


    @Transactional
    public ConversationResponse updateConversation(Long userId, Long conversationId, UpdateConversationRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);
        
        // Check permission
        if (!groupPermissionsService.hasPermission(conversationId, userId, "edit_group_info")) {
            throw BusinessException.forbidden("You don't have permission to edit group info");
        }

        String oldName = conversation.getName();
        boolean nameChanged = false;
        boolean avatarChanged = false;
        boolean descriptionChanged = false;

        if (request.getName() != null && !request.getName().equals(oldName)) {
            conversation.setName(request.getName());
            nameChanged = true;
        }

        if (request.getAvatarUrl() != null) {
            conversation.setAvatarUrl(request.getAvatarUrl());
            avatarChanged = true;
        }

        if (request.getDescription() != null) {
            conversation.setDescription(request.getDescription());
            descriptionChanged = true;
        }

        conversationRepository.save(conversation);
        
        // Create system messages for changes
        if (nameChanged) {
            systemMessageService.createGroupNameChangedMessage(conversationId, userId, oldName, request.getName());
        }
        if (avatarChanged) {
            systemMessageService.createGroupAvatarChangedMessage(conversationId, userId);
        }
        
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

        // Check total members
        long totalMembers = participantRepository.countByConversationId(conversationId);

        // If this is the last member, delete the conversation
        if (totalMembers == 1) {
            conversationRepository.delete(conversation);
            return;
        }

        // Check if user is admin
        boolean isAdmin = participant.getRole() == ConversationParticipant.Role.ADMIN;

        if (isAdmin) {
            // Count total admins
            long totalAdmins = participantRepository.countAdminsByConversationId(conversationId);

            // If this is the last admin, must transfer admin rights first
            if (totalAdmins == 1) {
                // Find oldest member to promote
                Optional<ConversationParticipant> oldestMember = participantRepository.findOldestMemberByConversationId(conversationId);

                if (oldestMember.isEmpty()) {
                    throw BusinessException.badRequest(
                            "You are the last admin. Please promote another member to admin before leaving.",
                            "LAST_ADMIN"
                    );
                }

                // Auto-promote oldest member
                ConversationParticipant newAdmin = oldestMember.get();
                newAdmin.setRole(ConversationParticipant.Role.ADMIN);
                participantRepository.save(newAdmin);

                // Create system message for auto-promotion
                systemMessageService.createUserPromotedMessage(conversationId, newAdmin.getUser().getId(), userId);
            }
        }

        // Now safe to leave
        System.out.println("DEBUG: Deleting participant - userId: " + userId + ", conversationId: " + conversationId);

        // Remove from conversation collection (for orphanRemoval)
        conversation.getParticipants().remove(participant);
        conversationRepository.save(conversation);

        System.out.println("DEBUG: Participant deleted successfully");

        // Create system message
        systemMessageService.createUserLeftMessage(conversationId, userId);
        System.out.println("DEBUG: System message created");
    }

    @Transactional
    public AddMembersResponse addMembers(Long userId, Long conversationId, AddMembersRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);
        
        // Check permission
        if (!groupPermissionsService.hasPermission(conversationId, userId, "add_members")) {
            throw BusinessException.forbidden("You don't have permission to add members");
        }

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
            
            // Create system message for each added member
            systemMessageService.createUserAddedMessage(conversationId, newUserId, userId);
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

        System.out.println("DEBUG removeMember: Deleting participant - memberUserId: " + memberUserId + ", conversationId: " + conversationId);

        // Remove from conversation collection (for orphanRemoval)
        conversation.getParticipants().remove(participant);
        conversationRepository.save(conversation);

        System.out.println("DEBUG removeMember: Participant deleted successfully");

        // Create system message for kicked member
        try {
            System.out.println("DEBUG removeMember: Creating system message...");
            systemMessageService.createUserKickedMessage(conversationId, memberUserId, userId);
            System.out.println("DEBUG removeMember: System message created successfully");
        } catch (Exception e) {
            System.out.println("DEBUG removeMember: ERROR creating system message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public ConversationResponse.ParticipantResponse updateMemberRole(Long userId, Long conversationId, Long memberUserId, UpdateMemberRoleRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        validateGroupAdmin(conversationId, userId);

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> BusinessException.notFound("Member not found", "RESOURCE_NOT_FOUND"));

        ConversationParticipant.Role oldRole = participant.getRole();
        ConversationParticipant.Role newRole = ConversationParticipant.Role.valueOf(request.getRole());
        
        participant.setRole(newRole);
        participantRepository.save(participant);
        
        // Create system message for role change
        if (oldRole == ConversationParticipant.Role.MEMBER && newRole == ConversationParticipant.Role.ADMIN) {
            systemMessageService.createUserPromotedMessage(conversationId, memberUserId, userId);
        } else if (oldRole == ConversationParticipant.Role.ADMIN && newRole == ConversationParticipant.Role.MEMBER) {
            systemMessageService.createUserDemotedMessage(conversationId, memberUserId, userId);
        }

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
                .muted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .blocked(settings.isBlocked())
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
                .muted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .blocked(settings.isBlocked())
                .notificationsEnabled(settings.isNotificationsEnabled())
                .customNickname(settings.getCustomNickname())
                .theme(settings.getTheme())
                .build();
    }

    /**
     * @deprecated Use ConversationSettingsService.muteConversation() instead
     * This method is kept for backward compatibility with duration support
     */
    @Transactional
    @Deprecated
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
                .muted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .build();
    }

    /**
     * @deprecated Use ConversationSettingsService.blockUser() instead
     */
    @Transactional
    @Deprecated
    public BlockUserResponse blockUser(Long userId, Long conversationId) {
        ConversationSettingsResponse response = conversationSettingsService.blockUser(userId, conversationId);
        return BlockUserResponse.builder()
                .blocked(response.getBlocked())
                .blockedAt(Instant.now())
                .build();
    }

    /**
     * @deprecated Use ConversationSettingsService.unblockUser() instead
     */
    @Transactional
    @Deprecated
    public BlockUserResponse unblockUser(Long userId, Long conversationId) {
        ConversationSettingsResponse response = conversationSettingsService.unblockUser(userId, conversationId);
        return BlockUserResponse.builder()
                .blocked(response.getBlocked())
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

    /**
     * Get mutual groups between current user and another user
     */
    public List<ConversationResponse> getMutualGroups(Long currentUserId, Long otherUserId) {
        // Validate that other user exists
        userRepository.findById(otherUserId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        List<Conversation> mutualGroups = conversationRepository.findMutualGroups(currentUserId, otherUserId);

        return mutualGroups.stream()
                .map(conversationMapper::toResponse)
                .toList();
    }
}
