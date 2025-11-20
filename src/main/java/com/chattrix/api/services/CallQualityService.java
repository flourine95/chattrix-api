package com.chattrix.api.services;

import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.CallQualityMetrics;
import com.chattrix.api.entities.NetworkQuality;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.repositories.CallQualityMetricsRepository;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.requests.ReportQualityRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing call quality metrics.
 * Handles quality reporting, validation, warnings, and statistics calculation.
 */
@ApplicationScoped
public class CallQualityService {

    private static final Logger LOGGER = Logger.getLogger(CallQualityService.class.getName());

    @Inject
    private CallQualityMetricsRepository qualityMetricsRepository;

    @Inject
    private CallRepository callRepository;

    @Inject
    private WebSocketNotificationService notificationService;

    /**
     * Report quality metrics for a call.
     * Validates the user is a participant, stores metrics, sends warnings if quality is poor,
     * and caches metrics in Redis.
     * 
     * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
     * 
     * @param callId The ID of the call
     * @param userId The ID of the user reporting quality
     * @param request The quality metrics to report
     * @throws ResourceNotFoundException if call not found
     * @throws UnauthorizedException if user is not a participant
     * @throws BadRequestException if quality metrics are invalid
     */
    @Transactional
    public void reportQuality(String callId, Long userId, ReportQualityRequest request) {
        // Verify call exists
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with ID: " + callId));

        // Verify user is participant
        if (!call.getCallerId().equals(userId) && !call.getCalleeId().equals(userId)) {
            throw new UnauthorizedException("User is not a participant in this call");
        }

        // Validate quality metrics
        validateQualityMetrics(request);

        // Store metrics in database
        CallQualityMetrics metrics = new CallQualityMetrics();
        metrics.setId(UUID.randomUUID().toString());
        metrics.setCallId(callId);
        metrics.setUserId(userId);
        metrics.setNetworkQuality(request.getNetworkQuality());
        metrics.setPacketLossRate(request.getPacketLossRate());
        metrics.setRoundTripTime(request.getRoundTripTime());
        metrics.setRecordedAt(request.getTimestamp() != null ? request.getTimestamp() : Instant.now());

        qualityMetricsRepository.save(metrics);

        LOGGER.log(Level.INFO, "Quality metrics reported for call {0} by user {1}: quality={2}, packetLoss={3}, rtt={4}",
                new Object[]{callId, userId, request.getNetworkQuality(), request.getPacketLossRate(), request.getRoundTripTime()});

        // Send warning if quality is poor
        if (isPoorQuality(request.getNetworkQuality())) {
            sendQualityWarning(call, userId, request.getNetworkQuality());
        }

        // Cache metrics in Redis (placeholder - Redis not yet implemented)
        // TODO: Implement Redis caching with 5-minute TTL
        // cacheQualityMetrics(callId, userId, metrics);
    }

    /**
     * Get call quality statistics for a specific call.
     * Calculates average quality and packet loss.
     * 
     * Requirements: 5.5
     * 
     * @param callId The ID of the call
     * @return Quality statistics including average quality and packet loss
     * @throws ResourceNotFoundException if call not found
     */
    public CallQualityMetricsRepository.QualityStatistics getCallQualityStats(String callId) {
        // Verify call exists
        callRepository.findById(callId)
                .orElseThrow(() -> new ResourceNotFoundException("Call not found with ID: " + callId));

        // Calculate and return statistics
        CallQualityMetricsRepository.QualityStatistics stats = qualityMetricsRepository.calculateAverageQuality(callId);

        LOGGER.log(Level.INFO, "Retrieved quality statistics for call {0}: quality={1}, avgPacketLoss={2}, avgRtt={3}",
                new Object[]{callId, stats.getNetworkQuality(), stats.getAvgPacketLossRate(), stats.getAvgRoundTripTime()});

        return stats;
    }

    /**
     * Validate quality metrics according to requirements.
     * 
     * @param request The quality metrics request to validate
     * @throws BadRequestException if validation fails
     */
    private void validateQualityMetrics(ReportQualityRequest request) {
        // Validate network quality enum (7.1)
        if (request.getNetworkQuality() == null) {
            throw new BadRequestException("Network quality is required", "INVALID_QUALITY");
        }

        // Validate packet loss rate is between 0 and 1 (7.2)
        if (request.getPacketLossRate() != null) {
            if (request.getPacketLossRate() < 0.0 || request.getPacketLossRate() > 1.0) {
                throw new BadRequestException("Packet loss rate must be between 0.0 and 1.0", "INVALID_PACKET_LOSS");
            }
        }

        // Validate round trip time is positive (7.3)
        if (request.getRoundTripTime() != null) {
            if (request.getRoundTripTime() < 0) {
                throw new BadRequestException("Round trip time must be non-negative", "INVALID_RTT");
            }
        }
    }

    /**
     * Check if network quality is considered poor.
     * 
     * @param quality The network quality to check
     * @return true if quality is POOR, BAD, or VERY_BAD
     */
    private boolean isPoorQuality(NetworkQuality quality) {
        return quality == NetworkQuality.POOR 
            || quality == NetworkQuality.BAD 
            || quality == NetworkQuality.VERY_BAD;
    }

    /**
     * Send quality warning to the other participant.
     * 
     * Requirements: 7.4
     * 
     * @param call The call with quality issues
     * @param reportingUserId The user who reported the quality issue
     * @param quality The network quality level
     */
    private void sendQualityWarning(Call call, Long reportingUserId, NetworkQuality quality) {
        // Determine the other participant
        Long otherUserId = call.getCallerId().equals(reportingUserId) 
            ? call.getCalleeId() 
            : call.getCallerId();

        // Send warning notification
        notificationService.sendQualityWarning(
            otherUserId.toString(), 
            call.getId(), 
            quality.name()
        );

        LOGGER.log(Level.INFO, "Sent quality warning to user {0} for call {1} with quality: {2}",
                new Object[]{otherUserId, call.getId(), quality});
    }

    /**
     * Cache quality metrics in Redis with 5-minute TTL.
     * Placeholder for future Redis implementation.
     * 
     * Requirements: 7.5
     * 
     * @param callId The call ID
     * @param userId The user ID
     * @param metrics The metrics to cache
     */
    private void cacheQualityMetrics(String callId, Long userId, CallQualityMetrics metrics) {
        // TODO: Implement Redis caching
        // String cacheKey = String.format("call:%s:quality:%d", callId, userId);
        // redisService.set(cacheKey, metrics, Duration.ofMinutes(5));
        
        LOGGER.log(Level.FINE, "Quality metrics caching not yet implemented for call {0}, user {1}",
                new Object[]{callId, userId});
    }
}
