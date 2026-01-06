package com.chattrix.api.websocket.handlers;

import com.chattrix.api.services.cache.OnlineStatusCache;
import com.chattrix.api.services.user.UserStatusBatchService;
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

    public void updateUserActivity(Long userId) {
        onlineStatusCache.markOnline(userId);
        batchService.queueLastSeenUpdate(userId);
    }
}
