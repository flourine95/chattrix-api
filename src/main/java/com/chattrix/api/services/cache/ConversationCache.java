package com.chattrix.api.services.cache;

import com.chattrix.api.responses.ConversationResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cache for conversation metadata (lastMessage, unreadCount, participants)
 * Used to speed up conversation list loading
 */
@ApplicationScoped
public class ConversationCache {
    
    private static final int CACHE_EXPIRY_MINUTES = 10;
    private static final int MAX_CACHE_SIZE = 100_000;
    
    // Cache key: userId_conversationId
    private final Cache<String, ConversationResponse> cache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
        .maximumSize(MAX_CACHE_SIZE)
        .build();
    
    /**
     * Generate cache key
     */
    private String getCacheKey(Long userId, Long conversationId) {
        return userId + "_" + conversationId;
    }
    
    /**
     * Get conversation from cache
     */
    public ConversationResponse get(Long userId, Long conversationId) {
        return cache.getIfPresent(getCacheKey(userId, conversationId));
    }
    
    /**
     * Put conversation into cache
     */
    public void put(Long userId, Long conversationId, ConversationResponse conversation) {
        cache.put(getCacheKey(userId, conversationId), conversation);
    }
    
    /**
     * Put multiple conversations into cache
     */
    public void putAll(Long userId, Map<Long, ConversationResponse> conversations) {
        Map<String, ConversationResponse> cacheEntries = conversations.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> getCacheKey(userId, entry.getKey()),
                Map.Entry::getValue
            ));
        cache.putAll(cacheEntries);
    }
    
    /**
     * Invalidate conversation for specific user
     */
    public void invalidate(Long userId, Long conversationId) {
        cache.invalidate(getCacheKey(userId, conversationId));
    }
    
    /**
     * Invalidate conversation for all participants
     * Call this when new message arrives or conversation is updated
     */
    public void invalidateForAllParticipants(Long conversationId, Set<Long> participantIds) {
        Set<String> keys = participantIds.stream()
            .map(userId -> getCacheKey(userId, conversationId))
            .collect(Collectors.toSet());
        cache.invalidateAll(keys);
    }
    
    /**
     * Invalidate all conversations for a user
     */
    public void invalidateAllForUser(Long userId) {
        String prefix = userId + "_";
        Set<String> keysToInvalidate = cache.asMap().keySet().stream()
            .filter(key -> key.startsWith(prefix))
            .collect(Collectors.toSet());
        cache.invalidateAll(keysToInvalidate);
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
            "ConversationCache - Size: %d, Hit Rate: %.2f%%",
            cache.estimatedSize(),
            cache.stats().hitRate() * 100
        );
    }
}
