package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.cache.MessageIdMappingCache;
import com.chattrix.api.services.cache.UnreadCountCache;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.ConversationUpdateDto;
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
    private MessageIdMappingCache idMappingCache;

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

        // DON'T set replyToMessage entity - it will be loaded during flush from metadata
        // This prevents OptimisticLockException when flushing
        if (message.getReplyToMessage() != null) {
            // Store replyToMessageId in metadata instead
            bufferedMessage.getMetadata().put("_replyToMessageId", message.getReplyToMessage().getId());
            log.debug("Stored replyToMessageId {} in buffered message metadata", message.getReplyToMessage().getId());
        }

        bufferedMessage.setOriginalMessage(message.getOriginalMessage());

        // Store in buffer
        messageBuffer.put(tempId, bufferedMessage);

        log.debug("Message buffered with temp ID: {}. Buffer size: {}",
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
     * Get buffered message by temporary ID
     * Used for reply validation when message hasn't been flushed yet
     */
    public Message getBufferedMessage(Long tempId) {
        if (tempId >= 0) {
            log.warn("Attempted to get buffered message with non-temporary ID: {}", tempId);
            return null;
        }

        Message message = messageBuffer.getIfPresent(tempId);
        if (message != null) {
            log.debug("Retrieved buffered message with temp ID: {}", tempId);
        }
        return message;
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

                // DON'T set replyToMessage here - will be resolved after building temp ID map
                // This prevents issues when reply and original message are in same batch

                if (message.getOriginalMessage() != null) {
                    managedMessage.setOriginalMessage(entityManager.merge(message.getOriginalMessage()));
                }

                managedMessages.add(managedMessage);
            }

            // 2. Build temp ID → real ID mapping BEFORE saving
            // This is needed to resolve reply relationships within the same batch
            Map<Long, Message> tempIdToManagedMessage = new HashMap<>();
            for (int i = 0; i < messagesToFlush.size(); i++) {
                Message original = messagesToFlush.get(i);
                Message managed = managedMessages.get(i);
                tempIdToManagedMessage.put(original.getId(), managed);
            }

            // 3. Resolve reply relationships within the batch
            for (Message managedMessage : managedMessages) {
                if (managedMessage.getMetadata() != null &&
                        managedMessage.getMetadata().containsKey("_replyToMessageId")) {

                    Long replyToMessageId = ((Number) managedMessage.getMetadata().get("_replyToMessageId")).longValue();
                    log.debug("Resolving replyToMessage with ID {} for message", replyToMessageId);

                    // Check if replyToMessage is in the same batch (temp ID)
                    if (replyToMessageId < 0 && tempIdToManagedMessage.containsKey(replyToMessageId)) {
                        Message replyToMessage = tempIdToManagedMessage.get(replyToMessageId);
                        managedMessage.setReplyToMessage(replyToMessage);
                        log.debug("Resolved replyToMessage from same batch: temp ID {}", replyToMessageId);
                    }
                    // Check if it was already flushed (check mapping cache)
                    else if (replyToMessageId < 0) {
                        Long realId = idMappingCache.getRealId(managedMessage.getConversation().getId(), replyToMessageId);
                        if (realId != null) {
                            Message replyToMessage = entityManager.find(Message.class, realId);
                            if (replyToMessage != null) {
                                managedMessage.setReplyToMessage(replyToMessage);
                                log.debug("Resolved replyToMessage from mapping cache: {} → {}", replyToMessageId, realId);
                            }
                        } else {
                            log.warn("ReplyToMessage with temp ID {} not found in batch or mapping cache", replyToMessageId);
                        }
                    }
                    // Real ID - load from DB
                    else if (replyToMessageId > 0) {
                        Message replyToMessage = entityManager.find(Message.class, replyToMessageId);
                        if (replyToMessage != null) {
                            managedMessage.setReplyToMessage(replyToMessage);
                            log.debug("Loaded replyToMessage from DB with ID {}", replyToMessageId);
                        } else {
                            log.warn("ReplyToMessage with ID {} not found in DB", replyToMessageId);
                        }
                    }

                    // Clean up metadata
                    managedMessage.getMetadata().remove("_replyToMessageId");
                }
            }

            // 4. Batch insert to DB
            List<Message> savedMessages = messageRepository.saveAll(managedMessages);

            // 5. Build temp ID → real ID mapping
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

            // 6. Update cache: temp ID → real ID
            for (MessageIdMapping mapping : idMappings.values()) {
                messageCache.updateMessageId(
                        mapping.conversationId,
                        mapping.tempId,
                        mapping.realId
                );

                // Store ID mapping for future lookups
                idMappingCache.addMapping(
                        mapping.conversationId,
                        mapping.tempId,
                        mapping.realId
                );
            }

            // 7. Update conversation.lastMessage for each conversation
            updateConversationLastMessages(savedMessages);

            // 8. Batch update unread counts (prevents deadlock)
            batchUpdateUnreadCounts(savedMessages);

            // 9. Broadcast ID updates via WebSocket
            broadcastIdUpdates(idMappings);

            // 10. Clear buffer for successfully saved messages
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
                    var conversation = entityManager.find(Conversation.class, conversationId);
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

        // Update each conversation's lastMessage in DB
        for (Map.Entry<Long, Message> entry : latestMessagePerConversation.entrySet()) {
            Long conversationId = entry.getKey();
            Message latestMessage = entry.getValue();

            try {
                // Fetch fresh conversation entity
                var conversation = entityManager.find(Conversation.class, conversationId);
                if (conversation != null) {
                    conversation.setLastMessage(latestMessage);
                    entityManager.merge(conversation);

                    log.debug("Updated lastMessage for conversation {} to message {}",
                            conversationId, latestMessage.getId());

                    // Broadcast conversation update to all participants
                    broadcastConversationUpdate(conversation, latestMessage);
                }
            } catch (Exception e) {
                log.error("Failed to update lastMessage for conversation {}: {}",
                        conversationId, e.getMessage());
            }
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
                try {
                    // Check if reply message still exists before merging
                    Message replyMessage = entityManager.find(Message.class, message.getReplyToMessage().getId());
                    if (replyMessage != null) {
                        managedMessage.setReplyToMessage(entityManager.merge(message.getReplyToMessage()));
                    } else {
                        log.warn("Reply message {} no longer exists, skipping", message.getReplyToMessage().getId());
                        managedMessage.setReplyToMessage(null);
                    }
                } catch (Exception e) {
                    log.warn("Failed to merge reply message {}: {}", message.getReplyToMessage().getId(), e.getMessage());
                    managedMessage.setReplyToMessage(null);
                }
            }
            if (message.getOriginalMessage() != null) {
                try {
                    // Check if original message still exists before merging
                    Message originalMessage = entityManager.find(Message.class, message.getOriginalMessage().getId());
                    if (originalMessage != null) {
                        managedMessage.setOriginalMessage(entityManager.merge(message.getOriginalMessage()));
                    } else {
                        log.warn("Original message {} no longer exists, skipping", message.getOriginalMessage().getId());
                        managedMessage.setOriginalMessage(null);
                    }
                } catch (Exception e) {
                    log.warn("Failed to merge original message {}: {}", message.getOriginalMessage().getId(), e.getMessage());
                    managedMessage.setOriginalMessage(null);
                }
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
     * Broadcast conversation update after lastMessage is updated
     */
    private void broadcastConversationUpdate(Conversation conversation, Message lastMessage) {
        try {
            ConversationUpdateDto updateDto =
                    new ConversationUpdateDto();
            updateDto.setConversationId(conversation.getId());
            updateDto.setUpdatedAt(conversation.getUpdatedAt());

            if (lastMessage != null) {
                ConversationUpdateDto.LastMessageDto lastMessageDto =
                        new ConversationUpdateDto.LastMessageDto();
                lastMessageDto.setId(lastMessage.getId());
                lastMessageDto.setContent(lastMessage.getContent());
                lastMessageDto.setSenderId(lastMessage.getSender().getId());
                lastMessageDto.setSenderUsername(lastMessage.getSender().getUsername());
                lastMessageDto.setSentAt(lastMessage.getSentAt());
                lastMessageDto.setType(lastMessage.getType().name());
                updateDto.setLastMessage(lastMessageDto);
            }

            WebSocketMessage<ConversationUpdateDto> message =
                    new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATE, updateDto);

            chatSessionService.broadcastToConversation(conversation.getId(), message);

            log.debug("Conversation update broadcasted: conversationId={}, lastMessageId={}",
                    conversation.getId(), lastMessage != null ? lastMessage.getId() : null);
        } catch (Exception e) {
            log.error("Failed to broadcast conversation update for conversation {}: {}",
                    conversation.getId(), e.getMessage());
        }
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
