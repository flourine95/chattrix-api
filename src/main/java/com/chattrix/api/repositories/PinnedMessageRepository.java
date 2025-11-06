package com.chattrix.api.repositories;

import com.chattrix.api.entities.PinnedMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PinnedMessageRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(PinnedMessage pinnedMessage) {
        if (pinnedMessage.getId() == null) {
            em.persist(pinnedMessage);
        } else {
            em.merge(pinnedMessage);
        }
    }

    public Optional<PinnedMessage> findById(Long id) {
        return Optional.ofNullable(em.find(PinnedMessage.class, id));
    }

    public List<PinnedMessage> findByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT p FROM PinnedMessage p " +
                                "JOIN FETCH p.message m " +
                                "JOIN FETCH m.sender " +
                                "WHERE p.conversation.id = :conversationId " +
                                "ORDER BY p.pinOrder ASC, p.pinnedAt DESC",
                        PinnedMessage.class)
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public Optional<PinnedMessage> findByConversationIdAndMessageId(Long conversationId, Long messageId) {
        List<PinnedMessage> results = em.createQuery(
                        "SELECT p FROM PinnedMessage p " +
                                "WHERE p.conversation.id = :conversationId " +
                                "AND p.message.id = :messageId",
                        PinnedMessage.class)
                .setParameter("conversationId", conversationId)
                .setParameter("messageId", messageId)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public long countByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM PinnedMessage p " +
                                "WHERE p.conversation.id = :conversationId",
                        Long.class)
                .setParameter("conversationId", conversationId)
                .getSingleResult();
    }

    public void delete(PinnedMessage pinnedMessage) {
        em.remove(em.contains(pinnedMessage) ? pinnedMessage : em.merge(pinnedMessage));
    }

    public void deleteByConversationIdAndMessageId(Long conversationId, Long messageId) {
        em.createQuery(
                        "DELETE FROM PinnedMessage p " +
                                "WHERE p.conversation.id = :conversationId " +
                                "AND p.message.id = :messageId")
                .setParameter("conversationId", conversationId)
                .setParameter("messageId", messageId)
                .executeUpdate();
    }

    public Integer getMaxPinOrder(Long conversationId) {
        Integer maxOrder = em.createQuery(
                        "SELECT MAX(p.pinOrder) FROM PinnedMessage p " +
                                "WHERE p.conversation.id = :conversationId",
                        Integer.class)
                .setParameter("conversationId", conversationId)
                .getSingleResult();
        return maxOrder != null ? maxOrder : 0;
    }
}

