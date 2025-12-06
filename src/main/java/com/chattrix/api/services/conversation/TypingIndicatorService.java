package com.chattrix.api.services.conversation;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@ApplicationScoped
public class TypingIndicatorService {

    // Timeout sau 3 giây không có activity thì tự động stop typing
    private static final long TYPING_TIMEOUT_SECONDS = 3;
    // Map từ conversationId -> Set<userId> đang typing
    private final Map<Long, Set<Long>> conversationTypingUsers = new ConcurrentHashMap<>();
    // Map từ key(conversationId_userId) -> ScheduledFuture để auto-stop typing sau timeout
    private final Map<String, ScheduledFuture<?>> typingTimeouts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    /**
     * Đánh dấu user đang typing trong conversation
     */
    public void startTyping(Long conversationId, Long userId) {
        conversationTypingUsers.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(userId);

        // Reset timeout timer cho user này
        String key = conversationId + "_" + userId;
        ScheduledFuture<?> oldTimeout = typingTimeouts.get(key);
        if (oldTimeout != null) {
            oldTimeout.cancel(false);
        }

        // Tạo timeout mới
        ScheduledFuture<?> newTimeout = scheduler.schedule(() -> {
            stopTyping(conversationId, userId);
        }, TYPING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        typingTimeouts.put(key, newTimeout);
    }

    /**
     * Đánh dấu user đã ngừng typing trong conversation
     */
    public void stopTyping(Long conversationId, Long userId) {
        Set<Long> typingUsers = conversationTypingUsers.get(conversationId);
        if (typingUsers != null) {
            typingUsers.remove(userId);
            if (typingUsers.isEmpty()) {
                conversationTypingUsers.remove(conversationId);
            }
        }

        // Hủy timeout timer
        String key = conversationId + "_" + userId;
        ScheduledFuture<?> timeout = typingTimeouts.remove(key);
        if (timeout != null) {
            timeout.cancel(false);
        }
    }

    /**
     * Lấy danh sách users đang typing trong conversation (trừ user hiện tại)
     */
    public Set<Long> getTypingUsersInConversation(Long conversationId, Long excludeUserId) {
        Set<Long> typingUsers = conversationTypingUsers.get(conversationId);
        if (typingUsers == null) {
            return Set.of();
        }

        return typingUsers.stream()
                .filter(userId -> !userId.equals(excludeUserId))
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Kiểm tra user có đang typing trong conversation không
     */
    public boolean isUserTyping(Long conversationId, Long userId) {
        Set<Long> typingUsers = conversationTypingUsers.get(conversationId);
        return typingUsers != null && typingUsers.contains(userId);
    }

    /**
     * Cleanup khi user disconnect
     */
    public void removeUserFromAllConversations(Long userId) {
        conversationTypingUsers.forEach((conversationId, typingUsers) -> {
            typingUsers.remove(userId);
        });

        // Remove empty conversations
        conversationTypingUsers.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Cancel all timeouts for this user
        typingTimeouts.entrySet().removeIf(entry -> {
            if (entry.getKey().endsWith("_" + userId)) {
                entry.getValue().cancel(false);
                return true;
            }
            return false;
        });
    }
}
