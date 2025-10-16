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
                                "WHERE c.user.id = :userId AND c.isBlocked = false " +
                                "ORDER BY c.isFavorite DESC, c.contactUser.fullName ASC",
                        Contact.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Contact> findFavoritesByUserId(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Contact c " +
                                "JOIN FETCH c.contactUser " +
                                "WHERE c.user.id = :userId AND c.isFavorite = true AND c.isBlocked = false " +
                                "ORDER BY c.contactUser.fullName ASC",
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
}
