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
        } catch (Exception e) {
            log.error("Error handling message [{}]: {}", type, e.getMessage(), e);
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