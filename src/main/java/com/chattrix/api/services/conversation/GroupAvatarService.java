package com.chattrix.api.services.conversation;

import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.ConversationMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.requests.UpdateGroupAvatarRequest;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.services.message.SystemMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
@ApplicationScoped
public class GroupAvatarService {

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationMapper conversationMapper;

    @Inject
    private GroupPermissionsService groupPermissionsService;

    @Inject
    private SystemMessageService systemMessageService;

    /**
     * Update group avatar URL (from Cloudinary)
     */
    @Transactional
    public ConversationResponse updateAvatar(Long userId, Long conversationId, UpdateGroupAvatarRequest request) {
        // Verify conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Can only update avatar for group conversations", "BAD_REQUEST");
        }

        // Check permission
        if (!groupPermissionsService.hasPermission(conversationId, userId, "edit_group_info")) {
            throw BusinessException.forbidden("You don't have permission to edit group avatar");
        }

        // Validate avatar URL
        if (request.getAvatarUrl() == null || request.getAvatarUrl().trim().isEmpty()) {
            throw BusinessException.badRequest("Avatar URL is required", "BAD_REQUEST");
        }

        // Update conversation avatar URL
        conversation.setAvatarUrl(request.getAvatarUrl());
        conversationRepository.save(conversation);

        // Create system message
        systemMessageService.createGroupAvatarChangedMessage(conversationId, userId);

        return conversationMapper.toResponse(conversation);
    }

    /**
     * Delete group avatar
     */
    @Transactional
    public ConversationResponse deleteAvatar(Long userId, Long conversationId) {
        // Verify conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Can only delete avatar for group conversations", "BAD_REQUEST");
        }

        // Check permission
        if (!groupPermissionsService.hasPermission(conversationId, userId, "edit_group_info")) {
            throw BusinessException.forbidden("You don't have permission to edit group avatar");
        }

        // Update conversation (set avatar to null)
        conversation.setAvatarUrl(null);
        conversationRepository.save(conversation);

        return conversationMapper.toResponse(conversation);
    }
}
