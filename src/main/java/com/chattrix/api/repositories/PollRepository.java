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
            initializePoll(poll);
        }
        
        return Optional.ofNullable(poll);
    }

    public void initializePoll(Poll poll) {
        // Force initialization of votes and related entities within transaction
        poll.getVotes().size();
        poll.getVotes().forEach(vote -> {
            vote.getUser().getId();
            vote.getPollOption().getId();
        });
        
        // Also initialize votes for each option to avoid LazyInitializationException in mapper
        poll.getOptions().forEach(option -> {
            option.getVotes().size();
            option.getVotes().forEach(vote -> {
                vote.getUser().getId();
            });
        });
    }

    /**
     * Find polls with cursor-based pagination
     */
    public List<Poll> findByConversationIdWithCursor(Long conversationId, Long cursor, int limit) {
        // First query to get poll IDs with cursor pagination
        StringBuilder jpql = new StringBuilder(
                "SELECT p.id FROM Poll p " +
                "WHERE p.conversation.id = :conversationId "
        );
        
        if (cursor != null) {
            jpql.append("AND p.id < :cursor ");
        }
        
        jpql.append("ORDER BY p.id DESC");
        
        var query = entityManager.createQuery(jpql.toString(), Long.class)
            .setParameter("conversationId", conversationId);
        
        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }
        
        List<Long> pollIds = query
            .setMaxResults(limit + 1)
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
                "ORDER BY p.id DESC",
                Poll.class)
            .setParameter("pollIds", pollIds)
            .getResultList()
            .stream()
            .peek(this::initializePoll)
            .toList();
    }

    public void delete(Poll poll) {
        if (entityManager.contains(poll)) {
            entityManager.remove(poll);
        } else {
            entityManager.remove(entityManager.merge(poll));
        }
    }
}
