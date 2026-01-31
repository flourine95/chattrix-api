package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.GroupPermissions;
import com.chattrix.api.entities.Message;
import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.enums.DeletePermissionLevel;
import com.chattrix.api.enums.PermissionLevel;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.GroupPermissionsRepository;
import com.chattrix.api.requests.UpdateGroupPermissionsRequest;
import com.chattrix.api.responses.GroupPermissionsResponse;
import com.chattrix.api.services.message.MessageCreationService;
import com.chattrix.api.services.message.SystemMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashMap;

@ApplicationScoped
public class GroupPermissionsService {

    @Inject
    private GroupPermissionsRepository permissionsRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private ConversationBroadcastService conversationBroadcastService;

    @Inject
    private SystemMessageService systemMessageService;

    @Inject
    private MessageCreationService messageCreationService;

    /**
     * Get permissions for a group conversation
     */
    public GroupPermissionsResponse getGroupPermissions(Long userId, Long conversationId) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        if (conversation.isGroupConversation()) {
            throw BusinessException.badRequest("Permissions only apply to group conversations", "INVALID_CONVERSATION_TYPE");
        }

        // Validate user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        // Get or create default permissions
        GroupPermissions permissions = permissionsRepository.findByConversationId(conversationId)
                .orElseGet(() -> createDefaultPermissions(conversation));

