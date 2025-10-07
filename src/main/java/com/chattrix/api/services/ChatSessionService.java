package com.chattrix.api.services;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatSessionService {

    // Lưu trữ các session đang hoạt động, map từ UserId sang Session
    private final Map<UUID, Session> activeSessions = new ConcurrentHashMap<>();

    public void addSession(UUID userId, Session session) {
        activeSessions.put(userId, session);
    }

    public void removeSession(UUID userId) {
        activeSessions.remove(userId);
    }

    public void sendMessageToUser(UUID userId, String message) {
        Session session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                // Xử lý lỗi, ví dụ: ghi log
                e.printStackTrace();
            }
        }
    }

    public <T> void sendMessageToUser(UUID userId, WebSocketMessage<T> message) {
        Session session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendObject(message);
            } catch (IOException | EncodeException e) {
                // Xử lý lỗi, ví dụ: ghi log
                e.printStackTrace();
            }
        }
    }

    public boolean isUserOnline(UUID userId) {
        return activeSessions.containsKey(userId);
    }
}
