package com.chattrix.api.services.cache;

import com.chattrix.api.responses.MessageResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache for recent messages in conversations
 * Used to reduce DB queries when loading message history
 */
@ApplicationScoped
public class MessageCache {
    
    private static final int CACHE_EXPIRY_MINUTES = 5;
    private static final int MAX_CACHE_SIZE = 5_000;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    
    // Cache key: conversationId -> List of recent messages
    private final Cache<Long, List<MessageResponse>> cache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
        .maximumSize(MAX_CACHE_SIZE)
        .build();
    
    /**
     * Get messages from cache
     */
    public List<MessageResponse> get(Long conversationId) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        return messages != null ? new ArrayList<>(messages) : null;
    }
    
    /**
     * Put messages into cache
     */
    public void put(Long conversationId, List<MessageResponse> messages) {
        // Only cache up to MAX_MESSAGES_PER_CONVERSATION
        List<MessageResponse> messagesToCache = messages.size() > MAX_MESSAGES_PER_CONVERSATION
            ? messages.subList(0, MAX_MESSAGES_PER_CONVERSATION)
            : messages;
        cache.put(conversationId, new ArrayList<>(messagesToCache));
    }
    
    /**
     * Add new message to cache (prepend to list)
     */
    public void addMessage(Long conversationId, MessageResponse message) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            List<MessageResponse> updatedMessages = new ArrayList<>();
            updatedMessages.add(message);
            updatedMessages.addAll(messages);
            
            // Keep only MAX_MESSAGES_PER_CONVERSATION
            if (updatedMessages.size() > MAX_MESSAGES_PER_CONVERSATION) {
                updatedMessages = updatedMessages.subList(0, MAX_MESSAGES_PER_CONVERSATION);
            }
            
            cache.put(conversationId, updatedMessages);
        }
    }
    
    /**
     * Update message in cache
     */
    public void updateMessage(Long conversationId, MessageResponse updatedMessage) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            List<MessageResponse> updatedMessages = new ArrayList<>();
            for (MessageResponse msg : messages) {
                if (msg.getId().equals(updatedMessage.getId())) {
                    updatedMessages.add(updatedMessage);
                } else {
                    updatedMessages.add(msg);
                }
            }
            cache.put(conversationId, updatedMessages);
        }
    }
    
    /**
     * Remove message from cache
     */
    public void removeMessage(Long conversationId, Long messageId) {
        List<MessageResponse> messages = cache.getIfPresent(conversationId);
        if (messages != null) {
            List<MessageResponse> updatedMessages = messages.stream()
                .filter(msg -> !msg.getId().equals(messageId))
                .collect(java.util.stream.Collectors.toList());
            cache.put(conversationId, updatedMessages);
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
