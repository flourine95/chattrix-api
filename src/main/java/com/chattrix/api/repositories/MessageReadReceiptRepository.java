package com.chattrix.api.repositories;

import com.chattrix.api.entities.MessageReadReceipt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MessageReadReceiptRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(MessageReadReceipt receipt) {
        if (receipt.getId() == null) {
            em.persist(receipt);
        } else {
            em.merge(receipt);
        }
    }

    public Optional<MessageReadReceipt> findByMessageIdAndUserId(Long messageId, Long userId) {
        List<MessageReadReceipt> results = em.createQuery(
                        "SELECT r FROM MessageReadReceipt r " +
                                "WHERE r.message.id = :messageId AND r.user.id = :userId",
                        MessageReadReceipt.class)
                .setParameter("messageId", messageId)
                .setParameter("userId", userId)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<MessageReadReceipt> findByMessageId(Long messageId) {
        return em.createQuery(
                        "SELECT r FROM MessageReadReceipt r " +
                                "JOIN FETCH r.user " +
                                "WHERE r.message.id = :messageId " +
                                "ORDER BY r.readAt ASC",
                        MessageReadReceipt.class)
                .setParameter("messageId", messageId)
                .getResultList();
    }

    public boolean existsByMessageIdAndUserId(Long messageId, Long userId) {
        Long count = em.createQuery(
                        "SELECT COUNT(r) FROM MessageReadReceipt r " +
                                "WHERE r.message.id = :messageId AND r.user.id = :userId",
                        Long.class)
                .setParameter("messageId", messageId)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }

    public long countByMessageId(Long messageId) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM MessageReadReceipt r " +
                                "WHERE r.message.id = :messageId",
                        Long.class)
                .setParameter("messageId", messageId)
                .getSingleResult();
    }

    public void deleteByMessageId(Long messageId) {
        em.createQuery("DELETE FROM MessageReadReceipt r WHERE r.message.id = :messageId")
                .setParameter("messageId", messageId)
                .executeUpdate();
    }
}

