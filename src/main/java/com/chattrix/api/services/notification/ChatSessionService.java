package com.chattrix.api.services.notification;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Slf4j
public class ChatSessionService {

    @Inject
    private ConversationRepository conversationRepository;

    // Lưu trữ các session đang hoạt động, map từ UserId sang List<Session>
    // Hỗ trợ đa thiết bị: 1 user có thể có nhiều sessions
    private final Map<Long, Set<Session>> activeSessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, Session session) {
        activeSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                      .add(session);
        log.info("User {} connected. Total devices: {}", userId, activeSessions.get(userId).size());
    }

    public void removeSession(Long userId) {
        activeSessions.remove(userId);
        log.info("User {} disconnected all devices", userId);
    }

    public void removeSession(Long userId, Session session) {
        Set<Session> sessions = activeSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                activeSessions.remove(userId);
            }
            log.info("User {} disconnected one device. Remaining: {}", 
                userId, sessions.size());
        }
    }

    public void sendMessageToUser(Long userId, String message) {
        Set<Session> sessions = activeSessions.get(userId);
        if (sessions != null) {
            sessions.removeIf(session -> {
                if (session == null || !session.isOpen()) {
                    return true; // Remove invalid session
                }
                
                try {
                    session.getBasicRemote().sendText(message);
                    return false; // Keep valid session
                } catch (IOException e) {
                    log.error("Failed to send message to user {}: {}", userId, e.getMessage());
                    return true; // Remove failed session
                }
            });
            
            // Remove user entry if no sessions left
            if (sessions.isEmpty()) {
                activeSessions.remove(userId);
            }
        }
    }

    public <T> void sendMessageToUser(Long userId, WebSocketMessage<T> message) {
        Set<Session> sessions = activeSessions.get(userId);
        if (sessions != null) {
            sessions.removeIf(session -> {
                if (session == null || !session.isOpen()) {
                    return true; // Remove invalid session
                }
                
                try {
                    session.getBasicRemote().sendObject(message);
                    return false; // Keep valid session
                } catch (IOException | EncodeException e) {
                    log.error("Failed to send message to user {}: {}", userId, e.getMessage());
                    return true; // Remove failed session
                }
            });
            
            // Remove user entry if no sessions left
            if (sessions.isEmpty()) {
                activeSessions.remove(userId);
            }
        }
    }

    /**
     * Send message directly without WebSocketMessage wrapper.
     * This avoids the nested structure issue.
     *
     * @param userId  The user ID to send to
     * @param message Any message object (will be serialized directly)
     */
    public void sendDirectMessage(Long userId, Object message) {
        Set<Session> sessions = activeSessions.get(userId);
        if (sessions != null) {
            sessions.removeIf(session -> {
                if (session == null || !session.isOpen()) {
                    return true; // Remove invalid session
                }
                
                try {
                    session.getBasicRemote().sendObject(message);
                    return false; // Keep valid session
                } catch (IOException | EncodeException e) {
                    log.error("Failed to send direct message to user {}: {}", userId, e.getMessage());
                    return true; // Remove failed session
                }
            });
            
            // Remove user entry if no sessions left
            if (sessions.isEmpty()) {
                activeSessions.remove(userId);
            }
        }
    }

    public <T> void broadcastToAllUsers(WebSocketMessage<T> message) {
        activeSessions.values().forEach(sessions -> {
            sessions.removeIf(session -> {
                if (session == null || !session.isOpen()) {
                    return true; // Remove invalid session
                }

                try {
                    session.getBasicRemote().sendObject(message);
                    return false; // Keep valid session
                } catch (IOException | EncodeException e) {
                    log.error("Failed to broadcast: {}", e.getMessage());
                    return true; // Remove failed session
                }
            });
        });
        
        // Remove empty user entries
        activeSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Broadcast message to all participants in a conversation
     * Used for Write-Behind pattern: notify clients of temp ID → real ID updates
     * Supports multi-device: sends to all devices of each participant
     * 
     * @param conversationId The conversation ID
     * @param message The WebSocket message to broadcast
     */
    public <T> void broadcastToConversation(Long conversationId, WebSocketMessage<T> message) {
        // Get conversation with participants
        Optional<Conversation> conversationOpt = conversationRepository.findByIdWithParticipants(conversationId);
        
        if (conversationOpt.isEmpty()) {
            log.warn("Cannot broadcast to conversation {}: not found", conversationId);
            return;
        }
        
        Conversation conversation = conversationOpt.get();
        
        // Broadcast to all participants (all devices)
        int successCount = 0;
        int failCount = 0;
        int totalDevices = 0;
        
        for (var participant : conversation.getParticipants()) {
            Long userId = participant.getUser().getId();
            Set<Session> sessions = activeSessions.get(userId);
            
            if (sessions != null) {
                totalDevices += sessions.size();
                
                for (Session session : sessions) {
                    if (session != null && session.isOpen()) {
                        try {
                            session.getBasicRemote().sendObject(message);
                            successCount++;
                        } catch (IOException | EncodeException e) {
                            log.error("Failed to broadcast to user {} device in conversation {}: {}", 
                                userId, conversationId, e.getMessage());
                            failCount++;
                        }
                    }
                }
            }
        }
        
        log.debug("Broadcasted to conversation {}: {} devices success, {} failed, {} offline", 
            conversationId, successCount, failCount, 
            conversation.getParticipants().size() - (successCount + failCount) / Math.max(1, totalDevices));
    }

    public boolean isUserOnline(Long userId) {
        Set<Session> sessions = activeSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        
        // User is online if at least one session is open
        return sessions.stream().anyMatch(session -> session != null && session.isOpen());
    }

    /**
     * Get all active sessions for a user (supports multiple devices)
     * Returns empty set if user has no active sessions
     */
    public Set<Session> getUserSessions(Long userId) {
        Set<Session> sessions = activeSessions.get(userId);
        if (sessions == null) {
            return Set.of();
        }
        
        // Filter out closed sessions
        Set<Session> activeSessions = sessions.stream()
            .filter(session -> session != null && session.isOpen())
            .collect(java.util.stream.Collectors.toSet());
        
        return activeSessions;
    }

    public Set<Long> getOnlineUserIds() {
        // Clean up invalid sessions and return active user IDs
        activeSessions.entrySet().removeIf(entry -> {
            Set<Session> sessions = entry.getValue();
            
            // Remove closed sessions
            sessions.removeIf(session -> session == null || !session.isOpen());
            
            // Remove user entry if no sessions left
            return sessions.isEmpty();
        });

        return activeSessions.keySet();
    }

    public int getActiveSessionCount() {
        return activeSessions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    /**
     * Get number of devices for a specific user
     */
    public int getUserDeviceCount(Long userId) {
        Set<Session> sessions = activeSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }
}
