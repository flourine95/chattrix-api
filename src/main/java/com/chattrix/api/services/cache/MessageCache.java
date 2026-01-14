package com.chattrix.api.services.cache;

import com.chattrix.api.responses.MessageResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cache for recent messages in conversations
 * Supports Write-Behind pattern: messages cached before DB insert
 * 
 * Cache structure: conversationId → UnflushedMessages (Map + List for fast lookup)
 */
@ApplicationScoped
@Slf4j
public class MessageCache {
    
    private static final int CACHE_EXPIRY_MINUTES = 5;
    private static final int MAX_CACHE_SIZE = 5_000;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;  // Increased for write-behind
    
    /**
     * Container for unflushed messages with both Map (O(1) lookup) and List (ordering)
     */
    private static class UnflushedMessages {
        // Map for O(1) lookup by ID
        private final Map<Long, MessageResponse> messageMap = new ConcurrentHashMap<>();
        // List for ordering (most recent first)
        private final List<MessageResponse> messageList = new ArrayList<>();
        
        public synchronized void add(MessageResponse message) {
            messageMap.put(message.getId(), message);
            messageList.add(0, message);  // Prepend (most recent first)
            
            // Keep only MAX_MESSAGES_PER_CONVERSATION
            if (messageList.size() > MAX_MESSAGES_PER_CONVERSATION) {
                MessageResponse removed = messageList.remove(messageList.size() - 1);
                messageMap.remove(removed.getId());
            }
        }
        
        public MessageResponse get(Long id) {
            return messageMap.get(id);
        }
        
        public synchronized List<MessageResponse> getAll() {
            return new ArrayList<>(messageList);
        }
        
        public synchronized void updateId(Long tempId, Long realId) {
            MessageResponse message = messageMap.remove(tempId);
            if (message != null) {
                message.setId(realId);
                messageMap.put(realId, message);
            }
        }
        
        public synchronized void remove(Long id) {
            MessageResponse message = messageMap.remove(id);
            if (message != null) {
                messageList.remove(message);
            }
        }
        
        public int size() {
            return messageMap.size();
        }
        
        public boolean isEmpty() {
            return messageMap.isEmpty();
        }
    }
    
    // Cache key: conversationId → UnflushedMessages
    private final Cache<Long, UnflushedMessages> cache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
        .maximumSize(MAX_CACHE_SIZE)
        .recordStats()
        .build();
    
    /**
     * Get unflushed messages from cache (Write-Behind pattern)
     * These are messages waiting to be batch inserted to DB
     */
    public List<MessageResponse> getUnflushed(Long conversationId) {
        UnflushedMessages messages = cache.getIfPresent(conversationId);
        return messages != null ? messages.getAll() : new ArrayList<>();
    }
    
    /**
     * Get single unflushed message by ID (O(1) lookup)
     */
    public MessageResponse getUnflushedById(Long conversationId, Long messageId) {
        UnflushedMessages messages = cache.getIfPresent(conversationId);
        return messages != null ? messages.get(messageId) : null;
    }
    
    /**
     * Add message to cache (Write-Behind: before DB insert)
     * Message will be flushed to DB by MessageBatchService
     */
    public void addUnflushed(Long conversationId, MessageResponse message) {
        UnflushedMessages messages = cache.get(conversationId, k -> new UnflushedMessages());
        messages.add(message);
        
        log.debug("Added unflushed message {} to conversation {}. Total unflushed: {}", 
                message.getId(), conversationId, messages.size());
    }
    
    /**
     * Remove messages from cache after successful DB flush
     * Called by MessageBatchService after batch insert
     */
    public void removeAfterFlush(Long conversationId, List<Long> flushedIds) {
        UnflushedMessages messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            flushedIds.forEach(messages::remove);
            
            if (messages.isEmpty()) {
                cache.invalidate(conversationId);
            }
        }
    }
    
    /**
     * Update temp ID to real ID after DB flush
     * Called by MessageBatchService after batch insert
     */
    public void updateMessageId(Long conversationId, Long tempId, Long realId) {
        UnflushedMessages messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            messages.updateId(tempId, realId);
        }
    }
    
    /**
     * Update message in cache (for edit/update operations)
     * Incremental update instead of full invalidation
     */
    public void updateMessage(Long conversationId, MessageResponse updatedMessage) {
        UnflushedMessages messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            MessageResponse existing = messages.get(updatedMessage.getId());
            if (existing != null) {
                // Remove old and add updated
                messages.remove(updatedMessage.getId());
                messages.add(updatedMessage);
                log.debug("Updated message in cache: conversationId={}, messageId={}", 
                        conversationId, updatedMessage.getId());
            }
        }
    }
    
    /**
     * Add new message to cache (incremental update)
     * More efficient than invalidating entire cache
     */
    public void addMessage(Long conversationId, MessageResponse message) {
        UnflushedMessages messages = cache.get(conversationId, k -> new UnflushedMessages());
        messages.add(message);
        
        log.debug("Added message to cache: conversationId={}, messageId={}", 
                conversationId, message.getId());
    }
    
    /**
     * Remove message from cache (for delete operations)
     */
    public void removeMessage(Long conversationId, Long messageId) {
        UnflushedMessages messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            messages.remove(messageId);
            
            if (messages.isEmpty()) {
                cache.invalidate(conversationId);
            }
        }
    }
    
    /**
     * Invalidate messages for conversation
     */
    public void invalidate(Long conversationId) {
        cache.invalidate(conversationId);
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        cache.invalidateAll();
    }
    
    /**
     * Get cache statistics
     */
    public String getStats() {
        return String.format(
            "MessageCache - Size: %d, Hit Rate: %.2f%%, Evictions: %d",
            cache.estimatedSize(),
            cache.stats().hitRate() * 100,
            cache.stats().evictionCount()
        );
    }
}
