package com.chattrix.api.services.cache;

import com.chattrix.api.responses.UserResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cache for user profile information (avatar, username, fullName)
 * Used to reduce DB queries when displaying messages, conversations, etc.
 */
@ApplicationScoped
public class UserProfileCache {
    
    private static final int CACHE_EXPIRY_HOURS = 1;
    private static final int MAX_CACHE_SIZE = 50_000;
    
    private final Cache<Long, UserResponse> cache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_HOURS, TimeUnit.HOURS)
        .maximumSize(MAX_CACHE_SIZE)
        .build();
    
    /**
     * Get user profile from cache
     */
    public UserResponse get(Long userId) {
        return cache.getIfPresent(userId);
    }
    
    /**
     * Get multiple user profiles from cache
     */
    public Map<Long, UserResponse> getAll(Set<Long> userIds) {
        return cache.getAllPresent(userIds);
    }
    
    /**
     * Put user profile into cache
     */
    public void put(Long userId, UserResponse userProfile) {
        cache.put(userId, userProfile);
    }
    
    /**
     * Put multiple user profiles into cache
     */
    public void putAll(Map<Long, UserResponse> userProfiles) {
        cache.putAll(userProfiles);
    }
    
    /**
     * Invalidate user profile (when user updates profile)
     */
    public void invalidate(Long userId) {
        cache.invalidate(userId);
    }
    
    /**
     * Invalidate multiple user profiles
     */
    public void invalidateAll(Set<Long> userIds) {
        cache.invalidateAll(userIds);
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
            "UserProfileCache - Size: %d, Hit Rate: %.2f%%",
            cache.estimatedSize(),
            cache.stats().hitRate() * 100
        );
    }
    
    /**
     * Get cached user IDs
     */
    public Set<Long> getCachedUserIds() {
        return cache.asMap().keySet();
    }
}
