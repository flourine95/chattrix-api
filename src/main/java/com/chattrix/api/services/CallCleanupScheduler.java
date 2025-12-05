package com.chattrix.api.services;

import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.CallStatus;
import com.chattrix.api.repositories.CallRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled service to cleanup orphaned calls.
 * Runs periodically to:
 * 1. End calls that have been in CONNECTING/CONNECTED state for too long
 * 2. Clean up stuck RINGING calls (should be handled by timeout, but as a safety net)
 */
@ApplicationScoped
@Transactional
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@NoArgsConstructor(force = true)
@Slf4j
public class CallCleanupScheduler {

    private static final int CLEANUP_INTERVAL_MINUTES = 5;
    private static final int MAX_CALL_DURATION_HOURS = 4; // Max 4 hours per call
    private static final int MAX_RINGING_MINUTES = 2; // Safety net for stuck RINGING

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    private final CallRepository callRepository;
    private final UserStatusService userStatusService;
    private final WebSocketNotificationService webSocketService;

    @PostConstruct
    public void init() {
        log.info("Starting CallCleanupScheduler - runs every {} minutes", CLEANUP_INTERVAL_MINUTES);

        scheduler.scheduleAtFixedRate(
            this::cleanupOrphanedCalls,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    /**
     * Main cleanup logic
     */
    @Transactional
    public void cleanupOrphanedCalls() {
        try {
            log.debug("Running orphaned call cleanup...");

            cleanupLongRunningCalls();
            cleanupStuckRingingCalls();

        } catch (Exception e) {
            log.error("Error during call cleanup", e);
        }
    }

    /**
     * End calls that have been running for too long
     */
    private void cleanupLongRunningCalls() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(MAX_CALL_DURATION_HOURS));

        List<Call> longCalls = callRepository.findLongRunningCalls(cutoff);

        for (Call call : longCalls) {
            log.warn("Force ending long-running call: {} (started at: {})",
                call.getId(), call.getStartTime());

            call.setStatus(CallStatus.ENDED);
            call.setEndTime(Instant.now());

            if (call.getStartTime() != null) {
                int duration = (int) Duration.between(call.getStartTime(), call.getEndTime()).getSeconds();
                call.setDurationSeconds(duration);
            }

            callRepository.save(call);

            // Try to notify both participants
            notifyBothParticipants(call, "Call automatically ended due to timeout");
        }

        if (!longCalls.isEmpty()) {
            log.info("Cleaned up {} long-running calls", longCalls.size());
        }
    }

    /**
     * End calls stuck in RINGING state (safety net, should not happen if timeout works)
     */
    private void cleanupStuckRingingCalls() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(MAX_RINGING_MINUTES));

        List<Call> stuckCalls = callRepository.findStuckRingingCalls(cutoff);

        for (Call call : stuckCalls) {
            log.warn("Force ending stuck RINGING call: {} (created at: {})",
                call.getId(), call.getCreatedAt());

            call.setStatus(CallStatus.MISSED);
            call.setEndTime(Instant.now());

            callRepository.save(call);

            // Notify both participants
            notifyBothParticipants(call, "Call missed");
        }

        if (!stuckCalls.isEmpty()) {
            log.info("Cleaned up {} stuck RINGING calls", stuckCalls.size());
        }
    }

    /**
     * Try to notify both participants about call end
     */
    private void notifyBothParticipants(Call call, String reason) {
        try {
            webSocketService.sendCallEnded(
                call.getCallerId().toString(),
                call.getId(),
                "system",
                call.getDurationSeconds() != null ? call.getDurationSeconds() : 0
            );
        } catch (Exception e) {
            log.debug("Could not notify caller {}", call.getCallerId());
        }

        try {
            webSocketService.sendCallEnded(
                call.getCalleeId().toString(),
                call.getId(),
                "system",
                call.getDurationSeconds() != null ? call.getDurationSeconds() : 0
            );
        } catch (Exception e) {
            log.debug("Could not notify callee {}", call.getCalleeId());
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down CallCleanupScheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("CallCleanupScheduler shutdown complete");
    }
}

