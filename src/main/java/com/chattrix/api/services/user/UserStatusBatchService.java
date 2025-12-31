package com.chattrix.api.services.user;

import com.chattrix.api.repositories.UserRepository;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Batch service for updating user lastSeen timestamps.
 * Queues updates in-memory and flushes to database every 30 seconds.
 * 
 * This reduces database load from 1000+ updates/minute to 2 batch queries/minute.
 */
@Singleton
@Slf4j
public class UserStatusBatchService {
    
    private final ConcurrentMap<Long, Instant> pendingUpdates = new ConcurrentHashMap<>();
    
    // Metrics
    private final AtomicLong totalBatchedUpdates = new AtomicLong(0);
    private final AtomicLong totalBatches = new AtomicLong(0);
    
    @Inject
    private UserRepository userRepository;
    
    /**
     * Queue lastSeen update (in-memory only, no DB write)
     */
    public void queueLastSeenUpdate(Long userId) {
        pendingUpdates.put(userId, Instant.now());
        log.debug("Queued lastSeen update for user: {}", userId);
    }
    
    /**
     * Batch update every 30 seconds
     * Flushes all pending updates to database in single query
     */
    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    @Transactional
    public void flushPendingUpdates() {
        if (pendingUpdates.isEmpty()) {
            log.debug("No pending lastSeen updates to flush");
            return;
        }
        
        // Get snapshot and clear pending updates
        Map<Long, Instant> updates = new HashMap<>(pendingUpdates);
        pendingUpdates.clear();
        
        try {
            // Batch update in single query
            userRepository.batchUpdateLastSeen(updates);
            
            // Update metrics
            totalBatchedUpdates.addAndGet(updates.size());
            totalBatches.incrementAndGet();
            
            log.info("Batch #{}: Updated lastSeen for {} users. Total: {} updates in {} batches",
                totalBatches.get(), updates.size(), 
                totalBatchedUpdates.get(), totalBatches.get());
        } catch (Exception e) {
            log.error("Failed to batch update lastSeen for {} users: {}", 
                updates.size(), e.getMessage(), e);
            
            // Re-queue failed updates
            pendingUpdates.putAll(updates);
        }
    }
    
    /**
     * Get current metrics
     */
    public Map<String, Object> getMetrics() {
        long batches = totalBatches.get();
        long updates = totalBatchedUpdates.get();
        
        return Map.of(
            "totalBatchedUpdates", updates,
            "totalBatches", batches,
            "avgBatchSize", batches > 0 ? updates / batches : 0,
            "pendingUpdates", pendingUpdates.size()
        );
    }
    
    /**
     * Get pending update count
     */
    public int getPendingUpdateCount() {
        return pendingUpdates.size();
    }
    
    /**
     * Flush pending updates on shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down UserStatusBatchService, flushing {} pending updates...", 
            pendingUpdates.size());
        
        if (!pendingUpdates.isEmpty()) {
            flushPendingUpdates();
        }
        
        log.info("UserStatusBatchService shutdown complete. Final stats: {}", getMetrics());
    }
}
