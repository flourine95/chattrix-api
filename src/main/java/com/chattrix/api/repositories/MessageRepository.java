package com.chattrix.api.repositories;

import com.chattrix.api.entities.Message;
import com.chattrix.api.enums.ScheduledStatus;
import com.chattrix.api.responses.MessageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.chattrix.api.enums.MessageType;
@ApplicationScoped
public class MessageRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Message save(Message message) {
        em.persist(message);
        return message;
    }

    /**
     * Batch insert messages for better performance
     * Uses JPA batch size of 50 for optimal performance
     */
    @Transactional
    public List<Message> saveAll(List<Message> messages) {
        List<Message> savedMessages = new ArrayList<>();
        
        final int batchSize = 50; // JPA batch size
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            em.persist(message);
            savedMessages.add(message);
            
            // Flush and clear every 50 entities to avoid memory issues
            if ((i + 1) % batchSize == 0) {
                em.flush();
                em.clear();
            }
        }
        
        // Final flush for remaining messages
        em.flush();
        em.clear();
        
        return savedMessages;
    }

    @Transactional
    public void delete(Message message) {
        em.remove(em.contains(message) ? message : em.merge(message));
    }

    public List<Message> findByConversationIdOrderBySentAtDesc(Long conversationId, int page, int size) {
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.conversation.id = :conversationId " +
                        "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus)) " +
                        "ORDER BY COALESCE(m.sentAt, m.scheduledTime, m.createdAt) DESC",
                Message.class
        );
        query.setParameter("conversationId", conversationId);
        query.setParameter("sentStatus", ScheduledStatus.SENT);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    public long countByConversationId(Long conversationId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(m) FROM Message m " +
                        "WHERE m.conversation.id = :conversationId " +
                        "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus))",
                Long.class
        );
        query.setParameter("conversationId", conversationId);
        query.setParameter("sentStatus", ScheduledStatus.SENT);
        return query.getSingleResult();
    }

    public Optional<Message> findById(Long messageId) {
        try {
            // Use EntityGraph to fetch nested relationships without alias
            EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndConversation");

            Message message = em.createQuery(
                            "SELECT m FROM Message m " +
                                    "WHERE m.id = :messageId",
                            Message.class
                    )
                    .setHint("jakarta.persistence.fetchgraph", entityGraph)
                    .setParameter("messageId", messageId)
                    .getSingleResult();
            return Optional.of(message);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Message> findByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT m FROM Message m " +
                                "LEFT JOIN FETCH m.sender " +
                                "WHERE m.conversation.id = :conversationId " +
                                "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus)) " +
                                "ORDER BY COALESCE(m.sentAt, m.scheduledTime, m.createdAt) ASC",
                        Message.class
                )
                .setParameter("conversationId", conversationId)
                .setParameter("sentStatus", ScheduledStatus.SENT)
                .getResultList();
    }

    /**
     * Find messages by conversation with cursor - Returns entities for MapStruct mapping
     */
    public List<Message> findByConversationIdWithCursor(
            Long conversationId, Long cursor, int limit, String sortDirection) {
        
        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String cursorCondition = "ASC".equalsIgnoreCase(sortDirection) ? "m.id > :cursor" : "m.id < :cursor";

        StringBuilder jpql = new StringBuilder(
                "SELECT m FROM Message m " +
                "WHERE m.conversation.id = :conversationId " +
                "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus)) "
        );

        if (cursor != null) {
            jpql.append("AND ").append(cursorCondition).append(" ");
        }

        jpql.append("ORDER BY m.id ").append(orderClause);

        TypedQuery<Message> query = em.createQuery(jpql.toString(), Message.class);
        
        query.setParameter("conversationId", conversationId);
        query.setParameter("sentStatus", ScheduledStatus.SENT);

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        query.setMaxResults(limit + 1);

        return query.getResultList();
    }

    public Optional<Message> findByIdSimple(Long messageId) {
        try {
            Message message = em.find(Message.class, messageId);
            return Optional.ofNullable(message);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Message> findByConversationIdAndType(Long conversationId, MessageType type) {
        return em.createQuery(
                        "SELECT m FROM Message m LEFT JOIN FETCH m.sender " +
                                "WHERE m.conversation.id = :conversationId AND m.type = :type " +
                                "ORDER BY m.createdAt DESC",
                        Message.class
                )
                .setParameter("conversationId", conversationId)
                .setParameter("type", type)
                .getResultList();
    }

    public List<Message> findByConversationAndType(Long conversationId, MessageType type, int limit) {
        return em.createQuery(
                        "SELECT m FROM Message m LEFT JOIN FETCH m.sender " +
                                "WHERE m.conversation.id = :conversationId AND m.type = :type " +
                                "ORDER BY m.id DESC",
                        Message.class
                )
                .setParameter("conversationId", conversationId)
                .setParameter("type", type)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Message> findByConversationAndTypeWithCursor(Long conversationId, MessageType type, Long cursor, int limit) {
        return em.createQuery(
                        "SELECT m FROM Message m LEFT JOIN FETCH m.sender " +
                                "WHERE m.conversation.id = :conversationId AND m.type = :type " +
                                "AND m.id < :cursor " +
                                "ORDER BY m.id DESC",
                        Message.class
                )
                .setParameter("conversationId", conversationId)
                .setParameter("type", type)
                .setParameter("cursor", cursor)
                .setMaxResults(limit)
                .getResultList();
    }

    // ==================== CHAT INFO METHODS ====================

    /**
     * Search messages with cursor-based pagination
     */
    public List<Message> searchMessagesByCursor(Long conversationId, String query, String type, Long senderId, Long cursor, int limit, String sortDirection) {
        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String cursorCondition = "ASC".equalsIgnoreCase(sortDirection) ? "m.id > :cursor" : "m.id < :cursor";

        StringBuilder jpql = new StringBuilder("SELECT m FROM Message m LEFT JOIN FETCH m.sender WHERE m.conversation.id = :conversationId");

        if (query != null && !query.trim().isEmpty()) {
            jpql.append(" AND LOWER(m.content) LIKE :query");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql.append(" AND m.type = :type");
        }

        if (senderId != null) {
            jpql.append(" AND m.sender.id = :senderId");
        }

        if (cursor != null) {
            jpql.append(" AND ").append(cursorCondition);
        }

        jpql.append(" ORDER BY m.id ").append(orderClause);

        TypedQuery<Message> typedQuery = em.createQuery(jpql.toString(), Message.class);
        typedQuery.setParameter("conversationId", conversationId);

        if (query != null && !query.trim().isEmpty()) {
            typedQuery.setParameter("query", "%" + query.toLowerCase() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            try {
                MessageType messageType = MessageType.valueOf(type.toUpperCase());
                typedQuery.setParameter("type", messageType);
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }

        if (senderId != null) {
            typedQuery.setParameter("senderId", senderId);
        }

        if (cursor != null) {
            typedQuery.setParameter("cursor", cursor);
        }

        typedQuery.setMaxResults(limit + 1);

        return typedQuery.getResultList();
    }

    /**
     * Find media files with cursor-based pagination and date filtering
     */
    public List<Message> findMediaByCursor(Long conversationId, String type, Instant startDate, Instant endDate, Long cursor, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT m FROM Message m LEFT JOIN FETCH m.sender WHERE m.conversation.id = :conversationId");

        if (type != null && !type.trim().isEmpty()) {
            String[] types = type.split(",");
            if (types.length > 1) {
                jpql.append(" AND m.type IN :types");
            } else {
                jpql.append(" AND m.type = :type");
            }
        } else {
            jpql.append(" AND m.type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'VOICE', 'LINK')");
        }

        if (startDate != null) {
            jpql.append(" AND m.sentAt >= :startDate");
        }
        if (endDate != null) {
            jpql.append(" AND m.sentAt <= :endDate");
        }

        if (cursor != null) {
            jpql.append(" AND m.id < :cursor");
        }

        jpql.append(" ORDER BY m.id DESC");

        TypedQuery<Message> typedQuery = em.createQuery(jpql.toString(), Message.class);
        typedQuery.setParameter("conversationId", conversationId);

        if (type != null && !type.trim().isEmpty()) {
            String[] types = type.split(",");
            if (types.length > 1) {
                List<MessageType> messageTypes = new ArrayList<>();
                for (String t : types) {
                    try {
                        messageTypes.add(MessageType.valueOf(t.trim().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                typedQuery.setParameter("types", messageTypes);
            } else {
                try {
                    MessageType messageType = MessageType.valueOf(type.toUpperCase());
                    typedQuery.setParameter("type", messageType);
                } catch (IllegalArgumentException e) {
                    return List.of();
                }
            }
        }

        if (startDate != null) {
            typedQuery.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            typedQuery.setParameter("endDate", endDate);
        }

        if (cursor != null) {
            typedQuery.setParameter("cursor", cursor);
        }

        typedQuery.setMaxResults(limit + 1);

        return typedQuery.getResultList();
    }

    public Optional<Message> findLatestByConversationId(Long conversationId) {
        try {
            Message message = em.createQuery(
                            "SELECT m FROM Message m " +
                                    "WHERE m.conversation.id = :conversationId " +
                                    "ORDER BY m.sentAt DESC",
                            Message.class)
                    .setParameter("conversationId", conversationId)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(message);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Long> findLatestMessageId(Long conversationId) {
        try {
            Long messageId = em.createQuery(
                            "SELECT m.id FROM Message m " +
                                    "WHERE m.conversation.id = :conversationId " +
                                    "ORDER BY m.sentAt DESC",
                            Long.class)
                    .setParameter("conversationId", conversationId)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(messageId);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find messages that are replies to a specific note
     */
    public List<Message> findByReplyToNoteId(Long noteId) {
        return em.createQuery(
                        "SELECT m FROM Message m " +
                                "LEFT JOIN FETCH m.sender " +
                                "WHERE m.replyToNote.id = :noteId " +
                                "ORDER BY m.sentAt DESC",
                        Message.class)
                .setParameter("noteId", noteId)
                .getResultList();
    }

    /**
     * Count messages that are replies to a specific note
     */
    public long countByReplyToNoteId(Long noteId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m WHERE m.replyToNote.id = :noteId",
                        Long.class)
                .setParameter("noteId", noteId)
                .getSingleResult();
    }
    
    /**
     * Find unread messages in a conversation for a user
     * Uses ConversationParticipant.lastReadMessageId to determine unread messages
     */
    public List<Message> findUnreadMessagesByLastRead(Long conversationId, Long userId, Long lastReadMessageId) {
        if (lastReadMessageId == null) {
            // No messages read yet, return all messages from others
            return em.createQuery(
                    "SELECT m FROM Message m " +
                    "LEFT JOIN FETCH m.sender " +
                    "WHERE m.conversation.id = :conversationId " +
                    "AND m.sender.id <> :userId " +
                    "ORDER BY m.sentAt ASC",
                    Message.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .getResultList();
        }
        
        // Return messages after lastReadMessageId
        return em.createQuery(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.sender " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.sender.id <> :userId " +
                "AND m.id > :lastReadMessageId " +
                "ORDER BY m.sentAt ASC",
                Message.class)
            .setParameter("conversationId", conversationId)
            .setParameter("userId", userId)
            .setParameter("lastReadMessageId", lastReadMessageId)
            .getResultList();
    }
    
    /**
     * Count unread messages in a conversation for a user
     * Uses ConversationParticipant.lastReadMessageId
     */
    public long countUnreadMessagesByLastRead(Long conversationId, Long userId, Long lastReadMessageId) {
        if (lastReadMessageId == null) {
            // No messages read yet, count all messages from others
            return em.createQuery(
                    "SELECT COUNT(m) FROM Message m " +
                    "WHERE m.conversation.id = :conversationId " +
                    "AND m.sender.id <> :userId",
                    Long.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .getSingleResult();
        }
        
        // Count messages after lastReadMessageId
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.sender.id <> :userId " +
                "AND m.id > :lastReadMessageId",
                Long.class)
            .setParameter("conversationId", conversationId)
            .setParameter("userId", userId)
            .setParameter("lastReadMessageId", lastReadMessageId)
            .getSingleResult();
    }

    // ==================== SCHEDULED MESSAGES METHODS ====================

    /**
     * Find scheduled messages that are due to be sent
     */
    public List<Message> findScheduledMessagesDue(Instant time) {
        return em.createQuery(
                        "SELECT m FROM Message m " +
                                "LEFT JOIN FETCH m.conversation " +
                                "LEFT JOIN FETCH m.sender " +
                                "WHERE m.scheduled = true " +
                                "AND m.scheduledStatus = :status " +
                                "AND m.scheduledTime <= :time " +
                                "ORDER BY m.scheduledTime ASC",
                        Message.class)
                .setParameter("status", ScheduledStatus.PENDING)
                .setParameter("time", time)
                .getResultList();
    }

    /**
     * Find scheduled messages with cursor-based pagination
     */
    public List<Message> findScheduledMessagesByCursor(Long senderId, Long conversationId, ScheduledStatus status, Long cursor, int limit) {
        StringBuilder jpql = new StringBuilder(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.conversation " +
                "LEFT JOIN FETCH m.sender " +
                "WHERE m.sender.id = :senderId " +
                "AND m.scheduled = true "
        );

        if (conversationId != null) {
            jpql.append("AND m.conversation.id = :conversationId ");
        }

        if (status != null) {
            jpql.append("AND m.scheduledStatus = :status ");
        }

        if (cursor != null) {
            jpql.append("AND m.id < :cursor ");
        }

        jpql.append("ORDER BY m.id DESC");

        TypedQuery<Message> query = em.createQuery(jpql.toString(), Message.class);
        query.setParameter("senderId", senderId);

        if (conversationId != null) {
            query.setParameter("conversationId", conversationId);
        }

        if (status != null) {
            query.setParameter("status", status);
        }

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        query.setMaxResults(limit + 1);

        return query.getResultList();
    }

    /**
     * Cancel all scheduled messages for a user in a conversation
     */
    @Transactional
    public int cancelScheduledMessagesByUserAndConversation(Long senderId, Long conversationId) {
        return em.createQuery(
                        "UPDATE Message m SET m.scheduledStatus = :cancelledStatus " +
                                "WHERE m.sender.id = :senderId " +
                                "AND m.conversation.id = :conversationId " +
                                "AND m.scheduled = true " +
                                "AND m.scheduledStatus = :pendingStatus")
                .setParameter("cancelledStatus", ScheduledStatus.CANCELLED)
                .setParameter("pendingStatus", ScheduledStatus.PENDING)
                .setParameter("senderId", senderId)
                .setParameter("conversationId", conversationId)
                .executeUpdate();
    }
    
    // Pinned messages methods
    /**
     * Find pinned messages - Returns entities for MapStruct mapping
     */
    public List<Message> findPinnedMessages(Long conversationId) {
        return em.createQuery(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.sender " +
                "LEFT JOIN FETCH m.conversation " +
                "LEFT JOIN FETCH m.replyToMessage " +
                "LEFT JOIN FETCH m.pinnedBy " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.pinned = true " +
                "ORDER BY m.pinnedAt DESC",
                Message.class
        )
        .setParameter("conversationId", conversationId)
        .getResultList();
    }
    
    public long countPinnedMessages(Long conversationId) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.pinned = true",
                Long.class
        )
        .setParameter("conversationId", conversationId)
        .getSingleResult();
    }
    
    /**
     * Find announcements with cursor-based pagination
     * @param cursor The ID of the last announcement from previous page (null for first page)
     * @param limit Number of items to fetch (will fetch limit + 1 to determine hasNextPage)
     */
    public List<Message> findAnnouncementsByCursor(Long conversationId, Long cursor, int limit) {
        StringBuilder jpql = new StringBuilder(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.sender " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.type = :announcementType "
        );
        
        if (cursor != null) {
            jpql.append("AND m.id < :cursor ");
        }
        
        jpql.append("ORDER BY m.id DESC");
        
        TypedQuery<Message> query = em.createQuery(jpql.toString(), Message.class);
        query.setParameter("conversationId", conversationId);
        query.setParameter("announcementType", MessageType.ANNOUNCEMENT);
        
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        
        query.setMaxResults(limit + 1); // Fetch one extra to determine hasNextPage
        
        return query.getResultList();
    }

    /**
     * Find announcements with cursor - DTO Projection (optimized)
     * Returns MessageResponse directly without entity mapping
     */
    public List<MessageResponse> findAnnouncementsByCursorAsDTO(
            Long conversationId, Long cursor, int limit) {
        
        StringBuilder jpql = new StringBuilder(
                "SELECT new com.chattrix.api.responses.MessageResponse(" +
                "  m.id, m.conversation.id, m.sender.id, m.sender.username, m.sender.fullName, m.sender.avatarUrl, " +
                "  m.content, m.type, m.metadata, " +
                "  CASE WHEN m.replyToMessage IS NOT NULL THEN m.replyToMessage.id ELSE null END, null, " +
                "  m.reactions, m.mentions, m.sentAt, m.createdAt, m.updatedAt, " +
                "  m.edited, m.editedAt, m.deleted, m.deletedAt, m.forwarded, " +
                "  CASE WHEN m.originalMessage IS NOT NULL THEN m.originalMessage.id ELSE null END, m.forwardCount, m.pinned, m.pinnedAt, " +
                "  CASE WHEN m.pinnedBy IS NOT NULL THEN m.pinnedBy.id ELSE null END, null, null, " +
                "  null, null, " +
                "  m.scheduled, m.scheduledTime, m.scheduledStatus" +
                ") " +
                "FROM Message m " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.type = :announcementType "
        );
        
        if (cursor != null) {
            jpql.append("AND m.id < :cursor ");
        }
        
        jpql.append("ORDER BY m.id DESC");
        
        TypedQuery<MessageResponse> query =
            em.createQuery(jpql.toString(), MessageResponse.class);
        
        query.setParameter("conversationId", conversationId);
        query.setParameter("announcementType", MessageType.ANNOUNCEMENT);
        
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        
        query.setMaxResults(limit + 1);
        
        return query.getResultList();
    }
    
    /**
     * Global search with cursor-based pagination
     */
    public List<Message> globalSearchMessagesByCursor(Long userId, String query, String type, Long cursor, int limit) {
        StringBuilder jpql = new StringBuilder(
            "SELECT m FROM Message m " +
            "LEFT JOIN FETCH m.sender " +
            "LEFT JOIN FETCH m.conversation " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM ConversationParticipant cp " +
            "  WHERE cp.conversation.id = m.conversation.id " +
            "  AND cp.user.id = :userId" +
            ")"
        );

        if (query != null && !query.trim().isEmpty()) {
            jpql.append(" AND LOWER(m.content) LIKE :query");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql.append(" AND m.type = :type");
        }

        if (cursor != null) {
            jpql.append(" AND m.id < :cursor");
        }

        jpql.append(" ORDER BY m.id DESC");

        TypedQuery<Message> typedQuery = em.createQuery(jpql.toString(), Message.class);
        typedQuery.setParameter("userId", userId);

        if (query != null && !query.trim().isEmpty()) {
            typedQuery.setParameter("query", "%" + query.toLowerCase() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            try {
                MessageType messageType = MessageType.valueOf(type.toUpperCase());
                typedQuery.setParameter("type", messageType);
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }

        if (cursor != null) {
            typedQuery.setParameter("cursor", cursor);
        }

        typedQuery.setMaxResults(limit + 1);

        return typedQuery.getResultList();
    }
    
    /**
     * Get message context - messages around a specific message for highlighting
     */
    public List<Message> getMessageContext(Long messageId, Long conversationId, int contextSize) {
        // Get the target message first to get its timestamp
        Message targetMessage = em.find(Message.class, messageId);
        if (targetMessage == null) {
            return List.of();
        }

        // Get messages before and after
        return em.createQuery(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.sender " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.sentAt BETWEEN :startTime AND :endTime " +
                "ORDER BY m.sentAt ASC",
                Message.class
        )
        .setParameter("conversationId", conversationId)
        .setParameter("startTime", targetMessage.getSentAt().minusSeconds(contextSize * 30)) // ~30s per message
        .setParameter("endTime", targetMessage.getSentAt().plusSeconds(contextSize * 30))
        .setMaxResults(contextSize * 2 + 1) // contextSize before + target + contextSize after
        .getResultList();
    }

    /**
     * Check if a birthday message has already been sent for a user in a conversation today
     */
    public boolean hasBirthdayMessageBeenSentToday(Long conversationId, Long birthdayUserId) {
        // Get start of today in UTC
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        
        // Native query to handle JSONB array containment check in PostgreSQL
        // Using standard SQL CAST to avoid parser confusion with :: operator
        String sql = "SELECT COUNT(*) FROM messages " +
                     "WHERE conversation_id = :conversationId " +
                     "AND type = 'SYSTEM' " +
                     "AND sent_at >= :startOfToday " +
                     "AND mentions @> CAST(:mentionPattern AS jsonb)";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("conversationId", conversationId);
        query.setParameter("startOfToday", Timestamp.from(startOfToday));
        query.setParameter("mentionPattern", "[" + birthdayUserId + "]");
        
        Number count = (Number) query.getSingleResult();
        return count.longValue() > 0;
    }
    
    // ==================== MEDIA STATISTICS METHODS ====================
    
    /**
     * Get media statistics for a conversation
     */
    public Map<String, Long> getMediaStatistics(Long conversationId) {
        String sql = "SELECT " +
                     "  COALESCE(SUM(CASE WHEN type = 'IMAGE' THEN 1 ELSE 0 END), 0) as images, " +
                     "  COALESCE(SUM(CASE WHEN type = 'VIDEO' THEN 1 ELSE 0 END), 0) as videos, " +
                     "  COALESCE(SUM(CASE WHEN type = 'AUDIO' THEN 1 ELSE 0 END), 0) as audios, " +
                     "  COALESCE(SUM(CASE WHEN type = 'FILE' THEN 1 ELSE 0 END), 0) as files, " +
                     "  COALESCE(SUM(CASE WHEN type = 'LINK' THEN 1 ELSE 0 END), 0) as links, " +
                     "  COALESCE(COUNT(*), 0) as total " +
                     "FROM messages " +
                     "WHERE conversation_id = :conversationId " +
                     "AND type IN ('IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'LINK') " +
                     "AND deleted = false";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("conversationId", conversationId);
        
        Object[] result = (Object[]) query.getSingleResult();
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("images", result[0] != null ? ((Number) result[0]).longValue() : 0L);
        stats.put("videos", result[1] != null ? ((Number) result[1]).longValue() : 0L);
        stats.put("audios", result[2] != null ? ((Number) result[2]).longValue() : 0L);
        stats.put("files", result[3] != null ? ((Number) result[3]).longValue() : 0L);
        stats.put("links", result[4] != null ? ((Number) result[4]).longValue() : 0L);
        stats.put("total", result[5] != null ? ((Number) result[5]).longValue() : 0L);
        
        return stats;
    }
    
    /**
     * Count media by type
     */
    public long countMediaByType(Long conversationId, MessageType type) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.type = :type",
                Long.class)
            .setParameter("conversationId", conversationId)
            .setParameter("type", type)
            .getSingleResult();
    }
    
    /**
     * Count all media types
     */
    public long countAllMedia(Long conversationId) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Message m " +
                "WHERE m.conversation.id = :conversationId " +
                "AND m.type IN ('IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'LINK')",
                Long.class)
            .setParameter("conversationId", conversationId)
            .getSingleResult();
    }
}
