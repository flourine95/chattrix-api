package com.chattrix.api.websocket.handlers.chat;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.*;
import com.chattrix.api.websocket.handlers.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ChatMessageHandler implements MessageHandler {

    private static final Logger LOGGER = Logger.getLogger(ChatMessageHandler.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private UserRepository userRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private WebSocketMapper webSocketMapper;
    @Inject
    private MessageMapper messageMapper;
    @Inject
    private UserMapper userMapper;

    @Override
    public void handle(Session session, Long userId, Object payload) {
        try {
            // Convert payload to DTO
            ChatMessageDto chatMessageDto = objectMapper.convertValue(payload, ChatMessageDto.class);

            // Validate required fields
            if (chatMessageDto.getConversationId() == null) {
                LOGGER.log(Level.WARNING, "Chat message missing conversationId from user: {0}", userId);
                return;
            }

            // Process the chat message
            processChatMessage(userId, chatMessageDto);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid chat message from user " + userId + ": " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "State error processing chat message from user " + userId + ": " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing chat message from user " + userId, e);
        }
    }

    @Override
    public String getMessageType() {
        return "chat.message";
    }

    /**
     * Process the chat message - extracted from ChatServerEndpoint
     */
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
                            .map(msg -> userMapper.toMentionedUserResponseList(mentionedUsers))
                            .orElse(List.of())
            );
        }

        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage =
                new WebSocketMessage<>("chat.message", outgoingDto);

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

                WebSocketMessage<MentionEventDto> mentionMessage =
                        new WebSocketMessage<>("message.mention", mentionEvent);
                chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
            }
        }

        // 6. Broadcast conversation update (lastMessage changed) to all participants
        broadcastConversationUpdate(conversation);

        LOGGER.log(Level.FINE, "Chat message processed successfully for user {0} in conversation {1}",
                new Object[]{senderId, chatMessageDto.getConversationId()});
    }

    /**
     * Broadcast conversation update to all participants
     */
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
                new WebSocketMessage<>("conversation.update", updateDto);

        // Broadcast to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, message);
        });
    }
}
