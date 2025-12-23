package com.chattrix.api.repositories;

import com.chattrix.api.entities.PollVote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@ApplicationScoped
public class PollVoteRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public PollVote save(PollVote pollVote) {
        if (pollVote.getId() == null) {
            entityManager.persist(pollVote);
            return pollVote;
        } else {
            return entityManager.merge(pollVote);
        }
    }

    public List<PollVote> findByPollIdAndUserId(Long pollId, Long userId) {
        return entityManager.createQuery(
                "SELECT pv FROM PollVote pv WHERE pv.poll.id = :pollId AND pv.user.id = :userId",
                PollVote.class)
            .setParameter("pollId", pollId)
            .setParameter("userId", userId)
            .getResultList();
    }

    public List<PollVote> findByPollOptionId(Long pollOptionId) {
        return entityManager.createQuery(
                "SELECT pv FROM PollVote pv WHERE pv.pollOption.id = :pollOptionId",
                PollVote.class)
            .setParameter("pollOptionId", pollOptionId)
            .getResultList();
    }

    public Long countDistinctVotersByPollId(Long pollId) {
        return entityManager.createQuery(
                "SELECT COUNT(DISTINCT pv.user.id) FROM PollVote pv WHERE pv.poll.id = :pollId",
                Long.class)
            .setParameter("pollId", pollId)
            .getSingleResult();
    }

    public void deleteByPollIdAndUserId(Long pollId, Long userId) {
        entityManager.createQuery(
                "DELETE FROM PollVote pv WHERE pv.poll.id = :pollId AND pv.user.id = :userId")
            .setParameter("pollId", pollId)
            .setParameter("userId", userId)
            .executeUpdate();
    }
}
