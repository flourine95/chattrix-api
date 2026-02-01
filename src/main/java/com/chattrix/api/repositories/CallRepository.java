package com.chattrix.api.repositories;

import com.chattrix.api.entities.Call;
import com.chattrix.api.enums.CallParticipantStatus;
import com.chattrix.api.enums.CallStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.time.Instant;
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
                            "SELECT DISTINCT c FROM Call c " +
                                    "LEFT JOIN c.participants p " +
                                    "WHERE (c.callerId = :userId OR p.userId = :userId) " +
                                    "AND c.status IN :activeStatuses " +
                                    "AND (p.status IS NULL OR p.status NOT IN :inactiveParticipantStatuses) " +
                                    "ORDER BY c.createdAt DESC",
                            Call.class)
                    .setParameter("userId", userId)
                    .setParameter("activeStatuses", List.of(
                            CallStatus.INITIATING,
                            CallStatus.RINGING,
                            CallStatus.CONNECTING,
                            CallStatus.CONNECTED
                    ))
                    .setParameter("inactiveParticipantStatuses", List.of(
                            CallParticipantStatus.LEFT,
                            CallParticipantStatus.REJECTED,
                            CallParticipantStatus.MISSED
                    ))
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(call);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Call> findActiveCallByConversationId(Long conversationId) {
        try {
            Call call = em.createQuery(
                            "SELECT c FROM Call c " +
                                    "WHERE c.conversationId = :conversationId " +
                                    "AND c.status IN :activeStatuses " +
                                    "ORDER BY c.createdAt DESC",
                            Call.class)
                    .setParameter("conversationId", conversationId)
                    .setParameter("activeStatuses", List.of(
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

    /**
     * Find calls that have been in CONNECTING or CONNECTED state for too long
     */
    public List<Call> findLongRunningCalls(Instant cutoffTime) {
        return em.createQuery(
                        "SELECT c FROM Call c " +
                                "WHERE c.status IN :activeStatuses " +
                                "AND c.startTime < :cutoff " +
                                "ORDER BY c.startTime ASC",
                        Call.class)
                .setParameter("activeStatuses", List.of(
                        CallStatus.CONNECTING,
                        CallStatus.CONNECTED
                ))
                .setParameter("cutoff", cutoffTime)
                .getResultList();
    }

    /**
     * Find calls stuck in RINGING state (safety net)
     */
    public List<Call> findStuckRingingCalls(Instant cutoffTime) {
        return em.createQuery(
                        "SELECT c FROM Call c " +
                                "WHERE c.status IN :ringingStatuses " +
                                "AND c.createdAt < :cutoff " +
                                "ORDER BY c.createdAt ASC",
                        Call.class)
                .setParameter("ringingStatuses", List.of(
                        CallStatus.INITIATING,
                        CallStatus.RINGING
                ))
                .setParameter("cutoff", cutoffTime)
                .getResultList();
    }

    /**
     * Find call history for a user (finished calls)
     * Returns calls with status ENDED, MISSED, or REJECTED
     */
    public List<Call> findCallHistoryByUserId(Long userId, int limit) {
        return em.createQuery(
                        "SELECT DISTINCT c FROM Call c " +
                                "LEFT JOIN c.participants p " +
                                "WHERE (c.callerId = :userId OR p.userId = :userId) " +
                                "AND c.status IN :finishedStatuses " +
                                "ORDER BY c.createdAt DESC",
                        Call.class)
                .setParameter("userId", userId)
                .setParameter("finishedStatuses", List.of(
                        CallStatus.ENDED,
                        CallStatus.MISSED,
                        CallStatus.REJECTED
                ))
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Find call history with cursor-based pagination
     */
    public List<Call> findCallHistoryByUserIdWithCursor(Long userId, Long cursor, int limit) {
        StringBuilder jpql = new StringBuilder(
                "SELECT DISTINCT c FROM Call c " +
                        "LEFT JOIN c.participants p " +
                        "WHERE (c.callerId = :userId OR p.userId = :userId) " +
                        "AND c.status IN :finishedStatuses "
        );

        if (cursor != null) {
            jpql.append("AND c.id < :cursor ");
        }

        jpql.append("ORDER BY c.createdAt DESC");

        TypedQuery<Call> query = em.createQuery(jpql.toString(), Call.class);
        query.setParameter("userId", userId);
        query.setParameter("finishedStatuses", List.of(
                CallStatus.ENDED,
                CallStatus.MISSED,
                CallStatus.REJECTED
        ));

        if (cursor != null) {
            query.setParameter("cursor", cursor);
        }

        query.setMaxResults(limit + 1);

        return query.getResultList();
    }

    /**
     * Find call history by user and status
     */
    public List<Call> findCallHistoryByUserIdAndStatus(Long userId, CallStatus status, int limit) {
        return em.createQuery(
                        "SELECT DISTINCT c FROM Call c " +
                                "LEFT JOIN c.participants p " +
                                "WHERE (c.callerId = :userId OR p.userId = :userId) " +
                                "AND c.status = :status " +
                                "ORDER BY c.createdAt DESC",
                        Call.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Count call history for a user
     */
    public long countCallHistoryByUserId(Long userId) {
        return em.createQuery(
                        "SELECT COUNT(DISTINCT c) FROM Call c " +
                                "LEFT JOIN c.participants p " +
                                "WHERE (c.callerId = :userId OR p.userId = :userId) " +
                                "AND c.status IN :finishedStatuses",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("finishedStatuses", List.of(
                        CallStatus.ENDED,
                        CallStatus.MISSED,
                        CallStatus.REJECTED
                ))
                .getSingleResult();
    }
}
