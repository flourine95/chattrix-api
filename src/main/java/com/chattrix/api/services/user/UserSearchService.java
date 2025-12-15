package com.chattrix.api.services.user;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.UserSearchMapper;
import com.chattrix.api.repositories.ContactRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.PaginatedResponse;
import com.chattrix.api.responses.UserSearchResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
}

