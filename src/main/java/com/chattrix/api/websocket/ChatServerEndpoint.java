package com.chattrix.api.websocket;

import com.chattrix.api.dto.MessageMetadata;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.MessageMetadataMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.services.auth.TokenService;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.cache.OnlineStatusCache;
import com.chattrix.api.services.cache.UserProfileCache;
import com.chattrix.api.services.call.CallService;
import com.chattrix.api.services.conversation.TypingIndicatorService;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.services.user.HeartbeatMonitorService;
import com.chattrix.api.services.user.UserStatusBatchService;
import com.chattrix.api.services.user.UserStatusBroadcastService;
import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Dependent
@Slf4j
@ServerEndpoint(value = "/ws/chat",
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class)
public class ChatServerEndpoint {

    @Inject
    private TokenService tokenService;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private OnlineStatusCache onlineStatusCache;
    @Inject
    private UserStatusBatchService batchService;
    @Inject
    private UserStatusBroadcastService broadcastService;
    @Inject
    private CallService callService;
    @Inject
    private HeartbeatMonitorService heartbeatMonitorService;
    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private MessageRepository messageRepository;
    @Inject
    private TypingIndicatorService typingIndicatorService;
    @Inject
    private WebSocketMapper webSocketMapper;
    @Inject
    private UserMapper userMapper;
    @Inject
    private MessageMapper messageMapper;
    @Inject
    private UserProfileCache userProfileCache;
    @Inject
    private MessageMetadataMapper metadataMapper;
    @Inject
    private CacheManager cacheManager;
    @Inject
    private MessageCache messageCache;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = getTokenFromQuery(session);

