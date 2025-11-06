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
        try {
            Conversation conversation = em.createQuery(
                            "SELECT DISTINCT c FROM Conversation c " +
                                    "JOIN c.participants p1 " +
                                    "JOIN c.participants p2 " +
                                    "WHERE c.type = 'DIRECT' " +
                                    "AND p1.user.id = :userId1 " +
                                    "AND p2.user.id = :userId2 " +
                                    "AND p1.user.id != p2.user.id", Conversation.class)
                    .setParameter("userId1", userId1)
                    .setParameter("userId2", userId2)
                    .getSingleResult();
            return Optional.of(conversation);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
