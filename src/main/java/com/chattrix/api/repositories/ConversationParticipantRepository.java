package com.chattrix.api.repositories;

import com.chattrix.api.entities.ConversationParticipant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class ConversationParticipantRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public ConversationParticipant save(ConversationParticipant participant) {
        if (participant.getId() == null) {
            em.persist(participant);
            return participant;
        } else {
            return em.merge(participant);
        }
    }

    public Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId) {
        try {
            ConversationParticipant participant = em.createQuery(
                            "SELECT cp FROM ConversationParticipant cp " +
                                    "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId",
                            ConversationParticipant.class)
                    .setParameter("conversationId", conversationId)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.of(participant);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void delete(ConversationParticipant participant) {
        if (!em.contains(participant)) {
            participant = em.merge(participant);
        }
        em.remove(participant);
        em.flush();
    }

    public long countByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT COUNT(cp) FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId",
                        Long.class)
                .setParameter("conversationId", conversationId)
                .getSingleResult();
    }

    public boolean isUserParticipant(Long conversationId, Long userId) {
        Long count = em.createQuery(
                        "SELECT COUNT(cp) FROM ConversationParticipant cp " +
                                "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId",
                        Long.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    public boolean isUserAdmin(Long conversationId, Long userId) {
        Long count = em.createQuery(
                        "SELECT COUNT(cp) FROM ConversationParticipant cp " +
                                "WHERE cp.conversation.id = :conversationId " +
                                "AND cp.user.id = :userId " +
                                "AND cp.role = 'ADMIN'",
                        Long.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    @Transactional
    public void incrementUnreadCount(Long conversationId, Long userId) {
        em.createQuery(
                        "UPDATE ConversationParticipant cp " +
                                "SET cp.unreadCount = cp.unreadCount + 1 " +
                                "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Transactional
    public void incrementUnreadCountForOthers(Long conversationId, Long excludeUserId) {
        em.createQuery(
                        "UPDATE ConversationParticipant cp " +
                                "SET cp.unreadCount = cp.unreadCount + 1 " +
                                "WHERE cp.conversation.id = :conversationId AND cp.user.id != :excludeUserId")
                .setParameter("conversationId", conversationId)
                .setParameter("excludeUserId", excludeUserId)
                .executeUpdate();
    }

    @Transactional
    public void resetUnreadCount(Long conversationId, Long userId, Long lastReadMessageId) {
        em.createQuery(
                        "UPDATE ConversationParticipant cp " +
                                "SET cp.unreadCount = 0, " +
                                "cp.lastReadMessageId = :lastReadMessageId, " +
                                "cp.lastReadAt = CURRENT_TIMESTAMP " +
                                "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .setParameter("lastReadMessageId", lastReadMessageId)
                .executeUpdate();
    }

    public Long getTotalUnreadCount(Long userId) {
        Long total = em.createQuery(
                        "SELECT SUM(cp.unreadCount) FROM ConversationParticipant cp " +
                                "WHERE cp.user.id = :userId",
                        Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return total != null ? total : 0L;
    }

    public java.util.List<ConversationParticipant> findByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT cp FROM ConversationParticipant cp " +
                                "WHERE cp.conversation.id = :conversationId",
                        ConversationParticipant.class)
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public java.util.List<ConversationParticipant> findByConversationIdWithCursor(Long conversationId, Long cursor, int limit) {
        StringBuilder jpql = new StringBuilder(
                "SELECT cp FROM ConversationParticipant cp " +
                "LEFT JOIN FETCH cp.user " +
                "WHERE cp.conversation.id = :conversationId ");

        if (cursor != null) {
            jpql.append("AND cp.user.id < :cursor ");
        }

        jpql.append("ORDER BY cp.user.id DESC");

        var query = em.createQuery(jpql.toString(), ConversationParticipant.class)
                .setParameter("conversationId", conversationId);

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        return query.setMaxResults(limit + 1).getResultList();
    }

    public long countAdminsByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT COUNT(cp) FROM ConversationParticipant cp " +
                                "WHERE cp.conversation.id = :conversationId AND cp.role = 'ADMIN'",
                        Long.class)
                .setParameter("conversationId", conversationId)
                .getSingleResult();
    }

    public Optional<ConversationParticipant> findOldestMemberByConversationId(Long conversationId) {
        try {
            ConversationParticipant participant = em.createQuery(
                            "SELECT cp FROM ConversationParticipant cp " +
                                    "WHERE cp.conversation.id = :conversationId AND cp.role = 'MEMBER' " +
                                    "ORDER BY cp.joinedAt ASC",
                            ConversationParticipant.class)
                    .setParameter("conversationId", conversationId)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(participant);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}

