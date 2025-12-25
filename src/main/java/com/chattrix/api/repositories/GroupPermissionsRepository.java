package com.chattrix.api.repositories;

import com.chattrix.api.entities.GroupPermissions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class GroupPermissionsRepository {
    
    @PersistenceContext
    private EntityManager em;
    
    @Transactional
    public GroupPermissions save(GroupPermissions permissions) {
        if (permissions.getId() == null) {
            em.persist(permissions);
            return permissions;
        } else {
            return em.merge(permissions);
        }
    }
    
    public Optional<GroupPermissions> findByConversationId(Long conversationId) {
        try {
            GroupPermissions permissions = em.createQuery(
                    "SELECT gp FROM GroupPermissions gp WHERE gp.conversation.id = :conversationId",
                    GroupPermissions.class
            )
            .setParameter("conversationId", conversationId)
            .getSingleResult();
            return Optional.of(permissions);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    @Transactional
    public void delete(GroupPermissions permissions) {
        em.remove(em.contains(permissions) ? permissions : em.merge(permissions));
    }
}
