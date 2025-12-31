package com.chattrix.api.services.message;

import com.chattrix.api.enums.MessageType;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@ApplicationScoped
public class SystemMessageService {
    
    @Inject
    private MessageRepository messageRepository;
    
    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private UserRepository userRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    public Message createUserJoinedMessage(Long conversationId, Long userId) {
        return createSystemMessage(conversationId, userId, "user_joined", null);
    }
    
    @Transactional
    public Message createUserLeftMessage(Long conversationId, Long userId) {
        return createSystemMessage(conversationId, userId, "user_left", null);
    }
    
    @Transactional
    public Message createUserKickedMessage(Long conversationId, Long kickedUserId, Long kickedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("kickedBy", kickedByUserId);
        return createSystemMessage(conversationId, kickedUserId, "user_kicked", metadata);
    }
    
    @Transactional
    public Message createUserAddedMessage(Long conversationId, Long addedUserId, Long addedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("addedBy", addedByUserId);
        return createSystemMessage(conversationId, addedUserId, "user_added", metadata);
    }

    @Transactional
    public Message createUserAddedMessage(Long conversationId, List<Long> addedUserIds, Long addedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("addedBy", addedByUserId);
        metadata.put("addedUserIds", addedUserIds);
        
        // Use the first user in the list as the primary actor for the base fields
        Long primaryUserId = addedUserIds.get(0);
        return createSystemMessage(conversationId, primaryUserId, "users_added", metadata);
    }
    
    @Transactional
    public Message createGroupNameChangedMessage(Long conversationId, Long changedByUserId, String oldName, String newName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("oldName", oldName);
        metadata.put("newName", newName);
        return createSystemMessage(conversationId, changedByUserId, "group_name_changed", metadata);
    }
    
    @Transactional
    public Message createGroupAvatarChangedMessage(Long conversationId, Long changedByUserId) {
        return createSystemMessage(conversationId, changedByUserId, "group_avatar_changed", null);
    }
    
    @Transactional
    public Message createUserPromotedMessage(Long conversationId, Long promotedUserId, Long promotedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("promotedBy", promotedByUserId);
        return createSystemMessage(conversationId, promotedUserId, "user_promoted", metadata);
    }
    
    @Transactional
    public Message createUserDemotedMessage(Long conversationId, Long demotedUserId, Long demotedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("demotedBy", demotedByUserId);
        return createSystemMessage(conversationId, demotedUserId, "user_demoted", metadata);
    }
    
    @Transactional
    public Message createMemberMutedMessage(Long conversationId, Long mutedUserId, Long mutedByUserId, Instant mutedUntil) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mutedBy", mutedByUserId);
        if (mutedUntil != null) {
            metadata.put("mutedUntil", mutedUntil.toEpochMilli());
        }
        return createSystemMessage(conversationId, mutedUserId, "member_muted", metadata);
    }
    
    @Transactional
    public Message createMemberUnmutedMessage(Long conversationId, Long unmutedUserId, Long unmutedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("unmutedBy", unmutedByUserId);
        return createSystemMessage(conversationId, unmutedUserId, "member_unmuted", metadata);
    }
    
    @Transactional
    public Message createUserJoinedViaLinkMessage(Long conversationId, Long userId, Long invitedByUserId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invitedBy", invitedByUserId);
        return createSystemMessage(conversationId, userId, "user_joined_via_link", metadata);
    }
    
    private Message createSystemMessage(Long conversationId, Long actorUserId, String eventType, Map<String, Object> additionalMetadata) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            User actor = userRepository.findById(actorUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Build message content as JSON
            Map<String, Object> content = new HashMap<>();
            content.put("type", eventType);
            content.put("userId", actorUserId);
            content.put("userName", actor.getFullName());
            content.put("username", actor.getUsername());
            content.put("timestamp", Instant.now().toEpochMilli());
            
            if (additionalMetadata != null) {
                content.putAll(additionalMetadata);
            }
            
            String contentJson = objectMapper.writeValueAsString(content);
            
            Message message = new Message();
            message.setConversation(conversation);
            message.setSender(actor);
            message.setContent(contentJson);
            message.setType(MessageType.SYSTEM);
            message.setSentAt(Instant.now());
            
            return messageRepository.save(message);
            
        } catch (Exception e) {
            System.err.println("Failed to create system message: " + e.getMessage());
            throw new RuntimeException("Failed to create system message", e);
        }
    }
}
