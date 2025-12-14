package com.chattrix.api.websocket;

import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.services.auth.TokenService;
import com.chattrix.api.services.user.UserStatusService;
import com.chattrix.api.services.call.CallService;
import com.chattrix.api.websocket.codec.GenericMessageEncoder;
import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.chattrix.api.websocket.handlers.MessageHandlerRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@ServerEndpoint(value = "/ws/chat",
        configurator = CdiAwareConfigurator.class,
        encoders = {MessageEncoder.class, GenericMessageEncoder.class},
        decoders = MessageDecoder.class)
public class ChatServerEndpoint {

    private static final Logger LOGGER = Logger.getLogger(ChatServerEndpoint.class.getName());

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

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = getTokenFromQuery(session);
        if (token == null || !tokenService.validateToken(token)) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            return;
        }

        Long userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not found"));
            return;
        }

        session.getUserProperties().put("userId", user.getId());
        chatSessionService.addSession(user.getId(), session);

        // Mark user as online
        userStatusService.setUserOnline(user.getId());

        // Broadcast user status change to other users
        broadcastUserStatusChange(user.getId(), true);
        System.out.println("User connected: " + userId);
    }

    @OnMessage
    @Transactional
    public void onMessage(Session session, WebSocketMessage<?> message) throws IOException {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) {
            LOGGER.warning("Received message from unauthenticated session");
            return;
        }

        // Update last seen when user sends any message
        userStatusService.updateLastSeen(userId);

        String messageType = message.getType();
        LOGGER.log(Level.FINE, "Received message type: {0} from user: {1}",
                new Object[]{messageType, userId});

        // Look up handler from registry
        handlerRegistry.getHandler(messageType).ifPresentOrElse(
                handler -> {
                    try {
                        LOGGER.log(Level.FINE, "Delegating to handler: {0}",
                                handler.getClass().getSimpleName());
                        handler.handle(session, userId, message.getPayload());
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in handler for message type: " + messageType, e);
                    }
                },
                () -> LOGGER.log(Level.WARNING, "No handler found for message type: {0}", messageType)
        );
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId, session);

            // Check if user still has other active sessions
            boolean hasOtherSessions = !chatSessionService.getUserSessions(userId).isEmpty();

            if (!hasOtherSessions) {
                // User completely disconnected - cleanup calls
                try {
                    callService.handleUserDisconnected(userId);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error handling user disconnection for calls", e);
                }

                // Mark user as offline
                userStatusService.setUserOffline(userId);

                // Broadcast user status change to other users
                broadcastUserStatusChange(userId, false);
            }

            System.out.println("User disconnected: " + userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        Long userId = (Long) session.getUserProperties().get("userId");
        System.err.println("WebSocket error for user " + userId + ": " + throwable.getMessage());
        throwable.printStackTrace();
    }


    private void broadcastUserStatusChange(Long userId, boolean isOnline) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Create status change message
        WebSocketMessage<Map<String, Object>> statusMessage = new WebSocketMessage<>("user.status", Map.of(
                "userId", userId.toString(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "online", isOnline,
                "lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : null
        ));

        // Broadcast to all connected users
        chatSessionService.broadcastToAllUsers(statusMessage);
    }

    private String getTokenFromQuery(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            return params.get("token").get(0);
        }
        return null;
    }
}