        if (token == null) {
            log.warn("WebSocket Rejected: No token provided.");
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "No token"));
            return;
        }

        if (!tokenService.validateToken(token)) {
            log.warn("WebSocket Rejected: Invalid token.");
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            return;
        }

        Long userId = tokenService.getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("WebSocket Rejected: User ID {} not found.", userId);
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not found"));
            return;
        }

        session.getUserProperties().put("userId", user.getId());
        chatSessionService.addSession(user.getId(), session);
        onlineStatusCache.markOnline(user.getId());
        batchService.queueLastSeenUpdate(user.getId());
        heartbeatMonitorService.recordHeartbeat(user.getId());
        broadcastService.broadcastUserStatusChange(user.getId(), true);

        log.info("User Connected: {} (ID: {})", user.getUsername(), userId);
    }

    @OnMessage
    public void onMessage(Session session, WebSocketMessage<?> message) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId == null) return;

        onlineStatusCache.markOnline(userId);
        batchService.queueLastSeenUpdate(userId);

        String type = message.getType();
        Object payload = message.getPayload();

        try {
            switch (type) {
                case "chat.message" -> handleChatMessage(userId, payload);
                case "typing.start" -> handleTypingEvent(userId, payload, true);
                case "typing.stop" -> handleTypingEvent(userId, payload, false);
                case "heartbeat" -> handleHeartbeat(session, userId);
                default -> log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling message [{}]: {}", type, e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId, session);

            boolean hasOtherSessions = !chatSessionService.getUserSessions(userId).isEmpty();

            if (!hasOtherSessions) {
                try {
                    callService.handleUserDisconnected(userId);
                } catch (Exception e) {
                    log.error("Error cleaning up calls for user {}: {}", userId, e.getMessage());
                }

                onlineStatusCache.markOffline(userId);
                batchService.queueLastSeenUpdate(userId);
                heartbeatMonitorService.removeHeartbeat(userId);
                broadcastService.broadcastUserStatusChange(userId, false);

                log.info("User Disconnected (Offline): ID {}", userId);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Bỏ qua lỗi ngắt kết nối thông thường (EOF, Closed, Broken pipe) đỡ rác log
        if (throwable instanceof IOException) {
            return;
        }

        Long userId = (Long) session.getUserProperties().get("userId");
        log.error("WebSocket Error for user {}: {}", userId, throwable.getMessage(), throwable);
    }

    // --- PRIVATE MESSAGE HANDLERS ---

    private void handleChatMessage(Long senderId, Object payload) {
        ChatMessageDto dto = objectMapper.convertValue(payload, ChatMessageDto.class);

        // 1. Validate & Lấy dữ liệu
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalStateException("User not found."));

        Conversation conv = conversationRepository.findByIdWithParticipants(dto.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));

        // Validate sender is participant
        boolean isParticipant = conv.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(senderId));
        if (!isParticipant) {
            throw new IllegalArgumentException("Sender is not a participant of this conversation");
        }

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (dto.getReplyToMessageId() != null) {
            replyToMessage = messageRepository.findById(dto.getReplyToMessageId())
                    .orElseThrow(() -> new IllegalArgumentException("Reply to message not found"));

            if (!replyToMessage.getConversation().getId().equals(dto.getConversationId())) {
                throw new IllegalArgumentException("Cannot reply to message from different conversation");
            }
        }

        // Validate mentions if provided
        if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
            List<Long> participantIds = conv.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .toList();

            for (Long mentionedUserId : dto.getMentions()) {
                if (!participantIds.contains(mentionedUserId)) {
                    throw new IllegalArgumentException("Cannot mention user who is not in this conversation");
                }
            }
        }

        // 2. Tạo và Lưu Message
        Message newMessage = new Message();
        newMessage.setSender(sender);
        newMessage.setConversation(conv);
        newMessage.setContent(dto.getContent());

        // Set message type
        MessageType messageType = MessageType.TEXT;
        if (dto.getType() != null) {
            try {
                messageType = MessageType.valueOf(dto.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                messageType = MessageType.TEXT;
            }
        }
        newMessage.setType(messageType);

        // Build metadata using MessageMetadataMapper (type-safe)
        MessageMetadata metadataDto = MessageMetadata.builder()
                .mediaUrl(dto.getMediaUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .fileName(dto.getFileName())
                .fileSize(dto.getFileSize())
                .duration(dto.getDuration())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .locationName(dto.getLocationName())
                .build();
        
        Map<String, Object> metadata = metadataMapper.toMap(metadataDto);
        newMessage.setMetadata(metadata);

        // Set reply and mentions
        newMessage.setReplyToMessage(replyToMessage);
        newMessage.setMentions(dto.getMentions());

        messageRepository.save(newMessage);

        // Update conversation's lastMessage
        conv.setLastMessage(newMessage);
        conversationRepository.save(conv);

        // Invalidate caches
        Set<Long> participantIds = conv.getParticipants().stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());
        cacheManager.invalidateConversationCaches(conv.getId(), participantIds);
        messageCache.invalidate(conv.getId());

        // 3. Broadcast
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(newMessage);

        // Populate mentioned users if mentions exist (use cache)
        if (newMessage.getMentions() != null && !newMessage.getMentions().isEmpty()) {
            List<Long> mentionIds = newMessage.getMentions();
            Map<Long, UserResponse> cachedUsers = userProfileCache.getAll(new HashSet<>(mentionIds));

            // Fetch missing users from DB and cache them
            List<Long> missingIds = mentionIds.stream()
                    .filter(id -> !cachedUsers.containsKey(id))
                    .toList();

            if (!missingIds.isEmpty()) {
                List<User> missingUsers = userRepository.findByIds(missingIds);
                Map<Long, UserResponse> newlyCached = missingUsers.stream()
                        .collect(Collectors.toMap(User::getId, userMapper::toResponse));
                userProfileCache.putAll(newlyCached);
                cachedUsers.putAll(newlyCached);
            }

            // Convert to MentionedUserResponse
            outgoingDto.setMentionedUsers(
                    mentionIds.stream()
                            .map(cachedUsers::get)
                            .filter(Objects::nonNull)
                            .map(ur -> {
                                var mention = new com.chattrix.api.responses.MentionedUserResponse();
                                mention.setId(ur.getId());
                                mention.setUserId(ur.getId());
                                mention.setUsername(ur.getUsername());
                                mention.setFullName(ur.getFullName());
                                return mention;
                            })
                            .toList()
            );
        }

        WebSocketMessage<OutgoingMessageDto> wsMsg = new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        conv.getParticipants().forEach(p ->
                chatSessionService.sendMessageToUser(p.getUser().getId(), wsMsg)
        );

        // Send mention notifications
        if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
            for (Long mentionedUserId : dto.getMentions()) {
                MentionEventDto mentionEvent = new MentionEventDto();
                mentionEvent.setMessageId(newMessage.getId());
                mentionEvent.setConversationId(conv.getId());
                mentionEvent.setSenderId(sender.getId());
                mentionEvent.setSenderName(sender.getFullName());
                mentionEvent.setContent(newMessage.getContent());
                mentionEvent.setMentionedUserId(mentionedUserId);
                mentionEvent.setCreatedAt(newMessage.getCreatedAt());

                WebSocketMessage<MentionEventDto> mentionMessage =
                        new WebSocketMessage<>(WebSocketEventType.MESSAGE_MENTION, mentionEvent);
                chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
            }
        }

        // Broadcast conversation update
        broadcastConversationUpdate(conv);
    }

    private void handleTypingEvent(Long userId, Object payload, boolean isStarting) {
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

    private void handleHeartbeat(Session session, Long userId) throws IOException, EncodeException {
        heartbeatMonitorService.recordHeartbeat(userId);

        HeartbeatAckDto ackPayload = HeartbeatAckDto.builder()
                .userId(userId)
                .timestamp(Instant.now())
                .build();
        WebSocketMessage<HeartbeatAckDto> ack = new WebSocketMessage<>(WebSocketEventType.HEARTBEAT_ACK, ackPayload);

        session.getBasicRemote().sendObject(ack);
    }

    private void broadcastConversationUpdate(Conversation conversation) {
        ConversationUpdateDto updateDto = new ConversationUpdateDto();
        updateDto.setConversationId(conversation.getId());
        updateDto.setUpdatedAt(conversation.getUpdatedAt());

        // Map lastMessage if exists
        if (conversation.getLastMessage() != null) {
            Message lastMsg = conversation.getLastMessage();
            ConversationUpdateDto.LastMessageDto lastMessageDto = new ConversationUpdateDto.LastMessageDto();
            lastMessageDto.setId(lastMsg.getId());
            lastMessageDto.setContent(lastMsg.getContent());
            lastMessageDto.setSenderId(lastMsg.getSender().getId());
            lastMessageDto.setSenderUsername(lastMsg.getSender().getUsername());
            lastMessageDto.setSentAt(lastMsg.getSentAt());
            lastMessageDto.setType(lastMsg.getType().name());
            updateDto.setLastMessage(lastMessageDto);
        }

        WebSocketMessage<ConversationUpdateDto> message =
                new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATE, updateDto);

        // Broadcast to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, message);
        });
    }

    private String getTokenFromQuery(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            return params.get("token").get(0);
        }
        return null;
    }
}