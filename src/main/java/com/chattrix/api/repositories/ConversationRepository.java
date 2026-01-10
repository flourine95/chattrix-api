package com.chattrix.api.repositories;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.responses.ConversationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * Sorted by ID DESC (newest conversations first).
     * Cursor is the ID of the last conversation from the previous page.
     * <p>
     * OPTIMIZED: Single query with JOIN FETCH to avoid N+1 problem
     */
    public List<Conversation> findByUserIdWithCursorAndFilter(Long userId, Long cursor, int limit, String filter) {
        // Build single query with all JOINs to avoid N+1
        StringBuilder jpql = new StringBuilder(
                "SELECT DISTINCT c FROM Conversation c " +
                        "JOIN FETCH c.participants cp " +
                        "JOIN FETCH cp.user u " +
                        "LEFT JOIN FETCH c.lastMessage lm " +
                        "LEFT JOIN FETCH lm.sender " +
                        "WHERE EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.id = :userId) ");

        // Apply filter at database level
        if ("unread".equalsIgnoreCase(filter)) {
            jpql.append("AND EXISTS (SELECT 1 FROM ConversationParticipant cp3 WHERE cp3.conversation = c AND cp3.user.id = :userId AND cp3.unreadCount > 0) ");
        } else if ("group".equalsIgnoreCase(filter)) {
            jpql.append("AND c.type = 'GROUP' ");
        }

        // Cursor pagination: get conversations with ID less than cursor
        if (cursor != null) {
            jpql.append("AND c.id < :cursor ");
        }

        // Order by ID DESC (newest conversations first)
        jpql.append("ORDER BY c.id DESC");

        var query = em.createQuery(jpql.toString(), Conversation.class)
                .setParameter("userId", userId);

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        // Fetch limit + 1 to check hasMore
        return query.setMaxResults(limit + 1).getResultList();
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

        return conversations.isEmpty() ? Optional.empty() : Optional.of(conversations.getFirst());
    }

    /**
     * Find conversations by user ID with cursor and filter.
     * Returns entities with participants eagerly loaded.
     * Supports filters: all (default), unread, group, archived
     * 
     * Sorting priority:
     * 1. Pinned conversations first (ordered by pinOrder ASC)
     * 2. Then by updatedAt DESC (most recent first)
     * 3. Then by id DESC (for stable pagination)
     */
    public List<Conversation> findByUserIdWithCursor(Long userId, Long cursor, int limit, String filter) {
        // First, get conversation IDs with filtering and sorting
        StringBuilder jpql = new StringBuilder(
                "SELECT DISTINCT c.id, cp.pinned, cp.pinOrder, c.updatedAt " +
                        "FROM Conversation c " +
                        "JOIN c.participants cp " +
                        "WHERE cp.user.id = :userId ");

        // Apply archived filter
        if ("archived".equalsIgnoreCase(filter)) {
            jpql.append("AND cp.archived = true ");
        } else {
            // Default: exclude archived conversations
            jpql.append("AND cp.archived = false ");
        }

        // Apply additional filters
        if ("unread".equalsIgnoreCase(filter)) {
            jpql.append("AND cp.unreadCount > 0 ");
        } else if ("group".equalsIgnoreCase(filter)) {
            jpql.append("AND c.type = 'GROUP' ");
        }

        // Cursor pagination - based on composite key (pinned, updatedAt, id)
        if (cursor != null) {
            // For cursor pagination with complex sorting, we need to compare the composite key
            // This is simplified - in production, you might want to encode cursor as base64 JSON
            jpql.append("AND c.id < :cursor ");
        }

        // Sorting: Pinned first (by pinOrder), then by updatedAt DESC, then by id DESC
        jpql.append("ORDER BY cp.pinned DESC, cp.pinOrder ASC NULLS LAST, c.updatedAt DESC, c.id DESC");

        var idQuery = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("userId", userId);

        if (cursor != null) {
            idQuery.setParameter("cursor", cursor);
        }

        List<Object[]> results = idQuery.setMaxResults(limit + 1).getResultList();

        if (results.isEmpty()) {
            return List.of();
        }

        // Extract conversation IDs
        List<Long> conversationIds = results.stream()
                .map(row -> (Long) row[0])
                .toList();

        // Then fetch full conversations with participants using EntityGraph
        // We need to maintain the same order as the ID query
        var entityGraph = em.createEntityGraph(Conversation.class);
        entityGraph.addSubgraph("participants").addAttributeNodes("user");

        List<Conversation> conversations = em.createQuery(
                        "SELECT DISTINCT c FROM Conversation c " +
                                "WHERE c.id IN :ids", Conversation.class)
                .setParameter("ids", conversationIds)
                .setHint("jakarta.persistence.fetchgraph", entityGraph)
                .getResultList();

        // Sort in memory to maintain the order from the first query
        // This is necessary because JPA doesn't preserve order with IN clause
        Map<Long, Conversation> conversationMap = conversations.stream()
                .collect(Collectors.toMap(Conversation::getId, Function.identity()));

        return conversationIds.stream()
                .map(conversationMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Find conversations by invite token from JSONB metadata
     * Uses native query to query JSONB field
     */
    public Optional<Conversation> findByInviteToken(String token) {
        try {
            // Native query to search JSONB metadata for invite token
            String sql = "SELECT c.* FROM conversations c " +
                    "WHERE c.metadata->>'inviteLink'->>'token' = :token " +
                    "AND c.metadata->>'inviteLink'->>'revoked' = 'false'";

            Conversation conversation = (Conversation) em.createNativeQuery(sql, Conversation.class)
                    .setParameter("token", token)
                    .getSingleResult();

            return Optional.of(conversation);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find conversations with active invite links
     * Uses native query to query JSONB field
     */
    public List<Conversation> findConversationsWithActiveInviteLinks(Long userId) {
        String sql = "SELECT DISTINCT c.* FROM conversations c " +
                "INNER JOIN conversation_participants cp ON c.id = cp.conversation_id " +
                "WHERE cp.user_id = :userId " +
                "AND c.metadata->>'inviteLink' IS NOT NULL " +
                "AND c.metadata->>'inviteLink'->>'revoked' = 'false' " +
                "ORDER BY c.updated_at DESC";

        return em.createNativeQuery(sql, Conversation.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /**
     * Find mutual groups between two users.
     * Returns entities to be mapped by MapStruct.
     */
    public List<Conversation> findMutualGroups(Long userId1, Long userId2) {
        return em.createQuery(
                        "SELECT DISTINCT c FROM Conversation c " +
                                "LEFT JOIN FETCH c.participants " +
                                "WHERE c.type = 'GROUP' " +
                                "AND EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversation = c AND cp1.user.id = :userId1) " +
                                "AND EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversation = c AND cp2.user.id = :userId2) " +
                                "ORDER BY c.updatedAt DESC", Conversation.class)
                .setParameter("userId1", userId1)
                .setParameter("userId2", userId2)
                .getResultList();
    }

    /**
     * Find conversation by ID and verify user is a participant.
     * Returns conversation regardless of archived status (for unarchive operation).
     * Uses EntityGraph to eagerly fetch participants and users in a single query.
     * 
     * @param userId User ID to verify access
     * @param conversationId Conversation ID to find
     * @param includeArchived Whether to include archived conversations
     * @return Optional containing conversation if found and user has access
     */
    public Optional<Conversation> findById(Long userId, Long conversationId, boolean includeArchived) {
        try {
            var entityGraph = em.createEntityGraph(Conversation.class);
            var participantsSubgraph = entityGraph.addSubgraph("participants");
            participantsSubgraph.addAttributeNodes("user");
            
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT c FROM Conversation c " +
                    "WHERE c.id = :conversationId " +
                    "AND EXISTS (SELECT 1 FROM ConversationParticipant cp " +
                    "            WHERE cp.conversation = c " +
                    "            AND cp.user.id = :userId");
            
            if (!includeArchived) {
                jpql.append(" AND cp.archived = false");
            }
            
            jpql.append(")");
            
            Conversation conversation = em.createQuery(jpql.toString(), Conversation.class)
                    .setParameter("conversationId", conversationId)
                    .setParameter("userId", userId)
                    .setHint("jakarta.persistence.fetchgraph", entityGraph)
                    .getSingleResult();
            
            return Optional.of(conversation);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Find conversation by ID and verify user is a participant.
     * Excludes archived conversations by default.
     */
    public Optional<Conversation> findById(Long userId, Long conversationId) {
        return findById(userId, conversationId, false);
    }
}
