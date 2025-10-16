package com.chattrix.api.websocket;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.ChatSessionService;
import com.chattrix.api.services.TokenService;
import com.chattrix.api.services.TypingIndicatorService;
import com.chattrix.api.services.UserStatusService;
import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
@ServerEndpoint(value = "/v1/chat",
        configurator = CdiAwareConfigurator.class,
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class)
public class ChatServerEndpoint {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Inject
    private TokenService tokenService;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private MessageRepository messageRepository;
    @Inject
    private TypingIndicatorService typingIndicatorService;
    @Inject
    private UserStatusService userStatusService;
    @Inject
    private WebSocketMapper webSocketMapper;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = getTokenFromQuery(session);
        if (token == null || !tokenService.validateToken(token)) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            return;
        }

        Long userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not found"));
            return;
        }

        session.getUserProperties().put("userId", user.getId());
        chatSessionService.addSession(user.getId(), session);

        // Mark user as online
        userStatusService.setUserOnline(user.getId());

        // Broadcast user status change to other users
        broadcastUserStatusChange(user.getId(), true);

        System.out.println("User connected: " + user.getUsername());
    }

    @OnMessage
    @Transactional
    public void onMessage(Session session, WebSocketMessage<?> message) throws IOException {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) return;

        // Update last seen when user sends any message
        userStatusService.updateLastSeen(userId);

        if ("chat.message".equals(message.getType())) {
            ChatMessageDto chatMessageDto = objectMapper.convertValue(message.getPayload(), ChatMessageDto.class);
            processChatMessage(userId, chatMessageDto);
        } else if ("typing.start".equals(message.getType())) {
            TypingIndicatorDto typingDto = objectMapper.convertValue(message.getPayload(), TypingIndicatorDto.class);
            processTypingStart(userId, typingDto);
        } else if ("typing.stop".equals(message.getType())) {
            TypingIndicatorDto typingDto = objectMapper.convertValue(message.getPayload(), TypingIndicatorDto.class);
            processTypingStop(userId, typingDto);
        }
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId, session);

            // Mark user as offline if no more active sessions
            userStatusService.setUserOffline(userId);

            // Broadcast user status change to other users
            broadcastUserStatusChange(userId, false);

            System.out.println("User disconnected: " + userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        Long userId = (Long) session.getUserProperties().get("userId");
        System.err.println("WebSocket error for user " + userId + ": " + throwable.getMessage());
        throwable.printStackTrace();
    }

    private void processChatMessage(Long senderId, ChatMessageDto chatMessageDto) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database."));

        Conversation conversation = conversationRepository.findByIdWithParticipants(chatMessageDto.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        // TODO: Add validation to ensure sender is a participant of the conversation

        // 1. Create and save the message
        Message newMessage = new Message();
        newMessage.setContent(chatMessageDto.getContent());
        newMessage.setSender(sender);
        newMessage.setConversation(conversation);
        newMessage.setType(Message.MessageType.TEXT);
        messageRepository.save(newMessage);

        // 2. Prepare the outgoing message DTO using mapper
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(newMessage);
        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage = new WebSocketMessage<>("chat.message", outgoingDto);

        // 3. Broadcast the message to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, outgoingWebSocketMessage);
        });
    }

    private void processTypingStart(Long userId, TypingIndicatorDto typingDto) {
        Long conversationId = typingDto.getConversationId();

        System.out.println("DEBUG: processTypingStart - userId: " + userId + ", conversationId: " + conversationId);

        // Validate conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        // TODO: Add validation to ensure user is a participant of the conversation

        // Mark user as typing
        typingIndicatorService.startTyping(conversationId, userId);
        System.out.println("DEBUG: User marked as typing");

        // Broadcast typing status to other participants
        broadcastTypingIndicator(conversation, userId);
    }

    private void processTypingStop(Long userId, TypingIndicatorDto typingDto) {
        Long conversationId = typingDto.getConversationId();

        System.out.println("DEBUG: processTypingStop - userId: " + userId + ", conversationId: " + conversationId);

        // Validate conversation exists
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        // Mark user as stopped typing
        typingIndicatorService.stopTyping(conversationId, userId);
        System.out.println("DEBUG: User marked as stopped typing");

        // Broadcast updated typing status to other participants
        broadcastTypingIndicator(conversation, userId);
    }

    private void broadcastTypingIndicator(Conversation conversation, Long excludeUserId) {
        Long conversationId = conversation.getId();

        // Get ALL typing users first (for debugging)
        Set<Long> allTypingUsers = typingIndicatorService.getTypingUsersInConversation(conversationId, null);
        System.out.println("DEBUG: All typing users in conversation " + conversationId + ": " + allTypingUsers);

        // For typing indicators, we want to show OTHER users who are typing
        // But if we're testing with single user, we might want to see our own typing for debugging
        Set<Long> typingUserIds;

        // If there's only one participant (testing scenario), include all typing users
        if (conversation.getParticipants().size() <= 1) {
            typingUserIds = allTypingUsers;
            System.out.println("DEBUG: Single user conversation - showing all typing users: " + typingUserIds);
        } else {
            // Normal case: exclude the user who triggered the event
            typingUserIds = typingIndicatorService.getTypingUsersInConversation(conversationId, excludeUserId);
            System.out.println("DEBUG: Multi-user conversation - typing users (excluding " + excludeUserId + "): " + typingUserIds);
        }

        // Convert user IDs to detailed user information using mapper
        List<TypingUserDto> typingUsers = typingUserIds.stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(webSocketMapper::toTypingUserResponse)
                .collect(java.util.stream.Collectors.toList());

        System.out.println("DEBUG: Final typing users to broadcast: " + typingUsers.size() + " users - " +
                typingUsers.stream().map(TypingUserDto::getUsername).collect(java.util.stream.Collectors.toList()));

        // Create response
        TypingIndicatorResponseDto response = new TypingIndicatorResponseDto(conversationId, typingUsers);
        WebSocketMessage<TypingIndicatorResponseDto> message = new WebSocketMessage<>("typing.indicator", response);

        // Broadcast to all participants
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            System.out.println("DEBUG: Broadcasting typing indicator to participant: " + participantId);
            chatSessionService.sendMessageToUser(participantId, message);
        });
    }

    private void broadcastUserStatusChange(Long userId, boolean isOnline) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // Create status change message
        WebSocketMessage<Map<String, Object>> statusMessage = new WebSocketMessage<>("user.status", Map.of(
                "userId", userId.toString(),
                "username", user.getUsername(),
                "displayName", user.getFullName(),
                "isOnline", isOnline,
                "lastSeen", user.getLastSeen() != null ? user.getLastSeen().toString() : null
        ));

        // Broadcast to all connected users
        chatSessionService.broadcastToAllUsers(statusMessage);
    }

    private String getTokenFromQuery(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            return params.get("token").get(0);
        }
        return null;
    }
}
