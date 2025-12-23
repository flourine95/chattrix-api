package com.chattrix.api.repositories;

import com.chattrix.api.entities.Poll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PollRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Poll save(Poll poll) {
        if (poll.getId() == null) {
            entityManager.persist(poll);
            return poll;
        } else {
            return entityManager.merge(poll);
        }
    }

    public Optional<Poll> findById(Long id) {
        Poll poll = entityManager.createQuery(
                "SELECT p FROM Poll p " +
                "LEFT JOIN FETCH p.creator " +
                "LEFT JOIN FETCH p.conversation " +
                "LEFT JOIN FETCH p.options " +
                "WHERE p.id = :id",
                Poll.class)
            .setParameter("id", id)
            .getResultStream()
            .findFirst()
            .orElse(null);
        
        if (poll != null) {
            // Force initialization of votes and related entities within transaction
            poll.getVotes().size();
            poll.getVotes().forEach(vote -> {
                vote.getUser().getId();
                vote.getPollOption().getId();
            });
        }
        
        return Optional.ofNullable(poll);
    }

    public List<Poll> findByConversationId(Long conversationId, int page, int size) {
        // First query to get poll IDs with pagination
        List<Long> pollIds = entityManager.createQuery(
                "SELECT p.id FROM Poll p " +
                "WHERE p.conversation.id = :conversationId " +
                "ORDER BY p.createdAt DESC",
                Long.class)
            .setParameter("conversationId", conversationId)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();

        if (pollIds.isEmpty()) {
            return List.of();
        }

        // Second query to fetch polls with all relationships
        return entityManager.createQuery(
                "SELECT DISTINCT p FROM Poll p " +
                "LEFT JOIN FETCH p.creator " +
                "LEFT JOIN FETCH p.conversation " +
                "LEFT JOIN FETCH p.options " +
                "WHERE p.id IN :pollIds " +
                "ORDER BY p.createdAt DESC",
                Poll.class)
            .setParameter("pollIds", pollIds)
            .getResultList()
            .stream()
            .peek(poll -> {
                // Force initialization of votes and related entities
                poll.getVotes().size();
                poll.getVotes().forEach(vote -> {
                    vote.getUser().getId();
                    vote.getPollOption().getId();
                });
            })
            .toList();
    }

    public Long countByConversationId(Long conversationId) {
        return entityManager.createQuery(
                "SELECT COUNT(p) FROM Poll p WHERE p.conversation.id = :conversationId",
                Long.class)
            .setParameter("conversationId", conversationId)
            .getSingleResult();
    }

    public void delete(Poll poll) {
        if (entityManager.contains(poll)) {
            entityManager.remove(poll);
        } else {
            entityManager.remove(entityManager.merge(poll));
        }
    }
}
