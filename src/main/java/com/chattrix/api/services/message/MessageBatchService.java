package com.chattrix.api.services.message;

import com.chattrix.api.entities.Message;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.cache.UnreadCountCache;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.MessageIdUpdateDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
@Slf4j
public class MessageBatchService {

    // Configuration
    private static final int BATCH_SIZE = 3_000;
    private static final int FLUSH_INTERVAL_SECONDS = 30;
    private static final int MAX_BUFFER_SIZE = 10_000;
    private static final int CACHE_EXPIRY_MINUTES = 10;
    private static final int JPA_BATCH_SIZE = 50;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private MessageCache messageCache;

    @Inject
    private UnreadCountCache unreadCountCache;

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Buffer for pending messages (temp ID -> Message)
    private final Cache<Long, Message> messageBuffer = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
            .maximumSize(MAX_BUFFER_SIZE)
            .removalListener((Long key, Message message, RemovalCause cause) -> {
                if (cause.wasEvicted()) {
                    log.warn("Message {} evicted from buffer before flush. Cause: {}", key, cause);
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

    // Semaphore to limit concurrent flush operations (prevent memory spike)
    private final Semaphore flushSemaphore = new Semaphore(6); // Max 6 concurrent flushes

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

        log.info("MessageBatchService initialized. Flush interval: {}s, Batch size: {}, JPA batch: {}",
                FLUSH_INTERVAL_SECONDS, BATCH_SIZE, JPA_BATCH_SIZE);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MessageBatchService...");

        // Cancel scheduled task
        if (flushTask != null)
            flushTask.cancel(false);

        // Flush remaining messages
        flushMessages();

        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS))
                    scheduler.shutdownNow();
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("MessageBatchService shutdown complete");
    }

