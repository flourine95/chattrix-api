package com.chattrix.api.repositories;

import com.chattrix.api.entities.PasswordResetToken;
import com.chattrix.api.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class PasswordResetTokenRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public PasswordResetToken save(PasswordResetToken token) {
        if (token.getId() == null) {
            em.persist(token);
            return token;
        } else {
            return em.merge(token);
        }
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        try {
            PasswordResetToken resetToken = em.createQuery(
                            "SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token",
                            PasswordResetToken.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(resetToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<PasswordResetToken> findValidTokenByUser(User user) {
        try {
            PasswordResetToken token = em.createQuery(
                            "SELECT prt FROM PasswordResetToken prt WHERE prt.user = :user AND prt.used = false AND prt.expiresAt > :now ORDER BY prt.createdAt DESC",
                            PasswordResetToken.class)
                    .setParameter("user", user)
                    .setParameter("now", Instant.now())
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(token);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void deleteExpiredTokens() {
        em.createQuery("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    @Transactional
    public void deleteByUser(User user) {
        em.createQuery("DELETE FROM PasswordResetToken prt WHERE prt.user = :user")
                .setParameter("user", user)
                .executeUpdate();
    }
}
