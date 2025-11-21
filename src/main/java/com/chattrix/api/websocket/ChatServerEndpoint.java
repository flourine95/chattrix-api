package com.chattrix.api.websocket;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.exceptions.UnauthorizedException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.*;
import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
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
@ServerEndpoint(value = "/ws/chat",
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
    @Inject
    private CallService callService;

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
        } else if ("heartbeat".equals(message.getType())) {
            // Client sends heartbeat to keep connection alive and update last_seen
            // last_seen is already updated above, just send acknowledgment
            processHeartbeat(session, userId);
        } else if ("call.accept".equals(message.getType())) {
            processCallAccept(session, userId, message);
        } else if ("call.reject".equals(message.getType())) {
            processCallReject(session, userId, message);
        } else if ("call.end".equals(message.getType())) {
            processCallEnd(session, userId, message);
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

        // Validate sender is a participant of the conversation
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(senderId));
        if (!isParticipant) {
            throw new IllegalArgumentException("Sender is not a participant of this conversation");
        }

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (chatMessageDto.getReplyToMessageId() != null) {
            replyToMessage = messageRepository.findByIdSimple(chatMessageDto.getReplyToMessageId())
                    .orElseThrow(() -> new IllegalArgumentException("Reply to message not found"));

            if (!replyToMessage.getConversation().getId().equals(chatMessageDto.getConversationId())) {
                throw new IllegalArgumentException("Cannot reply to message from different conversation");
            }
        }

        // Validate mentions if provided
        if (chatMessageDto.getMentions() != null && !chatMessageDto.getMentions().isEmpty()) {
            List<Long> participantIds = conversation.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .toList();

            for (Long mentionedUserId : chatMessageDto.getMentions()) {
                if (!participantIds.contains(mentionedUserId)) {
                    throw new IllegalArgumentException("Cannot mention user who is not in this conversation");
                }
            }
        }

        // 1. Create and save the message
        Message newMessage = new Message();
        newMessage.setContent(chatMessageDto.getContent());
        newMessage.setSender(sender);
        newMessage.setConversation(conversation);

        // Set message type
        Message.MessageType messageType = Message.MessageType.TEXT;
        if (chatMessageDto.getType() != null) {
            try {
                messageType = Message.MessageType.valueOf(chatMessageDto.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                messageType = Message.MessageType.TEXT;
            }
        }
        newMessage.setType(messageType);

        // Set rich media fields
        newMessage.setMediaUrl(chatMessageDto.getMediaUrl());
        newMessage.setThumbnailUrl(chatMessageDto.getThumbnailUrl());
        newMessage.setFileName(chatMessageDto.getFileName());
        newMessage.setFileSize(chatMessageDto.getFileSize());
        newMessage.setDuration(chatMessageDto.getDuration());

        // Set location fields
        newMessage.setLatitude(chatMessageDto.getLatitude());
        newMessage.setLongitude(chatMessageDto.getLongitude());
        newMessage.setLocationName(chatMessageDto.getLocationName());

        // Set reply and mentions
        newMessage.setReplyToMessage(replyToMessage);
        newMessage.setMentions(chatMessageDto.getMentions());

        messageRepository.save(newMessage);

        // 2. Update conversation's lastMessage and updatedAt
        conversation.setLastMessage(newMessage);
        conversationRepository.save(conversation);

        // 3. Prepare the outgoing message DTO using mapper
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(newMessage);

        // Populate mentioned users if mentions exist
        if (newMessage.getMentions() != null && !newMessage.getMentions().isEmpty()) {
            List<User> mentionedUsers = userRepository.findByIds(newMessage.getMentions());
            outgoingDto.setMentionedUsers(
                    messageRepository.findById(newMessage.getId())
                            .map(msg -> {
                                MessageMapper messageMapper =
                                        CDI.current().select(MessageMapper.class).get();
                                return messageMapper.toMentionedUserResponseList(mentionedUsers);
                            })
                            .orElse(List.of())
            );
        }

        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage = new WebSocketMessage<>("chat.message", outgoingDto);

        // 4. Broadcast the message to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, outgoingWebSocketMessage);
        });

        // 5. Send mention notifications to mentioned users
        if (chatMessageDto.getMentions() != null && !chatMessageDto.getMentions().isEmpty()) {
            for (Long mentionedUserId : chatMessageDto.getMentions()) {
                MentionEventDto mentionEvent = new MentionEventDto();
                mentionEvent.setMessageId(newMessage.getId());
                mentionEvent.setConversationId(conversation.getId());
                mentionEvent.setSenderId(sender.getId());
                mentionEvent.setSenderName(sender.getFullName());
                mentionEvent.setContent(newMessage.getContent());
                mentionEvent.setMentionedUserId(mentionedUserId);
                mentionEvent.setCreatedAt(newMessage.getCreatedAt());

                WebSocketMessage<MentionEventDto> mentionMessage = new WebSocketMessage<>("message.mention", mentionEvent);
                chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
            }
        }

        // 6. Broadcast conversation update (lastMessage changed) to all participants
        broadcastConversationUpdate(conversation);
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
                typingUsers.stream().map(TypingUserDto::getUsername).toList());

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

    private void processHeartbeat(Session session, Long userId) {
        // Send acknowledgment back to client
        WebSocketMessage<Map<String, Object>> ackMessage = new WebSocketMessage<>("heartbeat.ack", Map.of(
                "userId", userId.toString(),
                "timestamp", java.time.Instant.now().toString()
        ));

        try {
            session.getBasicRemote().sendObject(ackMessage);
        } catch (IOException | EncodeException e) {
            System.err.println("Error sending heartbeat acknowledgment: " + e.getMessage());
        }
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

        WebSocketMessage<ConversationUpdateDto> message = new WebSocketMessage<>("conversation.update", updateDto);

        // Broadcast to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
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

    /**
     * Process call accept message from WebSocket
     * Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 8.2, 8.4, 10.3, 10.4
     */
    private void processCallAccept(Session session, Long userId, WebSocketMessage<?> message) {
        try {
            // Parse CallAcceptDto from message payload
            CallAcceptDto callAcceptDto = objectMapper.convertValue(message.getPayload(), CallAcceptDto.class);

            // Extract callId from DTO
            String callId = callAcceptDto.getCallId();

            if (callId == null || callId.trim().isEmpty()) {
                sendCallError(session, null, "invalid_request", "Call ID is required");
                return;
            }

            System.out.println("Processing call accept for call: " + callId + " by user: " + userId);

            // Invoke CallService to process the acceptance
            callService.acceptCallViaWebSocket(callId, String.valueOf(userId));

            System.out.println("Call accepted successfully: " + callId);

        } catch (ResourceNotFoundException e) {
            // Call not found
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "call_not_found", e.getMessage());
            System.err.println("Call not found: " + e.getMessage());
        } catch (UnauthorizedException e) {
            // User is not authorized (not the callee)
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "unauthorized", e.getMessage());
            System.err.println("Unauthorized call accept: " + e.getMessage());
        } catch (BadRequestException e) {
            // Invalid call status or call has timed out
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "invalid_status", e.getMessage());
            System.err.println("Invalid call status: " + e.getMessage());
        } catch (Exception e) {
            // Generic error
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "service_error", "An unexpected error occurred");
            System.err.println("Error processing call accept: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process call reject message from WebSocket
     * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 8.2, 8.4, 10.3, 10.4
     */
    private void processCallReject(Session session, Long userId, WebSocketMessage<?> message) {
        try {
            // Parse CallRejectDto from message payload
            CallRejectDto callRejectDto = objectMapper.convertValue(message.getPayload(), CallRejectDto.class);

            // Extract callId and reason from DTO
            String callId = callRejectDto.getCallId();
            String reason = callRejectDto.getReason();

            if (callId == null || callId.trim().isEmpty()) {
                sendCallError(session, null, "invalid_request", "Call ID is required");
                return;
            }

            System.out.println("Processing call reject for call: " + callId + " by user: " + userId + " with reason: " + reason);

            // Invoke CallService to process the rejection
            callService.rejectCallViaWebSocket(callId, String.valueOf(userId), reason);

            System.out.println("Call rejected successfully: " + callId);

        } catch (com.chattrix.api.exceptions.ResourceNotFoundException e) {
            // Call not found
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "call_not_found", e.getMessage());
            System.err.println("Call not found: " + e.getMessage());
        } catch (com.chattrix.api.exceptions.UnauthorizedException e) {
            // User is not authorized (not the callee)
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "unauthorized", e.getMessage());
            System.err.println("Unauthorized call reject: " + e.getMessage());
        } catch (com.chattrix.api.exceptions.BadRequestException e) {
            // Invalid call status
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "invalid_status", e.getMessage());
            System.err.println("Invalid call status: " + e.getMessage());
        } catch (Exception e) {
            // Generic error
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "service_error", "An unexpected error occurred");
            System.err.println("Error processing call reject: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process call end message from WebSocket
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 8.2, 8.4, 10.3, 10.4
     */
    private void processCallEnd(Session session, Long userId, WebSocketMessage<?> message) {
        try {
            // Parse CallEndDto from message payload
            CallEndDto callEndDto = objectMapper.convertValue(message.getPayload(), CallEndDto.class);

            // Extract callId and durationSeconds from DTO
            String callId = callEndDto.getCallId();
            Integer durationSeconds = callEndDto.getDurationSeconds();

            if (callId == null || callId.trim().isEmpty()) {
                sendCallError(session, null, "invalid_request", "Call ID is required");
                return;
            }

            System.out.println("Processing call end for call: " + callId + " by user: " + userId +
                    " with duration: " + durationSeconds);

            // Invoke CallService to process the call ending
            callService.endCallViaWebSocket(callId, String.valueOf(userId), durationSeconds);

            System.out.println("Call ended successfully: " + callId);

        } catch (com.chattrix.api.exceptions.ResourceNotFoundException e) {
            // Call not found
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "call_not_found", e.getMessage());
            System.err.println("Call not found: " + e.getMessage());
        } catch (com.chattrix.api.exceptions.UnauthorizedException e) {
            // User is not authorized (not a participant)
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "unauthorized", e.getMessage());
            System.err.println("Unauthorized call end: " + e.getMessage());
        } catch (com.chattrix.api.exceptions.BadRequestException e) {
            // Invalid call status (call already ended)
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "invalid_status", e.getMessage());
            System.err.println("Invalid call status: " + e.getMessage());
        } catch (Exception e) {
            // Generic error
            String callId = extractCallIdFromPayload(message.getPayload());
            sendCallError(session, callId, "service_error", "An unexpected error occurred");
            System.err.println("Error processing call end: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send call error message to client
     * Validates: Requirements 8.2, 8.3, 8.4, 8.5
     */
    private void sendCallError(Session session, String callId, String errorType, String errorMessage) {
        try {
            CallErrorDto errorDto = new CallErrorDto();
            errorDto.setCallId(callId);
            errorDto.setErrorType(errorType);
            errorDto.setMessage(errorMessage);

            WebSocketMessage<CallErrorDto> wsMessage = new WebSocketMessage<>("call_error", errorDto);
            session.getBasicRemote().sendObject(wsMessage);

            System.err.println("Sent call error to user: " + errorType + " - " + errorMessage);
        } catch (Exception e) {
            System.err.println("Failed to send error message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to extract callId from payload for error handling
     */
    private String extractCallIdFromPayload(Object payload) {
        try {
            if (payload instanceof Map) {
                Object callId = ((Map<?, ?>) payload).get("callId");
                return callId != null ? callId.toString() : null;
            }
            // Try to extract as CallAcceptDto, CallRejectDto, or CallEndDto
            try {
                CallAcceptDto acceptDto = objectMapper.convertValue(payload, CallAcceptDto.class);
                return acceptDto.getCallId();
            } catch (Exception e1) {
                try {
                    CallRejectDto rejectDto = objectMapper.convertValue(payload, CallRejectDto.class);
                    return rejectDto.getCallId();
                } catch (Exception e2) {
                    try {
                        CallEndDto endDto = objectMapper.convertValue(payload, CallEndDto.class);
                        return endDto.getCallId();
                    } catch (Exception e3) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
    }
}
