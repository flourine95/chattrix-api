package com.chattrix.api.websocket;

import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.auth.TokenService;
import com.chattrix.api.services.call.CallService;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.services.user.HeartbeatMonitorService;
import com.chattrix.api.services.user.UserStatusService;
import com.chattrix.api.websocket.codec.GenericMessageEncoder;
import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.chattrix.api.websocket.handlers.MessageHandlerRegistry;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dependent
@Slf4j
@ServerEndpoint(value = "/ws/chat",
        configurator = CdiAwareConfigurator.class,
        encoders = {MessageEncoder.class, GenericMessageEncoder.class},
        decoders = MessageDecoder.class)
public class ChatServerEndpoint {

    @Inject
    private TokenService tokenService;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private UserStatusService userStatusService;
    @Inject
    private MessageHandlerRegistry handlerRegistry;
    @Inject
    private CallService callService;
    @Inject
    private HeartbeatMonitorService heartbeatMonitorService;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = getTokenFromQuery(session);

        if (token == null) {
            log.warn("WebSocket Rejected: No token provided.");
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "No token"));
            return;
        }

        if (!tokenService.validateToken(token)) {
            log.warn("WebSocket Rejected: Invalid token.");
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            return;
        }

        Long userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("WebSocket Rejected: User ID {} not found.", userId);
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not found"));
            return;
        }

        session.getUserProperties().put("userId", user.getId());
        chatSessionService.addSession(user.getId(), session);
        userStatusService.setUserOnline(user.getId());
        heartbeatMonitorService.recordHeartbeat(user.getId());
        broadcastUserStatusChange(user.getId(), true);

        log.info("User Connected: {} (ID: {})", user.getUsername(), userId);
    }

    @OnMessage
    public void onMessage(Session session, WebSocketMessage<?> message) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) return;

        userStatusService.updateLastSeen(userId);
        String messageType = message.getType();

        handlerRegistry.getHandler(messageType).ifPresentOrElse(
                handler -> {
                    try {
                        handler.handle(session, userId, message.getPayload());
                    } catch (Exception e) {
                        log.error("Error handling message type [{}]: {}", messageType, e.getMessage(), e);
                    }
                },
                () -> log.warn("No handler found for message type: {}", messageType)
        );
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId, session);

            boolean hasOtherSessions = !chatSessionService.getUserSessions(userId).isEmpty();

            if (!hasOtherSessions) {
                try {
                    callService.handleUserDisconnected(userId);
                } catch (Exception e) {
                    log.error("Error cleaning up calls for user {}: {}", userId, e.getMessage());
                }

                userStatusService.setUserOffline(userId);
                heartbeatMonitorService.removeHeartbeat(userId);
                broadcastUserStatusChange(userId, false);

                log.info("User Disconnected (Offline): ID {}", userId);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Bỏ qua lỗi ngắt kết nối thông thường (EOF, Closed, Broken pipe) đỡ rác log
        if (throwable instanceof IOException) {
            return;
        }

        Long userId = (Long) session.getUserProperties().get("userId");
        log.error("WebSocket Error for user {}: {}", userId, throwable.getMessage(), throwable);
    }

    private void broadcastUserStatusChange(Long userId, boolean isOnline) {
        try {
            User user = userRepository.findById(userId).orElse(null);
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
                    log.warn("Failed to broadcast status to recipient {}: {}", recipientId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting status for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private String getTokenFromQuery(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            return params.get("token").get(0);
        }
        return null;
    }
}