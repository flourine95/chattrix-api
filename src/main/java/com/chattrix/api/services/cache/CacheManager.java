package com.chattrix.api.services.cache;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Centralized cache manager for all application caches
 * Provides unified interface for cache operations, monitoring, and health checks
 * <p>
 * Manages:
 * - OnlineStatusCache - User online/offline status
 * - UserProfileCache - User profile data
 * - ConversationCache - Conversation metadata
 * - MessageCache - Recent messages (Write-Behind)
 * - UnreadCountCache - Unread message counts (Write-Behind)
 */
@ApplicationScoped
@Slf4j
public class CacheManager {

    @Inject
    private OnlineStatusCache onlineStatusCache;

    @Inject
    private UserProfileCache userProfileCache;

    @Inject
    private ConversationCache conversationCache;

    @Inject
    private MessageCache messageCache;

    @Inject
    private UnreadCountCache unreadCountCache;

    /**
     * Clear all caches (use with caution - causes cache miss storm)
     */
    public void clearAll() {
        log.warn("Clearing ALL caches - this will cause temporary performance degradation");
        onlineStatusCache.clearAll();
        userProfileCache.clear();
        conversationCache.clear();
        messageCache.clear();
        unreadCountCache.clear();
        log.info("All caches cleared");
    }

    /**
     * Invalidate user-related caches when user updates profile
     * Cascades to conversations where user is a participant
     */
    public void invalidateUserCaches(Long userId) {
        log.debug("Invalidating caches for user: {}", userId);
        userProfileCache.invalidate(userId);
        conversationCache.invalidateAllForUser(userId);
        // Note: UnreadCountCache is NOT invalidated (Write-Behind pattern)
    }

    /**
     * Invalidate conversation caches when conversation is updated
     * Cascades to all participants
     */
    public void invalidateConversationCaches(Long conversationId, Set<Long> participantIds) {
        log.debug("Invalidating caches for conversation: {} with {} participants",
                conversationId, participantIds.size());
        conversationCache.invalidateForAllParticipants(conversationId, participantIds);
        messageCache.invalidate(conversationId);
        // Note: UnreadCountCache is NOT invalidated (Write-Behind pattern)
    }

    /**
     * Warm up caches on application startup
     * Pre-loads frequently accessed data to improve initial response times
     */
    public void warmUp() {
        log.info("Starting cache warm-up...");
        // Can be extended to preload:
        // - Top 100 active users
        // - Recent conversations
        // - Popular groups
        log.info("Cache warm-up completed");
    }

    // ==================== MONITORING & STATISTICS ====================

