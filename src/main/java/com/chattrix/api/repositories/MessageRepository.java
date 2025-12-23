package com.chattrix.api.repositories;

import com.chattrix.api.entities.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.time.Instant;
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

    public List<Message> findByConversationIdWithSort(Long conversationId, int page, int size, String sortDirection) {
        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

        // Use EntityGraph to fetch nested relationships without alias
        EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndReply");

        // Only return messages that have been sent:
        // 1. Regular messages (sentAt is not null)
        // 2. Scheduled messages that have been sent (scheduled=true AND scheduledStatus=SENT)
        // Exclude: PENDING, CANCELLED, FAILED scheduled messages
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "WHERE m.conversation.id = :conversationId " +
                        "AND (m.sentAt IS NOT NULL OR (m.scheduled = true AND m.scheduledStatus = :sentStatus)) " +
                        "ORDER BY COALESCE(m.sentAt, m.scheduledTime, m.createdAt) " + orderClause,
                Message.class
        );
        query.setHint("jakarta.persistence.fetchgraph", entityGraph);
        query.setParameter("conversationId", conversationId);
        query.setParameter("sentStatus", Message.ScheduledStatus.SENT);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

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

    public List<Message> searchMessages(Long conversationId, String query, String type, Long senderId, int page, int size, String sortDirection) {
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

        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        jpql.append(" ORDER BY m.sentAt ").append(orderClause);

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
                // Invalid type, ignore
            }
        }

        if (senderId != null) {
            typedQuery.setParameter("senderId", senderId);
        }

        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        return typedQuery.getResultList();
    }

    public long countSearchMessages(Long conversationId, String query, String type, Long senderId) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId");

        if (query != null && !query.trim().isEmpty()) {
            jpql.append(" AND LOWER(m.content) LIKE :query");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql.append(" AND m.type = :type");
        }

        if (senderId != null) {
            jpql.append(" AND m.sender.id = :senderId");
        }

        TypedQuery<Long> typedQuery = em.createQuery(jpql.toString(), Long.class);
        typedQuery.setParameter("conversationId", conversationId);

        if (query != null && !query.trim().isEmpty()) {
            typedQuery.setParameter("query", "%" + query.toLowerCase() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            try {
                Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
                typedQuery.setParameter("type", messageType);
            } catch (IllegalArgumentException e) {
                // Invalid type, return 0
                return 0;
            }
        }

        if (senderId != null) {
            typedQuery.setParameter("senderId", senderId);
        }

        return typedQuery.getSingleResult();
    }

    public List<Message> findMediaByConversationId(Long conversationId, String type, int page, int size) {
        StringBuilder jpql = new StringBuilder("SELECT m FROM Message m LEFT JOIN FETCH m.sender WHERE m.conversation.id = :conversationId");

        if (type != null && !type.trim().isEmpty()) {
            jpql.append(" AND m.type = :type");
        } else {
            // Get all media types (IMAGE, VIDEO, AUDIO, DOCUMENT)
            jpql.append(" AND m.type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT')");
        }

        jpql.append(" ORDER BY m.sentAt DESC");

        TypedQuery<Message> typedQuery = em.createQuery(jpql.toString(), Message.class);
        typedQuery.setParameter("conversationId", conversationId);

        if (type != null && !type.trim().isEmpty()) {
            try {
                Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
                typedQuery.setParameter("type", messageType);
            } catch (IllegalArgumentException e) {
                // Invalid type, return empty list
                return List.of();
            }
        }

        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        return typedQuery.getResultList();
    }

    public long countMediaByConversationId(Long conversationId, String type) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId");

        if (type != null && !type.trim().isEmpty()) {
            jpql.append(" AND m.type = :type");
        } else {
            jpql.append(" AND m.type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT')");
        }

        TypedQuery<Long> typedQuery = em.createQuery(jpql.toString(), Long.class);
        typedQuery.setParameter("conversationId", conversationId);

        if (type != null && !type.trim().isEmpty()) {
            try {
                Message.MessageType messageType = Message.MessageType.valueOf(type.toUpperCase());
                typedQuery.setParameter("type", messageType);
            } catch (IllegalArgumentException e) {
                return 0;
            }
        }

        return typedQuery.getSingleResult();
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
     * Find scheduled messages by sender and status
     */
    public List<Message> findScheduledMessagesBySenderAndStatus(Long senderId, Message.ScheduledStatus status, int page, int size) {
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.conversation " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.sender.id = :senderId " +
                        "AND m.scheduled = true " +
                        "AND m.scheduledStatus = :status " +
                        "ORDER BY m.scheduledTime ASC",
                Message.class
        );
        query.setParameter("senderId", senderId);
        query.setParameter("status", status);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Find all scheduled messages by sender
     */
    public List<Message> findScheduledMessagesBySender(Long senderId, int page, int size) {
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.conversation " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.sender.id = :senderId " +
                        "AND m.scheduled = true " +
                        "ORDER BY m.scheduledTime ASC",
                Message.class
        );
        query.setParameter("senderId", senderId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Find scheduled messages by sender, conversation and status
     */
    public List<Message> findScheduledMessagesBySenderConversationAndStatus(
            Long senderId, Long conversationId, Message.ScheduledStatus status, int page, int size) {
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.conversation " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.sender.id = :senderId " +
                        "AND m.conversation.id = :conversationId " +
                        "AND m.scheduled = true " +
                        "AND m.scheduledStatus = :status " +
                        "ORDER BY m.scheduledTime ASC",
                Message.class
        );
        query.setParameter("senderId", senderId);
        query.setParameter("conversationId", conversationId);
        query.setParameter("status", status);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Find scheduled messages by sender and conversation
     */
    public List<Message> findScheduledMessagesBySenderAndConversation(
            Long senderId, Long conversationId, int page, int size) {
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.conversation " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.sender.id = :senderId " +
                        "AND m.conversation.id = :conversationId " +
                        "AND m.scheduled = true " +
                        "ORDER BY m.scheduledTime ASC",
                Message.class
        );
        query.setParameter("senderId", senderId);
        query.setParameter("conversationId", conversationId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Count scheduled messages by sender and status
     */
    public long countScheduledMessagesBySenderAndStatus(Long senderId, Message.ScheduledStatus status) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m " +
                                "WHERE m.sender.id = :senderId " +
                                "AND m.scheduled = true " +
                                "AND m.scheduledStatus = :status",
                        Long.class)
                .setParameter("senderId", senderId)
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * Count all scheduled messages by sender
     */
    public long countScheduledMessagesBySender(Long senderId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m " +
                                "WHERE m.sender.id = :senderId " +
                                "AND m.scheduled = true",
                        Long.class)
                .setParameter("senderId", senderId)
                .getSingleResult();
    }

    /**
     * Count scheduled messages by sender, conversation and status
     */
    public long countScheduledMessagesBySenderConversationAndStatus(
            Long senderId, Long conversationId, Message.ScheduledStatus status) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m " +
                                "WHERE m.sender.id = :senderId " +
                                "AND m.conversation.id = :conversationId " +
                                "AND m.scheduled = true " +
                                "AND m.scheduledStatus = :status",
                        Long.class)
                .setParameter("senderId", senderId)
                .setParameter("conversationId", conversationId)
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * Count scheduled messages by sender and conversation
     */
    public long countScheduledMessagesBySenderAndConversation(Long senderId, Long conversationId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM Message m " +
                                "WHERE m.sender.id = :senderId " +
                                "AND m.conversation.id = :conversationId " +
                                "AND m.scheduled = true",
                        Long.class)
                .setParameter("senderId", senderId)
                .setParameter("conversationId", conversationId)
                .getSingleResult();
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
}


