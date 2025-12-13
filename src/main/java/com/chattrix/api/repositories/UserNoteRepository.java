package com.chattrix.api.repositories;

import com.chattrix.api.entities.UserNote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserNoteRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public UserNote save(UserNote note) {
        if (note.getId() == null) {
            em.persist(note);
            return note;
        } else {
            return em.merge(note);
        }
    }

    public Optional<UserNote> findByUserId(Long userId) {
        try {
            UserNote note = em.createQuery(
                            "SELECT un FROM UserNote un " +
                                    "WHERE un.user.id = :userId " +
                                    "AND un.expiresAt > :now",
                            UserNote.class)
                    .setParameter("userId", userId)
                    .setParameter("now", Instant.now())
                    .getSingleResult();
            return Optional.of(note);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<UserNote> findById(Long noteId) {
        try {
            UserNote note = em.createQuery(
                            "SELECT un FROM UserNote un " +
                                    "JOIN FETCH un.user " +
                                    "WHERE un.id = :noteId " +
                                    "AND un.expiresAt > :now",
                            UserNote.class)
                    .setParameter("noteId", noteId)
                    .setParameter("now", Instant.now())
                    .getSingleResult();
            return Optional.of(note);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Get active notes from user's contacts/friends
     */
    public List<UserNote> findActiveNotesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return em.createQuery(
                        "SELECT un FROM UserNote un " +
                                "JOIN FETCH un.user " +
                                "WHERE un.user.id IN :userIds " +
                                "AND un.expiresAt > :now " +
                                "ORDER BY un.createdAt DESC",
                        UserNote.class)
                .setParameter("userIds", userIds)
                .setParameter("now", Instant.now())
                .getResultList();
    }

    @Transactional
    public void delete(UserNote note) {
        if (!em.contains(note)) {
            note = em.merge(note);
        }
        em.remove(note);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        em.createQuery("DELETE FROM UserNote un WHERE un.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /**
     * Clean up expired notes (should be run periodically)
     */
    @Transactional
    public int deleteExpiredNotes() {
        return em.createQuery(
                        "DELETE FROM UserNote un WHERE un.expiresAt <= :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    public long countActiveNotes() {
        return em.createQuery(
                        "SELECT COUNT(un) FROM UserNote un WHERE un.expiresAt > :now",
                        Long.class)
                .setParameter("now", Instant.now())
                .getSingleResult();
    }
}

