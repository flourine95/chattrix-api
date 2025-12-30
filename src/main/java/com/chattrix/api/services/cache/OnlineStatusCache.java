package com.chattrix.api.services.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * In-memory cache for managing user online status using Caffeine.
 * Replaces the 'online' boolean field in User entity.
 */
@ApplicationScoped
public class OnlineStatusCache {
    
    private static final int ONLINE_THRESHOLD_MINUTES = 2;
    private static final int CACHE_EXPIRY_MINUTES = 5;
    private static final int MAX_CACHE_SIZE = 100_000;
    
    private final Cache<Long, Instant> onlineUsers = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
        .maximumSize(MAX_CACHE_SIZE)
        .build();
    
    /**
     * Mark user as online with current timestamp
     */
    public void markOnline(Long userId) {
        onlineUsers.put(userId, Instant.now());
    }
    
    /**
     * Check if user is currently online
     * User is considered online if last heartbeat was within 2 minutes
     */
    public boolean isOnline(Long userId) {
        Instant lastSeen = onlineUsers.getIfPresent(userId);
        if (lastSeen == null) {
            return false;
        }
        
        return Duration.between(lastSeen, Instant.now()).toMinutes() < ONLINE_THRESHOLD_MINUTES;
    }
    
    /**
     * Get last seen timestamp for user
     */
    public Instant getLastSeen(Long userId) {
        return onlineUsers.getIfPresent(userId);
    }
    
    /**
     * Mark user as offline (remove from cache)
     */
    public void markOffline(Long userId) {
        onlineUsers.invalidate(userId);
    }
    
    /**
     * Get all currently online user IDs
     */
    public Set<Long> getOnlineUserIds() {
        Instant threshold = Instant.now().minus(ONLINE_THRESHOLD_MINUTES, java.time.temporal.ChronoUnit.MINUTES);
        
        return onlineUsers.asMap().entrySet().stream()
            .filter(entry -> entry.getValue().isAfter(threshold))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }
    
    /**
     * Get count of online users
     */
    public long getOnlineUserCount() {
        return getOnlineUserIds().size();
    }
    
    /**
     * Clear all cache (for testing or maintenance)
     */
    public void clearAll() {
        onlineUsers.invalidateAll();
    }
    
    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return String.format("Cache size: %d, Online users: %d", 
            onlineUsers.estimatedSize(), 
            getOnlineUserCount());
    }
}
