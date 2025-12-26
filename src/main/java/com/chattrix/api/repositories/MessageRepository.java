package com.chattrix.api.repositories;

import com.chattrix.api.entities.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MessageRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Message save(Message message) {
        em.persist(message);
        return message;
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
        query.setParameter("sentStatus", Message.ScheduledStatus.SENT);
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
        query.setParameter("sentStatus", Message.ScheduledStatus.SENT);
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
                .setParameter("sentStatus", Message.ScheduledStatus.SENT)
                .getResultList();
    }

    /**
     * Find messages by conversation with cursor-based pagination
     * @param cursor The ID of the last message from previous page (null for first page)
     * @param limit Number of items to fetch (will fetch limit + 1 to determine hasNextPage)
     * @param sortDirection ASC or DESC
     */
    public List<Message> findByConversationIdWithCursor(Long conversationId, Long cursor, int limit, String sortDirection) {
        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String cursorCondition = "ASC".equalsIgnoreCase(sortDirection) ? "m.id > :cursor" : "m.id < :cursor";

        EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndReply");

        StringBuilder jpql = new StringBuilder(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.poll " +
                "LEFT JOIN FETCH m.event " +
                "WHERE m.conversation.id = :conversationId " +
                "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus)) "
        );

        if (cursor != null) {
            jpql.append("AND ").append(cursorCondition).append(" ");
        }

        jpql.append("ORDER BY m.id ").append(orderClause);

        TypedQuery<Message> query = em.createQuery(jpql.toString(), Message.class);
        query.setHint("jakarta.persistence.fetchgraph", entityGraph);
        query.setParameter("conversationId", conversationId);
        query.setParameter("sentStatus", Message.ScheduledStatus.SENT);

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

    // ==================== CHAT INFO METHODS ====================

    /**
     * Search messages with cursor-based pagination
     */
    public List<Message> searchMessagesByCursor(Long conversationId, String query, String type, Long senderId, Long cursor, int limit, String sortDirection) {
        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String cursorCondition = "ASC".equalsIgnoreCase(sortDirection) ? "m.id > :cursor" : "m.id < :cursor";

        StringBuilder jpql = new StringBuilder("SELECT m FROM Message m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.poll LEFT JOIN FETCH m.event WHERE m.conversation.id = :conversationId");

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
                Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
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
                List<Message.MessageType> messageTypes = new ArrayList<>();
                for (String t : types) {
                    try {
                        messageTypes.add(Message.MessageType.valueOf(t.trim().toUpperCase()));
                    } catch (IllegalArgumentException ignored) {}
                }
                typedQuery.setParameter("types", messageTypes);
            } else {
                try {
                    Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
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
     * Delete messages that reference a specific poll
     */
    @Transactional
    public int deleteByPollId(Long pollId) {
        // First delete read receipts for these messages
        em.createQuery("DELETE FROM MessageReadReceipt rr WHERE rr.message.id IN (SELECT m.id FROM Message m WHERE m.poll.id = :pollId)")
                .setParameter("pollId", pollId)
                .executeUpdate();

        // Then delete the messages
        return em.createQuery("DELETE FROM Message m WHERE m.poll.id = :pollId")
                .setParameter("pollId", pollId)
                .executeUpdate();
    }

    /**
     * Delete messages that reference a specific event
     */
    @Transactional
    public int deleteByEventId(Long eventId) {
        // First delete read receipts for these messages
        em.createQuery("DELETE FROM MessageReadReceipt rr WHERE rr.message.id IN (SELECT m.id FROM Message m WHERE m.event.id = :eventId)")
                .setParameter("eventId", eventId)
                .executeUpdate();

        // Then delete the messages
        return em.createQuery("DELETE FROM Message m WHERE m.event.id = :eventId")
                .setParameter("eventId", eventId)
                .executeUpdate();
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
     * Find all unread messages in a conversation for a specific user
     */
    public List<Message> findUnreadMessages(Long conversationId, Long userId) {
        return em.createQuery(
                        "SELECT m FROM Message m " +
                                "WHERE m.conversation.id = :conversationId " +
                                "AND m.sender.id <> :userId " +
                                "AND NOT EXISTS (" +
                                "  SELECT 1 FROM MessageReadReceipt r " +
                                "  WHERE r.message.id = m.id AND r.user.id = :userId" +
                                ") " +
                                "ORDER BY m.sentAt ASC",
                        Message.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Find unread messages up to a specific message ID
     */
    public List<Message> findUnreadMessagesUpTo(Long conversationId, Long userId, Long lastMessageId) {
        // First get the sentAt time of the lastMessage
        Message lastMessage = em.find(Message.class, lastMessageId);
        if (lastMessage == null) {
            return findUnreadMessages(conversationId, userId);
        }

        return em.createQuery(
                        "SELECT m FROM Message m " +
                                "WHERE m.conversation.id = :conversationId " +
                                "AND m.sender.id <> :userId " +
                                "AND m.sentAt <= :lastSentAt " +
                                "AND NOT EXISTS (" +
                                "  SELECT 1 FROM MessageReadReceipt r " +
                                "  WHERE r.message.id = m.id AND r.user.id = :userId" +
                                ") " +
                                "ORDER BY m.sentAt ASC",
                        Message.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .setParameter("lastSentAt", lastMessage.getSentAt())
                .getResultList();
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
                .setParameter("status", Message.ScheduledStatus.PENDING)
                .setParameter("time", time)
                .getResultList();
    }

    /**
     * Find scheduled messages with cursor-based pagination
     */
    public List<Message> findScheduledMessagesByCursor(Long senderId, Long conversationId, Message.ScheduledStatus status, Long cursor, int limit) {
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
                .setParameter("cancelledStatus", Message.ScheduledStatus.CANCELLED)
                .setParameter("pendingStatus", Message.ScheduledStatus.PENDING)
                .setParameter("senderId", senderId)
                .setParameter("conversationId", conversationId)
                .executeUpdate();
    }
    
    // Pinned messages methods
    public List<Message> findPinnedMessages(Long conversationId) {
        return em.createQuery(
                "SELECT m FROM Message m " +
                "LEFT JOIN FETCH m.sender " +
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
        query.setParameter("announcementType", Message.MessageType.ANNOUNCEMENT);
        
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        
        query.setMaxResults(limit + 1); // Fetch one extra to determine hasNextPage
        
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
                Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
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
        Instant startOfToday = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        
        // Native query to handle JSONB array containment check in PostgreSQL
        // Using standard SQL CAST to avoid parser confusion with :: operator
        String sql = "SELECT COUNT(*) FROM messages " +
                     "WHERE conversation_id = :conversationId " +
                     "AND type = 'SYSTEM' " +
                     "AND sent_at >= :startOfToday " +
                     "AND mentions @> CAST(:mentionPattern AS jsonb)";
        
        Query query = em.createNativeQuery(sql);
        query.setParameter("conversationId", conversationId);
        query.setParameter("startOfToday", java.sql.Timestamp.from(startOfToday));
        query.setParameter("mentionPattern", "[" + birthdayUserId + "]");
        
        Number count = (Number) query.getSingleResult();
        return count.longValue() > 0;
    }
}
