package com.chattrix.api.repositories;

import com.chattrix.api.entities.User;
import com.chattrix.api.entities.VerificationToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class VerificationTokenRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public VerificationToken save(VerificationToken token) {
        if (token.getId() == null) {
            em.persist(token);
            return token;
        } else {
            return em.merge(token);
        }
    }

    public Optional<VerificationToken> findByToken(String token) {
        try {
            VerificationToken verificationToken = em.createQuery(
                            "SELECT vt FROM VerificationToken vt WHERE vt.token = :token",
                            VerificationToken.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(verificationToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<VerificationToken> findValidTokenByUser(User user) {
        try {
            VerificationToken token = em.createQuery(
                            "SELECT vt FROM VerificationToken vt WHERE vt.user = :user AND vt.isUsed = false AND vt.expiresAt > :now ORDER BY vt.createdAt DESC",
                            VerificationToken.class)
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
        em.createQuery("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    @Transactional
    public void deleteByUser(User user) {
        em.createQuery("DELETE FROM VerificationToken vt WHERE vt.user = :user")
                .setParameter("user", user)
                .executeUpdate();
    }
}

