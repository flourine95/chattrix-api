package com.chattrix.api.services.invite;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.GroupInviteLink;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.GroupInviteLinkRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.CreateInviteLinkRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.InviteLinkInfoResponse;
import com.chattrix.api.responses.InviteLinkResponse;
import com.chattrix.api.responses.JoinViaInviteResponse;
import com.chattrix.api.services.message.SystemMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class GroupInviteLinkService {

    @Inject
    private GroupInviteLinkRepository inviteLinkRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private SystemMessageService systemMessageService;

    /**
     * Create a new invite link
     */
    @Transactional
    public InviteLinkResponse createInviteLink(Long userId, Long conversationId, CreateInviteLinkRequest request) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw BusinessException.badRequest("Can only create invite links for group conversations", "INVALID_CONVERSATION_TYPE");
        }

        // Check if user is a participant (admin check can be added via permissions)
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this group");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Generate unique token
        String token = generateUniqueToken();

        // Calculate expiration
        Instant expiresAt = null;
        if (request.getExpiresIn() != null) {
            expiresAt = Instant.now().plusSeconds(request.getExpiresIn());
        }

        // Create invite link
        GroupInviteLink inviteLink = GroupInviteLink.builder()
                .conversation(conversation)
                .token(token)
                .createdBy(user)
                .expiresAt(expiresAt)
                .maxUses(request.getMaxUses())
                .currentUses(0)
                .revoked(false)
                .build();

        inviteLinkRepository.save(inviteLink);

        return toResponse(inviteLink);
    }

    /**
     * Get invite links for a conversation with cursor-based pagination
     */
    public CursorPaginatedResponse<InviteLinkResponse> getInviteLinks(Long userId, Long conversationId, Long cursor, int limit, boolean includeRevoked) {
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100;
        }

        // Check if user is a participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this group");
        }

        // Get links with cursor
        List<GroupInviteLink> links = inviteLinkRepository.findByConversationIdWithCursor(conversationId, cursor, limit, includeRevoked);

        boolean hasMore = links.size() > limit;
        if (hasMore) {
            links = links.subList(0, limit);
        }

        List<InviteLinkResponse> responses = links.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    /**
     * Revoke an invite link
     */
    @Transactional
    public InviteLinkResponse revokeInviteLink(Long userId, Long conversationId, Long linkId) {
        // Get invite link
        GroupInviteLink inviteLink = inviteLinkRepository.findById(linkId)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        // Verify it belongs to this conversation
        if (!inviteLink.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        // Check if user is admin or creator
        boolean isAdmin = participantRepository.isUserAdmin(conversationId, userId);
        boolean isCreator = inviteLink.getCreatedBy().getId().equals(userId);

        if (!isAdmin && !isCreator) {
            throw BusinessException.forbidden("Only admins or link creator can revoke invite links");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Revoke
        inviteLink.setRevoked(true);
        inviteLink.setRevokedAt(Instant.now());
        inviteLink.setRevokedBy(user);

        inviteLinkRepository.save(inviteLink);

        return toResponse(inviteLink);
    }

    /**
     * Get invite link info (public - no auth required)
     */
    public InviteLinkInfoResponse getInviteLinkInfo(String token) {
        GroupInviteLink inviteLink = inviteLinkRepository.findByToken(token)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        Conversation conversation = inviteLink.getConversation();

        // Count members
        long memberCount = participantRepository.countByConversationId(conversation.getId());

        return InviteLinkInfoResponse.builder()
                .token(inviteLink.getToken())
                .groupId(conversation.getId())
                .groupName(conversation.getName())
                .groupAvatar(conversation.getAvatarUrl())
                .memberCount((int) memberCount)
                .valid(inviteLink.isValid())
                .expiresAt(inviteLink.getExpiresAt())
                .createdBy(inviteLink.getCreatedBy().getId())
                .createdByUsername(inviteLink.getCreatedBy().getUsername())
                .createdByFullName(inviteLink.getCreatedBy().getFullName())
                .build();
    }

    /**
     * Join group via invite link
     */
    @Transactional
    public JoinViaInviteResponse joinViaInviteLink(Long userId, String token) {
        // Get invite link
        GroupInviteLink inviteLink = inviteLinkRepository.findByToken(token)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        // Validate link is still valid
        if (!inviteLink.isValid()) {
            throw BusinessException.badRequest("Invite link is no longer valid", "INVITE_LINK_INVALID");
        }

        Conversation conversation = inviteLink.getConversation();

        // Check if user is already a member
        if (participantRepository.isUserParticipant(conversation.getId(), userId)) {
            return JoinViaInviteResponse.builder()
                    .success(true)
                    .conversationId(conversation.getId())
                    .message("You are already a member of this group")
                    .build();
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Add user to conversation
        ConversationParticipant participant = ConversationParticipant.builder()
                .user(user)
                .conversation(conversation)
                .role(ConversationParticipant.Role.MEMBER)
                .build();

        participantRepository.save(participant);

        // Increment usage count
        inviteLink.setCurrentUses(inviteLink.getCurrentUses() + 1);
        inviteLinkRepository.save(inviteLink);

        // Create system message
        systemMessageService.createUserJoinedViaLinkMessage(conversation.getId(), userId, inviteLink.getCreatedBy().getId());

        return JoinViaInviteResponse.builder()
                .success(true)
                .conversationId(conversation.getId())
                .message("Successfully joined group")
                .build();
    }

    /**
     * Get invite link by ID (internal use)
     */
    public GroupInviteLink getInviteLink(Long linkId) {
        return inviteLinkRepository.findById(linkId)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));
    }

    /**
     * Get invite link by ID with conversation validation
     */
    public InviteLinkResponse getInviteLinkById(Long userId, Long conversationId, Long linkId) {
        // Get invite link
        GroupInviteLink inviteLink = inviteLinkRepository.findById(linkId)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        // Verify it belongs to this conversation
        if (!inviteLink.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Invite link not found in this conversation", "INVITE_LINK_NOT_FOUND");
        }

        // Check if user is a participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this group");
        }

        return toResponse(inviteLink);
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        } while (inviteLinkRepository.findByToken(token).isPresent());
        return token;
    }

    private InviteLinkResponse toResponse(GroupInviteLink inviteLink) {
        return InviteLinkResponse.builder()
                .id(inviteLink.getId())
                .token(inviteLink.getToken())
                .conversationId(inviteLink.getConversation().getId())
                .createdBy(inviteLink.getCreatedBy().getId())
                .createdByUsername(inviteLink.getCreatedBy().getUsername())
                .createdAt(inviteLink.getCreatedAt())
                .expiresAt(inviteLink.getExpiresAt())
                .maxUses(inviteLink.getMaxUses())
                .currentUses(inviteLink.getCurrentUses())
                .revoked(inviteLink.getRevoked())
                .revokedAt(inviteLink.getRevokedAt())
                .revokedBy(inviteLink.getRevokedBy() != null ? inviteLink.getRevokedBy().getId() : null)
                .valid(inviteLink.isValid())
                .build();
    }
}
