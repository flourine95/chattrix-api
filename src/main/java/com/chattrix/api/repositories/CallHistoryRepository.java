package com.chattrix.api.repositories;

import com.chattrix.api.entities.CallHistory;
import com.chattrix.api.entities.CallHistoryStatus;
import com.chattrix.api.entities.CallType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CallHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public CallHistory save(CallHistory callHistory) {
        if (callHistory.getId() == null) {
            em.persist(callHistory);
            return callHistory;
        } else {
            return em.merge(callHistory);
        }
    }

    public List<CallHistory> findByUserId(String userId, int page, int size, CallType callType, CallHistoryStatus status) {
        StringBuilder jpql = new StringBuilder("SELECT ch FROM CallHistory ch WHERE ch.userId = :userId");

        if (callType != null) {
            jpql.append(" AND ch.callType = :callType");
        }

        if (status != null) {
            jpql.append(" AND ch.status = :status");
        }

        jpql.append(" ORDER BY ch.timestamp DESC");

        TypedQuery<CallHistory> query = em.createQuery(jpql.toString(), CallHistory.class);
        query.setParameter("userId", userId);

        if (callType != null) {
            query.setParameter("callType", callType);
        }

        if (status != null) {
            query.setParameter("status", status);
        }

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    public long countByUserId(String userId, CallType callType, CallHistoryStatus status) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(ch) FROM CallHistory ch WHERE ch.userId = :userId");

        if (callType != null) {
            jpql.append(" AND ch.callType = :callType");
        }

        if (status != null) {
            jpql.append(" AND ch.status = :status");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        query.setParameter("userId", userId);

        if (callType != null) {
            query.setParameter("callType", callType);
        }

        if (status != null) {
            query.setParameter("status", status);
        }

        return query.getSingleResult();
    }

    @Transactional
    public void deleteByCallIdAndUserId(String callId, String userId) {
        em.createQuery("DELETE FROM CallHistory ch WHERE ch.callId = :callId AND ch.userId = :userId")
                .setParameter("callId", callId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /**
     * Finds call history entries for a user within a date range.
     * Used for statistics calculation.
     *
     * @param userId    the user ID
     * @param startDate the start of the date range
     * @param endDate   the end of the date range
     * @return list of call history entries within the date range
     */
    public List<CallHistory> findByUserIdAndDateRange(String userId, java.time.Instant startDate, java.time.Instant endDate) {
        return em.createQuery(
                        "SELECT ch FROM CallHistory ch WHERE ch.userId = :userId " +
                                "AND ch.timestamp >= :startDate AND ch.timestamp < :endDate",
                        CallHistory.class)
                .setParameter("userId", userId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }
}
