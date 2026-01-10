package com.chattrix.api.websocket.handlers;

import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.services.cache.UserProfileCache;
import com.chattrix.api.services.conversation.TypingIndicatorService;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.TypingIndicatorDto;
import com.chattrix.api.websocket.dto.TypingIndicatorResponseDto;
import com.chattrix.api.websocket.dto.TypingUserDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class TypingHandler {

    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private UserRepository userRepository;
    @Inject
    private TypingIndicatorService typingIndicatorService;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private UserMapper userMapper;
    @Inject
    private UserProfileCache userProfileCache;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void handleTypingEvent(Long userId, Object payload, boolean isStarting) {
        TypingIndicatorDto dto = objectMapper.convertValue(payload, TypingIndicatorDto.class);

        if (dto.getConversationId() == null) {
            log.warn("Typing event missing conversationId from user: {}", userId);
            return;
        }

        if (isStarting) {
            typingIndicatorService.startTyping(dto.getConversationId(), userId);
        } else {
            typingIndicatorService.stopTyping(dto.getConversationId(), userId);
        }

        // Broadcast trạng thái typing cho các thành viên khác
        conversationRepository.findByIdWithParticipants(dto.getConversationId()).ifPresent(conv -> {
            // Validate user is participant
            boolean isParticipant = conv.getParticipants().stream()
                    .anyMatch(p -> p.getUser().getId().equals(userId));

            if (!isParticipant) {
                log.warn("User {} is not a participant of conversation {}", userId, dto.getConversationId());
                return;
            }

            Set<Long> typingIds;
            if (conv.getParticipants().size() <= 1) {
                // Single user conversation - show all typing users
                typingIds = typingIndicatorService.getTypingUsersInConversation(conv.getId(), null);
            } else {
                // Normal case: exclude the user who triggered the event
                typingIds = typingIndicatorService.getTypingUsersInConversation(conv.getId(), userId);
            }

            // Use cache for typing users
            Map<Long, UserResponse> cachedUsers = userProfileCache.getAll(typingIds);

            // Fetch missing users from DB and cache them
            Set<Long> missingIds = typingIds.stream()
                    .filter(id -> !cachedUsers.containsKey(id))
                    .collect(Collectors.toSet());

            if (!missingIds.isEmpty()) {
                List<User> missingUsers = userRepository.findByIds(new ArrayList<>(missingIds));
                Map<Long, UserResponse> newlyCached = missingUsers.stream()
                        .collect(Collectors.toMap(User::getId, userMapper::toResponse));
                userProfileCache.putAll(newlyCached);
                cachedUsers.putAll(newlyCached);
            }

            List<TypingUserDto> typingUsers = typingIds.stream()
                    .map(cachedUsers::get)
                    .filter(Objects::nonNull)
                    .map(ur -> {
                        TypingUserDto typingDto = new TypingUserDto();
                        typingDto.setUserId(ur.getId());
                        typingDto.setUsername(ur.getUsername());
                        typingDto.setFullName(ur.getFullName());
                        return typingDto;
                    })
                    .toList();

            WebSocketMessage<TypingIndicatorResponseDto> msg = new WebSocketMessage<>(
                    WebSocketEventType.TYPING_INDICATOR,
                    new TypingIndicatorResponseDto(conv.getId(), typingUsers));

            conv.getParticipants().forEach(p ->
                    chatSessionService.sendMessageToUser(p.getUser().getId(), msg));
        });
    }
}
