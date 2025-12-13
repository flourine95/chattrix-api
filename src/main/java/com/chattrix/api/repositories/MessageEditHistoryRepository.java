package com.chattrix.api.repositories;

import com.chattrix.api.entities.MessageEditHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@ApplicationScoped
public class MessageEditHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(MessageEditHistory history) {
        if (history.getId() == null) {
            em.persist(history);
        } else {
            em.merge(history);
        }
    }

    public List<MessageEditHistory> findByMessageId(Long messageId) {
        return em.createQuery(
                        "SELECT h FROM MessageEditHistory h " +
                                "WHERE h.message.id = :messageId " +
                                "ORDER BY h.editedAt DESC",
                        MessageEditHistory.class)
                .setParameter("messageId", messageId)
                .getResultList();
    }

    public long countByMessageId(Long messageId) {
        return em.createQuery(
                        "SELECT COUNT(h) FROM MessageEditHistory h " +
                                "WHERE h.message.id = :messageId",
                        Long.class)
                .setParameter("messageId", messageId)
                .getSingleResult();
    }

    public void deleteByMessageId(Long messageId) {
        em.createQuery("DELETE FROM MessageEditHistory h WHERE h.message.id = :messageId")
                .setParameter("messageId", messageId)
                .executeUpdate();
    }
}

