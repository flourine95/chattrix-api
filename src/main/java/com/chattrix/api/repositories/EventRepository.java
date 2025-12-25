package com.chattrix.api.repositories;

import com.chattrix.api.entities.Event;
import com.chattrix.api.entities.EventRsvp;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Event save(Event event) {
        if (event.getId() == null) {
            em.persist(event);
            return event;
        } else {
            return em.merge(event);
        }
    }

    public Optional<Event> findById(Long id) {
        return Optional.ofNullable(em.find(Event.class, id));
    }

    public Optional<Event> findByIdWithDetails(Long id) {
        try {
            // First query: fetch event with creator and conversation
            Event event = em.createQuery(
                    "SELECT e FROM Event e " +
                            "LEFT JOIN FETCH e.creator " +
                            "LEFT JOIN FETCH e.conversation " +
                            "WHERE e.id = :id",
                    Event.class)
                    .setParameter("id", id)
                    .getSingleResult();

            // Second query: fetch rsvps with users (avoid alias issue)
            em.createQuery(
                    "SELECT e FROM Event e " +
                            "LEFT JOIN FETCH e.rsvps " +
                            "WHERE e.id = :id",
                    Event.class)
                    .setParameter("id", id)
                    .getSingleResult();

            // Third query: fetch users for rsvps
            em.createQuery(
                    "SELECT r FROM EventRsvp r " +
                            "LEFT JOIN FETCH r.user " +
                            "WHERE r.event.id = :id",
                    EventRsvp.class)
                    .setParameter("id", id)
                    .getResultList();

            return Optional.of(event);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Event> findByConversationId(Long conversationId) {
        // First query: get event IDs
        List<Long> eventIds = em.createQuery(
                "SELECT e.id FROM Event e " +
                        "WHERE e.conversation.id = :conversationId " +
                        "ORDER BY e.startTime ASC",
                Long.class)
                .setParameter("conversationId", conversationId)
                .getResultList();

        if (eventIds.isEmpty()) {
            return List.of();
        }

        // Second query: fetch events with creator
        List<Event> events = em.createQuery(
                "SELECT DISTINCT e FROM Event e " +
                        "LEFT JOIN FETCH e.creator " +
                        "WHERE e.id IN :eventIds " +
                        "ORDER BY e.startTime ASC",
                Event.class)
                .setParameter("eventIds", eventIds)
                .getResultList();

        // Third query: fetch all rsvps for these events
        if (!eventIds.isEmpty()) {
            em.createQuery(
                    "SELECT r FROM EventRsvp r " +
                            "LEFT JOIN FETCH r.user " +
                            "LEFT JOIN FETCH r.event " +
                            "WHERE r.event.id IN :eventIds",
                    EventRsvp.class)
                    .setParameter("eventIds", eventIds)
                    .getResultList();
        }

        return events;
    }

    @Transactional
    public void delete(Event event) {
        if (!em.contains(event)) {
            event = em.merge(event);
        }
        em.remove(event);
    }

    public void flush() {
        em.flush();
    }

    public void clear() {
        em.clear();
    }

    public void detach(Event event) {
        em.detach(event);
    }
}

