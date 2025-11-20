package com.chattrix.api.repositories;

import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.CallStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CallRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Call save(Call call) {
        if (call.getId() == null) {
            em.persist(call);
            return call;
        } else {
            return em.merge(call);
        }
    }

    public Optional<Call> findById(String callId) {
        return Optional.ofNullable(em.find(Call.class, callId));
    }

    public Optional<Call> findActiveCallByUserId(Long userId) {
        try {
            Call call = em.createQuery(
                    "SELECT c FROM Call c " +
                    "WHERE (c.callerId = :userId OR c.calleeId = :userId) " +
                    "AND c.status IN :activeStatuses " +
                    "ORDER BY c.createdAt DESC",
                    Call.class)
                    .setParameter("userId", userId)
                    .setParameter("activeStatuses", List.of(
                            CallStatus.INITIATING,
                            CallStatus.RINGING,
                            CallStatus.CONNECTING,
                            CallStatus.CONNECTED
                    ))
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(call);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Call> findByChannelId(String channelId) {
        return em.createQuery(
                "SELECT c FROM Call c WHERE c.channelId = :channelId ORDER BY c.createdAt DESC",
                Call.class)
                .setParameter("channelId", channelId)
                .getResultList();
    }

    @Transactional
    public void updateStatus(String callId, CallStatus status) {
        em.createQuery("UPDATE Call c SET c.status = :status, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :callId")
                .setParameter("status", status)
                .setParameter("callId", callId)
                .executeUpdate();
    }
}
