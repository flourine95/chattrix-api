package com.chattrix.api.websocket.handlers;

import com.chattrix.api.services.cache.OnlineStatusCache;
import com.chattrix.api.services.call.CallService;
import com.chattrix.api.services.user.HeartbeatMonitorService;
import com.chattrix.api.services.user.UserStatusBatchService;
import com.chattrix.api.services.user.UserStatusBroadcastService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ActivityHandler {

    @Inject
    private OnlineStatusCache onlineStatusCache;
    @Inject
    private UserStatusBatchService batchService;
    @Inject
    private UserStatusBroadcastService broadcastService;
    @Inject
    private HeartbeatMonitorService heartbeatMonitorService;
    @Inject
    private CallService callService;

    /**
     * Update user activity (called on every message)
     */
    public void updateUserActivity(Long userId) {
        onlineStatusCache.markOnline(userId);
        batchService.queueLastSeenUpdate(userId);
    }

    /**
     * Mark user as online when connecting
     */
    public void markUserOnline(Long userId) {
        onlineStatusCache.markOnline(userId);
        batchService.queueLastSeenUpdate(userId);
        heartbeatMonitorService.recordHeartbeat(userId);
        broadcastService.broadcastUserStatusChange(userId, true);
    }

    /**
     * Mark user as offline when disconnecting (only if no other sessions)
     */
    public void markUserOffline(Long userId) {
        callService.handleUserDisconnected(userId);
        onlineStatusCache.markOffline(userId);
        batchService.queueLastSeenUpdate(userId);
        heartbeatMonitorService.removeHeartbeat(userId);
        broadcastService.broadcastUserStatusChange(userId, false);
    }
}
