package com.chattrix.api.services;

import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class UserStatusService {

    @Inject
    private UserRepository userRepository;

    // Track active sessions for each user
    private final ConcurrentMap<UUID, Integer> activeSessionsCount = new ConcurrentHashMap<>();

    @Transactional
    public void setUserOnline(UUID userId) {
        // Increment session count
        activeSessionsCount.merge(userId, 1, Integer::sum);

        // Update user status in database
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setOnline(true);
            user.setLastSeen(Instant.now());
            userRepository.save(user);
        }
    }

    @Transactional
    public void setUserOffline(UUID userId) {
        // Decrement session count
        activeSessionsCount.compute(userId, (key, count) -> {
            if (count == null || count <= 1) {
                return null; // Remove from map
            }
            return count - 1;
        });

        // If no active sessions, mark user as offline
        if (!activeSessionsCount.containsKey(userId)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setOnline(false);
                user.setLastSeen(Instant.now());
                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void updateLastSeen(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setLastSeen(Instant.now());
            userRepository.save(user);
        }
    }

    public List<User> getOnlineUsers() {
        return userRepository.findByIsOnlineTrue();
    }

    public List<User> getOnlineUsersInConversation(UUID conversationId) {
        return userRepository.findOnlineUsersByConversationId(conversationId);
    }

    public boolean isUserOnline(UUID userId) {
        return activeSessionsCount.containsKey(userId);
    }

    public int getActiveSessionCount(UUID userId) {
        return activeSessionsCount.getOrDefault(userId, 0);
    }

    // Method to clean up stale online statuses (can be called periodically)
    @Transactional
    public void cleanupStaleOnlineStatuses() {
        // Mark users as offline if they haven't been seen in the last 5 minutes
        Instant threshold = Instant.now().minusSeconds(300); // 5 minutes
        List<User> staleUsers = userRepository.findStaleOnlineUsers(threshold);

        for (User user : staleUsers) {
            user.setOnline(false);
            userRepository.save(user);
            activeSessionsCount.remove(user.getId());
        }
    }
}
