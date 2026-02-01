package com.chattrix.api.repositories;

import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Find users by IDs - DTO Projection (optimized)
     * Returns UserResponse directly without entity mapping
     */
    public List<UserResponse> findByIdsAsDTO(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        "SELECT new com.chattrix.api.responses.UserResponse(" +
                                "  u.id, u.username, u.email, u.emailVerified, u.phone, " +
                                "  u.fullName, u.avatarUrl, u.bio, u.gender, u.dateOfBirth, " +
                                "  u.location, u.profileVisibility, u.lastSeen, u.createdAt, u.updatedAt" +
                                ") " +
                                "FROM User u " +
                                "WHERE u.id IN :ids",
                        UserResponse.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    /**
     * Find online users - now uses in-memory tracking via UserStatusService
     * This method is deprecated and returns empty list
     * @deprecated Use UserStatusService.isUserOnline() instead
     */
    @Deprecated
    public List<User> findByIsOnlineTrue() {
        // Online status is now tracked in-memory via UserStatusService
        // This method is kept for backward compatibility but returns empty list
        return List.of();
    }

    /**
     * Find online users in a conversation - now uses in-memory tracking
     * This method is deprecated and returns empty list
     * @deprecated Use UserStatusService.isUserOnline() for each participant instead
     */
    @Deprecated
    public List<User> findOnlineUsersByConversationId(Long conversationId) {
        // Online status is now tracked in-memory via UserStatusService
        // This method is kept for backward compatibility but returns empty list
        return List.of();
    }

    /**
     * Find stale online users - no longer needed as online status is in-memory
     * This method is deprecated and returns empty list
     * @deprecated Online status cleanup is now handled by UserStatusService
     */
    @Deprecated
    public List<User> findStaleOnlineUsers(Instant threshold) {
        // Online status is now tracked in-memory via UserStatusService
        // No database cleanup needed
        return List.of();
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

    /**
     * Search users with cursor-based pagination.
     * Uses user ID as cursor for efficient pagination.
     */
    public List<User> searchUsersWithCursor(String query, Long excludeUserId, Long cursor, int limit) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        
        StringBuilder jpql = new StringBuilder(
                "SELECT u FROM User u " +
                "WHERE u.id != :excludeUserId " +
                "AND (LOWER(u.username) LIKE :query " +
                "OR LOWER(u.fullName) LIKE :query " +
                "OR LOWER(u.email) LIKE :query) ");
        
        if (cursor != null) {
            jpql.append("AND u.id < :cursor ");
        }
        
        jpql.append("ORDER BY " +
                "CASE " +
                "  WHEN LOWER(u.username) = :exactQuery THEN 1 " +
                "  WHEN LOWER(u.fullName) = :exactQuery THEN 2 " +
                "  WHEN LOWER(u.username) LIKE :startQuery THEN 3 " +
                "  WHEN LOWER(u.fullName) LIKE :startQuery THEN 4 " +
                "  ELSE 5 " +
                "END, " +
                "u.id DESC");
        
        var query_obj = em.createQuery(jpql.toString(), User.class)
                .setParameter("excludeUserId", excludeUserId)
                .setParameter("query", searchPattern)
                .setParameter("exactQuery", query.toLowerCase())
                .setParameter("startQuery", query.toLowerCase() + "%");
        
        if (cursor != null) {
            query_obj.setParameter("cursor", cursor);
        }
        
        return query_obj.setMaxResults(limit + 1).getResultList();
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
     * Find all users - DTO Projection (optimized)
     * Returns UserResponse directly without entity mapping
     */
    public List<UserResponse> findAllAsDTO() {
        return em.createQuery(
                        "SELECT new com.chattrix.api.responses.UserResponse(" +
                                "  u.id, u.username, u.email, u.emailVerified, u.phone, " +
                                "  u.fullName, u.avatarUrl, u.bio, u.gender, u.dateOfBirth, " +
                                "  u.location, u.profileVisibility, u.lastSeen, u.createdAt, u.updatedAt" +
                                ") " +
                                "FROM User u",
                        UserResponse.class)
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
        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(contactUserIds);
        allUserIds.addAll(contactedByUserIds);
        allUserIds.addAll(conversationMemberIds);

        return new ArrayList<>(allUserIds);
    }

    /**
     * Find users whose birthday is today (same month and day, regardless of year)
     */
    public List<User> findUsersWithBirthdayToday() {
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.dateOfBirth IS NOT NULL " +
                                "AND EXTRACT(MONTH FROM u.dateOfBirth) = EXTRACT(MONTH FROM CURRENT_DATE) " +
                                "AND EXTRACT(DAY FROM u.dateOfBirth) = EXTRACT(DAY FROM CURRENT_DATE) " +
                                "ORDER BY u.fullName", User.class)
                .getResultList();
    }

    /**
     * Find users whose birthday is within the next N days
     */
    public List<User> findUsersWithUpcomingBirthdays(int daysAhead) {
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.dateOfBirth IS NOT NULL " +
                                "ORDER BY u.fullName", User.class)
                .getResultList();
        // Note: Complex date logic will be handled in service layer
        // because JPQL doesn't handle year-wrap-around birthdays well
    }

    /**
     * Find users with birthdays in a specific month and day range
     */
    public List<User> findUsersByBirthdayMonthAndDay(int month, int day) {
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.dateOfBirth IS NOT NULL " +
                                "AND EXTRACT(MONTH FROM u.dateOfBirth) = :month " +
                                "AND EXTRACT(DAY FROM u.dateOfBirth) = :day " +
                                "ORDER BY u.fullName", User.class)
                .setParameter("month", month)
                .setParameter("day", day)
                .getResultList();
    }

    /**
     * Find recent active users for cache warming
     * Returns users who have been active in the last 7 days
     */
    public List<User> findRecentActiveUsers(int limit) {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        return em.createQuery(
                        "SELECT u FROM User u " +
                                "WHERE u.lastSeen >= :threshold " +
                                "ORDER BY u.lastSeen DESC", User.class)
                .setParameter("threshold", sevenDaysAgo)
                .setMaxResults(limit)
                .getResultList();
    }
    
    /**
     * Batch update lastSeen for multiple users in single query.
     * Uses CASE WHEN for efficient batch update.
     * 
     * Example: Update 100 users in 1 query instead of 100 separate queries.
     * 
     * @param updates Map of userId -> lastSeen timestamp
     */
    @Transactional
    public void batchUpdateLastSeen(Map<Long, Instant> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        
        // Build CASE WHEN query for batch update
        StringBuilder sql = new StringBuilder("UPDATE users SET last_seen = CASE id ");
        
        for (Map.Entry<Long, Instant> entry : updates.entrySet()) {
            sql.append("WHEN ").append(entry.getKey())
               .append(" THEN TIMESTAMP '").append(entry.getValue()).append("' ");
        }
        
        sql.append("END WHERE id IN (");
        sql.append(updates.keySet().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",")));
        sql.append(")");
        
        int updatedCount = em.createNativeQuery(sql.toString()).executeUpdate();
        
        // Log if mismatch (some users might have been deleted)
        if (updatedCount != updates.size()) {
            System.out.println("Warning: Batch update expected " + updates.size() + 
                " users but updated " + updatedCount);
        }
    }

    public List<User> findAllById(Set<Long> allUserIds) {
        if (allUserIds == null || allUserIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery("SELECT u FROM User u WHERE u.id IN :allUserIds", User.class)
                .setParameter("allUserIds", allUserIds)
                .getResultList();
    }
}

