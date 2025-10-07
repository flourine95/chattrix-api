package com.chattrix.api.websocket;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.ChatSessionService;
import com.chattrix.api.services.TokenService;
import com.chattrix.api.websocket.codec.MessageDecoder;
import com.chattrix.api.websocket.codec.MessageEncoder;
import com.chattrix.api.websocket.dto.ChatMessageDto;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@ServerEndpoint(value = "/v1/chat",
        configurator = CdiAwareConfigurator.class,
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
    private ConversationRepository conversationRepository;
    @Inject
    private MessageRepository messageRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = getTokenFromQuery(session);
        if (token == null || !tokenService.validateToken(token)) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid token"));
            return;
        }

        String username = tokenService.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "User not found"));
            return;
        }

        session.getUserProperties().put("userId", user.getId());
        chatSessionService.addSession(user.getId(), session);
        System.out.println("User connected: " + user.getUsername());
    }

    @OnMessage
    @Transactional
    public void onMessage(Session session, WebSocketMessage<?> message) throws IOException {
        UUID userId = (UUID) session.getUserProperties().get("userId");
        if (userId == null) return;

        if ("chat.message".equals(message.getType())) {
            ChatMessageDto chatMessageDto = objectMapper.convertValue(message.getPayload(), ChatMessageDto.class);
            processChatMessage(userId, chatMessageDto);
        }
    }

    private void processChatMessage(UUID senderId, ChatMessageDto chatMessageDto) {
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

        // 2. Prepare the outgoing message DTO
        OutgoingMessageDto outgoingDto = OutgoingMessageDto.fromEntity(newMessage);
        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage = new WebSocketMessage<>("chat.message", outgoingDto);

        // 3. Broadcast the message to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            UUID participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, outgoingWebSocketMessage);
        });
    }

    @OnClose
    public void onClose(Session session) {
        UUID userId = (UUID) session.getUserProperties().get("userId");
        if (userId != null) {
            chatSessionService.removeSession(userId);
            System.out.println("User disconnected: " + userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
        throwable.printStackTrace();
    }

    private String getTokenFromQuery(Session session) {
        Map<String, List<String>> params = session.getRequestParameterMap();
        if (params.containsKey("token")) {
            return params.get("token").get(0);
        }
        return null;
    }
}
