package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ForbiddenException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.requests.UpdateMessageRequest;
import com.chattrix.api.responses.MediaResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.ConversationUpdateDto;
import com.chattrix.api.websocket.dto.MentionEventDto;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class MessageService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private MessageMapper messageMapper;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private WebSocketMapper webSocketMapper;

    @Inject
    private ConversationParticipantRepository participantRepository;

    public List<MessageResponse> getMessages(Long userId, Long conversationId, int page, int size, String sort) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdWithSort(conversationId, page, size, sort);
        return messages.stream()
                .map(this::mapMessageToResponse)
                .toList();
    }

    public MessageResponse getMessage(Long userId, Long conversationId, Long messageId) {
        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // Get specific message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Verify message belongs to this conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found");
        }

        return mapMessageToResponse(message);
    }

    @Transactional
    public MessageResponse sendMessage(Long userId, Long conversationId, ChatMessageRequest request) {
        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // Get sender
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (request.replyToMessageId() != null) {
            replyToMessage = messageRepository.findByIdSimple(request.replyToMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reply to message not found"));

            // Verify reply message belongs to same conversation
            if (!replyToMessage.getConversation().getId().equals(conversationId)) {
                throw new BadRequestException("Cannot reply to message from different conversation");
            }
        }

        // Validate mentions if provided
        if (request.mentions() != null && !request.mentions().isEmpty()) {
            List<Long> participantIds = conversation.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .toList();

            for (Long mentionedUserId : request.mentions()) {
                if (!participantIds.contains(mentionedUserId)) {
                    throw new BadRequestException("Cannot mention user who is not in this conversation");
                }
            }
        }

        // Create and save message
        Message message = new Message();
        message.setContent(request.content());
        message.setSender(sender);
        message.setConversation(conversation);

        // Set message type
        Message.MessageType messageType = Message.MessageType.TEXT;
        if (request.type() != null) {
            try {
                messageType = Message.MessageType.valueOf(request.type().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid message type: " + request.type());
            }
        }
        message.setType(messageType);

        // Set rich media fields
        message.setMediaUrl(request.mediaUrl());
        message.setThumbnailUrl(request.thumbnailUrl());
        message.setFileName(request.fileName());
        message.setFileSize(request.fileSize());
        message.setDuration(request.duration());

        // Set location fields
        message.setLatitude(request.latitude());
        message.setLongitude(request.longitude());
        message.setLocationName(request.locationName());

        // Set reply and mentions
        message.setReplyToMessage(replyToMessage);
        message.setMentions(request.mentions());

        messageRepository.save(message);

        // Update conversation's lastMessage and updatedAt
        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        // Broadcast message to all participants via WebSocket
        broadcastMessage(message, conversation);

        // Broadcast conversation update
        broadcastConversationUpdate(conversation);

        return mapMessageToResponse(message);
    }

    @Transactional
    public MessageResponse updateMessage(Long userId, Long conversationId, Long messageId, UpdateMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in this conversation");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own messages");
        }

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);

        // Broadcast update via WebSocket
        Map<String, Object> payload = Map.of(
                "messageId", message.getId(),
                "conversationId", message.getConversation().getId(),
                "content", message.getContent(),
                "isEdited", true,
                "updatedAt", message.getUpdatedAt().toString()
        );
        WebSocketMessage<Map<String, Object>> wsMessage = new WebSocketMessage<>("message.updated", payload);

        message.getConversation().getParticipants().forEach(participant -> {
            chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
        });

        return mapMessageToResponse(message);
    }

    @Transactional
    public void deleteMessage(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in this conversation");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own messages");
        }

        Conversation conversation = message.getConversation();
        boolean wasLastMessage = Objects.equals(conversation.getLastMessage().getId(), message.getId());

        messageRepository.delete(message);

        // If the deleted message was the last message, update the conversation's last message
        if (wasLastMessage) {
            Message newLastMessage = messageRepository.findLatestByConversationId(conversationId).orElse(null);
            conversation.setLastMessage(newLastMessage);
            conversationRepository.save(conversation);
            broadcastConversationUpdate(conversation);
        }

        // Broadcast deletion via WebSocket
        Map<String, Object> payload = Map.of(
                "messageId", messageId,
                "conversationId", conversationId
        );
        WebSocketMessage<Map<String, Object>> wsMessage = new WebSocketMessage<>("message.deleted", payload);

        conversation.getParticipants().forEach(participant -> {
            chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
        });
    }


    private void broadcastMessage(Message message, Conversation conversation) {
        // Prepare the outgoing message DTO using mapper
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);

        // Populate mentioned users if mentions exist
        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            List<User> mentionedUsers = userRepository.findByIds(message.getMentions());
            outgoingDto.setMentionedUsers(messageMapper.toMentionedUserResponseList(mentionedUsers));
        }

        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage = new WebSocketMessage<>("chat.message", outgoingDto);

        // Broadcast the message to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, outgoingWebSocketMessage);
        });

        // Send mention notifications to mentioned users
        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            for (Long mentionedUserId : message.getMentions()) {
                MentionEventDto mentionEvent = new MentionEventDto();
                mentionEvent.setMessageId(message.getId());
                mentionEvent.setConversationId(conversation.getId());
                mentionEvent.setSenderId(message.getSender().getId());
                mentionEvent.setSenderName(message.getSender().getFullName());
                mentionEvent.setContent(message.getContent());
                mentionEvent.setMentionedUserId(mentionedUserId);
                mentionEvent.setCreatedAt(message.getCreatedAt());

                WebSocketMessage<MentionEventDto> mentionMessage = new WebSocketMessage<>("message.mention", mentionEvent);
                chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
            }
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

    private MessageResponse mapMessageToResponse(Message message) {
        MessageResponse response = messageMapper.toResponse(message);

        // Map mentioned users if mentions exist
        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            List<User> mentionedUsers = userRepository.findByIds(message.getMentions());
            response.setMentionedUsers(messageMapper.toMentionedUserResponseList(mentionedUsers));
        }

        return response;
    }

    // ==================== CHAT INFO METHODS ====================

    public Map<String, Object> searchMessages(Long userId, Long conversationId, String query, String type, Long senderId, int page, int size, String sort) {
        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        List<Message> messages = messageRepository.searchMessages(conversationId, query, type, senderId, page, size, sort);
        long totalElements = messageRepository.countSearchMessages(conversationId, query, type, senderId);
        long totalPages = (totalElements + size - 1) / size;

        List<MessageResponse> messageResponses = messages.stream()
                .map(this::mapMessageToResponse)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("data", messageResponses);
        result.put("pagination", Map.of(
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
        ));

        return result;
    }

    public Map<String, Object> getMediaFiles(Long userId, Long conversationId, String type, int page, int size) {
        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        List<Message> messages = messageRepository.findMediaByConversationId(conversationId, type, page, size);
        long totalElements = messageRepository.countMediaByConversationId(conversationId, type);
        long totalPages = (totalElements + size - 1) / size;

        List<MediaResponse> mediaResponses = messages.stream()
                .map(this::mapMessageToMediaResponse)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("data", mediaResponses);
        result.put("pagination", Map.of(
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
        ));

        return result;
    }

    private MediaResponse mapMessageToMediaResponse(Message message) {
        MediaResponse response = new MediaResponse();
        response.setId(message.getId());
        response.setType(message.getType().name());
        response.setMediaUrl(message.getMediaUrl());
        response.setThumbnailUrl(message.getThumbnailUrl());
        response.setFileName(message.getFileName());
        response.setFileSize(message.getFileSize());
        response.setDuration(message.getDuration());
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setSentAt(message.getSentAt());
        return response;
    }
}
