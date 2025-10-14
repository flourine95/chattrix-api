package com.chattrix.api.repositories;

import com.chattrix.api.entities.RefreshToken;
import com.chattrix.api.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RefreshTokenRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void save(RefreshToken token) {
        entityManager.persist(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
        try {
            RefreshToken refreshToken = entityManager
                    .createQuery("SELECT rt FROM RefreshToken rt WHERE rt.token = :token", RefreshToken.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(refreshToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<RefreshToken> findValidToken(String token) {
        try {
            RefreshToken refreshToken = entityManager
                    .createQuery("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.isRevoked = false AND rt.expiresAt > :now", RefreshToken.class)
                    .setParameter("token", token)
                    .setParameter("now", Instant.now())
                    .getSingleResult();
            return Optional.of(refreshToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<RefreshToken> findByAccessTokenId(String accessTokenId) {
        try {
            RefreshToken refreshToken = entityManager
                    .createQuery("SELECT rt FROM RefreshToken rt WHERE rt.accessTokenId = :accessTokenId AND rt.isRevoked = false", RefreshToken.class)
                    .setParameter("accessTokenId", accessTokenId)
                    .getSingleResult();
            return Optional.of(refreshToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void update(RefreshToken token) {
        entityManager.merge(token);
    }

    @Transactional
    public void revokeAllByUser(User user) {
        entityManager
                .createQuery("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :now WHERE rt.user = :user AND rt.isRevoked = false")
                .setParameter("user", user)
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    public List<RefreshToken> findActiveTokensByUser(User user) {
        return entityManager
                .createQuery("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now", RefreshToken.class)
                .setParameter("user", user)
                .setParameter("now", Instant.now())
                .getResultList();
    }

    @Transactional
    public int deleteExpiredTokens() {
        return entityManager
                .createQuery("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    @Transactional
    public int deleteRevokedTokens() {
        return entityManager
                .createQuery("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true AND rt.revokedAt < :threshold")
                .setParameter("threshold", Instant.now().minusSeconds(86400 * 7)) // Xóa token đã revoke > 7 ngày
                .executeUpdate();
    }
}
