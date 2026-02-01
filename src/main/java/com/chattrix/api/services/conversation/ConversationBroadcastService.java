package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.ConversationMapper;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class ConversationBroadcastService {

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private ConversationMapper conversationMapper;

    /**
     * Broadcast conversation created event to all participants
     */
    public void broadcastConversationCreated(Conversation conversation, Long createdBy, String createdByUsername) {
        try {
            log.debug("Broadcasting conversation created: conversationId={}, createdBy={}",
                    conversation.getId(), createdBy);

            // Map conversation to response for each participant
            conversation.getParticipants().forEach(participant -> {
                Long userId = participant.getUser().getId();
                ConversationResponse conversationResponse = conversationMapper.toResponseWithUnreadCount(conversation, userId);

                ConversationCreatedDto dto = ConversationCreatedDto.builder()
                        .conversation(conversationResponse)
                        .createdBy(createdBy)
                        .createdByUsername(createdByUsername)
                        .build();

                WebSocketMessage<ConversationCreatedDto> message =
                        new WebSocketMessage<>(WebSocketEventType.CONVERSATION_CREATED, dto);

                chatSessionService.sendMessageToUser(userId, message);
            });

            log.info("Broadcasted conversation created to {} participants",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast conversation created: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast conversation updated event (name, avatar, description)
     */
    public void broadcastConversationUpdated(Conversation conversation, Long updatedBy, String updatedByUsername) {
        try {
            log.debug("Broadcasting conversation updated: conversationId={}, updatedBy={}",
                    conversation.getId(), updatedBy);

            ConversationUpdatedDto dto = ConversationUpdatedDto.builder()
                    .conversationId(conversation.getId())
                    .name(conversation.getName())
                    .avatarUrl(conversation.getAvatarUrl())
                    .description(conversation.getDescription())
                    .updatedBy(updatedBy)
                    .updatedByUsername(updatedByUsername)
                    .updatedAt(Instant.now())
                    .build();

            WebSocketMessage<ConversationUpdatedDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATED, dto);

            broadcastToAllParticipants(conversation, message);

            log.info("Broadcasted conversation updated to {} participants",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast conversation updated: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast members added event
     */
    public void broadcastMembersAdded(Conversation conversation, List<User> addedUsers, Long actionBy, String actionByUsername) {
        try {
            log.debug("Broadcasting members added: conversationId={}, count={}, actionBy={}",
                    conversation.getId(), addedUsers.size(), actionBy);

            List<ConversationMemberDto.MemberInfo> memberInfos = addedUsers.stream()
                    .map(user -> ConversationMemberDto.MemberInfo.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .fullName(user.getFullName())
                            .avatarUrl(user.getAvatarUrl())
                            .role("MEMBER")
                            .build())
                    .collect(Collectors.toList());

            ConversationMemberDto dto = ConversationMemberDto.builder()
                    .conversationId(conversation.getId())
                    .members(memberInfos)
                    .actionBy(actionBy)
                    .actionByUsername(actionByUsername)
                    .timestamp(Instant.now())
                    .build();

            WebSocketMessage<ConversationMemberDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_MEMBER_ADDED, dto);

            // Log all participants who will receive the event
            List<Long> participantIds = conversation.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .collect(Collectors.toList());
            log.info("Broadcasting CONVERSATION_MEMBER_ADDED to participants: {}", participantIds);

            broadcastToAllParticipants(conversation, message);

            log.info("Broadcasted members added to {} participants",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast members added: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast member removed event (kicked)
     */
    public void broadcastMemberRemoved(Conversation conversation, User removedUser, Long actionBy, String actionByUsername) {
        try {
            log.debug("Broadcasting member removed: conversationId={}, removedUserId={}, actionBy={}",
                    conversation.getId(), removedUser.getId(), actionBy);

            ConversationMemberDto.MemberInfo memberInfo = ConversationMemberDto.MemberInfo.builder()
                    .userId(removedUser.getId())
                    .username(removedUser.getUsername())
                    .fullName(removedUser.getFullName())
                    .avatarUrl(removedUser.getAvatarUrl())
                    .build();

            ConversationMemberDto dto = ConversationMemberDto.builder()
                    .conversationId(conversation.getId())
                    .members(List.of(memberInfo))
                    .actionBy(actionBy)
                    .actionByUsername(actionByUsername)
                    .timestamp(Instant.now())
                    .build();

            WebSocketMessage<ConversationMemberDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_MEMBER_REMOVED, dto);

            // Broadcast to remaining participants + removed user
            broadcastToAllParticipants(conversation, message);
            chatSessionService.sendMessageToUser(removedUser.getId(), message);

            log.info("Broadcasted member removed to {} participants + removed user",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast member removed: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast member left event (self-leave)
     */
    public void broadcastMemberLeft(Conversation conversation, User leftUser) {
        try {
            log.debug("Broadcasting member left: conversationId={}, userId={}",
                    conversation.getId(), leftUser.getId());

            ConversationMemberDto.MemberInfo memberInfo = ConversationMemberDto.MemberInfo.builder()
                    .userId(leftUser.getId())
                    .username(leftUser.getUsername())
                    .fullName(leftUser.getFullName())
                    .avatarUrl(leftUser.getAvatarUrl())
                    .build();

            ConversationMemberDto dto = ConversationMemberDto.builder()
                    .conversationId(conversation.getId())
                    .members(List.of(memberInfo))
                    .actionBy(leftUser.getId())
                    .actionByUsername(leftUser.getUsername())
                    .timestamp(Instant.now())
                    .build();

            WebSocketMessage<ConversationMemberDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_MEMBER_LEFT, dto);

            broadcastToAllParticipants(conversation, message);

            log.info("Broadcasted member left to {} participants",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast member left: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast role updated event (promote/demote)
     */
    public void broadcastRoleUpdated(Conversation conversation, User targetUser, String oldRole, String newRole,
                                     Long updatedBy, String updatedByUsername) {
        try {
            log.debug("Broadcasting role updated: conversationId={}, userId={}, oldRole={}, newRole={}, updatedBy={}",
                    conversation.getId(), targetUser.getId(), oldRole, newRole, updatedBy);

            ConversationRoleUpdatedDto dto = ConversationRoleUpdatedDto.builder()
                    .conversationId(conversation.getId())
                    .userId(targetUser.getId())
                    .username(targetUser.getUsername())
                    .fullName(targetUser.getFullName())
                    .oldRole(oldRole)
                    .newRole(newRole)
                    .updatedBy(updatedBy)
                    .updatedByUsername(updatedByUsername)
                    .timestamp(Instant.now())
                    .build();

            WebSocketMessage<ConversationRoleUpdatedDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_ROLE_UPDATED, dto);

            broadcastToAllParticipants(conversation, message);

            log.info("Broadcasted role updated to {} participants",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast role updated: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast permissions updated event
     */
    public void broadcastPermissionsUpdated(Conversation conversation, Map<String, String> permissions,
                                            Long updatedBy, String updatedByUsername) {
        try {
            log.debug("Broadcasting permissions updated: conversationId={}, updatedBy={}",
                    conversation.getId(), updatedBy);

            ConversationPermissionsUpdatedDto dto = ConversationPermissionsUpdatedDto.builder()
                    .conversationId(conversation.getId())
                    .permissions(permissions)
                    .updatedBy(updatedBy)
                    .updatedByUsername(updatedByUsername)
                    .timestamp(Instant.now())
                    .build();

            WebSocketMessage<ConversationPermissionsUpdatedDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_PERMISSIONS_UPDATED, dto);

            broadcastToAllParticipants(conversation, message);

            log.info("Broadcasted permissions updated to {} participants",
                    conversation.getParticipants().size());
        } catch (Exception e) {
            log.error("Failed to broadcast permissions updated: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast conversation deleted event
     */
    public void broadcastConversationDeleted(Long conversationId, List<Long> participantIds, String reason) {
        try {
            log.debug("Broadcasting conversation deleted: conversationId={}, reason={}",
                    conversationId, reason);

            ConversationDeletedDto dto = ConversationDeletedDto.builder()
                    .conversationId(conversationId)
                    .reason(reason)
                    .timestamp(Instant.now())
                    .build();

            WebSocketMessage<ConversationDeletedDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_DELETED, dto);

            participantIds.forEach(userId ->
                    chatSessionService.sendMessageToUser(userId, message));

            log.info("Broadcasted conversation deleted to {} participants", participantIds.size());
        } catch (Exception e) {
            log.error("Failed to broadcast conversation deleted: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to broadcast to all participants
     */
    private <T> void broadcastToAllParticipants(Conversation conversation, WebSocketMessage<T> message) {
        conversation.getParticipants().forEach(participant ->
                chatSessionService.sendMessageToUser(participant.getUser().getId(), message));
    }
}
