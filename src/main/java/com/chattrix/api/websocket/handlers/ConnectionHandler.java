package com.chattrix.api.websocket.handlers;

import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.auth.TokenService;
import com.chattrix.api.services.notification.ChatSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class ConnectionHandler {

    @Inject
    private TokenService tokenService;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private ActivityHandler activityHandler;

    public void handleOpen(Session session) throws IOException {
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
        activityHandler.markUserOnline(user.getId());

        log.info("User Connected: {} (ID: {})", user.getUsername(), userId);
    }

    public void handleClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId, session);

            boolean hasOtherSessions = !chatSessionService.getUserSessions(userId).isEmpty();

            if (!hasOtherSessions) {
                activityHandler.markUserOffline(userId);
                log.info("User Disconnected (Offline): ID {}", userId);
            }
        }
    }

    private String getTokenFromQuery(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            return params.get("token").getFirst();
        }
        return null;
    }
}
