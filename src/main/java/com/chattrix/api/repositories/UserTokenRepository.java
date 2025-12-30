package com.chattrix.api.repositories;

import com.chattrix.api.entities.TokenType;
import com.chattrix.api.entities.UserToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserTokenRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public UserToken save(UserToken token) {
        if (token.getId() == null) {
            entityManager.persist(token);
            return token;
        } else {
            return entityManager.merge(token);
        }
    }

    public Optional<UserToken> findByToken(String token) {
        try {
            UserToken userToken = entityManager.createQuery(
                    "SELECT t FROM UserToken t WHERE t.token = :token", UserToken.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(userToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<UserToken> findByTokenAndType(String token, TokenType type) {
        try {
            UserToken userToken = entityManager.createQuery(
                    "SELECT t FROM UserToken t WHERE t.token = :token AND t.type = :type", UserToken.class)
                    .setParameter("token", token)
                    .setParameter("type", type)
                    .getSingleResult();
            return Optional.of(userToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<UserToken> findByUserIdAndType(Long userId, TokenType type) {
        return entityManager.createQuery(
                "SELECT t FROM UserToken t WHERE t.user.id = :userId AND t.type = :type ORDER BY t.createdAt DESC", 
                UserToken.class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .getResultList();
    }

    public Optional<UserToken> findLatestByUserIdAndType(Long userId, TokenType type) {
        try {
            UserToken token = entityManager.createQuery(
                    "SELECT t FROM UserToken t WHERE t.user.id = :userId AND t.type = :type " +
                    "ORDER BY t.createdAt DESC", UserToken.class)
                    .setParameter("userId", userId)
                    .setParameter("type", type)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(token);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public void deleteExpiredTokens() {
        entityManager.createQuery(
                "DELETE FROM UserToken t WHERE t.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    public void deleteUsedTokens() {
        entityManager.createQuery(
                "DELETE FROM UserToken t WHERE t.used = true")
                .executeUpdate();
    }

    public void deleteByUserId(Long userId) {
        entityManager.createQuery(
                "DELETE FROM UserToken t WHERE t.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public void deleteByUserIdAndType(Long userId, TokenType type) {
        entityManager.createQuery(
                "DELETE FROM UserToken t WHERE t.user.id = :userId AND t.type = :type")
                .setParameter("userId", userId)
                .setParameter("type", type)
                .executeUpdate();
    }

    public long countByUserIdAndType(Long userId, TokenType type) {
        return entityManager.createQuery(
                "SELECT COUNT(t) FROM UserToken t WHERE t.user.id = :userId AND t.type = :type", Long.class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .getSingleResult();
    }

    public List<UserToken> findExpiredTokens() {
        return entityManager.createQuery(
                "SELECT t FROM UserToken t WHERE t.expiresAt < :now", UserToken.class)
                .setParameter("now", Instant.now())
                .getResultList();
    }
}
