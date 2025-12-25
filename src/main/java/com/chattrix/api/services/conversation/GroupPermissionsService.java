package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.GroupPermissions;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.GroupPermissionsRepository;
import com.chattrix.api.requests.UpdateGroupPermissionsRequest;
import com.chattrix.api.responses.GroupPermissionsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GroupPermissionsService {
    
    @Inject
    private GroupPermissionsRepository permissionsRepository;
    
    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private ConversationParticipantRepository participantRepository;
    
    /**
     * Get permissions for a group conversation
     */
    public GroupPermissionsResponse getGroupPermissions(Long userId, Long conversationId) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));
        
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
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
        
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
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
            permissions.setSendMessages(GroupPermissions.PermissionLevel.valueOf(request.getSendMessages()));
        }
        if (request.getAddMembers() != null) {
            permissions.setAddMembers(GroupPermissions.PermissionLevel.valueOf(request.getAddMembers()));
        }
        if (request.getRemoveMembers() != null) {
            permissions.setRemoveMembers(GroupPermissions.PermissionLevel.valueOf(request.getRemoveMembers()));
        }
        if (request.getEditGroupInfo() != null) {
            permissions.setEditGroupInfo(GroupPermissions.PermissionLevel.valueOf(request.getEditGroupInfo()));
        }
        if (request.getPinMessages() != null) {
            permissions.setPinMessages(GroupPermissions.PermissionLevel.valueOf(request.getPinMessages()));
        }
        if (request.getDeleteMessages() != null) {
            permissions.setDeleteMessages(GroupPermissions.DeletePermissionLevel.valueOf(request.getDeleteMessages()));
        }
        if (request.getCreatePolls() != null) {
            permissions.setCreatePolls(GroupPermissions.PermissionLevel.valueOf(request.getCreatePolls()));
        }
        
        permissionsRepository.save(permissions);
        
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
        if (conversation.getType() == Conversation.ConversationType.DIRECT) {
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
            case "send_messages" -> isAdmin || permissions.getSendMessages() == GroupPermissions.PermissionLevel.ALL;
            case "add_members" -> isAdmin || permissions.getAddMembers() == GroupPermissions.PermissionLevel.ALL;
            case "remove_members" -> isAdmin; // Always admin only
            case "edit_group_info" -> isAdmin || permissions.getEditGroupInfo() == GroupPermissions.PermissionLevel.ALL;
            case "pin_messages" -> isAdmin || permissions.getPinMessages() == GroupPermissions.PermissionLevel.ALL;
            case "create_polls" -> isAdmin || permissions.getCreatePolls() == GroupPermissions.PermissionLevel.ALL;
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
        if (conversation.getType() == Conversation.ConversationType.DIRECT) {
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
                .sendMessages(GroupPermissions.PermissionLevel.ALL)
                .addMembers(GroupPermissions.PermissionLevel.ADMIN_ONLY)
                .removeMembers(GroupPermissions.PermissionLevel.ADMIN_ONLY)
                .editGroupInfo(GroupPermissions.PermissionLevel.ADMIN_ONLY)
                .pinMessages(GroupPermissions.PermissionLevel.ADMIN_ONLY)
                .deleteMessages(GroupPermissions.DeletePermissionLevel.ADMIN_ONLY)
                .createPolls(GroupPermissions.PermissionLevel.ALL)
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
