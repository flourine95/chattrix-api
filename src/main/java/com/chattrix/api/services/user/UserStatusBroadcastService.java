package com.chattrix.api.services.user;

import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.services.cache.UserProfileCache;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.UserStatusEventDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized service for broadcasting user status changes.
 * Eliminates duplicate broadcast logic across ChatServerEndpoint and HeartbeatMonitorService.
 */
@ApplicationScoped
@Slf4j
public class UserStatusBroadcastService {
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private ChatSessionService chatSessionService;
    
    @Inject
    private UserProfileCache userProfileCache;
    
    @Inject
    private UserMapper userMapper;
    
    /**
     * Broadcast user status change to all relevant users.
     * Uses cache to avoid DB queries.
     * 
     * @param userId User whose status changed
     * @param isOnline true if user went online, false if offline
     */
    public void broadcastUserStatusChange(Long userId, boolean isOnline) {
        broadcastUserStatusChange(userId, isOnline, java.time.Instant.now());
    }
    
    /**
     * Broadcast user status change with explicit lastSeen timestamp.
     * 
     * @param userId User whose status changed
     * @param isOnline true if user went online, false if offline
     * @param lastSeen The current lastSeen timestamp to broadcast
     */
    public void broadcastUserStatusChange(Long userId, boolean isOnline, java.time.Instant lastSeen) {
        try {
            // Build status payload with provided lastSeen timestamp
            UserStatusEventDto payload = UserStatusEventDto.builder()
                    .userId(userId)
                    .status(isOnline ? "online" : "offline")
                    .lastSeen(lastSeen)
                    .build();
            
            WebSocketMessage<UserStatusEventDto> statusMessage = 
                new WebSocketMessage<>(WebSocketEventType.USER_STATUS, payload);
            
            // Get recipients (users who should receive this status update)
            List<Long> recipientUserIds = 
                userRepository.findUserIdsWhoShouldReceiveStatusUpdates(userId);
            
            log.info("Broadcasting status change for user {} (online: {}) to {} recipients: {}",
                userId, isOnline, recipientUserIds.size(), recipientUserIds);
            
            // Broadcast to all recipients
            int successCount = 0;
            for (Long recipientId : recipientUserIds) {
                try {
                    chatSessionService.sendDirectMessage(recipientId, statusMessage);
                    successCount++;
                } catch (Exception e) {
                    log.debug("Failed to send status update to user {}: {}", 
                        recipientId, e.getMessage());
                }
            }
            
            log.info("Broadcasted status change for user {} (online: {}, lastSeen: {}) - sent to {}/{} recipients",
                userId, isOnline, lastSeen, successCount, recipientUserIds.size());
                
        } catch (Exception e) {
            log.error("Error broadcasting status change for user {}: {}", 
                userId, e.getMessage(), e);
        }
    }
}
