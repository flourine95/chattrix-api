package com.chattrix.api.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class RateLimitingService {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final LoadingCache<String, AtomicInteger> requestCountsPerIp;

    public RateLimitingService() {
        requestCountsPerIp = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    @Nonnull
                    public AtomicInteger load(@Nonnull String key) {
                        return new AtomicInteger(0);
                    }
                });
    }

    public boolean isAllowed(String ipAddress) {
        try {
            AtomicInteger count = requestCountsPerIp.get(ipAddress);
            return count.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        } catch (ExecutionException e) {
            return true;
        }
    }
}

