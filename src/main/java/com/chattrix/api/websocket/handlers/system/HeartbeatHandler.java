package com.chattrix.api.websocket.handlers.system;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.chattrix.api.websocket.handlers.MessageHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for heartbeat messages.
 * Responds with an acknowledgment to keep the WebSocket connection alive.
 */
@ApplicationScoped
public class HeartbeatHandler implements MessageHandler {

    private static final Logger LOGGER = Logger.getLogger(HeartbeatHandler.class.getName());

    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            // Send acknowledgment
            WebSocketMessage<Map<String, Object>> ackMessage =
                    new WebSocketMessage<>("heartbeat.ack", Map.of(
                            "userId", userId.toString(),
                            "timestamp", Instant.now().toString()
                    ));

            session.getBasicRemote().sendObject(ackMessage);

            LOGGER.log(Level.FINE, "Heartbeat acknowledgment sent to user: {0}", userId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error sending heartbeat acknowledgment to user: " + userId, e);
        }
    }

    @Override
    public String getMessageType() {
        return "heartbeat";
    }
}
