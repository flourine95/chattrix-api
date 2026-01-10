package com.chattrix.api.services.cache;

import com.chattrix.api.repositories.ConversationParticipantRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service to sync unread counts from cache to database.
 * Runs every 30 seconds to batch update DB, reducing DB load and preventing deadlocks.
 * 
 * This is the Write-Behind pattern for unread counts:
 * 1. Increment happens in-memory (fast, no DB lock)
 * 2. Periodic sync to DB (batched, efficient)
 * 3. On shutdown, force sync to prevent data loss
 */
@Singleton
@Startup
@Slf4j
public class UnreadCountSyncService {
    
    @Inject
    private UnreadCountCache unreadCountCache;
    
    @Inject
    private ConversationParticipantRepository participantRepository;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    
    @PostConstruct
    public void init() {
        log.info("UnreadCountSyncService initialized - will sync every 30 seconds");
    }
    
    /**
     * Scheduled sync every 30 seconds
     * EJB Timer automatically handles transaction boundaries
     */
    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    @Transactional
    public void syncToDatabase() {
        if (!isRunning.get()) {
            log.debug("Sync skipped - service is shutting down");
            return;
        }
        
        try {
            Map<String, Integer> allCounts = unreadCountCache.getAllForSync();
            
            if (allCounts.isEmpty()) {
                log.debug("No unread counts to sync");
                return;
            }
            
            log.info("Syncing {} unread counts to database", allCounts.size());
            int syncedCount = 0;
            int errorCount = 0;
            
            for (Map.Entry<String, Integer> entry : allCounts.entrySet()) {
                try {
                    Long[] ids = UnreadCountCache.parseKey(entry.getKey());
                    Long conversationId = ids[0];
                    Long userId = ids[1];
                    Integer count = entry.getValue();
                    
                    // Update DB with cached count
                    participantRepository.setUnreadCount(conversationId, userId, count);
                    syncedCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to sync unread count for key: {}", entry.getKey(), e);
                    errorCount++;
                }
            }
            
            log.info("Unread count sync completed: synced={}, errors={}", syncedCount, errorCount);
            
        } catch (Exception e) {
            log.error("Error during unread count sync", e);
        }
    }
    
    /**
     * Force sync on shutdown to prevent data loss
     */
    @PreDestroy
    public void shutdown() {
        log.info("UnreadCountSyncService shutting down - forcing final sync");
        isRunning.set(false);
        
        try {
            syncToDatabase();
            log.info("Final sync completed successfully");
        } catch (Exception e) {
            log.error("Error during final sync on shutdown", e);
        }
    }
}
