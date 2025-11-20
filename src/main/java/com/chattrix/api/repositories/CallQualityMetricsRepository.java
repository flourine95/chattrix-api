package com.chattrix.api.repositories;

import com.chattrix.api.entities.CallQualityMetrics;
import com.chattrix.api.entities.NetworkQuality;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CallQualityMetricsRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public CallQualityMetrics save(CallQualityMetrics metrics) {
        if (metrics.getId() == null) {
            em.persist(metrics);
            return metrics;
        } else {
            return em.merge(metrics);
        }
    }

    public List<CallQualityMetrics> findByCallId(String callId) {
        return em.createQuery(
                "SELECT cqm FROM CallQualityMetrics cqm WHERE cqm.callId = :callId ORDER BY cqm.recordedAt ASC",
                CallQualityMetrics.class)
                .setParameter("callId", callId)
                .getResultList();
    }

    public QualityStatistics calculateAverageQuality(String callId) {
        List<CallQualityMetrics> metrics = findByCallId(callId);

        if (metrics.isEmpty()) {
            return new QualityStatistics(null, null, null);
        }

        // Calculate average packet loss rate
        double totalPacketLoss = 0.0;
        int packetLossCount = 0;
        for (CallQualityMetrics metric : metrics) {
            if (metric.getPacketLossRate() != null) {
                totalPacketLoss += metric.getPacketLossRate();
                packetLossCount++;
            }
        }
        Double avgPacketLoss = packetLossCount > 0 ? totalPacketLoss / packetLossCount : null;

        // Calculate average RTT
        long totalRtt = 0;
        int rttCount = 0;
        for (CallQualityMetrics metric : metrics) {
            if (metric.getRoundTripTime() != null) {
                totalRtt += metric.getRoundTripTime();
                rttCount++;
            }
        }
        Integer avgRtt = rttCount > 0 ? (int) (totalRtt / rttCount) : null;

        // Determine overall network quality (most common or worst)
        NetworkQuality overallQuality = determineOverallQuality(metrics);

        return new QualityStatistics(overallQuality, avgPacketLoss, avgRtt);
    }

    private NetworkQuality determineOverallQuality(List<CallQualityMetrics> metrics) {
        int excellent = 0, good = 0, poor = 0, bad = 0, veryBad = 0;

        for (CallQualityMetrics metric : metrics) {
            if (metric.getNetworkQuality() != null) {
                switch (metric.getNetworkQuality()) {
                    case EXCELLENT -> excellent++;
                    case GOOD -> good++;
                    case POOR -> poor++;
                    case BAD -> bad++;
                    case VERY_BAD -> veryBad++;
                    case UNKNOWN -> {}
                }
            }
        }

        // Return the worst quality that appeared
        if (veryBad > 0) return NetworkQuality.VERY_BAD;
        if (bad > 0) return NetworkQuality.BAD;
        if (poor > 0) return NetworkQuality.POOR;
        if (good > 0) return NetworkQuality.GOOD;
        if (excellent > 0) return NetworkQuality.EXCELLENT;
        return NetworkQuality.UNKNOWN;
    }

    public static class QualityStatistics {
        private final NetworkQuality networkQuality;
        private final Double avgPacketLossRate;
        private final Integer avgRoundTripTime;

        public QualityStatistics(NetworkQuality networkQuality, Double avgPacketLossRate, Integer avgRoundTripTime) {
            this.networkQuality = networkQuality;
            this.avgPacketLossRate = avgPacketLossRate;
            this.avgRoundTripTime = avgRoundTripTime;
        }

        public NetworkQuality getNetworkQuality() {
            return networkQuality;
        }

        public Double getAvgPacketLossRate() {
            return avgPacketLossRate;
        }

        public Integer getAvgRoundTripTime() {
            return avgRoundTripTime;
        }
    }
}
