package com.chattrix.api.services.cache;

import com.chattrix.api.responses.MessageResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cache for recent messages in conversations
 * Supports Write-Behind pattern: messages cached before DB insert
 * 
 * Cache structure: conversationId → List of MessageResponse (unflushed messages)
 */
@ApplicationScoped
@Slf4j
public class MessageCache {
    
    private static final int CACHE_EXPIRY_MINUTES = 5;
    private static final int MAX_CACHE_SIZE = 5_000;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;  // Increased for write-behind
    
    // Cache key: conversationId → List of recent messages (not yet in DB)
    private final Cache<Long, List<MessageResponse>> cache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
        .maximumSize(MAX_CACHE_SIZE)
        .build();
    
    /**
     * Get unflushed messages from cache (Write-Behind pattern)
     * These are messages waiting to be batch inserted to DB
     */
    public List<MessageResponse> getUnflushed(Long conversationId) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }
    
    /**
     * Add message to cache (Write-Behind: before DB insert)
     * Message will be flushed to DB by MessageBatchService
     */
    public void addUnflushed(Long conversationId, MessageResponse message) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages == null)
            messages = new ArrayList<>();
        
        // Prepend new message (most recent first)
        messages.add(0, message);
        
        // Keep only MAX_MESSAGES_PER_CONVERSATION
        if (messages.size() > MAX_MESSAGES_PER_CONVERSATION)
            messages = messages.subList(0, MAX_MESSAGES_PER_CONVERSATION);
        
        cache.put(conversationId, messages);
    }
    
    /**
     * Remove messages from cache after successful DB flush
     * Called by MessageBatchService after batch insert
     */
    public void removeAfterFlush(Long conversationId, List<Long> flushedIds) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            messages = messages.stream()
                .filter(msg -> !flushedIds.contains(msg.getId()))
                .collect(Collectors.toList());
            
            if (messages.isEmpty())
                cache.invalidate(conversationId);
            else
                cache.put(conversationId, messages);
        }
    }
    
    /**
     * Update temp ID to real ID after DB flush
     * Called by MessageBatchService after batch insert
     */
    public void updateMessageId(Long conversationId, Long tempId, Long realId) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            for (MessageResponse msg : messages) {
                if (msg.getId().equals(tempId)) {
                    msg.setId(realId);
                    break;
                }
            }
            cache.put(conversationId, messages);
        }
    }
    
    /**
     * Update message in cache (for edit/update operations)
     * Incremental update instead of full invalidation
     */
    public void updateMessage(Long conversationId, MessageResponse updatedMessage) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            List<MessageResponse> updatedMessages = new ArrayList<>();
            for (MessageResponse msg : messages) {
                if (msg.getId().equals(updatedMessage.getId()))
                    updatedMessages.add(updatedMessage);
                else
                    updatedMessages.add(msg);
            }
            cache.put(conversationId, updatedMessages);
            log.debug("Updated message in cache: conversationId={}, messageId={}", conversationId, updatedMessage.getId());
        }
    }
    
    /**
     * Add new message to cache (incremental update)
     * More efficient than invalidating entire cache
     */
    public void addMessage(Long conversationId, MessageResponse message) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages == null) {
            messages = new ArrayList<>();
        } else {
            messages = new ArrayList<>(messages); // Create mutable copy
        }
        
        // Prepend new message (most recent first)
        messages.add(0, message);
        
        // Keep only MAX_MESSAGES_PER_CONVERSATION
        if (messages.size() > MAX_MESSAGES_PER_CONVERSATION)
            messages = messages.subList(0, MAX_MESSAGES_PER_CONVERSATION);
        
        cache.put(conversationId, messages);
        log.debug("Added message to cache: conversationId={}, messageId={}", conversationId, message.getId());
    }
    
    /**
     * Remove message from cache (for delete operations)
     */
    public void removeMessage(Long conversationId, Long messageId) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            messages = messages.stream()
                .filter(msg -> !msg.getId().equals(messageId))
                .collect(Collectors.toList());
            
            if (messages.isEmpty())
                cache.invalidate(conversationId);
            else
                cache.put(conversationId, messages);
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
            "MessageCache - Size: %d, Hit Rate: %.2f%%",
            cache.estimatedSize(),
            cache.stats().hitRate() * 100
        );
    }
}
