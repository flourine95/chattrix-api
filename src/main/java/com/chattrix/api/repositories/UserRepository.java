package com.chattrix.api.repositories;

import com.chattrix.api.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext
    EntityManager em;

    public boolean existsUsername(String username) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }

    public Optional<User> findByUsername(String username) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    public List<User> findByIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT u FROM User u WHERE u.id IN :ids", User.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public List<User> findByIsOnlineTrue() {
        return em.createQuery("SELECT u FROM User u WHERE u.isOnline = true ORDER BY u.displayName", User.class)
                .getResultList();
    }

    public List<User> findOnlineUsersByConversationId(UUID conversationId) {
        return em.createQuery(
                        "SELECT DISTINCT u FROM User u " +
                                "JOIN u.conversationParticipants cp " +
                                "WHERE cp.conversation.id = :conversationId AND u.isOnline = true " +
                                "ORDER BY u.displayName", User.class)
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public List<User> findStaleOnlineUsers(Instant threshold) {
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.isOnline = true AND u.lastSeen < :threshold", User.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            em.persist(user);
            return user;
        } else {
            return em.merge(user);
        }
    }
}
