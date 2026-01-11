package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.conversation.GroupPermissionsService;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.ConversationUpdateDto;
import com.chattrix.api.websocket.dto.MentionEventDto;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized service for message creation logic.
 * Used by both REST API and WebSocket handlers to ensure consistency.
 */
@ApplicationScoped
@Slf4j
public class MessageCreationService {

    @Inject
    private MessageRepository messageRepository;
    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ConversationParticipantRepository participantRepository;
    @Inject
    private MessageMapper messageMapper;
    @Inject
    private WebSocketMapper webSocketMapper;
    @Inject
    private MessageCache messageCache;
    @Inject
    private CacheManager cacheManager;
    @Inject
    private MessageBatchService messageBatchService;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private GroupPermissionsService groupPermissionsService;

    /**
     * Create and send a message (used by both REST and WebSocket)
     * 
     * @param senderId User ID of sender
     * @param conversationId Conversation ID
     * @param content Message content
     * @param type Message type (TEXT, IMAGE, etc.)
     * @param metadata Additional metadata (media URLs, location, etc.)
     * @param replyToMessageId Optional reply to message ID
     * @param mentions Optional list of mentioned user IDs
     * @param useWriteBehind If true, use write-behind pattern (buffer before DB insert)
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse createMessage(
            Long senderId,
            Long conversationId,
            String content,
            String type,
            Map<String, Object> metadata,
            Long replyToMessageId,
            List<Long> mentions,
            boolean useWriteBehind
    ) {
        log.debug("Creating message: senderId={}, conversationId={}, type={}, useWriteBehind={}", 
                senderId, conversationId, type, useWriteBehind);

        // 1. Validate and load entities
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(senderId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        // Check if user is muted (for group conversations)
        if (conversation.isGroupConversation()) {
            ConversationParticipant participant = conversation.getParticipant(senderId).orElse(null);
            if (participant != null && participant.isCurrentlyMuted())
                throw BusinessException.forbidden("You are muted in this conversation");
            
            // Check send_messages permission
            if (!groupPermissionsService.hasPermission(conversationId, senderId, "send_messages"))
                throw BusinessException.forbidden("You don't have permission to send messages in this group");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // 2. Validate reply to message if provided
        Message replyToMessage = null;
        if (replyToMessageId != null) {
            replyToMessage = messageRepository.findByIdSimple(replyToMessageId)
                    .orElseThrow(() -> BusinessException.notFound("Reply to message not found"));

            if (!replyToMessage.getConversation().getId().equals(conversationId))
                throw BusinessException.badRequest("Cannot reply to message from different conversation");
        }

        // 3. Validate mentions if provided
        if (mentions != null && !mentions.isEmpty()) {
            Set<Long> participantIds = conversation.getParticipantIds();
            for (Long mentionedUserId : mentions) {
                if (!participantIds.contains(mentionedUserId))
                    throw BusinessException.badRequest("Cannot mention user who is not in this conversation");
            }
        }

        // 4. Build message entity
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        message.setConversation(conversation);
        message.setSentAt(Instant.now());

        // Set message type
        MessageType messageType = MessageType.TEXT;
        if (type != null) {
            try {
                messageType = MessageType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid message type: {}, defaulting to TEXT", type);
            }
        }
        message.setType(messageType);

        // Set metadata
        if (metadata != null)
            message.setMetadata(new HashMap<>(metadata));
        else
            message.setMetadata(new HashMap<>());

        message.setReplyToMessage(replyToMessage);
        message.setMentions(mentions);

        // 5. Save message (Write-Behind or Direct)
        if (useWriteBehind) {
            // Buffer message for batch insert
            Long tempId = messageBatchService.bufferMessage(message);
            message.setId(tempId);
            log.debug("Message buffered with temp ID: {}", tempId);
        } else {
            // Direct DB insert
            messageRepository.save(message);
            log.debug("Message saved directly to DB with ID: {}", message.getId());
        }

        // 6. Map to response
        MessageResponse response = messageMapper.toResponse(message);

        // 7. Update cache
        if (useWriteBehind) {
            messageCache.addUnflushed(conversationId, response);
        } else {
            messageCache.invalidate(conversationId);
        }

        // 8. Update conversation (outside transaction for write-behind)
        if (!useWriteBehind) {
            conversation.setLastMessage(message);
        }
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // 9. Auto-unarchive for all participants
        conversation.getParticipants().forEach(participant -> {
            if (participant.isArchived()) {
                participant.setArchived(false);
                participant.setArchivedAt(null);
                participantRepository.save(participant);
            }
        });

        // 10. Invalidate caches
        Set<Long> participantIds = conversation.getParticipantIds();
        cacheManager.invalidateConversationCaches(conversationId, participantIds);

        log.debug("Message created successfully: messageId={}, conversationId={}, senderId={}", 
                message.getId(), conversationId, senderId);

        return response;
    }

    /**
     * Broadcast message to all participants via WebSocket
     * Should be called OUTSIDE transaction to avoid holding DB locks
     */
    public void broadcastMessage(Message message, Conversation conversation) {
        log.debug("Broadcasting message: messageId={}, conversationId={}", 
                message.getId(), conversation.getId());

        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);
        WebSocketMessage<OutgoingMessageDto> wsMessage = 
                new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        conversation.getParticipants().forEach(p ->
                chatSessionService.sendMessageToUser(p.getUser().getId(), wsMessage)
        );
    }

    /**
     * Broadcast conversation update to all participants
     * Should be called OUTSIDE transaction
     */
    public void broadcastConversationUpdate(Conversation conversation) {
        log.debug("Broadcasting conversation update: conversationId={}", conversation.getId());

        ConversationUpdateDto updateDto = new ConversationUpdateDto();
        updateDto.setConversationId(conversation.getId());
        updateDto.setUpdatedAt(conversation.getUpdatedAt());

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

        conversation.getParticipants().forEach(participant ->
                chatSessionService.sendMessageToUser(participant.getUser().getId(), message)
        );
    }

    /**
     * Broadcast conversation update with temporary message (before DB flush)
     * This allows clients to see lastMessage immediately with temp ID
     * Will be updated again after flush with real ID
     */
    public void broadcastConversationUpdateWithTempMessage(Conversation conversation, Message tempMessage) {
        log.debug("Broadcasting conversation update with temp message: conversationId={}, tempMessageId={}", 
                conversation.getId(), tempMessage.getId());

        ConversationUpdateDto updateDto = new ConversationUpdateDto();
        updateDto.setConversationId(conversation.getId());
        updateDto.setUpdatedAt(conversation.getUpdatedAt());

        // Use temp message as lastMessage
        ConversationUpdateDto.LastMessageDto lastMessageDto = new ConversationUpdateDto.LastMessageDto();
        lastMessageDto.setId(tempMessage.getId());  // Temp ID (negative)
        lastMessageDto.setContent(tempMessage.getContent());
        lastMessageDto.setSenderId(tempMessage.getSender().getId());
        lastMessageDto.setSenderUsername(tempMessage.getSender().getUsername());
        lastMessageDto.setSentAt(tempMessage.getSentAt());
        lastMessageDto.setType(tempMessage.getType().name());
        updateDto.setLastMessage(lastMessageDto);

        WebSocketMessage<ConversationUpdateDto> message =
                new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATE, updateDto);

        conversation.getParticipants().forEach(participant ->
                chatSessionService.sendMessageToUser(participant.getUser().getId(), message)
        );
    }

    /**
     * Send mention notifications to mentioned users
     * Should be called OUTSIDE transaction
     */
    public void sendMentionNotifications(Message message, List<Long> mentions) {
        if (mentions == null || mentions.isEmpty()) return;

        log.debug("Sending mention notifications: messageId={}, mentions={}", message.getId(), mentions);

        User sender = message.getSender();
        Conversation conversation = message.getConversation();

        for (Long mentionedUserId : mentions) {
            MentionEventDto mentionEvent = new MentionEventDto();
            mentionEvent.setMessageId(message.getId());
            mentionEvent.setConversationId(conversation.getId());
            mentionEvent.setSenderId(sender.getId());
            mentionEvent.setSenderName(sender.getFullName());
            mentionEvent.setContent(message.getContent());
            mentionEvent.setMentionedUserId(mentionedUserId);
            mentionEvent.setCreatedAt(message.getCreatedAt());

            WebSocketMessage<MentionEventDto> mentionMessage =
                    new WebSocketMessage<>(WebSocketEventType.MESSAGE_MENTION, mentionEvent);
            chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
        }
    }
}
