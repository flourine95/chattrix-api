package com.chattrix.api.websocket.handlers;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.message.MessageCreationService;
import com.chattrix.api.websocket.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class MessageHandler {

    @Inject
    private UserRepository userRepository;
    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private MessageCreationService messageCreationService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void handleChatMessage(Long senderId, Object payload) {
        ChatMessageDto dto = objectMapper.convertValue(payload, ChatMessageDto.class);
        
        log.debug("WebSocket: Handling message from user {} to conversation {}", senderId, dto.getConversationId());

        // Build metadata from DTO
        Map<String, Object> metadata = new HashMap<>();
        if (dto.getMediaUrl() != null) metadata.put("mediaUrl", dto.getMediaUrl());
        if (dto.getThumbnailUrl() != null) metadata.put("thumbnailUrl", dto.getThumbnailUrl());
        if (dto.getFileName() != null) metadata.put("fileName", dto.getFileName());
        if (dto.getFileSize() != null) metadata.put("fileSize", dto.getFileSize());
        if (dto.getDuration() != null) metadata.put("duration", dto.getDuration());
        if (dto.getLatitude() != null) metadata.put("latitude", dto.getLatitude());
        if (dto.getLongitude() != null) metadata.put("longitude", dto.getLongitude());
        if (dto.getLocationName() != null) metadata.put("locationName", dto.getLocationName());

        // Use centralized MessageCreationService WITH Write-Behind
        // Write-Behind improves performance by batching database writes
        MessageResponse response = messageCreationService.createMessage(
                senderId,
                dto.getConversationId(),
                dto.getContent(),
                dto.getType(),
                metadata.isEmpty() ? null : metadata,
                dto.getReplyToMessageId(),
                dto.getMentions(),
                true  // Enable Write-Behind for better performance
        );

        // Broadcast outside transaction
        Conversation conversation = conversationRepository.findByIdWithParticipants(dto.getConversationId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        
        // Reconstruct message entity for broadcasting (with temp ID)
        Message message = new Message();
        message.setId(response.getId());  // This is temp ID (negative) when Write-Behind enabled
        message.setContent(response.getContent());
        message.setSentAt(response.getSentAt());
        message.setType(MessageType.valueOf(response.getType()));
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMetadata(metadata);
        message.setMentions(dto.getMentions());
        
        // Set reply to message if provided
        if (dto.getReplyToMessageId() != null && response.getReplyToMessage() != null) {
            Message replyToMessage = new Message();
            replyToMessage.setId(response.getReplyToMessage().getId());
            replyToMessage.setContent(response.getReplyToMessage().getContent());
            replyToMessage.setType(MessageType.valueOf(response.getReplyToMessage().getType()));
            
            User replySender = new User();
            replySender.setId(response.getReplyToMessage().getSenderId());
            replySender.setUsername(response.getReplyToMessage().getSenderUsername());
            replySender.setFullName(response.getReplyToMessage().getSenderFullName());
            replyToMessage.setSender(replySender);
            
            message.setReplyToMessage(replyToMessage);
        }
        
        messageCreationService.broadcastMessage(message, conversation);
        
        // Broadcast conversation update with temp lastMessage immediately
        // This allows clients to see lastMessage right away (with temp ID)
        // Will be updated again after flush with real ID
        messageCreationService.broadcastConversationUpdateWithTempMessage(conversation, message);
        
        if (dto.getMentions() != null && !dto.getMentions().isEmpty()) {
            messageCreationService.sendMentionNotifications(message, dto.getMentions());
        }
        
        log.info("WebSocket message sent successfully: messageId={}, conversationId={}", 
                response.getId(), dto.getConversationId());
    }
}
