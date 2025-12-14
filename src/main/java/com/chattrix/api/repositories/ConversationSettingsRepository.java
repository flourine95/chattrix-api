package com.chattrix.api.repositories;

import com.chattrix.api.entities.ConversationSettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class ConversationSettingsRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public ConversationSettings save(ConversationSettings settings) {
        if (settings.getId() == null) {
            em.persist(settings);
            return settings;
        } else {
            return em.merge(settings);
        }
    }

    public Optional<ConversationSettings> findByUserIdAndConversationId(Long userId, Long conversationId) {
        try {
            ConversationSettings settings = em.createQuery(
                            "SELECT cs FROM ConversationSettings cs " +
                                    "WHERE cs.user.id = :userId AND cs.conversation.id = :conversationId",
                            ConversationSettings.class)
                    .setParameter("userId", userId)
                    .setParameter("conversationId", conversationId)
                    .getSingleResult();
            return Optional.of(settings);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<ConversationSettings> findByConversationIdAndUserId(Long conversationId, Long userId) {
        return findByUserIdAndConversationId(userId, conversationId);
    }

    public Integer getMaxPinOrder(Long userId) {
        Integer maxOrder = em.createQuery(
                        "SELECT MAX(cs.pinOrder) FROM ConversationSettings cs " +
                                "WHERE cs.user.id = :userId AND cs.pinned = true",
                        Integer.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return maxOrder != null ? maxOrder : 0;
    }
}

