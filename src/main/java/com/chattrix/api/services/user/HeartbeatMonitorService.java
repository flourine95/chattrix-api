package com.chattrix.api.services.user;

import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@Slf4j
public class HeartbeatMonitorService {

    private final ConcurrentMap<Long, Instant> lastHeartbeat = new ConcurrentHashMap<>();
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 30;

    @Inject
    private UserStatusService userStatusService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ChatSessionService chatSessionService;

    public void recordHeartbeat(Long userId) {
        lastHeartbeat.put(userId, Instant.now());
        log.debug("Recorded heartbeat for user: {}", userId);
    }

    public void removeHeartbeat(Long userId) {
        lastHeartbeat.remove(userId);
        log.debug("Removed heartbeat tracking for user: {}", userId);
    }

    @Schedule(second = "*/15", minute = "*", hour = "*", persistent = false)
    public void checkStaleHeartbeats() {
        Instant threshold = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

        lastHeartbeat.entrySet().removeIf(entry -> {
            Long userId = entry.getKey();
            Instant lastBeat = entry.getValue();

            if (lastBeat.isBefore(threshold)) {
                log.info("User {} heartbeat timeout. Last heartbeat: {}", userId, lastBeat);

                try {
                    userStatusService.setUserOffline(userId);
                    broadcastUserStatusChange(userId, false);
                    log.info("Marked user {} as offline due to heartbeat timeout", userId);
                    return true;
                } catch (Exception e) {
                    log.error("Error marking user {} offline: {}", userId, e.getMessage());
                }
            }
            return false;
        });
    }

    public int getTrackedUsersCount() {
        return lastHeartbeat.size();
    }

    public boolean isUserTracked(Long userId) {
        return lastHeartbeat.containsKey(userId);
    }

    public Instant getLastHeartbeat(Long userId) {
        return lastHeartbeat.get(userId);
    }

    private void broadcastUserStatusChange(Long userId, boolean isOnline) {
        try {
            var user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId.toString());
            payload.put("username", user.getUsername());
            payload.put("fullName", user.getFullName());
            payload.put("online", isOnline);
            payload.put("lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : null);

            WebSocketMessage<Map<String, Object>> statusMessage = new WebSocketMessage<>("user.status", payload);
            List<Long> recipientUserIds = userRepository.findUserIdsWhoShouldReceiveStatusUpdates(userId);

            for (Long recipientId : recipientUserIds) {
                try {
                    chatSessionService.sendDirectMessage(recipientId, statusMessage);
                } catch (Exception e) {
                    log.warn("Failed to send status update to user {}: {}", recipientId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting status change: {}", e.getMessage());
        }
    }
}