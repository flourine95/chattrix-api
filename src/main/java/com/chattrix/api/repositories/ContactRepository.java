package com.chattrix.api.repositories;

import com.chattrix.api.entities.Contact;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ContactRepository {

    @PersistenceContext
    private EntityManager em;

    public void save(Contact contact) {
        if (contact.getId() == null) {
            em.persist(contact);
        } else {
            em.merge(contact);
        }
    }

    public Optional<Contact> findById(Long id) {
        return Optional.ofNullable(em.find(Contact.class, id));
    }

    public List<Contact> findByUserId(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Contact c " +
                                "JOIN FETCH c.contactUser " +
                                "WHERE c.user.id = :userId AND c.status = 'ACCEPTED' " +
                                "ORDER BY c.favorite DESC, c.contactUser.fullName ASC",
                        Contact.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Contact> findFavoritesByUserId(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Contact c " +
                                "JOIN FETCH c.contactUser " +
                                "WHERE c.user.id = :userId AND c.favorite = true AND c.status = 'ACCEPTED' " +
                                "ORDER BY c.contactUser.fullName ASC",
                        Contact.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Contact> findPendingRequestsReceived(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Contact c " +
                                "JOIN FETCH c.user " +
                                "WHERE c.contactUser.id = :userId AND c.status = 'PENDING' " +
                                "ORDER BY c.requestedAt DESC",
                        Contact.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Contact> findPendingRequestsSent(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Contact c " +
                                "JOIN FETCH c.contactUser " +
                                "WHERE c.user.id = :userId AND c.status = 'PENDING' " +
                                "ORDER BY c.requestedAt DESC",
                        Contact.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Contact> findBlockedByUserId(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Contact c " +
                                "JOIN FETCH c.contactUser " +
                                "WHERE c.user.id = :userId AND c.status = 'BLOCKED' " +
                                "ORDER BY c.updatedAt DESC",
                        Contact.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public Optional<Contact> findByUserIdAndContactUserId(Long userId, Long contactUserId) {
        List<Contact> results = em.createQuery(
                        "SELECT c FROM Contact c " +
                                "WHERE c.user.id = :userId AND c.contactUser.id = :contactUserId",
                        Contact.class)
                .setParameter("userId", userId)
                .setParameter("contactUserId", contactUserId)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void delete(Contact contact) {
        em.remove(em.contains(contact) ? contact : em.merge(contact));
    }

    public boolean existsByUserIdAndContactUserId(Long userId, Long contactUserId) {
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Contact c " +
                                "WHERE c.user.id = :userId AND c.contactUser.id = :contactUserId",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("contactUserId", contactUserId)
                .getSingleResult();
        return count > 0;
    }

    /**
     * Check if two users are connected (mutual contacts with ACCEPTED status)
     */
    public boolean areUsersConnected(Long userId1, Long userId2) {
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Contact c " +
                                "WHERE ((c.user.id = :userId1 AND c.contactUser.id = :userId2) " +
                                "   OR (c.user.id = :userId2 AND c.contactUser.id = :userId1)) " +
                                "AND c.status = 'ACCEPTED'",
                        Long.class)
                .setParameter("userId1", userId1)
                .setParameter("userId2", userId2)
                .getSingleResult();
        return count > 0;
    }
}
