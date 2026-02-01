package com.chattrix.api.services.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory cache for unread message counts.
 * Prevents race conditions and deadlocks from direct DB updates.
 * 
 * Cache structure: "conversationId:userId" → AtomicInteger (unread count)
 * 
 * This acts as a Redis-like counter without needing external Redis.
 * Periodically synced to DB by UnreadCountSyncService.
 */
@ApplicationScoped
@Slf4j
public class UnreadCountCache {
    
    private static final int CACHE_EXPIRY_HOURS = 24;
    private static final int MAX_CACHE_SIZE = 100_000;
    
    // Cache key: "conversationId:userId" → unread count
    private final Cache<String, AtomicInteger> cache = Caffeine.newBuilder()
        .expireAfterAccess(CACHE_EXPIRY_HOURS, TimeUnit.HOURS)
        .maximumSize(MAX_CACHE_SIZE)
        .build();
    
    /**
     * Get unread count for user in conversation
     */
    public int get(Long conversationId, Long userId) {
        String key = buildKey(conversationId, userId);
        AtomicInteger count = cache.getIfPresent(key);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Set unread count for user in conversation
     */
    public void set(Long conversationId, Long userId, int count) {
        String key = buildKey(conversationId, userId);
        cache.put(key, new AtomicInteger(count));
        log.debug("Set unread count: conversationId={}, userId={}, count={}", conversationId, userId, count);
    }
    
    /**
     * Increment unread count for user in conversation
     * Thread-safe using AtomicInteger
     */
    public int increment(Long conversationId, Long userId) {
        String key = buildKey(conversationId, userId);
        AtomicInteger count = cache.get(key, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        log.debug("Incremented unread count: conversationId={}, userId={}, newCount={}", conversationId, userId, newCount);
        return newCount;
    }
    
    /**
     * Increment unread count by amount
     */
    public int incrementBy(Long conversationId, Long userId, int amount) {
        String key = buildKey(conversationId, userId);
        AtomicInteger count = cache.get(key, k -> new AtomicInteger(0));
        int newCount = count.addAndGet(amount);
        log.debug("Incremented unread count by {}: conversationId={}, userId={}, newCount={}", 
                amount, conversationId, userId, newCount);
        return newCount;
    }
    
    /**
     * Reset unread count to 0 for user in conversation
     */
    public void reset(Long conversationId, Long userId) {
        String key = buildKey(conversationId, userId);
        cache.put(key, new AtomicInteger(0));
        log.debug("Reset unread count: conversationId={}, userId={}", conversationId, userId);
    }
    
    /**
     * Remove from cache (will be reloaded from DB on next access)
     */
    public void invalidate(Long conversationId, Long userId) {
        String key = buildKey(conversationId, userId);
        cache.invalidate(key);
    }
    
    /**
     * Get all cached entries for syncing to DB
     * Returns map of "conversationId:userId" → count
     */
    public Map<String, Integer> getAllForSync() {
        Map<String, Integer> result = new HashMap<>();
        cache.asMap().forEach((key, atomicCount) -> {
            result.put(key, atomicCount.get());
        });
        return result;
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        cache.invalidateAll();
        log.info("Cleared all unread count cache");
    }
    
    /**
     * Get cache statistics
     */
    public String getStats() {
        return String.format(
            "UnreadCountCache - Size: %d, Hit Rate: %.2f%%",
            cache.estimatedSize(),
            cache.stats().hitRate() * 100
        );
    }
    
    private String buildKey(Long conversationId, Long userId) {
        return conversationId + ":" + userId;
    }
    
    /**
     * Parse key back to conversationId and userId
     * Returns [conversationId, userId]
     */
    public static Long[] parseKey(String key) {
        String[] parts = key.split(":");
        return new Long[] {
            Long.parseLong(parts[0]),
            Long.parseLong(parts[1])
        };
    }
}
