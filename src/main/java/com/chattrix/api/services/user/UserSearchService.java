package com.chattrix.api.services.user;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.UserSearchMapper;
import com.chattrix.api.repositories.ContactRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.PaginatedResponse;
import com.chattrix.api.responses.UserSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserSearchService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserSearchMapper userSearchMapper;

    @Transactional
    public PaginatedResponse<UserSearchResponse> searchUsers(Long currentUserId, String query, int page, int size) {
        // Tìm kiếm users với pagination
        List<User> users = userRepository.searchUsersWithPagination(query, currentUserId, page, size);

        // Lấy total count
        long total = userRepository.countSearchUsers(query, currentUserId);

        // Chuyển đổi sang response và thêm thông tin bổ sung
        List<UserSearchResponse> responses = userSearchMapper.toSearchResponseList(users);

        // Thêm thông tin về contact và conversation cho mỗi user
        for (UserSearchResponse response : responses) {
            // Kiểm tra xem user có phải là contact không
            boolean isContact = contactRepository.existsByUserIdAndContactUserId(
                    currentUserId,
                    response.getId()
            );
            response.setContact(isContact);

            // Kiểm tra xem đã có conversation trực tiếp với user này chưa
            Optional<Conversation> existingConversation = conversationRepository
                    .findDirectConversationBetweenUsers(currentUserId, response.getId());

            if (existingConversation.isPresent()) {
                response.setHasConversation(true);
                response.setConversationId(existingConversation.get().getId());
            } else {
                response.setHasConversation(false);
                response.setConversationId(null);
            }
        }

        return new PaginatedResponse<>(responses, page, size, total);
    }

    /**
     * Search users with cursor-based pagination.
     */
    @Transactional
    public CursorPaginatedResponse<UserSearchResponse> searchUsersWithCursor(Long currentUserId, String query, Long cursor, int limit) {
        // Validate limit
        if (limit < 1) {
            limit = 1;
        }
        if (limit > 100) {
            limit = 100;
        }

        // Search users with cursor
        List<User> users = userRepository.searchUsersWithCursor(query, currentUserId, cursor, limit);

        // Check if there are more items
        boolean hasMore = users.size() > limit;
        if (hasMore) {
            users = users.subList(0, limit);
        }

        // Convert to response and add additional info
        List<UserSearchResponse> responses = userSearchMapper.toSearchResponseList(users);

        // Add contact and conversation info for each user
        for (UserSearchResponse response : responses) {
            // Check if user is a contact
            boolean isContact = contactRepository.existsByUserIdAndContactUserId(
                    currentUserId,
                    response.getId()
            );
            response.setContact(isContact);

            // Check if there's a direct conversation with this user
            Optional<Conversation> existingConversation = conversationRepository
                    .findDirectConversationBetweenUsers(currentUserId, response.getId());

            if (existingConversation.isPresent()) {
                response.setHasConversation(true);
                response.setConversationId(existingConversation.get().getId());
            } else {
                response.setHasConversation(false);
                response.setConversationId(null);
            }
        }

        // Calculate next cursor
        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }
}
