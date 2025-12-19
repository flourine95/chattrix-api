package com.chattrix.api.services.user;

import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class UserStatusService {

    // Track active sessions for each user
    private final ConcurrentMap<Long, Integer> activeSessionsCount = new ConcurrentHashMap<>();
    @Inject
    private UserRepository userRepository;

    @Transactional
    public void setUserOnline(Long userId) {
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
    public void setUserOffline(Long userId) {
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
    public void updateLastSeen(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setLastSeen(Instant.now());
            userRepository.save(user);
        }
    }

    public List<User> getOnlineUsers() {
        return userRepository.findByIsOnlineTrue();
    }

    public List<User> getOnlineUsersInConversation(Long conversationId) {
        return userRepository.findOnlineUsersByConversationId(conversationId);
    }

    public boolean isUserOnline(Long userId) {
        return activeSessionsCount.containsKey(userId);
    }

    public int getActiveSessionCount(Long userId) {
        return activeSessionsCount.getOrDefault(userId, 0);
    }

    // Method to clean up stale online statuses (can be called periodically)
    @Transactional
    public void cleanupStaleOnlineStatuses() {
        // Mark users as offline if they haven't been seen in the last 2 minutes
        Instant threshold = Instant.now().minusSeconds(120); // 2 minutes
        List<User> staleUsers = userRepository.findStaleOnlineUsers(threshold);

        for (User user : staleUsers) {
            user.setOnline(false);
            userRepository.save(user);
            activeSessionsCount.remove(user.getId());
        }
    }
}
