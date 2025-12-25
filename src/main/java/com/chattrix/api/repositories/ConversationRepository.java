package com.chattrix.api.repositories;

import com.chattrix.api.entities.Conversation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ConversationRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Conversation> findByIdWithParticipants(Long conversationId) {
        try {
            Conversation conversation = em.createQuery(
                            "SELECT DISTINCT c FROM Conversation c " +
                                    "LEFT JOIN FETCH c.participants " +
                                    "LEFT JOIN FETCH c.participants.user " +
                                    "WHERE c.id = :id", Conversation.class)
                    .setParameter("id", conversationId)
                    .getSingleResult();
            return Optional.of(conversation);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Conversation> findByUserId(Long userId) {
        return em.createQuery(
                        "SELECT DISTINCT c FROM Conversation c " +
                                "LEFT JOIN FETCH c.participants " +
                                "LEFT JOIN FETCH c.participants.user " +
                                "WHERE EXISTS (SELECT 1 FROM ConversationParticipant cp WHERE cp.conversation = c AND cp.user.id = :userId) " +
                                "ORDER BY c.updatedAt DESC", Conversation.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Find conversations by user ID with cursor-based pagination.
     * Uses conversation ID as cursor for efficient pagination.
     */
    public List<Conversation> findByUserIdWithCursor(Long userId, Long cursor, int limit) {
        // First, get conversation IDs with cursor pagination (without FETCH JOIN)
        StringBuilder jpql = new StringBuilder(
                "SELECT c.id, c.updatedAt FROM Conversation c " +
                "WHERE EXISTS (SELECT 1 FROM ConversationParticipant cp WHERE cp.conversation = c AND cp.user.id = :userId) ");
        
        if (cursor != null) {
            jpql.append("AND c.id < :cursor ");
        }
        
        jpql.append("ORDER BY c.updatedAt DESC, c.id DESC");
        
        var idQuery = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("userId", userId);
        
        if (cursor != null) {
            idQuery.setParameter("cursor", cursor);
        }
        
        List<Object[]> results = idQuery.setMaxResults(limit + 1).getResultList();
        
        if (results.isEmpty()) {
            return List.of();
        }
        
        // Extract IDs
        List<Long> conversationIds = results.stream()
                .map(row -> (Long) row[0])
                .toList();
        
        // Then fetch full conversations with participants and users
        return em.createQuery(
                "SELECT DISTINCT c FROM Conversation c " +
                "LEFT JOIN FETCH c.participants " +
                "LEFT JOIN FETCH c.participants.user " +
                "WHERE c.id IN :ids " +
                "ORDER BY c.updatedAt DESC, c.id DESC", Conversation.class)
                .setParameter("ids", conversationIds)
                .getResultList();
    }

    /**
     * Find conversations by user ID with cursor and filter.
     * Supports filtering by unread and group conversations.
     */
    public List<Conversation> findByUserIdWithCursorAndFilter(Long userId, Long cursor, int limit, String filter) {
        // First, get conversation IDs with cursor pagination (without FETCH JOIN)
        StringBuilder jpql = new StringBuilder(
                "SELECT c.id, c.updatedAt FROM Conversation c " +
                "WHERE EXISTS (SELECT 1 FROM ConversationParticipant cp WHERE cp.conversation = c AND cp.user.id = :userId) ");
        
        // Apply filter at database level
        if ("unread".equalsIgnoreCase(filter)) {
            jpql.append("AND EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.id = :userId AND cp2.unreadCount > 0) ");
        } else if ("group".equalsIgnoreCase(filter)) {
            jpql.append("AND c.type = 'GROUP' ");
        }
        
        if (cursor != null) {
            jpql.append("AND c.id < :cursor ");
        }
        
        jpql.append("ORDER BY c.updatedAt DESC, c.id DESC");
        
        var idQuery = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("userId", userId);
        
        if (cursor != null) {
            idQuery.setParameter("cursor", cursor);
        }
        
        List<Object[]> results = idQuery.setMaxResults(limit + 1).getResultList();
        
        if (results.isEmpty()) {
            return List.of();
        }
        
        // Extract IDs
        List<Long> conversationIds = results.stream()
                .map(row -> (Long) row[0])
                .toList();
        
        // Then fetch full conversations with participants and users
        return em.createQuery(
                "SELECT DISTINCT c FROM Conversation c " +
                "LEFT JOIN FETCH c.participants " +
                "LEFT JOIN FETCH c.participants.user " +
                "WHERE c.id IN :ids " +
                "ORDER BY c.updatedAt DESC, c.id DESC", Conversation.class)
                .setParameter("ids", conversationIds)
                .getResultList();
    }

    @Transactional
    public Conversation save(Conversation conversation) {
        if (conversation.getId() == null) {
            em.persist(conversation);
            return conversation;
        } else {
            return em.merge(conversation);
        }
    }

    public Optional<Conversation> findById(Long conversationId) {
        Conversation conversation = em.find(Conversation.class, conversationId);
        return Optional.ofNullable(conversation);
    }

    public Optional<Conversation> findDirectConversationBetweenUsers(Long userId1, Long userId2) {
        List<Conversation> conversations = em.createQuery(
                        "SELECT DISTINCT c FROM Conversation c " +
                                "JOIN c.participants p1 " +
                                "JOIN c.participants p2 " +
                                "WHERE c.type = 'DIRECT' " +
                                "AND p1.user.id = :userId1 " +
                                "AND p2.user.id = :userId2 " +
                                "AND p1.user.id != p2.user.id " +
                                "ORDER BY c.updatedAt DESC", Conversation.class)
                .setParameter("userId1", userId1)
                .setParameter("userId2", userId2)
                .setMaxResults(1)
                .getResultList();
        
        return conversations.isEmpty() ? Optional.empty() : Optional.of(conversations.get(0));
    }

    /**
     * Find mutual groups between two users
     */
    public List<Conversation> findMutualGroups(Long userId1, Long userId2) {
        return em.createQuery(
                "SELECT DISTINCT c FROM Conversation c " +
                        "LEFT JOIN FETCH c.participants " +
                        "LEFT JOIN FETCH c.participants.user " +
                        "WHERE c.type = 'GROUP' " +
                        "AND EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.id = :userId1) " +
                        "AND EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.id = :userId2) " +
                        "ORDER BY c.updatedAt DESC", Conversation.class)
                .setParameter("userId1", userId1)
                .setParameter("userId2", userId2)
                .getResultList();
    }

    @Transactional
    public void delete(Conversation conversation) {
        if (!em.contains(conversation)) {
            conversation = em.merge(conversation);
        }
        em.remove(conversation);
    }
}
