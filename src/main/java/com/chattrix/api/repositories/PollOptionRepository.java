package com.chattrix.api.repositories;

import com.chattrix.api.entities.PollOption;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PollOptionRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public PollOption save(PollOption pollOption) {
        if (pollOption.getId() == null) {
            entityManager.persist(pollOption);
            return pollOption;
        } else {
            return entityManager.merge(pollOption);
        }
    }

    public Optional<PollOption> findById(Long id) {
        PollOption pollOption = entityManager.find(PollOption.class, id);
        return Optional.ofNullable(pollOption);
    }

    public List<PollOption> findByPollId(Long pollId) {
        return entityManager.createQuery(
                "SELECT po FROM PollOption po WHERE po.poll.id = :pollId ORDER BY po.optionOrder",
                PollOption.class)
            .setParameter("pollId", pollId)
            .getResultList();
    }
}
