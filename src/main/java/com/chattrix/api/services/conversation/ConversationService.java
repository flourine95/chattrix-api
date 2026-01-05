package com.chattrix.api.services.conversation;

import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.ConversationMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.*;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.*;
import com.chattrix.api.utils.PaginationHelper;
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
    //     @Inject
    //     private ConversationSettingsRepository settingsRepository;
    //     @Inject
    //     private MessageReadReceiptRepository readReceiptRepository;
    @Inject
    private com.chattrix.api.services.message.SystemMessageService systemMessageService;
    @Inject
    private com.chattrix.api.services.conversation.GroupPermissionsService groupPermissionsService;
    // @Inject
    // private ConversationSettingsService conversationSettingsService;
    @Inject
    private com.chattrix.api.services.cache.ConversationCache conversationCache;
    @Inject
    private com.chattrix.api.services.cache.CacheManager cacheManager;

    @Transactional
    public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw BusinessException.badRequest("At least one participant is required", "BAD_REQUEST");
        }

        if ("DIRECT".equals(request.getType())) {
            long count = request.getParticipantIds().stream().filter(id -> !id.equals(currentUserId)).count();
            if (count != 1)
                throw BusinessException.badRequest("DIRECT conversation must have exactly 1 other participant.", "BAD_REQUEST");
            
            // Check for existing DIRECT conversation
            Long otherUserId = request.getParticipantIds().stream()
                    .filter(id -> !id.equals(currentUserId))
                    .findFirst()
                    .orElseThrow();
            
            Optional<Conversation> existingConversation = conversationRepository
                    .findDirectConversationBetweenUsers(currentUserId, otherUserId);
            
            if (existingConversation.isPresent()) {
                Conversation conv = existingConversation.get();
                
                // Auto-unarchive for both users if archived
                conv.getParticipants().forEach(participant -> {
                    if (participant.isArchived()) {
                        participant.setArchived(false);
                        participant.setArchivedAt(null);
                        participantRepository.save(participant);
                    }
                });
                
                // Invalidate cache for both users
                Set<Long> participantIds = conv.getParticipants().stream()
                        .map(p -> p.getUser().getId())
                        .collect(java.util.stream.Collectors.toSet());
                cacheManager.invalidateConversationCaches(conv.getId(), participantIds);
                
                // Return existing conversation with full history
                return enrichConversationResponse(conv, currentUserId);
            }
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
                .type("GROUP".equals(request.getType()) ? ConversationType.GROUP : ConversationType.DIRECT)
                .build();

        Set<ConversationParticipant> participants = new HashSet<>();

        participants.add(ConversationParticipant.builder()
                .user(currentUser)
                .conversation(conversation)
                .role(ConversationParticipant.Role.ADMIN)
                .build());

        List<Long> addedUserIds = new ArrayList<>();
        for (Long participantId : request.getParticipantIds()) {
            if (!participantId.equals(currentUserId)) {
                User participant = userRepository.findById(participantId)
                        .orElseThrow(() -> BusinessException.badRequest("Participant not found: " + participantId, "BAD_REQUEST"));

                participants.add(ConversationParticipant.builder()
                        .user(participant)
                        .conversation(conversation)
                        .role(ConversationParticipant.Role.MEMBER)
                        .build());
                addedUserIds.add(participantId);
            }
        }

        conversation.setParticipants(participants);
        conversationRepository.save(conversation);

        // Create system message for group creation with members
        if (conversation.getType() == ConversationType.GROUP && !addedUserIds.isEmpty()) {
            systemMessageService.createUserAddedMessage(conversation.getId(), addedUserIds, currentUserId);
        }

        // Invalidate cache for all participants
        Set<Long> allParticipantIds = participants.stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversation.getId(), allParticipantIds);

        return enrichConversationResponse(conversation, currentUserId);
    }

    @Transactional
    public PaginatedResponse<ConversationResponse> getConversations(Long userId, String filter, int page, int size) {
        // FIXED: Use cursor-based pagination with filter at database level
        // Calculate cursor from page number (simulate offset-based pagination with cursor)
        Long cursor = null; // Start from beginning for first page
        
        // For subsequent pages, we need to fetch all previous pages to get the cursor
        // This is a limitation of cursor pagination when used with offset-based API
        // Better solution: migrate API to cursor-based pagination
        int totalToFetch = (page + 1) * size;
        
        List<Conversation> conversations = conversationRepository.findByUserIdWithCursorAndFilter(
            userId, cursor, totalToFetch, filter);
        
        // Calculate total (this is expensive but needed for offset pagination)
        // TODO: Add countByUserIdWithFilter() method to repository for better performance
        long totalElements = conversationRepository.findByUserId(userId).size();
        if (filter != null) {
            switch (filter.toLowerCase()) {
                case "unread":
                    totalElements = conversationRepository.findByUserId(userId).stream()
                            .filter(conv -> conv.getParticipants().stream()
                                    .anyMatch(p -> p.getUser().getId().equals(userId) && p.getUnreadCount() > 0))
                            .count();
                    break;
                case "group":
                    totalElements = conversationRepository.findByUserId(userId).stream()
                            .filter(conv -> conv.getType() == ConversationType.GROUP)
                            .count();
                    break;
            }
        }
        
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, conversations.size());
        
        // Get page of conversations
        List<Conversation> pagedConversations = conversations.subList(
            Math.min(startIndex, conversations.size()), 
            Math.min(endIndex, conversations.size()));
        
        // Map to response with cache
        List<ConversationResponse> responses = pagedConversations.stream()
                .map(conv -> {
                    // Try cache first
                    ConversationResponse cached = conversationCache.get(userId, conv.getId());
                    if (cached != null) {
                        return cached;
                    }
                    
                    // Cache miss - enrich and cache
                    ConversationResponse response = enrichConversationResponse(conv, userId);
                    conversationCache.put(userId, conv.getId(), response);
                    return response;
                })
                .toList();

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
     * Cursor is the conversation ID of the last item from previous page.
     */
    @Transactional
    public CursorPaginatedResponse<ConversationResponse> getConversationsWithCursor(Long userId, String filter, Long cursor, int limit) {
        limit = PaginationHelper.validateLimit(limit);

        List<Conversation> conversations = conversationRepository.findByUserIdWithCursorAndFilter(userId, cursor, limit, filter);

        var result = PaginationHelper.processForPagination(conversations, limit);

        List<ConversationResponse> responses = result.items().stream()
                .map(conv -> {
                    // Try cache first
                    ConversationResponse cached = conversationCache.get(userId, conv.getId());
                    if (cached != null)
                        return cached;
                    
                    // Cache miss - enrich and cache
                    ConversationResponse response = enrichConversationResponse(conv, userId);
                    conversationCache.put(userId, conv.getId(), response);
                    return response;
                })
                .toList();

        Long nextCursor = result.hasMore() && !responses.isEmpty() 
                ? responses.get(responses.size() - 1).getId() 
                : null;

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    @Transactional
    public ConversationResponse getConversation(Long userId, Long conversationId) {
        // Try cache first
        ConversationResponse cached = conversationCache.get(userId, conversationId);
        if (cached != null) {
            return cached;
        }
        
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));
        validateParticipant(conversation, userId);
        
        ConversationResponse response = enrichConversationResponse(conversation, userId);
        conversationCache.put(userId, conversationId, response);
        return response;
    }

    /**
     * Helper method to enrich conversation response with unread count, last message details, and user settings
     */
    private ConversationResponse enrichConversationResponse(Conversation conv, Long userId) {
        ConversationResponse response = conversationMapper.toResponse(conv);

        // 1. Set unreadCount for current user
        conv.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(p -> response.setUnreadCount(p.getUnreadCount()));

        // 2. Populate readBy for lastMessage if exists
        // TODO: Implement when MessageReadReceipt entity is created
        /*
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
        */

        // 3. Populate settings for current user
        // TODO: Implement when ConversationSettings entity is created
        /*
        ConversationSettings settings = settingsRepository
                .findByUserIdAndConversationId(userId, conv.getId())
                .orElseGet(() -> createDefaultSettings(userId, conv.getId()));

        response.setSettings(ConversationSettingsResponse.builder()
                .conversationId(conv.getId())
                .muted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .blocked(settings.isBlocked())
                .notificationsEnabled(settings.isNotificationsEnabled())
                .customNickname(settings.getCustomNickname())
                .theme(settings.getTheme())
                .pinned(settings.isPinned())
                .pinOrder(settings.getPinOrder())
                .archived(settings.isArchived())
                .hidden(settings.isHidden())
                .build());
        */

        return response;
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
        if (!participantRepository.isUserParticipant(conversationId, userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        limit = PaginationHelper.validateLimit(limit);

        List<ConversationParticipant> participants = participantRepository.findByConversationIdWithCursor(conversationId, cursor, limit);

        var result = PaginationHelper.processForPagination(participants, limit);

        List<User> users = result.items().stream()
                .map(ConversationParticipant::getUser)
                .toList();
        List<ConversationMemberResponse> responses = userMapper.toConversationMemberResponseList(users);

        Long nextCursor = result.hasMore() && !responses.isEmpty()
                ? responses.get(responses.size() - 1).getId()
                : null;

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
        
        // Invalidate cache for all participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, participantIds);
        
        return enrichConversationResponse(conversation, userId);
    }

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        // DELETE = Archive conversation for current user
        archiveConversation(userId, conversationId);
    }

    @Transactional
    public void archiveConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        participant.setArchived(true);
        participant.setArchivedAt(Instant.now());
        participantRepository.save(participant);

        // Invalidate cache
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void unarchiveConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        participant.setArchived(false);
        participant.setArchivedAt(null);
        participantRepository.save(participant);

        // Invalidate cache
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void muteConversation(Long userId, Long conversationId, Long durationMinutes) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        participant.setMuted(true);
        participant.setMutedAt(Instant.now());
        
        // If duration is provided, set mutedUntil
        if (durationMinutes != null && durationMinutes > 0) {
            participant.setMutedUntil(Instant.now().plus(durationMinutes, java.time.temporal.ChronoUnit.MINUTES));
        } else {
            // Permanent mute
            participant.setMutedUntil(null);
        }
        
        participantRepository.save(participant);

        // Invalidate cache
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void unmuteConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        participant.setMuted(false);
        participant.setMutedAt(null);
        participant.setMutedUntil(null);
        participantRepository.save(participant);

        // Invalidate cache
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void pinConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (participant.isPinned()) {
            throw BusinessException.badRequest("Conversation is already pinned", "ALREADY_PINNED");
        }

        // Get max pin order for user
        Integer maxPinOrder = participantRepository.getMaxPinOrder(userId);
        
        participant.setPinned(true);
        participant.setPinnedAt(Instant.now());
        participant.setPinOrder(maxPinOrder != null ? maxPinOrder + 1 : 1);
        participantRepository.save(participant);

        // Invalidate cache
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void unpinConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (!participant.isPinned()) {
            throw BusinessException.badRequest("Conversation is not pinned", "NOT_PINNED");
        }

        participant.setPinned(false);
        participant.setPinnedAt(null);
        participant.setPinOrder(null);
        participantRepository.save(participant);

        // Invalidate cache
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void leaveConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Cannot leave a direct conversation", "BAD_REQUEST");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.badRequest("You are not a member of this conversation", "BAD_REQUEST"));

        // Check total members
        long totalMembers = participantRepository.countByConversationId(conversationId);

        // If this is the last member, delete the conversation
        if (totalMembers == 1) {
            participantRepository.delete(participant);
            // TODO: Add conversationRepository.delete() method or use EntityManager.remove()
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
        
        // Invalidate cache for all participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, participantIds);
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
        List<Long> newlyAddedUserIds = new ArrayList<>();

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
            
            newlyAddedUserIds.add(newUserId);
        }

        // Create ONE system message for all added members
        if (!newlyAddedUserIds.isEmpty()) {
            systemMessageService.createUserAddedMessage(conversationId, newlyAddedUserIds, userId);
        }

        // Invalidate cache for all participants (including new ones)
        Set<Long> allParticipantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, allParticipantIds);

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
        
        // Invalidate cache for all remaining participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, participantIds);
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

        // Invalidate cache for all participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, participantIds);

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

        // TODO: Implement when ConversationSettings entity is created
        /*
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
        */
        
        // Return default settings for now
        return ConversationSettingsResponse.builder()
                .conversationId(conversationId)
                .muted(false)
                .mutedUntil(null)
                .blocked(false)
                .notificationsEnabled(true)
                .customNickname(null)
                .theme(null)
                .build();
    }

    @Transactional
    public ConversationSettingsResponse updateConversationSettings(Long userId, Long conversationId, UpdateConversationSettingsRequest request) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        // TODO: Implement when ConversationSettings entity is created
        /*
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
                .pinned(settings.isPinned())
                .pinOrder(settings.getPinOrder())
                .archived(settings.isArchived())
                .hidden(settings.isHidden())
                .build();
        */
        
        // Return updated settings for now
        return ConversationSettingsResponse.builder()
                .conversationId(conversationId)
                .muted(false)
                .mutedUntil(null)
                .blocked(false)
                .notificationsEnabled(request.getNotificationsEnabled() != null ? request.getNotificationsEnabled() : true)
                .customNickname(request.getCustomNickname())
                .theme(request.getTheme())
                .pinned(false)
                .pinOrder(null)
                .archived(false)
                .hidden(false)
                .build();
    }

    /**
     * @deprecated Use // ConversationSettingsService. // TODO: ImplementmuteConversation() instead
     * This method is kept for backward compatibility with duration support
     */
    @Transactional
    @Deprecated
    public MuteConversationResponse muteConversation(Long userId, Long conversationId, MuteConversationRequest request) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        // TODO: Implement when ConversationSettings entity is created
        /*
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
        */
        
        // Return mute response for now
        Integer duration = request.getDuration();
        boolean muted = duration != null && duration != 0;
        Instant mutedUntil = (duration != null && duration > 0) ? Instant.now().plusSeconds(duration) : null;
        
        return MuteConversationResponse.builder()
                .muted(muted)
                .mutedUntil(mutedUntil)
                .build();
    }

    /**
     * @deprecated Use // ConversationSettingsService. // TODO: ImplementblockUser() instead
     */
    @Transactional
    @Deprecated
    public BlockUserResponse blockUser(Long userId, Long conversationId) {
        // TODO: Implement when ConversationSettingsService exists
        // ConversationSettingsResponse response = conversationSettingsService.blockUser(userId, conversationId);
        return BlockUserResponse.builder()
                .blocked(true)
                .blockedAt(Instant.now())
                .build();
    }

    /**
     * @deprecated Use ConversationSettingsService.unblockUser() instead
     */
    @Transactional
    @Deprecated
    public BlockUserResponse unblockUser(Long userId, Long conversationId) {
        // TODO: Implement when ConversationSettingsService exists
        // ConversationSettingsResponse response = conversationSettingsService.unblockUser(userId, conversationId);
        return BlockUserResponse.builder()
                .blocked(false)
                .blockedAt(null)
                .build();
    }

    // TODO: Uncomment when ConversationSettings entity is created
    /*
    @Transactional
    protected ConversationSettings createDefaultSettings(Long userId, Long conversationId) {
        // Double check to avoid race conditions
        Optional<ConversationSettings> existing = // settingsRepository. // TODO: ImplementfindByUserIdAndConversationId(userId, conversationId);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        ConversationSettings settings = ConversationSettings.builder()
                .user(user)
                .conversation(conversation)
                .build();
        
        try {
            return // // settingsRepository. // TODO: Implementsave(settings);
        } catch (RuntimeException e) {
            // If save fails due to unique constraint, try to find it one last time
            return // settingsRepository. // TODO: ImplementfindByUserIdAndConversationId(userId, conversationId)
                    .orElseThrow(() -> e);
        }
    }
    */

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
        if (c.getType() != ConversationType.GROUP) {
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
                .map(conv -> enrichConversationResponse(conv, currentUserId))
                .toList();
    }
}
