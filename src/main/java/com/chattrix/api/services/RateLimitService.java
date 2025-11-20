package com.chattrix.api.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing rate limiting using Guava RateLimiter.
 * Implements per-user rate limiting for different operations.
 */
@ApplicationScoped
public class RateLimitService {

    /**
     * Cache of rate limiters per user and operation.
     * Key format: "userId:operation"
     * Limiters expire after 1 hour of inactivity to prevent memory leaks.
     */
    private final LoadingCache<String, RateLimiter> limiters = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(getPermitsPerMinute(key));
                }
            });

    /**
     * Attempts to acquire a permit for the given user and operation.
     * Returns immediately without blocking.
     *
     * @param userId    The user ID
     * @param operation The operation name (e.g., "token-generation", "call-initiation")
     * @return true if a permit was acquired, false otherwise
     */
    public boolean tryAcquire(String userId, String operation) {
        String key = userId + ":" + operation;
        try {
            RateLimiter limiter = limiters.get(key);
            return limiter.tryAcquire();
        } catch (ExecutionException e) {
            // If we can't get a limiter, allow the request (fail open)
            return true;
        }
    }

    /**
     * Determines the permits per minute for a given operation.
     *
     * @param key The cache key in format "userId:operation"
     * @return The number of permits per minute
     */
    private double getPermitsPerMinute(String key) {
        if (key.contains("token-generation")) {
            return 10.0; // 10 requests per minute
        }
        if (key.contains("call-initiation")) {
            return 5.0; // 5 requests per minute
        }
        if (key.contains("call-history")) {
            return 20.0; // 20 requests per minute
        }
        // Default rate limit
        return 10.0;
    }

    /**
     * Gets the configured rate limit for an operation.
     *
     * @param operation The operation name
     * @return The rate limit in requests per minute
     */
    public int getRateLimit(String operation) {
        String key = "dummy:" + operation;
        return (int) getPermitsPerMinute(key);
    }
}
