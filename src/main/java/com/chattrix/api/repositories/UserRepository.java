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

@ApplicationScoped
public class UserRepository {

    @PersistenceContext
    EntityManager em;

    public boolean existsUsername(String username) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }

    public boolean existsEmail(String email) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult() > 0;
    }

    public boolean existsUsernameExcludingUser(String username, Long userId) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username AND u.id != :userId", Long.class)
                .setParameter("username", username)
                .setParameter("userId", userId)
                .getSingleResult() > 0;
    }

    public boolean existsEmailExcludingUser(String email, Long userId) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email AND u.id != :userId", Long.class)
                .setParameter("email", email)
                .setParameter("userId", userId)
                .getSingleResult() > 0;
    }

    public Optional<User> findByEmail(String email) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
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

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail", User.class)
                    .setParameter("usernameOrEmail", usernameOrEmail)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    public List<User> findByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT u FROM User u WHERE u.id IN :ids", User.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public List<User> findByIsOnlineTrue() {
        return em.createQuery("SELECT u FROM User u WHERE u.online = true ORDER BY u.fullName", User.class)
                .getResultList();
    }

    public List<User> findOnlineUsersByConversationId(Long conversationId) {
        return em.createQuery(
                        "SELECT DISTINCT u FROM User u " +
                                "JOIN u.conversationParticipants cp " +
                                "WHERE cp.conversation.id = :conversationId AND u.online = true " +
                                "ORDER BY u.fullName", User.class)
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public List<User> findStaleOnlineUsers(Instant threshold) {
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.online = true AND u.lastSeen < :threshold", User.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    public List<User> searchUsers(String query, Long excludeUserId, int limit) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.id != :excludeUserId " +
                                "AND (LOWER(u.username) LIKE :query " +
                                "OR LOWER(u.fullName) LIKE :query " +
                                "OR LOWER(u.email) LIKE :query) " +
                                "ORDER BY " +
                                "CASE " +
                                "  WHEN LOWER(u.username) = :exactQuery THEN 1 " +
                                "  WHEN LOWER(u.fullName) = :exactQuery THEN 2 " +
                                "  WHEN LOWER(u.username) LIKE :startQuery THEN 3 " +
                                "  WHEN LOWER(u.fullName) LIKE :startQuery THEN 4 " +
                                "  ELSE 5 " +
                                "END, " +
                                "u.fullName ASC", User.class)
                .setParameter("excludeUserId", excludeUserId)
                .setParameter("query", searchPattern)
                .setParameter("exactQuery", query.toLowerCase())
                .setParameter("startQuery", query.toLowerCase() + "%")
                .setMaxResults(limit)
                .getResultList();
    }

    public List<User> searchUsersWithPagination(String query, Long excludeUserId, int page, int size) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.id != :excludeUserId " +
                                "AND (LOWER(u.username) LIKE :query " +
                                "OR LOWER(u.fullName) LIKE :query " +
                                "OR LOWER(u.email) LIKE :query) " +
                                "ORDER BY " +
                                "CASE " +
                                "  WHEN LOWER(u.username) = :exactQuery THEN 1 " +
                                "  WHEN LOWER(u.fullName) = :exactQuery THEN 2 " +
                                "  WHEN LOWER(u.username) LIKE :startQuery THEN 3 " +
                                "  WHEN LOWER(u.fullName) LIKE :startQuery THEN 4 " +
                                "  ELSE 5 " +
                                "END, " +
                                "u.fullName ASC", User.class)
                .setParameter("excludeUserId", excludeUserId)
                .setParameter("query", searchPattern)
                .setParameter("exactQuery", query.toLowerCase())
                .setParameter("startQuery", query.toLowerCase() + "%")
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countSearchUsers(String query, Long excludeUserId) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        return em.createQuery(
                        "SELECT COUNT(u) FROM User u " +
                                "WHERE u.id != :excludeUserId " +
                                "AND (LOWER(u.username) LIKE :query " +
                                "OR LOWER(u.fullName) LIKE :query " +
                                "OR LOWER(u.email) LIKE :query)", Long.class)
                .setParameter("excludeUserId", excludeUserId)
                .setParameter("query", searchPattern)
                .getSingleResult();
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

    public List<User> findByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT u FROM User u WHERE u.id IN :userIds", User.class)
                .setParameter("userIds", userIds)
                .getResultList();
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    /**
     * Find all users who should receive status updates for a given user
     * (users who have this user as a contact OR share a conversation with them)
     */
    public List<Long> findUserIdsWhoShouldReceiveStatusUpdates(Long userId) {
        // Get users who have this user as a contact (bidirectional)
        List<Long> contactUserIds = em.createQuery(
                        "SELECT DISTINCT c.user.id FROM Contact c " +
                                "WHERE c.contactUser.id = :userId AND c.status = 'ACCEPTED'", Long.class)
                .setParameter("userId", userId)
                .getResultList();

        // Get users who are contacts of this user
        List<Long> contactedByUserIds = em.createQuery(
                        "SELECT DISTINCT c.contactUser.id FROM Contact c " +
                                "WHERE c.user.id = :userId AND c.status = 'ACCEPTED'", Long.class)
                .setParameter("userId", userId)
                .getResultList();

        // Get users in same conversations
        List<Long> conversationMemberIds = em.createQuery(
                        "SELECT DISTINCT cp.user.id FROM ConversationParticipant cp " +
                                "WHERE cp.conversation.id IN (" +
                                "  SELECT cp2.conversation.id FROM ConversationParticipant cp2 " +
                                "  WHERE cp2.user.id = :userId" +
                                ") AND cp.user.id != :userId", Long.class)
                .setParameter("userId", userId)
                .getResultList();

        // Merge all results and remove duplicates using Set
        Set<Long> allUserIds = new java.util.HashSet<>();
        allUserIds.addAll(contactUserIds);
        allUserIds.addAll(contactedByUserIds);
        allUserIds.addAll(conversationMemberIds);

        return new java.util.ArrayList<>(allUserIds);
    }
}