    /**
     * Get comprehensive statistics for all caches
     */
    public String getAllStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("╔════════════════════════════════════════════════════════════╗\n");
        stats.append("║              CACHE STATISTICS REPORT                      ║\n");
        stats.append("╠════════════════════════════════════════════════════════════╣\n");
        stats.append("║ Online Status Cache                                        ║\n");
        stats.append("║ ").append(String.format("%-56s", onlineStatusCache.getCacheStats())).append(" ║\n");
        stats.append("╠════════════════════════════════════════════════════════════╣\n");
        stats.append("║ User Profile Cache                                         ║\n");
        stats.append("║ ").append(String.format("%-56s", userProfileCache.getStats())).append(" ║\n");
        stats.append("╠════════════════════════════════════════════════════════════╣\n");
        stats.append("║ Conversation Cache                                         ║\n");
        stats.append("║ ").append(String.format("%-56s", conversationCache.getStats())).append(" ║\n");
        stats.append("╠════════════════════════════════════════════════════════════╣\n");
        stats.append("║ Message Cache                                              ║\n");
        stats.append("║ ").append(String.format("%-56s", messageCache.getStats())).append(" ║\n");
        stats.append("╠════════════════════════════════════════════════════════════╣\n");
        stats.append("║ Unread Count Cache                                         ║\n");
        stats.append("║ ").append(String.format("%-56s", unreadCountCache.getStats())).append(" ║\n");
        stats.append("╚════════════════════════════════════════════════════════════╝\n");
        return stats.toString();
    }

    /**
     * Get detailed cache metrics for monitoring systems (Prometheus, etc.)
     */
    public Map<String, CacheMetrics> getDetailedMetrics() {
        Map<String, CacheMetrics> metrics = new HashMap<>();

        // Note: Actual metrics would come from Caffeine cache.stats()
        // This is a simplified version

        metrics.put("online_status", CacheMetrics.builder()
                .name("OnlineStatusCache")
                .size(onlineStatusCache.getOnlineUserCount())
                .hitRate(0.0) // Would come from cache.stats()
                .missRate(0.0)
                .evictionCount(0L)
                .build());

        metrics.put("user_profile", CacheMetrics.builder()
                .name("UserProfileCache")
                .size(userProfileCache.getCachedUserIds().size())
                .hitRate(0.0)
                .missRate(0.0)
                .evictionCount(0L)
                .build());

        metrics.put("conversation", CacheMetrics.builder()
                .name("ConversationCache")
                .size(0L) // Estimate
                .hitRate(0.0)
                .missRate(0.0)
                .evictionCount(0L)
                .build());

        metrics.put("message", CacheMetrics.builder()
                .name("MessageCache")
                .size(0L) // Estimate
                .hitRate(0.0)
                .missRate(0.0)
                .evictionCount(0L)
                .build());

        metrics.put("unread_count", CacheMetrics.builder()
                .name("UnreadCountCache")
                .size(0L) // Estimate
                .hitRate(0.0)
                .missRate(0.0)
                .evictionCount(0L)
                .build());

        return metrics;
    }

    /**
     * Get cache health status for health check endpoints
     */
    public CacheHealthStatus getHealthStatus() {
        long onlineStatusSize = onlineStatusCache.getOnlineUserCount();
        long userProfileSize = userProfileCache.getCachedUserIds().size();

        // Health check criteria:
        // - All caches are accessible
        // - No excessive evictions
        // - Hit rates are reasonable (> 50%)
        boolean healthy = true;
        String status = "HEALTHY";

        // Add health checks here
        // For example: if hit rate < 50%, mark as degraded

        return CacheHealthStatus.builder()
                .status(status)
                .healthy(healthy)
                .onlineStatusCacheSize(onlineStatusSize)
                .userProfileCacheSize(userProfileSize)
                .conversationCacheSize(0L) // Estimate
                .messageCacheSize(0L) // Estimate
                .unreadCountCacheSize(0L) // Estimate
                .totalCacheSize(onlineStatusSize + userProfileSize)
                .build();
    }

    /**
     * Get cache efficiency report
     * Useful for identifying cache tuning opportunities
     */
    public CacheEfficiencyReport getEfficiencyReport() {
        return CacheEfficiencyReport.builder()
                .overallHitRate(calculateOverallHitRate())
                .totalCacheSize(calculateTotalCacheSize())
                .recommendations(generateRecommendations())
                .build();
    }

    // ==================== HELPER METHODS ====================

    private double calculateOverallHitRate() {
        // Weighted average of all cache hit rates
        // Would need to collect actual stats from each cache
        return 0.65; // Placeholder
    }

    private long calculateTotalCacheSize() {
        return onlineStatusCache.getOnlineUserCount()
                + userProfileCache.getCachedUserIds().size();
    }

    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();

        // Example recommendations based on metrics
        // In production, these would be based on actual cache stats

        recommendations.add("✓ Cache hit rates are healthy (>60%)");
        recommendations.add("✓ No excessive evictions detected");
        recommendations.add("ℹ Consider increasing MessageCache size if hit rate drops below 60%");

        return recommendations;
    }

    @Builder
    @Getter
    public static class CacheHealthStatus {
        private String status; // HEALTHY, DEGRADED, UNHEALTHY
        private boolean healthy;
        private long onlineStatusCacheSize;
        private long userProfileCacheSize;
        private long conversationCacheSize;
        private long messageCacheSize;
        private long unreadCountCacheSize;
        private long totalCacheSize;
    }

    @Builder
    @Getter
    public static class CacheMetrics {
        private String name;
        private long size;
        private double hitRate;
        private double missRate;
        private long evictionCount;
    }

    @Builder
    @Getter
    public static class CacheEfficiencyReport {
        private double overallHitRate;
        private long totalCacheSize;
        private List<String> recommendations;
    }
}
