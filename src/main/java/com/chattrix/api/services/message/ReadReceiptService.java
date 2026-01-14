package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.services.cache.UnreadCountCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing read receipts and unread counts
 */
@ApplicationScoped
@Slf4j
public class ReadReceiptService {

    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private ConversationParticipantRepository participantRepository;
    
    @Inject
    private MessageRepository messageRepository;
    
    @Inject
    private UnreadCountCache unreadCountCache;

    /**
     * Mark all messages in conversation as read
     * Resets unread count to 0
     */
    @Transactional
    public void markConversationAsRead(Long userId, Long conversationId, Long lastMessageId) {
        log.debug("Marking conversation {} as read for user {}, lastMessageId: {}", 
                conversationId, userId, lastMessageId);
        
        // Validate conversation exists and user is participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));
        
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant in this conversation");
        }
        
        // If lastMessageId not provided, get the latest message ID
        if (lastMessageId == null) {
            lastMessageId = messageRepository.findLatestMessageId(conversationId).orElse(null);
        }
        
        // Reset unread count in database
        participantRepository.resetUnreadCount(conversationId, userId, lastMessageId);
        
        // Reset unread count in cache
        unreadCountCache.reset(conversationId, userId);
        
        log.info("Marked conversation {} as read for user {}, lastReadMessageId: {}", 
                conversationId, userId, lastMessageId);
    }

    /**
     * Get total unread count across all conversations for a user
     */
    public int getTotalUnreadCount(Long userId) {
        Long total = participantRepository.getTotalUnreadCount(userId);
        return total != null ? total.intValue() : 0;
    }
}
