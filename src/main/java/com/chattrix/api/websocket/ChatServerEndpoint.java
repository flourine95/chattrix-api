package com.chattrix.api.websocket;

import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.chattrix.api.websocket.handlers.*;
import com.chattrix.api.websocket.handlers.MessageHandler;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Dependent
@Slf4j
@ServerEndpoint(value = "/ws/chat",
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class)
public class ChatServerEndpoint {

    @Inject
    private ConnectionHandler connectionHandler;

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private TypingHandler typingHandler;

    @Inject
    private HeartbeatHandler heartbeatHandler;

    @Inject
    private ActivityHandler activityHandler;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        connectionHandler.handleOpen(session);
    }

    @OnMessage
    public void onMessage(Session session, WebSocketMessage<?> message) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) return;

        activityHandler.updateUserActivity(userId);

        String type = message.getType();
        Object payload = message.getPayload();

        try {
            switch (type) {
                case "chat.message" -> messageHandler.handleChatMessage(userId, payload);
                case "typing.start" -> typingHandler.handleTypingEvent(userId, payload, true);
                case "typing.stop" -> typingHandler.handleTypingEvent(userId, payload, false);
                case "heartbeat" -> heartbeatHandler.handleHeartbeat(session, userId);
                default -> log.warn("Unknown message type: {}", type);
            }
        } catch (com.chattrix.api.exceptions.BusinessException e) {
            // Business exceptions (validation, permissions, etc.) - send to client
            log.warn("Business error handling message [{}] for user {}: {}", type, userId, e.getMessage());
            sendErrorToClient(session, type, e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            // Unexpected errors - log and send generic error
            log.error("Error handling message [{}] for user {}: {}", type, userId, e.getMessage(), e);
            sendErrorToClient(session, type, "An error occurred while processing your message", "INTERNAL_ERROR");
        }
    }
    
    private void sendErrorToClient(Session session, String originalType, String errorMessage, String errorCode) {
        try {
            java.util.Map<String, Object> errorPayload = new java.util.HashMap<>();
            errorPayload.put("error", errorMessage);
            errorPayload.put("code", errorCode);
            errorPayload.put("originalType", originalType);
            
            WebSocketMessage<java.util.Map<String, Object>> errorMsg = 
                new WebSocketMessage<>("error", errorPayload);
            
            session.getBasicRemote().sendObject(errorMsg);
        } catch (Exception e) {
            log.error("Failed to send error message to client: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        connectionHandler.handleClose(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if (throwable instanceof IOException) {
            return;
        }

        Long userId = (Long) session.getUserProperties().get("userId");
        log.error("WebSocket Error for user {}: {}", userId, throwable.getMessage(), throwable);
    }
}