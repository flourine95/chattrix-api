package com.chattrix.api.websocket.handlers.typing;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.ChatSessionService;
import com.chattrix.api.services.TypingIndicatorService;
import com.chattrix.api.websocket.dto.TypingIndicatorDto;
import com.chattrix.api.websocket.dto.TypingIndicatorResponseDto;
import com.chattrix.api.websocket.dto.TypingUserDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.chattrix.api.websocket.handlers.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for typing stop messages.
 * Processes when a user stops typing in a conversation and broadcasts
 * the updated typing indicator status to other participants.
 * 
 * Validates: Requirements 1.1, 1.2, 1.4, 1.5, 4.4, 5.2, 5.3, 5.4, 5.5
 */
@ApplicationScoped
public class TypingStopHandler implements MessageHandler {
    
    private static final Logger LOGGER = Logger.getLogger(TypingStopHandler.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Inject
    private TypingIndicatorService typingIndicatorService;
    
    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private WebSocketMapper webSocketMapper;
    
    @Inject
    private ChatSessionService chatSessionService;
    
    @Inject
    private UserRepository userRepository;
    
    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            // Convert payload to DTO
            TypingIndicatorDto dto = objectMapper.convertValue(payload, TypingIndicatorDto.class);
            
            Long conversationId = dto.getConversationId();
            
            if (conversationId == null) {
                LOGGER.log(Level.WARNING, "Typing stop message missing conversationId from user: {0}", userId);
                return;
            }
            
            LOGGER.log(Level.FINE, "Processing typing stop - userId: {0}, conversationId: {1}", 
                new Object[]{userId, conversationId});
            
            // Validate conversation exists and user is participant
            Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
            
            // Validate user is a participant of the conversation
            boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
            
            if (!isParticipant) {
                LOGGER.log(Level.WARNING, "User {0} is not a participant of conversation {1}", 
                    new Object[]{userId, conversationId});
                return;
            }
            
            // Mark user as stopped typing
            typingIndicatorService.stopTyping(conversationId, userId);
            LOGGER.log(Level.FINE, "User {0} marked as stopped typing in conversation {1}", 
                new Object[]{userId, conversationId});
            
            // Broadcast updated typing status to other participants
            broadcastTypingIndicator(conversation, userId);
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid typing stop request from user " + userId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing typing stop for user " + userId, e);
        }
    }
    
    @Override
    public String getMessageType() {
        return "typing.stop";
    }
    
    /**
     * Broadcast typing indicator to all participants in the conversation
     */
    private void broadcastTypingIndicator(Conversation conversation, Long excludeUserId) {
        Long conversationId = conversation.getId();
        
        // Get ALL typing users first (for debugging)
        Set<Long> allTypingUsers = typingIndicatorService.getTypingUsersInConversation(conversationId, null);
        LOGGER.log(Level.FINE, "All typing users in conversation {0}: {1}", 
            new Object[]{conversationId, allTypingUsers});
        
        // For typing indicators, we want to show OTHER users who are typing
        // But if we're testing with single user, we might want to see our own typing for debugging
        Set<Long> typingUserIds;
        
        // If there's only one participant (testing scenario), include all typing users
        if (conversation.getParticipants().size() <= 1) {
            typingUserIds = allTypingUsers;
            LOGGER.log(Level.FINE, "Single user conversation - showing all typing users: {0}", typingUserIds);
        } else {
            // Normal case: exclude the user who triggered the event
            typingUserIds = typingIndicatorService.getTypingUsersInConversation(conversationId, excludeUserId);
            LOGGER.log(Level.FINE, "Multi-user conversation - typing users (excluding {0}): {1}", 
                new Object[]{excludeUserId, typingUserIds});
        }
        
        // Convert user IDs to detailed user information using mapper
        List<TypingUserDto> typingUsers = typingUserIds.stream()
            .map(userRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(webSocketMapper::toTypingUserResponse)
            .collect(java.util.stream.Collectors.toList());
        
        LOGGER.log(Level.FINE, "Final typing users to broadcast: {0} users - {1}", 
            new Object[]{typingUsers.size(), 
                typingUsers.stream().map(TypingUserDto::getUsername).toList()});
        
        // Create response
        TypingIndicatorResponseDto response = new TypingIndicatorResponseDto(conversationId, typingUsers);
        WebSocketMessage<TypingIndicatorResponseDto> message = 
            new WebSocketMessage<>("typing.indicator", response);
        
        // Broadcast to all participants
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            LOGGER.log(Level.FINE, "Broadcasting typing indicator to participant: {0}", participantId);
            chatSessionService.sendMessageToUser(participantId, message);
        });
    }
}
