package com.chattrix.api.services.message;

import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.*;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.requests.UpdateMessageRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MediaResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.responses.PaginatedResponse;
import com.chattrix.api.responses.ReadReceiptResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.ConversationUpdateDto;
import com.chattrix.api.websocket.dto.MentionEventDto;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
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
    private UserMapper userMapper;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private WebSocketMapper webSocketMapper;

    @Inject
    private ConversationParticipantRepository participantRepository;

    //     @Inject
    //     private MessageReadReceiptRepository readReceiptRepository;

    //     @Inject
    //     private MessageEditHistoryRepository messageEditHistoryRepository;

    //     @Inject
    //     private PollMapper pollMapper;

    //     @Inject
    //     private EventMapper eventMapper;

    // @Inject
    // private PollRepository pollRepository;

    // @Inject
    // private EventRepository eventRepository;

    @Transactional
    public CursorPaginatedResponse<MessageResponse> getMessages(Long userId, Long conversationId, Long cursor, int limit, String sort) {
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100;
        }

        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        List<Message> messages = messageRepository.findByConversationIdWithCursor(conversationId, cursor, limit, sort);

        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = messages.subList(0, limit);
        }

        List<MessageResponse> responses = messages.stream()
                .map(m -> mapMessageToResponse(m, userId))
                .toList();

        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    @Transactional
    public MessageResponse getMessage(Long userId, Long conversationId, Long messageId) {
        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        // Get specific message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        // Verify message belongs to this conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND");
        }

        return mapMessageToResponse(message, userId);
    }

    @Transactional
    public MessageResponse sendMessage(Long userId, Long conversationId, ChatMessageRequest request) {
        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }
        
        // Check if user is muted (for group conversations)
        if (conversation.getType() == ConversationType.GROUP) {
            ConversationParticipant participant = conversation.getParticipants().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .findFirst()
                    .orElse(null);
            
            if (participant != null && participant.isCurrentlyMuted()) {
                throw BusinessException.forbidden("You are muted in this conversation");
            }
        }

        // Get sender
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (request.replyToMessageId() != null) {
            replyToMessage = messageRepository.findByIdSimple(request.replyToMessageId())
                    .orElseThrow(() -> BusinessException.notFound("Reply to message not found", "RESOURCE_NOT_FOUND"));

            // Verify reply message belongs to same conversation
            if (!replyToMessage.getConversation().getId().equals(conversationId)) {
                throw BusinessException.badRequest("Cannot reply to message from different conversation", "BAD_REQUEST");
            }
        }

        // Validate mentions if provided
        if (request.mentions() != null && !request.mentions().isEmpty()) {
            List<Long> participantIds = conversation.getParticipants().stream()
                    .map(p -> p.getUser().getId())
                    .toList();

            for (Long mentionedUserId : request.mentions()) {
                if (!participantIds.contains(mentionedUserId)) {
                    throw BusinessException.badRequest("Cannot mention user who is not in this conversation", "BAD_REQUEST");
                }
            }
        }

        // Create and save message
        Message message = new Message();
        message.setContent(request.content());
        message.setSender(sender);
        message.setConversation(conversation);

        // Set message type
        MessageType messageType = MessageType.TEXT;
        if (request.type() != null) {
            try {
                messageType = MessageType.valueOf(request.type().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw BusinessException.badRequest("Invalid message type: " + request.type(), "BAD_REQUEST");
            }
        }
        message.setType(messageType);

        // TODO: Set rich media fields using MessageMetadata
        // message.setMediaUrl(request.mediaUrl());
        // message.setThumbnailUrl(request.thumbnailUrl());
        // message.setFileName(request.fileName());
        // message.setFileSize(request.fileSize());
        // message.setDuration(request.duration());

        // TODO: Set location fields using MessageMetadata
        // message.setLatitude(request.latitude());
        // message.setLongitude(request.longitude());
        // message.setLocationName(request.locationName());

        // Set reply and mentions
        message.setReplyToMessage(replyToMessage);
        message.setMentions(request.mentions());

        messageRepository.save(message);

        // Update conversation's lastMessage and updatedAt
        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        // Increment unread count for all participants except the sender
        participantRepository.incrementUnreadCountForOthers(conversationId, userId);

        // Broadcast message to all participants via WebSocket
        broadcastMessage(message, conversation);

        // Broadcast conversation update
        broadcastConversationUpdate(conversation);

        return mapMessageToResponse(message, userId);
    }

    @Transactional
    public MessageResponse updateMessage(Long userId, Long conversationId, Long messageId, UpdateMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Message not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw BusinessException.forbidden("You can only edit your own messages");
        }

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);

        // Broadcast update via WebSocket
        MessageUpdateEventDto payload = MessageUpdateEventDto.builder()
                .messageId(message.getId())
                .conversationId(message.getConversation().getId())
                .content(message.getContent())
                .isEdited(true)
                .updatedAt(message.getUpdatedAt())
                .build();
        WebSocketMessage<MessageUpdateEventDto> wsMessage = new WebSocketMessage<>(WebSocketEventType.MESSAGE_UPDATED, payload);

        message.getConversation().getParticipants().forEach(participant -> {
            chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
        });

        return mapMessageToResponse(message, userId);
    }

    @Transactional
    public void deleteMessage(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Message not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw BusinessException.forbidden("You can only delete your own messages");
        }

        Conversation conversation = message.getConversation();
        boolean wasLastMessage = conversation.getLastMessage() != null &&
                Objects.equals(conversation.getLastMessage().getId(), message.getId());

        // If this is the last message, clear the reference BEFORE deleting
        if (wasLastMessage) {
            conversation.setLastMessage(null);
            conversationRepository.save(conversation);
        }

        // Delete related data first to avoid foreign key constraint violations
        // TODO: Uncomment when MessageReadReceipt and MessageEditHistory entities exist
        // readReceiptRepository.deleteByMessageId(messageId);
        // messageEditHistoryRepository.deleteByMessageId(messageId);

        messageRepository.delete(message);

        // If the deleted message was the last message, update the conversation's last message
        if (wasLastMessage) {
            Message newLastMessage = messageRepository.findLatestByConversationId(conversationId).orElse(null);
            conversation.setLastMessage(newLastMessage);
            conversationRepository.save(conversation);
            broadcastConversationUpdate(conversation);
        }

        // Broadcast deletion via WebSocket
        MessageDeleteEventDto payload = MessageDeleteEventDto.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .build();
        WebSocketMessage<MessageDeleteEventDto> wsMessage = new WebSocketMessage<>(WebSocketEventType.MESSAGE_DELETED, payload);

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
            outgoingDto.setMentionedUsers(userMapper.toMentionedUserResponseList(mentionedUsers));
        }

        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage = new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

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

                WebSocketMessage<MentionEventDto> mentionMessage = new WebSocketMessage<>(WebSocketEventType.MESSAGE_MENTION, mentionEvent);
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

        WebSocketMessage<ConversationUpdateDto> message = new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATE, updateDto);

        // Broadcast to all participants in the conversation
        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, message);
        });
    }

    private MessageResponse mapMessageToResponse(Message message, Long userId) {
        MessageResponse response = messageMapper.toResponse(message);

        // Map mentioned users if mentions exist
        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            List<User> mentionedUsers = userRepository.findByIds(message.getMentions());
            response.setMentionedUsers(userMapper.toMentionedUserResponseList(mentionedUsers));
        }

        // TODO: Populate read receipts when MessageReadReceipt entity exists
        /*
        long readCount = readReceiptRepository.countByMessageId(message.getId());
        response.setReadCount(readCount);

        // Optionally populate readBy list (you can comment this out if it causes performance issues)
        if (readCount > 0) {
            List<MessageReadReceipt> readReceipts = readReceiptRepository.findByMessageId(message.getId());
            List<ReadReceiptResponse> readByList = readReceipts.stream()
                    .map(receipt -> {
                        ReadReceiptResponse readReceiptResponse = new ReadReceiptResponse();
                        readReceiptResponse.setUserId(receipt.getUser().getId());
                        readReceiptResponse.setUsername(receipt.getUser().getUsername());
                        readReceiptResponse.setFullName(receipt.getUser().getFullName());
                        readReceiptResponse.setAvatarUrl(receipt.getUser().getAvatarUrl());
                        readReceiptResponse.setReadAt(receipt.getReadAt());
                        return readReceiptResponse;
                    })
                    .toList();
            response.setReadBy(readByList);
        }
        */

        // TODO: Populate Poll details when Poll entity exists
        /*
        if (message.getType() == MessageType.POLL && message.getPoll() != null) {
            pollRepository.initializePoll(message.getPoll());
            response.setPoll(pollMapper.toResponseWithDetails(message.getPoll(), userId, userMapper));
        }
        */

        // TODO: Populate Event details when Event entity exists
        /*
        if (message.getType() == MessageType.EVENT && message.getEvent() != null) {
            response.setEvent(enrichEventResponse(message.getEvent(), userId));
        }
        */

        return response;
    }

    // TODO: Uncomment when Event entity is created
    /*
    private com.chattrix.api.responses.EventResponse enrichEventResponse(com.chattrix.api.entities.Event event, Long userId) {
        com.chattrix.api.responses.EventResponse response = eventMapper.toResponse(event);

        // Calculate RSVP counts
        long goingCount = event.getRsvps().stream()
                .filter(r -> r.getStatus() == com.chattrix.api.entities.EventRsvp.RsvpStatus.GOING)
                .count();
        long maybeCount = event.getRsvps().stream()
                .filter(r -> r.getStatus() == com.chattrix.api.entities.EventRsvp.RsvpStatus.MAYBE)
                .count();
        long notGoingCount = event.getRsvps().stream()
                .filter(r -> r.getStatus() == com.chattrix.api.entities.EventRsvp.RsvpStatus.NOT_GOING)
                .count();

        response.setGoingCount((int) goingCount);
        response.setMaybeCount((int) maybeCount);
        response.setNotGoingCount((int) notGoingCount);

        // Set current user's RSVP status
        event.getRsvps().stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(r -> response.setCurrentUserRsvpStatus(r.getStatus().name()));

        // Map RSVPs
        List<com.chattrix.api.responses.EventRsvpResponse> rsvpResponses = event.getRsvps().stream()
                .map(eventMapper::toRsvpResponse)
                .collect(java.util.stream.Collectors.toList());
        response.setRsvps(rsvpResponses);

        return response;
    }
    */

    // ==================== CHAT INFO METHODS ====================

    @Transactional
    public CursorPaginatedResponse<MessageResponse> searchMessages(Long userId, Long conversationId, String query, String type, Long senderId, Long cursor, int limit, String sort) {
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100;
        }

        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        List<Message> messages = messageRepository.searchMessagesByCursor(conversationId, query, type, senderId, cursor, limit, sort);

        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = messages.subList(0, limit);
        }

        List<MessageResponse> messageResponses = messages.stream()
                .map(m -> mapMessageToResponse(m, userId))
                .toList();

        Long nextCursor = null;
        if (hasMore && !messageResponses.isEmpty()) {
            nextCursor = messageResponses.get(messageResponses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(messageResponses, nextCursor, limit);
    }

    public CursorPaginatedResponse<MediaResponse> getMediaFiles(Long userId, Long conversationId, String type, String startDate, String endDate, Long cursor, int limit) {
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100;
        }

        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        Instant start = null;
        Instant end = null;
        try {
            if (startDate != null && !startDate.isEmpty()) {
                start = Instant.parse(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                end = Instant.parse(endDate);
            }
        } catch (Exception e) {
            throw BusinessException.badRequest("Invalid date format. Use ISO-8601 (e.g., 2023-01-01T00:00:00Z)", "INVALID_DATE");
        }

        List<Message> messages = messageRepository.findMediaByCursor(conversationId, type, start, end, cursor, limit);

        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = messages.subList(0, limit);
        }

        List<MediaResponse> mediaResponses = messages.stream()
                .map(this::mapMessageToMediaResponse)
                .toList();

        Long nextCursor = null;
        if (hasMore && !mediaResponses.isEmpty()) {
            nextCursor = mediaResponses.get(mediaResponses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(mediaResponses, nextCursor, limit);
    }

    private MediaResponse mapMessageToMediaResponse(Message message) {
        MediaResponse response = new MediaResponse();
        response.setId(message.getId());
        response.setType(message.getType().name());
        // response.setMediaUrl(message.getMediaUrl()); // TODO: Use metadata
        // response.setThumbnailUrl(message.getThumbnailUrl()); // TODO: Use metadata
        // response.setFileName(message.getFileName()); // TODO: Use metadata
        // response.setFileSize(message.getFileSize()); // TODO: Use metadata
        // response.setDuration(message.getDuration()); // TODO: Use metadata
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setSentAt(message.getSentAt());
        return response;
    }
}
