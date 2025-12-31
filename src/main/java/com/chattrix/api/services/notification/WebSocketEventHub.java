package com.chattrix.api.services.notification;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class WebSocketEventHub {
    
    @Inject
    private ChatSessionService chatSessionService;
    
    private long totalEventsSent = 0;
    private final Map<String, Long> eventCountByType = new HashMap<>();
    private final Map<String, Instant> lastEventTimeByType = new HashMap<>();
    
    // ========== CORE SEND METHODS (with monitoring) ==========
    
    /**
     * Send WebSocket message to a single user (with monitoring)
     */
    public <T> void sendToUser(Long userId, String eventType, T payload) {
        try {
            WebSocketMessage<T> message = new WebSocketMessage<>(eventType, payload);
            chatSessionService.sendMessageToUser(userId, message);
            
            // Track metrics
            recordEvent(eventType);
            log.debug("Sent {} to user {}", eventType, userId);
            
        } catch (Exception e) {
            log.error("Failed to send {} to user {}: {}", eventType, userId, e.getMessage());
        }
    }
    
    /**
     * Send WebSocket message to multiple users (with monitoring)
     */
    public <T> void sendToUsers(List<Long> userIds, String eventType, T payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        WebSocketMessage<T> message = new WebSocketMessage<>(eventType, payload);
        int successCount = 0;
        
        for (Long userId : userIds) {
            try {
                chatSessionService.sendMessageToUser(userId, message);
                successCount++;
            } catch (Exception e) {
                log.debug("Failed to send {} to user {}: {}", eventType, userId, e.getMessage());
            }
        }
        
        // Track metrics
        recordEvent(eventType, successCount);
        log.debug("Sent {} to {}/{} users", eventType, successCount, userIds.size());
    }
    
    /**
     * Send raw WebSocketMessage (for backward compatibility)
     */
    public <T> void send(Long userId, WebSocketMessage<T> message) {
        try {
            chatSessionService.sendMessageToUser(userId, message);
            recordEvent(message.getType());
            log.debug("Sent {} to user {}", message.getType(), userId);
        } catch (Exception e) {
            log.error("Failed to send {} to user {}: {}", message.getType(), userId, e.getMessage());
        }
    }
    
    // ========== METRICS & MONITORING ==========
    
    /**
     * Record event for metrics
     */
    private void recordEvent(String eventType) {
        recordEvent(eventType, 1);
    }
    
    private void recordEvent(String eventType, int count) {
        totalEventsSent += count;
        eventCountByType.put(eventType, eventCountByType.getOrDefault(eventType, 0L) + count);
        lastEventTimeByType.put(eventType, Instant.now());
    }
    
    /**
     * Get metrics for monitoring
     */
    @Getter
    public static class WebSocketMetrics {
        private final long totalEventsSent;
        private final Map<String, Long> eventCountByType;
        private final Map<String, Instant> lastEventTimeByType;
        
        public WebSocketMetrics(long totalEventsSent, 
                               Map<String, Long> eventCountByType,
                               Map<String, Instant> lastEventTimeByType) {
            this.totalEventsSent = totalEventsSent;
            this.eventCountByType = eventCountByType;
            this.lastEventTimeByType = lastEventTimeByType;
        }
    }
    
    /**
     * Get current metrics
     */
    public WebSocketMetrics getMetrics() {
        return new WebSocketMetrics(
            totalEventsSent,
            new HashMap<>(eventCountByType),
            new HashMap<>(lastEventTimeByType)
        );
    }
    
    /**
     * Reset metrics
     */
    public void resetMetrics() {
        totalEventsSent = 0;
        eventCountByType.clear();
        lastEventTimeByType.clear();
        log.info("WebSocket metrics reset");
    }
    
    /**
     * Get metrics as formatted string
     */
    public String getMetricsReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== WebSocket Metrics ===\n");
        report.append(String.format("Total events sent: %d\n", totalEventsSent));
        report.append("\nEvents by type:\n");
        
        eventCountByType.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .forEach(entry -> {
                Instant lastTime = lastEventTimeByType.get(entry.getKey());
                report.append(String.format("  %-30s: %6d (last: %s)\n", 
                    entry.getKey(), 
                    entry.getValue(),
                    lastTime != null ? lastTime.toString() : "never"));
            });
        
        return report.toString();
    }
}
