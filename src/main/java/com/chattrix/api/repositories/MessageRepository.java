package com.chattrix.api.repositories;

import com.chattrix.api.entities.Message;
import jakarta.enterprise.context.ApplicationScoped;
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
            Message message = em.createQuery(
                            "SELECT m FROM Message m " +
                                    "LEFT JOIN FETCH m.sender " +
                                    "LEFT JOIN FETCH m.conversation " +
                                    "WHERE m.id = :messageId",
                            Message.class
                    )
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
        TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.conversation.id = :conversationId " +
                        "ORDER BY m.sentAt " + orderClause,
                Message.class
        );
        query.setParameter("conversationId", conversationId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }
}
