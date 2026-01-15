package com.chattrix.api.services.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cache for mapping temporary message IDs to real database IDs.
 * This is needed when messages are flushed from write-behind cache
 * but users still reference them by temporary IDs.
 * 
 * Uses Caffeine cache with TTL to prevent memory leaks.
 */
@ApplicationScoped
@Slf4j
public class MessageIdMappingCache {

    private static final int TTL_HOURS = 1;  // Mappings expire after 1 hour
    private static final int MAX_SIZE = 50_000;  // Max 50K mappings

    // Global map: tempId -> realId (with TTL)
    private final Cache<Long, Long> idMappings = Caffeine.newBuilder()
            .expireAfterWrite(TTL_HOURS, TimeUnit.HOURS)
            .maximumSize(MAX_SIZE)
            .recordStats()
            .build();
    
    // Per-conversation map: conversationId -> Map<tempId, realId> (with TTL)
    private final Cache<Long, Map<Long, Long>> conversationMappings = Caffeine.newBuilder()
            .expireAfterWrite(TTL_HOURS, TimeUnit.HOURS)
            .maximumSize(10_000)  // Max 10K conversations
            .recordStats()
            .build();

    /**
     * Store mapping from temporary ID to real ID
     */
    public void addMapping(Long conversationId, Long tempId, Long realId) {
        if (tempId >= 0 || realId <= 0) {
            log.warn("Invalid ID mapping: tempId={}, realId={}", tempId, realId);
            return;
        }
        
        // Store in global map
        idMappings.put(tempId, realId);
        
        // Store in conversation-specific map
        Map<Long, Long> convMap = conversationMappings.get(conversationId, k -> new ConcurrentHashMap<>());
        convMap.put(tempId, realId);
        
        log.debug("Added ID mapping: {} -> {} for conversation {} (total: {})", 
                tempId, realId, conversationId, idMappings.estimatedSize());
    }

    /**
     * Get real ID from temporary ID
     */
    public Long getRealId(Long tempId) {
        return idMappings.getIfPresent(tempId);
    }

    /**
     * Get real ID from temporary ID for specific conversation
     */
    public Long getRealId(Long conversationId, Long tempId) {
        Map<Long, Long> convMappings = conversationMappings.getIfPresent(conversationId);
        return convMappings != null ? convMappings.get(tempId) : null;
    }

    /**
     * Check if temporary ID has been mapped
     */
    public boolean hasMapping(Long tempId) {
        return idMappings.getIfPresent(tempId) != null;
    }

    /**
     * Remove mapping (manual cleanup if needed)
     */
    public void removeMapping(Long tempId) {
        Long realId = idMappings.getIfPresent(tempId);
        idMappings.invalidate(tempId);
        if (realId != null) {
            log.debug("Removed ID mapping: {} -> {}", tempId, realId);
        }
    }

    /**
     * Clear all mappings for a conversation
     */
    public void clearConversation(Long conversationId) {
        Map<Long, Long> convMappings = conversationMappings.getIfPresent(conversationId);
        if (convMappings != null) {
            // Remove from global map
            convMappings.keySet().forEach(idMappings::invalidate);
            // Remove conversation map
            conversationMappings.invalidate(conversationId);
            log.debug("Cleared {} ID mappings for conversation {}", convMappings.size(), conversationId);
        }
    }

    /**
     * Clear all mappings (for testing or maintenance)
     */
    public void clearAll() {
        long size = idMappings.estimatedSize();
        idMappings.invalidateAll();
        conversationMappings.invalidateAll();
        log.info("Cleared all {} ID mappings", size);
    }

    /**
     * Get total number of mappings
     */
    public long size() {
        return idMappings.estimatedSize();
    }
    
    /**
     * Get cache statistics
     */
    public String getStats() {
        return String.format(
            "MessageIdMappingCache - Size: %d/%d, Hit Rate: %.2f%%, Evictions: %d",
            idMappings.estimatedSize(),
            MAX_SIZE,
            idMappings.stats().hitRate() * 100,
            idMappings.stats().evictionCount()
        );
    }
}
