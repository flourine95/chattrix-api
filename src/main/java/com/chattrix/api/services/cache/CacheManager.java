package com.chattrix.api.services.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central cache manager for all application caches
 * Provides unified interface for cache operations and monitoring
 */
@ApplicationScoped
public class CacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    @Inject
    private OnlineStatusCache onlineStatusCache;
    
    @Inject
    private UserProfileCache userProfileCache;
    
    @Inject
    private ConversationCache conversationCache;
    
    @Inject
    private MessageCache messageCache;
    
    /**
     * Clear all caches
     */
    public void clearAll() {
        logger.info("Clearing all caches...");
        onlineStatusCache.clearAll();
        userProfileCache.clear();
        conversationCache.clear();
        messageCache.clear();
        logger.info("All caches cleared");
    }
    
    /**
     * Get statistics for all caches
     */
    public String getAllStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Cache Statistics ===\n");
        stats.append(onlineStatusCache.getCacheStats()).append("\n");
        stats.append(userProfileCache.getStats()).append("\n");
        stats.append(conversationCache.getStats()).append("\n");
        stats.append(messageCache.getStats()).append("\n");
        return stats.toString();
    }
    
    /**
     * Invalidate user-related caches when user updates profile
     */
    public void invalidateUserCaches(Long userId) {
        logger.debug("Invalidating caches for user: {}", userId);
        userProfileCache.invalidate(userId);
        conversationCache.invalidateAllForUser(userId);
    }
    
    /**
     * Invalidate conversation caches when conversation is updated
     */
    public void invalidateConversationCaches(Long conversationId, java.util.Set<Long> participantIds) {
        logger.debug("Invalidating caches for conversation: {}", conversationId);
        conversationCache.invalidateForAllParticipants(conversationId, participantIds);
        messageCache.invalidate(conversationId);
    }
    
    /**
     * Warm up caches (optional - can be called on startup)
     */
    public void warmUp() {
        logger.info("Warming up caches...");
        // Implementation depends on your needs
        // For example: pre-load hot users, recent conversations, etc.
        logger.info("Cache warm-up completed");
    }
    
    /**
     * Get cache health status
     */
    public CacheHealthStatus getHealthStatus() {
        return CacheHealthStatus.builder()
            .onlineStatusCacheSize(onlineStatusCache.getOnlineUserCount())
            .userProfileCacheSize(userProfileCache.getCachedUserIds().size())
            .conversationCacheSize(getConversationCacheSize())
            .messageCacheSize(getMessageCacheSize())
            .healthy(true)
            .build();
    }
    
    private long getConversationCacheSize() {
        // Estimate based on cache stats
        return 0; // Implement if needed
    }
    
    private long getMessageCacheSize() {
        // Estimate based on cache stats
        return 0; // Implement if needed
    }
    
    @lombok.Builder
    @lombok.Getter
    public static class CacheHealthStatus {
        private long onlineStatusCacheSize;
        private long userProfileCacheSize;
        private long conversationCacheSize;
        private long messageCacheSize;
        private boolean healthy;
    }
}
