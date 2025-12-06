package com.chattrix.api.services.notification;

import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatSessionService {

    // Lưu trữ các session đang hoạt động, map từ UserId sang Session
    private final Map<Long, Session> activeSessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, Session session) {
        activeSessions.put(userId, session);
    }

    public void removeSession(Long userId) {
        activeSessions.remove(userId);
    }

    public void removeSession(Long userId, Session session) {
        // Remove only if the session matches
        activeSessions.remove(userId, session);
    }

    public void sendMessageToUser(Long userId, String message) {
        Session session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                // Xử lý lỗi, ví dụ: ghi log
                e.printStackTrace();
                // Remove invalid session
                activeSessions.remove(userId);
            }
        }
    }

    public <T> void sendMessageToUser(Long userId, WebSocketMessage<T> message) {
        Session session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendObject(message);
            } catch (IOException | EncodeException e) {
                // Xử lý lỗi, ví dụ: ghi log
                e.printStackTrace();
                // Remove invalid session
                activeSessions.remove(userId);
            }
        }
    }
    
    /**
     * Send message directly without WebSocketMessage wrapper.
     * This avoids the nested structure issue.
     * 
     * @param userId The user ID to send to
     * @param message Any message object (will be serialized directly)
     */
    public void sendDirectMessage(Long userId, Object message) {
        Session session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendObject(message);
            } catch (IOException | EncodeException e) {
                // Xử lý lỗi, ví dụ: ghi log
                e.printStackTrace();
                // Remove invalid session
                activeSessions.remove(userId);
            }
        }
    }

    public <T> void broadcastToAllUsers(WebSocketMessage<T> message) {
        activeSessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session == null || !session.isOpen()) {
                return true; // Remove invalid sessions
            }

            try {
                session.getBasicRemote().sendObject(message);
                return false; // Keep valid sessions
            } catch (IOException | EncodeException e) {
                e.printStackTrace();
                return true; // Remove sessions that failed to send
            }
        });
    }

    public boolean isUserOnline(Long userId) {
        Session session = activeSessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * Get all active sessions for a user (currently only supports 1 session per user)
     * Returns empty set if user has no active sessions
     */
    public Set<Session> getUserSessions(Long userId) {
        Session session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            return Set.of(session);
        }
        return Set.of();
    }

    public Set<Long> getOnlineUserIds() {
        // Clean up invalid sessions and return active user IDs
        activeSessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            return session == null || !session.isOpen();
        });

        return activeSessions.keySet();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