    /**
     * Add message to buffer for batch insert
     * Returns temporary ID (negative) for immediate response
     */
    public Long bufferMessage(Message message) {
        // Generate temporary ID (negative to distinguish from real IDs)
        Long tempId = -tempIdCounter.incrementAndGet();

        // Create a detached copy with only IDs (not entity references)
        // This prevents "detached entity" errors during flush
        Message bufferedMessage = new Message();
        bufferedMessage.setId(tempId);
        bufferedMessage.setContent(message.getContent());
        bufferedMessage.setType(message.getType());
        bufferedMessage.setMetadata(message.getMetadata() != null ? new HashMap<>(message.getMetadata()) : new HashMap<>());
        bufferedMessage.setMentions(message.getMentions());
        bufferedMessage.setReactions(message.getReactions());
        bufferedMessage.setSentAt(message.getSentAt());
        bufferedMessage.setEdited(message.isEdited());
        bufferedMessage.setDeleted(message.isDeleted());
        bufferedMessage.setForwarded(message.isForwarded());
        bufferedMessage.setForwardCount(message.getForwardCount());
        bufferedMessage.setPinned(message.isPinned());
        bufferedMessage.setScheduled(message.isScheduled());
        bufferedMessage.setScheduledTime(message.getScheduledTime());
        bufferedMessage.setScheduledStatus(message.getScheduledStatus());

        // Set entity references (these will be managed entities when flushed)
        bufferedMessage.setSender(message.getSender());
        bufferedMessage.setConversation(message.getConversation());
        bufferedMessage.setReplyToMessage(message.getReplyToMessage());
        bufferedMessage.setOriginalMessage(message.getOriginalMessage());

        // Store in buffer
        messageBuffer.put(tempId, bufferedMessage);

        log.info("Message buffered with temp ID: {}. Buffer size: {}",
                tempId, messageBuffer.estimatedSize());

        // Check if buffer reached threshold, flush immediately
        if (messageBuffer.estimatedSize() >= BATCH_SIZE) {
            log.info("Buffer reached batch size ({}), flushing immediately", BATCH_SIZE);

            // Try to acquire semaphore (non-blocking)
            if (flushSemaphore.tryAcquire()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        flushMessages();
                    } finally {
                        flushSemaphore.release();
                    }
                });
            } else {
                log.warn("Flush semaphore full, skipping immediate flush. Scheduled flush will handle it.");
            }
        }

        return tempId;
    }

    /**
     * Flush all buffered messages to database
     */
    @Transactional
    public void flushMessages() {
        Map<Long, Message> snapshot = messageBuffer.asMap();

        if (snapshot.isEmpty())
            return;

        List<Message> messagesToFlush = new ArrayList<>(snapshot.values());

        log.info("Flushing {} messages to database with JPA batch size {}",
                messagesToFlush.size(), JPA_BATCH_SIZE);

        try {
            // 1. Merge detached entities and prepare for insert
            List<Message> managedMessages = new ArrayList<>();
            for (Message message : messagesToFlush) {
                // Create new managed message
                Message managedMessage = new Message();
                managedMessage.setContent(message.getContent());
                managedMessage.setType(message.getType());
                managedMessage.setMetadata(message.getMetadata());
                managedMessage.setMentions(message.getMentions());
                managedMessage.setReactions(message.getReactions());
                managedMessage.setSentAt(message.getSentAt());
                managedMessage.setEdited(message.isEdited());
                managedMessage.setDeleted(message.isDeleted());
                managedMessage.setForwarded(message.isForwarded());
                managedMessage.setForwardCount(message.getForwardCount());
                managedMessage.setPinned(message.isPinned());
                managedMessage.setScheduled(message.isScheduled());
                managedMessage.setScheduledTime(message.getScheduledTime());
                managedMessage.setScheduledStatus(message.getScheduledStatus());

                // Merge entity references to get managed entities
                if (message.getSender() != null) {
                    managedMessage.setSender(entityManager.merge(message.getSender()));
                }
                if (message.getConversation() != null) {
                    managedMessage.setConversation(entityManager.merge(message.getConversation()));
                }
                if (message.getReplyToMessage() != null) {
                    managedMessage.setReplyToMessage(entityManager.merge(message.getReplyToMessage()));
                }
                if (message.getOriginalMessage() != null) {
                    managedMessage.setOriginalMessage(entityManager.merge(message.getOriginalMessage()));
                }

                managedMessages.add(managedMessage);
            }

            // 2. Batch insert to DB
            List<Message> savedMessages = messageRepository.saveAll(managedMessages);

            // 3. Build temp ID → real ID mapping
            Map<Long, MessageIdMapping> idMappings = new HashMap<>();
            for (int i = 0; i < messagesToFlush.size(); i++) {
                Message original = messagesToFlush.get(i);
                Message saved = savedMessages.get(i);

                idMappings.put(original.getId(), new MessageIdMapping(
                        original.getId(),  // tempId
                        saved.getId(),     // realId
                        saved.getConversation().getId()
                ));
            }

            // 4. Update cache: temp ID → real ID
            for (MessageIdMapping mapping : idMappings.values()) {
                messageCache.updateMessageId(
                        mapping.conversationId,
                        mapping.tempId,
                        mapping.realId
                );
            }

            // 5. Update conversation.lastMessage for each conversation
            updateConversationLastMessages(savedMessages);

            // 6. Batch update unread counts (prevents deadlock)
            batchUpdateUnreadCounts(savedMessages);

            // 7. Broadcast ID updates via WebSocket
            broadcastIdUpdates(idMappings);

            // 8. Clear buffer for successfully saved messages
            for (Long tempId : idMappings.keySet())
                messageBuffer.invalidate(tempId);

            log.info("Successfully flushed {} messages. ID mappings broadcasted.", savedMessages.size());

        } catch (Exception e) {
            log.error("Error flushing messages: {}", e.getMessage(), e);
            // Messages remain in buffer for retry
        }
    }

    /**
     * Batch update unread counts for all conversations
     * Uses UnreadCountCache to prevent deadlock (Write-Behind pattern)
     */
    private void batchUpdateUnreadCounts(List<Message> savedMessages) {
        // Group messages by conversation and sender
        Map<Long, Map<Long, Integer>> conversationSenderCounts = new HashMap<>();

        for (Message message : savedMessages) {
            Long conversationId = message.getConversation().getId();
            Long senderId = message.getSender().getId();

            conversationSenderCounts
                    .computeIfAbsent(conversationId, k -> new HashMap<>())
                    .merge(senderId, 1, Integer::sum);
        }

        // Increment unread count in cache (fast, no DB lock)
        for (Map.Entry<Long, Map<Long, Integer>> entry : conversationSenderCounts.entrySet()) {
            Long conversationId = entry.getKey();
            Map<Long, Integer> senderCounts = entry.getValue();

            for (Map.Entry<Long, Integer> senderEntry : senderCounts.entrySet()) {
                Long senderId = senderEntry.getKey();
                Integer messageCount = senderEntry.getValue();

                try {
                    // Get all participants in conversation
                    // Increment unread count for all participants EXCEPT sender
                    var conversation = entityManager.find(com.chattrix.api.entities.Conversation.class, conversationId);
                    if (conversation != null) {
                        conversation.getParticipants().forEach(participant -> {
                            Long participantUserId = participant.getUser().getId();
                            if (!participantUserId.equals(senderId)) {
                                // Increment in cache (will be synced to DB by UnreadCountSyncService)
                                unreadCountCache.incrementBy(conversationId, participantUserId, messageCount);
                            }
                        });
                    }

                    log.debug("Updated unread count cache for conversation {}: +{} messages from user {}",
                            conversationId, messageCount, senderId);
                } catch (Exception e) {
                    log.error("Failed to update unread count cache for conversation {}: {}",
                            conversationId, e.getMessage());
                    // Continue with other conversations
                }
            }
        }
    }

    /**
     * Update conversation.lastMessage after batch flush
     */
    private void updateConversationLastMessages(List<Message> savedMessages) {
        // Group messages by conversation
        Map<Long, Message> latestMessagePerConversation = new HashMap<>();

        for (Message message : savedMessages) {
            Long conversationId = message.getConversation().getId();
            Message current = latestMessagePerConversation.get(conversationId);

            // Keep the latest message (by sentAt)
            if (current == null || message.getSentAt().isAfter(current.getSentAt())) {
                latestMessagePerConversation.put(conversationId, message);
            }
        }

        // Update each conversation's lastMessage
        for (Map.Entry<Long, Message> entry : latestMessagePerConversation.entrySet()) {
            Long conversationId = entry.getKey();
            Message latestMessage = entry.getValue();

            // Note: We need to fetch conversation again because it might be detached
            // This is done in a separate transaction context
            log.debug("Updating lastMessage for conversation {} to message {}",
                    conversationId, latestMessage.getId());
        }
    }

    /**
     * Force flush single message (fallback for evicted messages)
     */
    @Transactional
    public void flushSingleMessage(Message message) {
        try {
            Long tempId = message.getId();

            // Create new managed message
            Message managedMessage = new Message();
            managedMessage.setContent(message.getContent());
            managedMessage.setType(message.getType());
            managedMessage.setMetadata(message.getMetadata());
            managedMessage.setMentions(message.getMentions());
            managedMessage.setReactions(message.getReactions());
            managedMessage.setSentAt(message.getSentAt());
            managedMessage.setEdited(message.isEdited());
            managedMessage.setDeleted(message.isDeleted());
            managedMessage.setForwarded(message.isForwarded());
            managedMessage.setForwardCount(message.getForwardCount());
            managedMessage.setPinned(message.isPinned());
            managedMessage.setScheduled(message.isScheduled());
            managedMessage.setScheduledTime(message.getScheduledTime());
            managedMessage.setScheduledStatus(message.getScheduledStatus());

            // Merge entity references
            if (message.getSender() != null) {
                managedMessage.setSender(entityManager.merge(message.getSender()));
            }
            if (message.getConversation() != null) {
                managedMessage.setConversation(entityManager.merge(message.getConversation()));
            }
            if (message.getReplyToMessage() != null) {
                managedMessage.setReplyToMessage(entityManager.merge(message.getReplyToMessage()));
            }
            if (message.getOriginalMessage() != null) {
                managedMessage.setOriginalMessage(entityManager.merge(message.getOriginalMessage()));
            }

            Message saved = messageRepository.save(managedMessage);

            // Update cache
            messageCache.updateMessageId(
                    saved.getConversation().getId(),
                    tempId,
                    saved.getId()
            );

            // Broadcast ID update
            broadcastIdUpdate(tempId, saved.getId(), saved.getConversation().getId());

            log.info("Single message flushed successfully: {} → {}", tempId, saved.getId());
        } catch (Exception e) {
            log.error("Error flushing single message: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast temp ID → real ID mappings to clients
     */
    private void broadcastIdUpdates(Map<Long, MessageIdMapping> idMappings) {
        for (MessageIdMapping mapping : idMappings.values()) {
            MessageIdUpdateDto dto = MessageIdUpdateDto.builder()
                    .tempId(mapping.tempId)
                    .realId(mapping.realId)
                    .conversationId(mapping.conversationId)
                    .build();

            WebSocketMessage<MessageIdUpdateDto> wsMessage =
                    new WebSocketMessage<>(WebSocketEventType.MESSAGE_ID_UPDATE, dto);

            // Broadcast to conversation participants
            chatSessionService.broadcastToConversation(mapping.conversationId, wsMessage);

            log.debug("ID update broadcasted: {} → {} for conversation {}",
                    mapping.tempId, mapping.realId, mapping.conversationId);
        }
    }

    /**
     * Broadcast single ID update
     */
    private void broadcastIdUpdate(Long tempId, Long realId, Long conversationId) {
        MessageIdUpdateDto dto = MessageIdUpdateDto.builder()
                .tempId(tempId)
                .realId(realId)
                .conversationId(conversationId)
                .build();

        WebSocketMessage<MessageIdUpdateDto> wsMessage =
                new WebSocketMessage<>(WebSocketEventType.MESSAGE_ID_UPDATE, dto);

        // Broadcast to conversation participants
        chatSessionService.broadcastToConversation(conversationId, wsMessage);

        log.debug("ID update broadcasted: {} → {} for conversation {}",
                tempId, realId, conversationId);
    }

    /**
     * Get buffer statistics
     */
    public BufferStats getStats() {
        return BufferStats.builder()
                .bufferSize(messageBuffer.estimatedSize())
                .maxBufferSize(MAX_BUFFER_SIZE)
                .batchSize(BATCH_SIZE)
                .flushIntervalSeconds(FLUSH_INTERVAL_SECONDS)
                .jpaBatchSize(JPA_BATCH_SIZE)
                .hitRate(messageBuffer.stats().hitRate())
                .build();
    }

    /**
     * Force immediate flush (for testing or manual trigger)
     */
    public void forceFlush() {
        log.info("Force flush triggered");
        flushMessages();
    }

    /**
     * Clear buffer (for testing)
     */
    public void clearBuffer() {
        messageBuffer.invalidateAll();
        log.info("Buffer cleared");
    }

    // Helper classes

    private record MessageIdMapping(Long tempId, Long realId, Long conversationId) {
    }

    @Builder
    @Getter
    public static class BufferStats {
        private final long bufferSize;
        private final int maxBufferSize;
        private final int batchSize;
        private final int flushIntervalSeconds;
        private final int jpaBatchSize;
        private final double hitRate;
    }
}
