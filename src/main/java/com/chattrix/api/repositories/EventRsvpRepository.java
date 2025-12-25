package com.chattrix.api.repositories;

import com.chattrix.api.entities.EventRsvp;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class EventRsvpRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public EventRsvp save(EventRsvp rsvp) {
        if (rsvp.getId() == null) {
            em.persist(rsvp);
            return rsvp;
        } else {
            return em.merge(rsvp);
        }
    }

    public Optional<EventRsvp> findByEventIdAndUserId(Long eventId, Long userId) {
        try {
            EventRsvp rsvp = em.createQuery(
                    "SELECT r FROM EventRsvp r " +
                            "WHERE r.event.id = :eventId AND r.user.id = :userId",
                    EventRsvp.class)
                    .setParameter("eventId", eventId)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return Optional.of(rsvp);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void delete(EventRsvp rsvp) {
        if (!em.contains(rsvp)) {
            rsvp = em.merge(rsvp);
        }
        em.remove(rsvp);
    }
}

