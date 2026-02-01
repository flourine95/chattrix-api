package com.chattrix.api.mappers;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.responses.ConversationResponse;
import com.chattrix.api.responses.ConversationSettingsResponse;
import org.hibernate.LazyInitializationException;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ConversationMapper {

    @Mapping(target = "unreadCount", ignore = true)
    @Mapping(target = "settings", ignore = true)
    ConversationResponse toResponse(Conversation conversation);

    List<ConversationResponse> toResponseList(List<Conversation> conversations);
    
    /**
     * Map conversation to response with unreadCount extracted for specific user.
     * Uses MapStruct expression to automatically extract unreadCount from participants.
     */
    @Mapping(target = "unreadCount", expression = "java(extractUnreadCount(conversation, userId))")
    @Mapping(target = "settings", expression = "java(extractSettings(conversation, userId))")
    ConversationResponse toResponseWithUnreadCount(Conversation conversation, Long userId);

    /**
     * Map list of conversations with unreadCount for specific user.
     */
    default List<ConversationResponse> toResponseListWithUnreadCount(List<Conversation> conversations, Long userId) {
        return conversations.stream()
                .map(conv -> toResponseWithUnreadCount(conv, userId))
                .toList();
    }

    /**
     * Extract unreadCount for specific user from conversation participants.
     * IMPORTANT: This method accesses lazy-loaded collections. Ensure the conversation
     * entity is fetched with participants JOIN FETCHED in the repository query.
     */
    default Integer extractUnreadCount(Conversation conversation, Long userId) {
        if (conversation.getParticipants() == null || userId == null) {
            return 0;
        }
        
        try {
            return conversation.getParticipants().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                    .findFirst()
                    .map(ConversationParticipant::getUnreadCount)
                    .orElse(0);
        } catch (LazyInitializationException e) {
            // Fallback if participants not fetched - return 0
            return 0;
        }
    }

    /**
     * Extract user-specific settings from conversation participant.
     * IMPORTANT: This method accesses lazy-loaded collections. Ensure the conversation
     * entity is fetched with participants JOIN FETCHED in the repository query.
     */
    default ConversationSettingsResponse extractSettings(Conversation conversation, Long userId) {
        if (conversation.getParticipants() == null || userId == null) {
            return null;
        }
        
        try {
            return conversation.getParticipants().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                    .findFirst()
                    .map(p -> ConversationSettingsResponse.builder()
                            .conversationId(conversation.getId())
                            .muted(p.isMuted())
                            .mutedUntil(p.getMutedUntil())
                            .pinned(p.isPinned())
                            .pinOrder(p.getPinOrder())
                            .archived(p.isArchived())
                            .notificationsEnabled(!p.isMuted()) // Inverse of muted
                            .build())
                    .orElse(null);
        } catch (LazyInitializationException e) {
            // Fallback if participants not fetched - return null
            return null;
        }
    }

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "lastSeen", source = "user.lastSeen")
    @Mapping(target = "online", ignore = true) // Online status from cache
    public abstract ConversationResponse.ParticipantResponse toParticipantResponse(ConversationParticipant participant);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    @Mapping(target = "senderAvatarUrl", source = "sender.avatarUrl")
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    public abstract ConversationResponse.MessageResponse toMessageResponse(Message message);
}
