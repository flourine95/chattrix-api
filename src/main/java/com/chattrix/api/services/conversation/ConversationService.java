package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.ConversationMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.AddMembersRequest;
import com.chattrix.api.requests.CreateConversationRequest;
import com.chattrix.api.requests.UpdateConversationRequest;
import com.chattrix.api.requests.UpdateMemberRoleRequest;
import com.chattrix.api.responses.AddMembersResponse;
import com.chattrix.api.responses.ConversationMemberResponse;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.ConversationCache;
import com.chattrix.api.services.conversation.ConversationBroadcastService;
import com.chattrix.api.services.message.SystemMessageService;
import com.chattrix.api.utils.PaginationHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class ConversationService {

    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private ConversationParticipantRepository participantRepository;
    @Inject
    private UserRepository userRepository;
    @Inject
    private GroupPermissionsService groupPermissionsService;
    @Inject
    private SystemMessageService systemMessageService;
    @Inject
    private ConversationMapper conversationMapper;
    @Inject
    private UserMapper userMapper;
    @Inject
    private ConversationCache conversationCache;
    @Inject
    private CacheManager cacheManager;
    @Inject
    private ConversationBroadcastService conversationBroadcastService;

    @Transactional
    public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
        Set<Long> targetUserIds = request.getParticipantIdsExcluding(currentUserId);

        request.validateParticipantCount(targetUserIds.size());

        // TODO: Validate Block (Nếu có service relationship)
        // relationshipService.validateNoBlock(currentUserId, targetUserIds);

        if (request.isDirect()) {
            Long otherUserId = targetUserIds.iterator().next();

            Optional<Conversation> existingConversation = conversationRepository
                    .findDirectConversationBetweenUsers(currentUserId, otherUserId);

            if (existingConversation.isPresent()) {
                return restoreExistingConversation(existingConversation.get(), currentUserId);
            }
        }

        return createNewConversation(currentUserId, targetUserIds, request);
    }

    private ConversationResponse restoreExistingConversation(Conversation conv, Long currentUserId) {
        boolean needsCacheInvalidation = false;
        Set<Long> userIdsToInvalidate = new HashSet<>();

        for (ConversationParticipant p : conv.getParticipants()) {
            if (p.isArchived()) {
                p.setArchived(false);
                p.setArchivedAt(null);

                userIdsToInvalidate.add(p.getUser().getId());
                needsCacheInvalidation = true;
            }
            // Logic mở rộng: Nếu user đã rời nhóm (left) thì add lại vào đây
        }

        // LƯU Ý: Không cần gọi saveAll() vì đang trong @Transactional.
        // Hibernate tự động Dirty Check và update DB khi kết thúc hàm.

        if (needsCacheInvalidation) {
            cacheManager.invalidateConversationCaches(conv.getId(), userIdsToInvalidate);
        }

        // Reload conversation with fresh data and map to response
        Conversation reloadedConv = conversationRepository.findById(currentUserId, conv.getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation restored but not found"));

        return conversationMapper.toResponseWithUnreadCount(reloadedConv, currentUserId);
    }

    private ConversationResponse createNewConversation(Long currentUserId, Set<Long> targetUserIds, CreateConversationRequest request) {
        Set<Long> allUserIds = new HashSet<>(targetUserIds);
        allUserIds.add(currentUserId);

        List<User> allUsers = userRepository.findAllById(allUserIds);

        if (allUsers.size() != allUserIds.size()) {
            Set<Long> foundIds = allUsers.stream().map(User::getId).collect(Collectors.toSet());
            allUserIds.removeAll(foundIds);
            throw BusinessException.badRequest("Users not found: " + allUserIds);
        }

        Map<Long, User> userMap = allUsers.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        User currentUser = userMap.get(currentUserId);

        Conversation conversation = Conversation.builder()
                .name(request.isGroup() ? request.getName() : null)
                .type(request.isGroup() ? ConversationType.GROUP : ConversationType.DIRECT)
                .build();

        conversation.addParticipant(currentUser, ConversationParticipant.Role.ADMIN);
        for (Long targetId : targetUserIds) {
            conversation.addParticipant(userMap.get(targetId), ConversationParticipant.Role.MEMBER);
        }

        Conversation savedConv = conversationRepository.save(conversation);

        if (savedConv.isGroupConversation() && !targetUserIds.isEmpty()) {
            systemMessageService.createUserAddedMessage(
                    savedConv.getId(),
                    new ArrayList<>(targetUserIds),
                    currentUserId
            );
        }

        // Reload conversation with fresh data and map to response
        Conversation reloadedConv = conversationRepository.findById(currentUserId, savedConv.getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation created but not found"));

        // Broadcast conversation created event
        conversationBroadcastService.broadcastConversationCreated(
                reloadedConv,
                currentUserId,
                currentUser.getUsername()
        );

        return conversationMapper.toResponseWithUnreadCount(reloadedConv, currentUserId);
    }

    /**
     * Get conversations with cursor-based pagination and filtering.
     * Cursor is the conversation ID of the last item from previous page.
     * 
     * Supported filters:
     * - all: All conversations (default)
     * - direct: Direct (1-1) conversations only
     * - group: Group conversations only
     * - unread: Conversations with unread messages
     * - archived: Archived conversations
     */
    @Transactional
    public CursorPaginatedResponse<ConversationResponse> getConversations(Long userId, String filter, Long cursor, int limit) {
        // Validate filter
        if (filter != null && !filter.equals("all") && !filter.equals("direct") && !filter.equals("group") 
                && !filter.equals("unread") && !filter.equals("archived")) {
            throw BusinessException.badRequest("Invalid filter. Must be 'all', 'direct', 'group', 'unread', or 'archived'");
        }

        limit = PaginationHelper.validateLimit(limit);

        // Query entities
        List<Conversation> conversations =
                conversationRepository.findByUserIdWithCursor(userId, cursor, limit, filter);
        var result = PaginationHelper.processForPagination(conversations, limit);

        // Map with userId - mapper automatically extracts unreadCount
        List<ConversationResponse> responses = conversationMapper.toResponseListWithUnreadCount(result.items(), userId);

        Long nextCursor = result.hasMore() && !responses.isEmpty()
                ? responses.getLast().getId()
                : null;

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    @Transactional
    public ConversationResponse getConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(userId, conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        return conversationMapper.toResponseWithUnreadCount(conversation, userId);
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

        // Check permission (this handles both admin and ALL permission cases)
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
                .collect(Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, participantIds);

        // Reload and map to response
        Conversation updatedConv = conversationRepository.findById(userId, conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        // Broadcast conversation updated event
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        conversationBroadcastService.broadcastConversationUpdated(
                updatedConv,
                userId,
                currentUser.getUsername()
        );

        return conversationMapper.toResponseWithUnreadCount(updatedConv, userId);
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
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        participant.setArchived(true);
        participant.setArchivedAt(Instant.now());
        participantRepository.save(participant);

        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void unarchiveConversation(Long userId, Long conversationId) {
        // Use includeArchived=true to find archived conversations
        Conversation conversation = conversationRepository.findById(userId, conversationId, true)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        ConversationParticipant participant = conversation.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Participant not found"));

        if (!participant.isArchived()) {
            throw BusinessException.badRequest("Conversation is not archived");
        }

        participant.setArchived(false);
        participant.setArchivedAt(null);
        participantRepository.save(participant);

        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void muteConversation(Long userId, Long conversationId, Long durationMinutes) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        participant.setMuted(true);
        participant.setMutedAt(Instant.now());

        if (durationMinutes != null && durationMinutes > 0) {
            participant.setMutedUntil(Instant.now().plus(durationMinutes, ChronoUnit.MINUTES));
        } else {
            participant.setMutedUntil(null);
        }

        participantRepository.save(participant);
        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void unmuteConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        participant.setMuted(false);
        participant.setMutedAt(null);
        participant.setMutedUntil(null);
        participantRepository.save(participant);

        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void pinConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (participant.isPinned()) {
            throw BusinessException.badRequest("Conversation is already pinned");
        }

        Integer maxPinOrder = participantRepository.getMaxPinOrder(userId);

        participant.setPinned(true);
        participant.setPinnedAt(Instant.now());
        participant.setPinOrder(maxPinOrder != null ? maxPinOrder + 1 : 1);
        participantRepository.save(participant);

        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void unpinConversation(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!participant.isPinned()) {
            throw BusinessException.badRequest("Conversation is not pinned");
        }

        Integer oldPinOrder = participant.getPinOrder();

        participant.setPinned(false);
        participant.setPinnedAt(null);
        participant.setPinOrder(null);
        participantRepository.save(participant);

        // Adjust pin orders of other conversations
        if (oldPinOrder != null) {
            adjustPinOrdersAfterUnpin(userId, oldPinOrder);
        }

        conversationCache.invalidate(userId, conversationId);
    }

    @Transactional
    public void reorderPinnedConversation(Long userId, Long conversationId, Integer newPinOrder) {
        if (newPinOrder == null || newPinOrder < 1) {
            throw BusinessException.badRequest("Pin order must be at least 1");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!participant.isPinned()) {
            throw BusinessException.badRequest("Conversation is not pinned. Pin it first before reordering.");
        }

        Integer oldPinOrder = participant.getPinOrder();

        if (oldPinOrder != null && oldPinOrder.equals(newPinOrder)) {
            // No change needed
            return;
        }

        // Get all pinned conversations for this user (excluding current one)
        List<ConversationParticipant> pinnedParticipants = participantRepository
                .findPinnedByUserId(userId);

        // Remove current conversation from list
        pinnedParticipants.removeIf(p -> p.getConversation().getId().equals(conversationId));

        // Adjust pin orders
        if (oldPinOrder == null) {
            // First time setting order - shift others down
            for (ConversationParticipant p : pinnedParticipants) {
                if (p.getPinOrder() != null && p.getPinOrder() >= newPinOrder) {
                    p.setPinOrder(p.getPinOrder() + 1);
                    participantRepository.save(p);
                }
            }
        } else if (newPinOrder < oldPinOrder) {
            // Moving up - shift others down
            for (ConversationParticipant p : pinnedParticipants) {
                Integer order = p.getPinOrder();
                if (order != null && order >= newPinOrder && order < oldPinOrder) {
                    p.setPinOrder(order + 1);
                    participantRepository.save(p);
                }
            }
        } else {
            // Moving down - shift others up
            for (ConversationParticipant p : pinnedParticipants) {
                Integer order = p.getPinOrder();
                if (order != null && order > oldPinOrder && order <= newPinOrder) {
                    p.setPinOrder(order - 1);
                    participantRepository.save(p);
                }
            }
        }

        // Set new order for current conversation
        participant.setPinOrder(newPinOrder);
        participantRepository.save(participant);

        conversationCache.invalidate(userId, conversationId);
        log.info("Reordered pinned conversation {} for user {} from {} to {}",
                conversationId, userId, oldPinOrder, newPinOrder);
    }

    /**
     * Adjust pin orders after unpinning a conversation.
     * Shifts down all conversations with higher pin order.
     */
    private void adjustPinOrdersAfterUnpin(Long userId, Integer unpinnedOrder) {
        List<ConversationParticipant> pinnedParticipants = participantRepository
                .findPinnedByUserId(userId);

        for (ConversationParticipant p : pinnedParticipants) {
            if (p.getPinOrder() != null && p.getPinOrder() > unpinnedOrder) {
                p.setPinOrder(p.getPinOrder() - 1);
                participantRepository.save(p);
            }
        }
    }

    @Transactional
    public void leaveConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Cannot leave a direct conversation");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.badRequest("You are not a member of this conversation"));

        // Check total members
        long totalMembers = participantRepository.countByConversationId(conversationId);

        // If this is the last member, delete the conversation
        if (totalMembers == 1) {
            // Get all participant IDs before deleting
            List<Long> participantIds = conversation.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .collect(Collectors.toList());

            participantRepository.delete(participant);

            // Broadcast conversation deleted event
            conversationBroadcastService.broadcastConversationDeleted(
                    conversationId,
                    participantIds,
                    "Last member left the group"
            );

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
        conversation.getParticipants().remove(participant);
        conversationRepository.save(conversation);

        systemMessageService.createUserLeftMessage(conversationId, userId);

        // Broadcast member left event
        User leftUser = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        conversationBroadcastService.broadcastMemberLeft(conversation, leftUser);

        // Invalidate cache for all participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());
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

            // Broadcast members added event
            List<User> addedUsersList = addedMembers.stream()
                    .map(m -> userRepository.findById(m.getUserId()).orElse(null))
                    .filter(u -> u != null)
                    .collect(Collectors.toList());

            User actionByUser = userRepository.findById(userId)
                    .orElseThrow(() -> BusinessException.notFound("User not found"));

            conversationBroadcastService.broadcastMembersAdded(
                    conversation,
                    addedUsersList,
                    userId,
                    actionByUser.getUsername()
            );
        }

        // Invalidate cache for all participants (including new ones)
        Set<Long> allParticipantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());
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
            throw BusinessException.badRequest("Use leave endpoint to remove yourself from the conversation");
        }

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> BusinessException.notFound("Member not found"));

        conversation.getParticipants().remove(participant);
        conversationRepository.save(conversation);

        systemMessageService.createUserKickedMessage(conversationId, memberUserId, userId);

        // Broadcast member removed event
        User removedUser = userRepository.findById(memberUserId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        User actionByUser = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        conversationBroadcastService.broadcastMemberRemoved(
                conversation,
                removedUser,
                userId,
                actionByUser.getUsername()
        );

        // Invalidate cache for all remaining participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());
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

        // Broadcast role updated event
        User targetUser = participant.getUser();
        User actionByUser = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        conversationBroadcastService.broadcastRoleUpdated(
                conversation,
                targetUser,
                oldRole.name(),
                newRole.name(),
                userId,
                actionByUser.getUsername()
        );

        // Invalidate cache for all participants
        Set<Long> participantIds = conversation.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());
        cacheManager.invalidateConversationCaches(conversationId, participantIds);

        return ConversationResponse.ParticipantResponse.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .fullName(participant.getUser().getFullName())
                .avatarUrl(participant.getUser().getAvatarUrl())
                .role(participant.getRole().name())
                .build();
    }


    private void validateParticipant(Conversation conversation, Long userId) {
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
        if (!isParticipant)
            throw BusinessException.forbidden("You are not a participant of this conversation");
    }

    private void validateGroupAdmin(Long conversationId, Long userId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("Access denied");
        }
        Conversation c = conversationRepository.findById(conversationId).orElseThrow();
        if (c.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Only group conversations support this action");
        }
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only admins can perform this action");
        }
    }

    /**
     * Get mutual groups between current user and another user
     */
    public List<ConversationResponse> getMutualGroups(Long currentUserId, Long otherUserId) {
        userRepository.findById(otherUserId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        List<Conversation> conversations = conversationRepository.findMutualGroups(currentUserId, otherUserId);
        return conversationMapper.toResponseListWithUnreadCount(conversations, currentUserId);
    }
}
