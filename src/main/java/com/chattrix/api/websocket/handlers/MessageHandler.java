package com.chattrix.api.websocket.handlers;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class MessageHandler {

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
    private CacheManager cacheManager;
    @Inject
    private MessageCache messageCache;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void handleChatMessage(Long senderId, Object payload) {
        ChatMessageDto dto = objectMapper.convertValue(payload, ChatMessageDto.class);

        // 1. Validate & Lấy dữ liệu
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        Conversation conv = conversationRepository.findByIdWithParticipants(dto.getConversationId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        // Validate sender is participant
        boolean isParticipant = conv.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(senderId));
        if (!isParticipant) {
            throw BusinessException.forbidden("Sender is not a participant of this conversation");
        }

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (dto.getReplyToMessageId() != null) {
            replyToMessage = messageRepository.findById(dto.getReplyToMessageId())
                    .orElseThrow(() -> BusinessException.notFound("Reply to message not found"));

            if (!replyToMessage.getConversation().getId().equals(dto.getConversationId())) {
                throw BusinessException.badRequest("Cannot reply to message from different conversation");
            }
        }

        // Validate mentions if provided
        if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
            List<Long> participantIds = conv.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .toList();

            for (Long mentionedUserId : dto.getMentions()) {
                if (!participantIds.contains(mentionedUserId)) {
                    throw BusinessException.badRequest("Cannot mention user who is not in this conversation");
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

        // Build metadata map directly
        Map<String, Object> metadata = new HashMap<>();
        if (dto.getMediaUrl() != null) metadata.put("mediaUrl", dto.getMediaUrl());
        if (dto.getThumbnailUrl() != null) metadata.put("thumbnailUrl", dto.getThumbnailUrl());
        if (dto.getFileName() != null) metadata.put("fileName", dto.getFileName());
        if (dto.getFileSize() != null) metadata.put("fileSize", dto.getFileSize());
        if (dto.getDuration() != null) metadata.put("duration", dto.getDuration());
        if (dto.getLatitude() != null) metadata.put("latitude", dto.getLatitude());
        if (dto.getLongitude() != null) metadata.put("longitude", dto.getLongitude());
        if (dto.getLocationName() != null) metadata.put("locationName", dto.getLocationName());

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

        WebSocketMessage<OutgoingMessageDto> wsMsg = new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        conv.getParticipants().forEach(p ->
                chatSessionService.sendMessageToUser(p.getUser().getId(), wsMsg)
        );

        // Send mention notifications
        if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
            sendMentionNotifications(newMessage, sender, conv, dto.getMentions());
        }

        // Broadcast conversation update
        broadcastConversationUpdate(conv);
    }

    private void sendMentionNotifications(Message message, User sender, Conversation conv, List<Long> mentions) {
        for (Long mentionedUserId : mentions) {
            MentionEventDto mentionEvent = new MentionEventDto();
            mentionEvent.setMessageId(message.getId());
            mentionEvent.setConversationId(conv.getId());
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
}
