package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PinnedMessageService {
    
    private static final int MAX_PINNED_MESSAGES = 3;
    
    @Inject
    private MessageRepository messageRepository;
    
    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private ConversationParticipantRepository participantRepository;
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private MessageMapper messageMapper;
    
    @Inject
    private ChatSessionService chatSessionService;
    
    @Inject
    private com.chattrix.api.services.conversation.GroupPermissionsService groupPermissionsService;
    
    @Transactional
    public MessageResponse pinMessage(Long userId, Long conversationId, Long messageId) {
        // Validate conversation exists
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));
        
        // Validate user is participant
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.forbidden("You are not a participant of this conversation"));
        
        // Check permission
        if (!groupPermissionsService.hasPermission(conversationId, userId, "pin_messages")) {
            throw BusinessException.forbidden("You don't have permission to pin messages");
        }
        
        // Get message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "MESSAGE_NOT_FOUND"));
        
        // Validate message belongs to conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.badRequest("Message does not belong to this conversation", "INVALID_MESSAGE");
        }
        
        // Check if already pinned
        if (message.isPinned()) {
            throw BusinessException.badRequest("Message is already pinned", "ALREADY_PINNED");
        }
        
        // Check pinned message limit
        long pinnedCount = messageRepository.countPinnedMessages(conversationId);
        if (pinnedCount >= MAX_PINNED_MESSAGES) {
            throw BusinessException.badRequest(
                "Maximum " + MAX_PINNED_MESSAGES + " messages can be pinned", 
                "MAX_PINNED_REACHED"
            );
        }
        
        // Get user who is pinning
        User pinningUser = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));
        
        // Pin the message
        message.setPinned(true);
        message.setPinnedAt(Instant.now());
        message.setPinnedBy(pinningUser);
        messageRepository.save(message);
        
        // Send WebSocket notification
        MessageResponse response = messageMapper.toResponse(message);
        sendPinNotification(conversationId, "MESSAGE_PINNED", response);
        
        return response;
    }
    
    @Transactional
    public MessageResponse unpinMessage(Long userId, Long conversationId, Long messageId) {
        // Validate conversation exists
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));
        
        // Validate user is participant
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.forbidden("You are not a participant of this conversation"));
        
        // Check permission
        if (!groupPermissionsService.hasPermission(conversationId, userId, "pin_messages")) {
            throw BusinessException.forbidden("You don't have permission to unpin messages");
        }
        
        // Get message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "MESSAGE_NOT_FOUND"));
        
        // Validate message belongs to conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.badRequest("Message does not belong to this conversation", "INVALID_MESSAGE");
        }
        
        // Check if message is pinned
        if (!message.isPinned()) {
            throw BusinessException.badRequest("Message is not pinned", "NOT_PINNED");
        }
        
        // Unpin the message
        message.setPinned(false);
        message.setPinnedAt(null);
        message.setPinnedBy(null);
        messageRepository.save(message);
        
        // Send WebSocket notification
        MessageResponse response = messageMapper.toResponse(message);
        sendPinNotification(conversationId, "MESSAGE_UNPINNED", response);
        
        return response;
    }
    
    public List<MessageResponse> getPinnedMessages(Long userId, Long conversationId) {
        // Validate conversation exists
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));
        
        // Validate user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }
        
        // Get pinned messages
        List<Message> pinnedMessages = messageRepository.findPinnedMessages(conversationId);
        
        return pinnedMessages.stream()
                .map(messageMapper::toResponse)
                .toList();
    }
    
    private void sendPinNotification(Long conversationId, String eventType, MessageResponse messageResponse) {
        try {
            MessagePinEventDto data = MessagePinEventDto.builder()
                    .action(eventType)
                    .message(messageResponse)
                    .build();
            
            WebSocketMessage<MessagePinEventDto> message = new WebSocketMessage<>(WebSocketEventType.MESSAGE_PIN, data);
            
            List<Long> participantIds = participantRepository
                    .findByConversationId(conversationId)
                    .stream()
                    .map(cp -> cp.getUser().getId())
                    .toList();
            
            for (Long participantId : participantIds) {
                try {
                    chatSessionService.sendDirectMessage(participantId, message);
                } catch (Exception e) {
                    System.err.println("Failed to send pin notification to user " + participantId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send pin notification: " + e.getMessage());
        }
    }
}
