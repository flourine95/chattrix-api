package com.chattrix.api.services.message;

import com.chattrix.api.entities.Message;
import com.chattrix.api.repositories.MessageRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to batch message inserts for better performance
 * Messages are buffered in Caffeine cache and bulk inserted periodically
 * 
 * Configuration:
 * - Batch size: 5,000 messages (threshold for flush)
 * - Flush interval: 60 seconds (1 minute)
 * - JPA batch size: 50 (SQL statements per network request)
 * - Max buffer: 10,000 messages
 */
@ApplicationScoped
public class MessageBatchService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageBatchService.class);
    
    // Configuration
    private static final int BATCH_SIZE = 5_000;            // Flush when buffer reaches 5,000 messages
    private static final int FLUSH_INTERVAL_SECONDS = 60;   // Flush every 60 seconds (1 minute)
    private static final int MAX_BUFFER_SIZE = 10_000;      // Maximum buffer size before forced flush
    private static final int CACHE_EXPIRY_MINUTES = 10;     // Cache expiry time
    private static final int JPA_BATCH_SIZE = 50;           // JPA batch size for SQL statements
    
    @Inject
    private MessageRepository messageRepository;
    
    // Buffer for pending messages
    private final Cache<Long, Message> messageBuffer = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
        .maximumSize(MAX_BUFFER_SIZE)
        .removalListener((Long key, Message message, RemovalCause cause) -> {
            if (cause.wasEvicted() && message != null) {
                logger.warn("Message {} evicted from buffer before flush. Cause: {}", key, cause);
                // Force flush this message
                flushSingleMessage(message);
            }
        })
        .recordStats()
        .build();
    
    // Counter for generating temporary IDs
    private final AtomicLong tempIdCounter = new AtomicLong(0);
    
    // Scheduled executor for periodic flush
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> flushTask;
    
    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "MessageBatchFlush");
            thread.setDaemon(true);
            return thread;
        });
        
        // Schedule periodic flush
        flushTask = scheduler.scheduleAtFixedRate(
            this::flushMessages,
            FLUSH_INTERVAL_SECONDS,
            FLUSH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        logger.info("MessageBatchService initialized. Flush interval: {}s, Batch size: {}, JPA batch: {}",
            FLUSH_INTERVAL_SECONDS, BATCH_SIZE, JPA_BATCH_SIZE);
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MessageBatchService...");
        
        // Cancel scheduled task
        if (flushTask != null) {
            flushTask.cancel(false);
        }
        
        // Flush remaining messages
        flushMessages();
        
        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("MessageBatchService shutdown complete");
    }
    
    /**
     * Add message to buffer for batch insert
     * Returns temporary ID for immediate response
     */
    public Long bufferMessage(Message message) {
        // Generate temporary ID (negative to distinguish from real IDs)
        Long tempId = -tempIdCounter.incrementAndGet();
        
        // Store in buffer
        messageBuffer.put(tempId, message);
        
        logger.debug("Message buffered with temp ID: {}. Buffer size: {}", 
            tempId, messageBuffer.estimatedSize());
        
        // Check if buffer reached threshold, flush immediately
        if (messageBuffer.estimatedSize() >= BATCH_SIZE) {
            logger.info("Buffer reached batch size ({}), flushing immediately", BATCH_SIZE);
            flushMessages();
        }
        
        return tempId;
    }
    
    /**
     * Flush all buffered messages to database
     */
    @Transactional
    public void flushMessages() {
        List<Message> messagesToFlush = new ArrayList<>(messageBuffer.asMap().values());
        
        if (messagesToFlush.isEmpty()) {
            return;
        }
        
        logger.info("Flushing {} messages to database with JPA batch size {}", 
            messagesToFlush.size(), JPA_BATCH_SIZE);
        
        try {
            // Batch insert with JPA batch size
            messageRepository.saveAll(messagesToFlush);
            
            // Clear buffer for successfully saved messages
            messageBuffer.invalidateAll();
            
            logger.info("Successfully flushed {} messages", messagesToFlush.size());
            
        } catch (Exception e) {
            logger.error("Error flushing messages: {}", e.getMessage(), e);
            // Messages remain in buffer for retry
        }
    }
    
    /**
     * Force flush single message (fallback for evicted messages)
     */
    @Transactional
    public void flushSingleMessage(Message message) {
        try {
            messageRepository.save(message);
            logger.info("Single message flushed successfully");
        } catch (Exception e) {
            logger.error("Error flushing single message: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get buffer statistics
     */
    public BufferStats getStats() {
        CacheStats stats = messageBuffer.stats();
        return BufferStats.builder()
            .bufferSize(messageBuffer.estimatedSize())
            .maxBufferSize(MAX_BUFFER_SIZE)
            .batchSize(BATCH_SIZE)
            .flushIntervalSeconds(FLUSH_INTERVAL_SECONDS)
            .jpaBatchSize(JPA_BATCH_SIZE)
            .hitRate(stats.hitRate())
            .build();
    }
    
    /**
     * Force immediate flush (for testing or manual trigger)
     */
    public void forceFlush() {
        logger.info("Force flush triggered");
        flushMessages();
    }
    
    /**
     * Clear buffer (for testing)
     */
    public void clearBuffer() {
        messageBuffer.invalidateAll();
        logger.info("Buffer cleared");
    }
    
    @lombok.Builder
    @lombok.Getter
    public static class BufferStats {
        private final long bufferSize;
        private final int maxBufferSize;
        private final int batchSize;
        private final int flushIntervalSeconds;
        private final int jpaBatchSize;
        private final double hitRate;
    }
}