        return toResponse(permissions);
    }

    /**
     * Update permissions for a group conversation (admin only)
     */
    @Transactional
    public GroupPermissionsResponse updateGroupPermissions(Long userId, Long conversationId, UpdateGroupPermissionsRequest request) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        if (conversation.isGroupConversation()) {
            throw BusinessException.badRequest("Permissions only apply to group conversations", "INVALID_CONVERSATION_TYPE");
        }

        // Validate user is admin
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only admins can update group permissions");
        }

        // Get or create permissions
        GroupPermissions permissions = permissionsRepository.findByConversationId(conversationId)
                .orElseGet(() -> createDefaultPermissions(conversation));

        // Update permissions
        if (request.getSendMessages() != null) {
            permissions.setSendMessages(PermissionLevel.valueOf(request.getSendMessages()));
        }
        if (request.getAddMembers() != null) {
            permissions.setAddMembers(PermissionLevel.valueOf(request.getAddMembers()));
        }
        if (request.getRemoveMembers() != null) {
            permissions.setRemoveMembers(PermissionLevel.valueOf(request.getRemoveMembers()));
        }
        if (request.getEditGroupInfo() != null) {
            permissions.setEditGroupInfo(PermissionLevel.valueOf(request.getEditGroupInfo()));
        }
        if (request.getPinMessages() != null) {
            permissions.setPinMessages(PermissionLevel.valueOf(request.getPinMessages()));
        }
        if (request.getDeleteMessages() != null) {
            permissions.setDeleteMessages(DeletePermissionLevel.valueOf(request.getDeleteMessages()));
        }
        if (request.getCreatePolls() != null) {
            permissions.setCreatePolls(PermissionLevel.valueOf(request.getCreatePolls()));
        }

        permissionsRepository.save(permissions);

        // Build permissions map for broadcasting
        java.util.Map<String, String> permissionsMap = new HashMap<>();
        if (request.getSendMessages() != null) permissionsMap.put("sendMessages", request.getSendMessages());
        if (request.getAddMembers() != null) permissionsMap.put("addMembers", request.getAddMembers());
        if (request.getRemoveMembers() != null) permissionsMap.put("removeMembers", request.getRemoveMembers());
        if (request.getEditGroupInfo() != null) permissionsMap.put("editGroupInfo", request.getEditGroupInfo());
        if (request.getPinMessages() != null) permissionsMap.put("pinMessages", request.getPinMessages());
        if (request.getDeleteMessages() != null) permissionsMap.put("deleteMessages", request.getDeleteMessages());
        if (request.getCreatePolls() != null) permissionsMap.put("createPolls", request.getCreatePolls());

        // Create system message for permissions change
        Message systemMessage = systemMessageService.createPermissionsChangedMessage(
                conversationId,
                userId,
                permissionsMap
        );

        // Broadcast system message to all participants
        messageCreationService.broadcastMessage(systemMessage, conversation);

        // Broadcast permissions updated event

        com.chattrix.api.entities.User actionByUser = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> user.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        conversationBroadcastService.broadcastPermissionsUpdated(
                conversation,
                permissionsMap,
                userId,
                actionByUser.getUsername()
        );

        return toResponse(permissions);
    }

    /**
     * Check if user has permission to perform an action
     */
    public boolean hasPermission(Long conversationId, Long userId, String action) {
        // Get conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null) {
            return false;
        }

        // Direct conversations have no restrictions
        if (conversation.getType() == ConversationType.DIRECT) {
            return true;
        }

        // Get user's role
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(null);

        if (participant == null) {
            return false;
        }

        boolean isAdmin = participant.getRole() == ConversationParticipant.Role.ADMIN;

        // Get permissions
        GroupPermissions permissions = permissionsRepository.findByConversationId(conversationId)
                .orElseGet(() -> createDefaultPermissions(conversation));

        // Check permission based on action
        return switch (action) {
            case "send_messages" -> isAdmin || permissions.getSendMessages() == PermissionLevel.ALL;
            case "add_members" -> isAdmin || permissions.getAddMembers() == PermissionLevel.ALL;
            case "remove_members" -> isAdmin; // Always admin only
            case "edit_group_info" -> isAdmin || permissions.getEditGroupInfo() == PermissionLevel.ALL;
            case "pin_messages" -> isAdmin || permissions.getPinMessages() == PermissionLevel.ALL;
            case "create_polls" -> isAdmin || permissions.getCreatePolls() == PermissionLevel.ALL;
            default -> false;
        };
    }

    /**
     * Check if user can delete a specific message
     */
    public boolean canDeleteMessage(Long conversationId, Long userId, Long messageOwnerId) {
        // Get conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null) {
            return false;
        }

        // Direct conversations - only owner can delete
        if (conversation.getType() == ConversationType.DIRECT) {
            return userId.equals(messageOwnerId);
        }

        // Get user's role
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(null);

        if (participant == null) {
            return false;
        }

        boolean isAdmin = participant.getRole() == ConversationParticipant.Role.ADMIN;
        boolean isOwner = userId.equals(messageOwnerId);

        // Get permissions
        GroupPermissions permissions = permissionsRepository.findByConversationId(conversationId)
                .orElseGet(() -> createDefaultPermissions(conversation));

        return switch (permissions.getDeleteMessages()) {
            case OWNER -> isOwner;
            case ADMIN_ONLY -> isAdmin;
            case ALL -> true;
        };
    }

    @Transactional
    private GroupPermissions createDefaultPermissions(Conversation conversation) {
        GroupPermissions permissions = GroupPermissions.builder()
                .conversation(conversation)
                .sendMessages(PermissionLevel.ALL)
                .addMembers(PermissionLevel.ADMIN_ONLY)
                .removeMembers(PermissionLevel.ADMIN_ONLY)
                .editGroupInfo(PermissionLevel.ADMIN_ONLY)
                .pinMessages(PermissionLevel.ADMIN_ONLY)
                .deleteMessages(DeletePermissionLevel.ADMIN_ONLY)
                .createPolls(PermissionLevel.ALL)
                .build();

        return permissionsRepository.save(permissions);
    }

    private GroupPermissionsResponse toResponse(GroupPermissions permissions) {
        return GroupPermissionsResponse.builder()
                .conversationId(permissions.getConversation().getId())
                .sendMessages(permissions.getSendMessages().name())
                .addMembers(permissions.getAddMembers().name())
                .removeMembers(permissions.getRemoveMembers().name())
                .editGroupInfo(permissions.getEditGroupInfo().name())
                .pinMessages(permissions.getPinMessages().name())
                .deleteMessages(permissions.getDeleteMessages().name())
                .createPolls(permissions.getCreatePolls().name())
                .build();
    }
}
