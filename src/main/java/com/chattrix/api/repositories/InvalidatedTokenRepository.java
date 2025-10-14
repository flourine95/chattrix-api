package com.chattrix.api.repositories;

import com.chattrix.api.entities.InvalidatedToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class InvalidatedTokenRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void save(InvalidatedToken token) {
        entityManager.persist(token);
    }

    public Optional<InvalidatedToken> findByToken(String token) {
        try {
            InvalidatedToken invalidatedToken = entityManager
                    .createQuery("SELECT it FROM InvalidatedToken it WHERE it.token = :token", InvalidatedToken.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(invalidatedToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean isTokenInvalidated(String token) {
        return findByToken(token).isPresent();
    }

    @Transactional
    public int deleteExpiredTokens() {
        return entityManager
                .createQuery("DELETE FROM InvalidatedToken it WHERE it.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
    }
}
