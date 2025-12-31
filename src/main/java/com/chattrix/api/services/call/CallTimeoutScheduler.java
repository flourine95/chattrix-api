package com.chattrix.api.services.call;

import com.chattrix.api.entities.Call;
import com.chattrix.api.enums.CallStatus;
import com.chattrix.api.repositories.CallRepository;
import com.chattrix.api.services.notification.WebSocketNotificationService;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for scheduling and handling call timeouts.
 * Uses ScheduledExecutorService to automatically mark calls as MISSED
 * if they are not answered within the configured timeout period (60 seconds).
 */
@ApplicationScoped
public class CallTimeoutScheduler {

    private static final Logger LOGGER = Logger.getLogger(CallTimeoutScheduler.class.getName());
    private static final int TIMEOUT_SECONDS = 60;
    private static final int THREAD_POOL_SIZE = 10;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(THREAD_POOL_SIZE);

    // Track scheduled futures so we can cancel them if needed
    private final Map<String, ScheduledFuture<?>> scheduledTimeouts = new ConcurrentHashMap<>();

    @Inject
    private CallRepository callRepository;

    @Inject
    private WebSocketNotificationService notificationService;

    /**
     * Schedule a timeout for a call. If the call is not answered within 60 seconds,
     * it will be automatically marked as MISSED and both participants will be notified.
     *
     * @param callId   The ID of the call to schedule timeout for
     * @param callerId The ID of the caller
     * @param calleeId The ID of the callee
     */
    public void scheduleTimeout(String callId, String callerId, String calleeId) {
        LOGGER.log(Level.INFO, "Scheduling timeout for call {0} in {1} seconds",
                new Object[]{callId, TIMEOUT_SECONDS});

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            handleCallTimeout(callId, callerId, calleeId);
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);

        scheduledTimeouts.put(callId, future);
    }

    /**
     * Cancel a scheduled timeout for a call. This should be called when a call
     * is answered or rejected before the timeout occurs.
     *
     * @param callId The ID of the call to cancel timeout for
     */
    public void cancelTimeout(String callId) {
        ScheduledFuture<?> future = scheduledTimeouts.remove(callId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(false);
            LOGGER.log(Level.INFO, "Cancelled timeout for call {0}: {1}",
                    new Object[]{callId, cancelled});
        }
    }

    /**
     * Handle call timeout by updating the call status to MISSED and notifying
     * both participants via WebSocket.
     *
     * @param callId   The ID of the call that timed out
     * @param callerId The ID of the caller
     * @param calleeId The ID of the callee
     */
    private void handleCallTimeout(String callId, String callerId, String calleeId) {
        try {
            LOGGER.log(Level.INFO, "Handling timeout for call {0}", callId);

            // Fetch the call to check its current status
            Call call = callRepository.findById(callId).orElse(null);

            if (call == null) {
                LOGGER.log(Level.WARNING, "Call {0} not found when handling timeout", callId);
                scheduledTimeouts.remove(callId);
                return;
            }

            // Only update to MISSED if the call is still in INITIATING or RINGING status
            if (call.getStatus() == CallStatus.INITIATING || call.getStatus() == CallStatus.RINGING) {
                // Update call status to MISSED
                callRepository.updateStatus(callId, CallStatus.MISSED);

                LOGGER.log(Level.INFO, "Updated call {0} status to MISSED", callId);

                // Send timeout notifications to both participants
                notificationService.sendCallTimeout(
                        String.valueOf(callerId),
                        String.valueOf(calleeId),
                        callId
                );

                LOGGER.log(Level.INFO, "Sent timeout notifications for call {0}", callId);
            } else {
                LOGGER.log(Level.INFO, "Call {0} status is {1}, skipping timeout handling",
                        new Object[]{callId, call.getStatus()});
            }

            // Remove from tracking map
            scheduledTimeouts.remove(callId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling timeout for call " + callId, e);
            scheduledTimeouts.remove(callId);
        }
    }

    /**
     * Shutdown the scheduler gracefully when the application is stopped.
     * This ensures all scheduled tasks are completed or cancelled properly.
     */
    @PreDestroy
    public void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down CallTimeoutScheduler");

        // Cancel all pending timeouts
        scheduledTimeouts.values().forEach(future -> future.cancel(false));
        scheduledTimeouts.clear();

        // Shutdown the scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.log(Level.INFO, "CallTimeoutScheduler shutdown complete");
    }
}
