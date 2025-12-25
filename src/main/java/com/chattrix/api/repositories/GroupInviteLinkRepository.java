package com.chattrix.api.repositories;

import com.chattrix.api.entities.GroupInviteLink;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GroupInviteLinkRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public GroupInviteLink save(GroupInviteLink inviteLink) {
        if (inviteLink.getId() == null) {
            entityManager.persist(inviteLink);
            return inviteLink;
        } else {
            return entityManager.merge(inviteLink);
        }
    }

    public Optional<GroupInviteLink> findById(Long id) {
        List<GroupInviteLink> results = entityManager
                .createQuery("SELECT i FROM GroupInviteLink i " +
                        "LEFT JOIN FETCH i.createdBy " +
                        "LEFT JOIN FETCH i.revokedBy " +
                        "LEFT JOIN FETCH i.conversation " +
                        "WHERE i.id = :id", GroupInviteLink.class)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<GroupInviteLink> findByToken(String token) {
        List<GroupInviteLink> results = entityManager
                .createQuery("SELECT i FROM GroupInviteLink i " +
                        "LEFT JOIN FETCH i.createdBy " +
                        "LEFT JOIN FETCH i.revokedBy " +
                        "LEFT JOIN FETCH i.conversation " +
                        "WHERE i.token = :token", GroupInviteLink.class)
                .setParameter("token", token)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<GroupInviteLink> findByConversationId(Long conversationId) {
        return entityManager
                .createQuery("SELECT i FROM GroupInviteLink i " +
                        "LEFT JOIN FETCH i.createdBy " +
                        "LEFT JOIN FETCH i.revokedBy " +
                        "LEFT JOIN FETCH i.conversation " +
                        "WHERE i.conversation.id = :conversationId " +
                        "ORDER BY i.createdAt DESC", GroupInviteLink.class)
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public List<GroupInviteLink> findActiveByConversationId(Long conversationId) {
        return entityManager
                .createQuery("SELECT i FROM GroupInviteLink i WHERE i.conversation.id = :conversationId AND i.revoked = false ORDER BY i.createdAt DESC", GroupInviteLink.class)
                .setParameter("conversationId", conversationId)
                .getResultList();
    }

    public void delete(GroupInviteLink inviteLink) {
        entityManager.remove(entityManager.contains(inviteLink) ? inviteLink : entityManager.merge(inviteLink));
    }

    /**
     * Find invite links with cursor-based pagination
     */
    public List<GroupInviteLink> findByConversationIdWithCursor(Long conversationId, Long cursor, int limit, boolean includeRevoked) {
        StringBuilder jpql = new StringBuilder(
                "SELECT i FROM GroupInviteLink i " +
                "LEFT JOIN FETCH i.createdBy " +
                "LEFT JOIN FETCH i.revokedBy " +
                "LEFT JOIN FETCH i.conversation " +
                "WHERE i.conversation.id = :conversationId "
        );

        if (!includeRevoked) {
            jpql.append("AND i.revoked = false ");
        }

        if (cursor != null) {
            jpql.append("AND i.id < :cursor ");
        }

        jpql.append("ORDER BY i.id DESC");

        var query = entityManager.createQuery(jpql.toString(), GroupInviteLink.class)
                .setParameter("conversationId", conversationId);

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        return query.setMaxResults(limit + 1).getResultList();
    }
}
