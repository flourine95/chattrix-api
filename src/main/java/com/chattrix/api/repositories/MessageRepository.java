package com.chattrix.api.repositories;

import com.chattrix.api.entities.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

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
                        "ORDER BY m.sentAt DESC",
                Message.class
        );
        query.setParameter("conversationId", conversationId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    public long countByConversationId(Long conversationId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId",
                Long.class
        );
        query.setParameter("conversationId", conversationId);
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
                                "ORDER BY m.sentAt ASC",
                        Message.class
                )
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public List<Message> findByConversationIdWithSort(Long conversationId, int page, int size, String sortDirection) {
        String orderClause = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

        // Use EntityGraph to fetch nested relationships without alias
        EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndReply");

        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "WHERE m.conversation.id = :conversationId " +
                        "ORDER BY m.sentAt " + orderClause,
                Message.class
        );
        query.setHint("jakarta.persistence.fetchgraph", entityGraph);
        query.setParameter("conversationId", conversationId);
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
}
