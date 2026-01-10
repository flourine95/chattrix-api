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
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.conversation.GroupPermissionsService;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.utils.PaginationHelper;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class MessageService {

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
    private MessageCreationService messageCreationService;

    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private GroupPermissionsService groupPermissionsService;


    @Transactional
    public CursorPaginatedResponse<MessageResponse> getMessages(Long userId, Long conversationId, Long cursor, int limit, String sort) {
        limit = PaginationHelper.validateLimit(limit);

        validateAndGetConversation(conversationId, userId);

        // Write-Behind: Merge unflushed cache + flushed DB messages

        // 1. Get unflushed messages from cache (not yet in DB)
        List<MessageResponse> unflushedMessages = messageCache.getUnflushed(conversationId);

        // 2. Get flushed messages from DB as entities
        List<Message> flushedEntities = 
            messageRepository.findByConversationIdWithCursor(conversationId, cursor, limit, sort);

        // 3. Map entities to DTOs using MapStruct
        List<MessageResponse> flushedResponses = flushedEntities.stream()
                .map(messageMapper::toResponse)
                .toList();

        // 4. Merge: unflushed first (newest), then flushed
        List<MessageResponse> allMessages = new ArrayList<>();
        allMessages.addAll(unflushedMessages);
        allMessages.addAll(flushedResponses);

        // 5. Sort by sentAt descending (newest first)
        allMessages.sort(Comparator.comparing(MessageResponse::getSentAt).reversed());

        // 6. Apply cursor filter if provided
        if (cursor != null) {
            allMessages = allMessages.stream()
                    .filter(msg -> msg.getId() < cursor)
                    .toList();
        }

        // 7. Apply limit + 1 to check hasMore
        boolean hasMore = allMessages.size() > limit;
        if (hasMore)
            allMessages = allMessages.subList(0, limit);

        // 8. Calculate next cursor
        Long nextCursor = hasMore && !allMessages.isEmpty()
                ? allMessages.get(allMessages.size() - 1).getId()
                : null;

        return new CursorPaginatedResponse<>(allMessages, nextCursor, limit);
    }

    @Transactional
    public MessageResponse getMessage(Long userId, Long conversationId, Long messageId) {
        validateAndGetConversation(conversationId, userId);
        Message message = validateAndGetMessage(messageId, conversationId);
        return messageMapper.toResponse(message);
    }

    @Transactional
    public MessageResponse sendMessage(Long userId, Long conversationId, ChatMessageRequest request) {
        log.debug("REST API: Sending message from user {} to conversation {}", userId, conversationId);

        // Use centralized MessageCreationService without Write-Behind for REST API
        // Write-Behind is only for WebSocket to improve performance
        MessageResponse response = messageCreationService.createMessage(
                userId,
                conversationId,
                request.content(),
                request.type(),
                request.metadata(),
                request.replyToMessageId(),
                request.mentions(),
                false  // Disable Write-Behind for REST API to ensure immediate persistence
        );

        // Broadcast outside transaction (async)
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));
        
        // Reconstruct message entity for broadcasting (temp ID)
        Message message = new Message();
        message.setId(response.getId());
        message.setContent(response.getContent());
        message.setSentAt(response.getSentAt());
        message.setType(MessageType.valueOf(response.getType()));
        message.setConversation(conversation);
        
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        message.setSender(sender);
        
        if (request.metadata() != null)
            message.setMetadata(new HashMap<>(request.metadata()));
        
        messageCreationService.broadcastMessage(message, conversation);
        messageCreationService.broadcastConversationUpdate(conversation);
        
        if (request.mentions() != null && !request.mentions().isEmpty()) {
            messageCreationService.sendMentionNotifications(message, request.mentions());
        }

        return response;
    }

    @Transactional
    public MessageResponse updateMessage(Long userId, Long conversationId, Long messageId, UpdateMessageRequest request) {
        Message message = validateAndGetMessage(messageId, conversationId);
        validateMessageOwnership(message, userId);

        String oldContent = message.getContent();
        String newContent = request.getContent();

        if (!oldContent.equals(newContent)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> editHistory = (List<Map<String, Object>>) message.getMetadata().get("editHistory");
            if (editHistory == null) editHistory = new ArrayList<>();

            Map<String, Object> editRecord = new HashMap<>();
            editRecord.put("oldContent", oldContent);
            editRecord.put("newContent", newContent);
            editRecord.put("editedAt", Instant.now().toString());
            editRecord.put("editedBy", userId);

            editHistory.add(editRecord);
            message.getMetadata().put("editHistory", editHistory);
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        message.setUpdatedAt(Instant.now());
        messageRepository.save(message);

        invalidateCaches(conversationId, message.getConversation().getParticipantIds());

        MessageUpdateEventDto payload = MessageUpdateEventDto.builder()
                .messageId(message.getId())
                .conversationId(message.getConversation().getId())
                .content(message.getContent())
                .isEdited(true)
                .updatedAt(message.getUpdatedAt())
                .build();
        WebSocketMessage<MessageUpdateEventDto> wsMessage = new WebSocketMessage<>(WebSocketEventType.MESSAGE_UPDATED, payload);

        message.getConversation().getParticipants()
                .forEach(p -> chatSessionService.sendMessageToUser(p.getUser().getId(), wsMessage));

        return messageMapper.toResponse(message);
    }

    @Transactional
    public List<Map<String, Object>> getEditHistory(Long userId, Long conversationId, Long messageId) {
        Message message = validateAndGetMessage(messageId, conversationId);

        if (!message.getConversation().isUserParticipant(userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> editHistory = (List<Map<String, Object>>) message.getMetadata().get("editHistory");

        return editHistory != null ? editHistory : new ArrayList<>();
    }

    @Transactional
    public void deleteMessage(Long userId, Long conversationId, Long messageId) {
        Message message = validateAndGetMessage(messageId, conversationId);
        validateMessageOwnership(message, userId);

        Conversation conversation = message.getConversation();
        boolean wasLastMessage = conversation.getLastMessage() != null &&
                Objects.equals(conversation.getLastMessage().getId(), message.getId());

        if (wasLastMessage) {
            conversation.setLastMessage(null);
            conversationRepository.save(conversation);
        }

        messageRepository.delete(message);

        if (wasLastMessage) {
            Message newLastMessage = messageRepository.findLatestByConversationId(conversationId).orElse(null);
            conversation.setLastMessage(newLastMessage);
            conversationRepository.save(conversation);
            broadcastConversationUpdate(conversation);
        }

        invalidateCaches(conversationId, conversation.getParticipantIds());

        MessageDeleteEventDto payload = MessageDeleteEventDto.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .build();
        WebSocketMessage<MessageDeleteEventDto> wsMessage = new WebSocketMessage<>(WebSocketEventType.MESSAGE_DELETED, payload);

        conversation.getParticipants()
                .forEach(p -> chatSessionService.sendMessageToUser(p.getUser().getId(), wsMessage));
    }

    // ==================== CHAT INFO METHODS ====================

    @Transactional
    public CursorPaginatedResponse<MessageResponse> searchMessages(Long userId, Long conversationId, String query, String type, Long senderId, Long cursor, int limit, String sort) {
        limit = PaginationHelper.validateLimit(limit);

        validateUserIsParticipant(conversationId, userId);

        List<Message> messages = messageRepository.searchMessagesByCursor(conversationId, query, type, senderId, cursor, limit, sort);

        return PaginationHelper.buildResponse(messages, limit, messageMapper::toResponse, MessageResponse::getId);
    }

    public CursorPaginatedResponse<MessageResponse> getMediaFiles(Long userId, Long conversationId, String type, String startDate, String endDate, Long cursor, int limit) {
        limit = PaginationHelper.validateLimit(limit);

        validateUserIsParticipant(conversationId, userId);

        Instant start = null;
        Instant end = null;
        try {
            if (startDate != null && !startDate.isEmpty()) start = Instant.parse(startDate);
            if (endDate != null && !endDate.isEmpty()) end = Instant.parse(endDate);
        } catch (Exception e) {
            throw BusinessException.badRequest("Invalid date format. Use ISO-8601 (e.g., 2023-01-01T00:00:00Z)", "INVALID_DATE");
        }

        List<Message> messages = messageRepository.findMediaByCursor(conversationId, type, start, end, cursor, limit);

        return PaginationHelper.buildResponse(messages, limit, messageMapper::toResponse, MessageResponse::getId);
    }

    // ==================== POLL METHODS ====================

    @Transactional
    public MessageResponse createPoll(Long userId, Long conversationId, CreatePollRequest request) {
        log.debug("Creating poll in conversation {} by user {}", conversationId, userId);

        Conversation conversation = validateAndGetConversation(conversationId, userId);
        validateGroupPermission(conversation, userId, "create_polls");

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // Build poll options
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < request.getOptions().size(); i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("id", (long) i);
            option.put("text", request.getOptions().get(i));
            option.put("votes", new ArrayList<Long>());
            options.add(option);
        }

        // Build poll metadata
        Map<String, Object> pollData = new HashMap<>();
        pollData.put("question", request.getQuestion());
        pollData.put("options", options);
        pollData.put("allowMultiple", request.getAllowMultipleVotes() != null ? request.getAllowMultipleVotes() : false);
        pollData.put("anonymous", false);
        if (request.getExpiresAt() != null)
            pollData.put("closesAt", request.getExpiresAt().toString());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("poll", pollData);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getQuestion())
                .type(MessageType.POLL)
                .metadata(metadata)
                .build();

        messageRepository.save(message);
        log.info("Poll created: messageId={}, conversationId={}", message.getId(), conversationId);

        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        participantRepository.incrementUnreadCountForOthers(conversationId, userId);
        invalidateCaches(conversationId, conversation.getParticipantIds());

        MessageResponse response = messageMapper.toResponse(message);
        broadcastMessage(message, conversation);
        broadcastConversationUpdate(conversation);

        return response;
    }

    @Transactional
    public MessageResponse votePoll(Long userId, Long conversationId, Long messageId, VotePollRequest request) {
        log.debug("User {} voting on poll {} with options {}", userId, messageId, request.getOptionIds());

        Message message = validateAndGetMessage(messageId, conversationId);

        if (!message.isPollMessage())
            throw BusinessException.badRequest("Message is not a poll", "INVALID_MESSAGE_TYPE");

        validateUserIsParticipant(conversationId, userId);

        @SuppressWarnings("unchecked")
        Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");

        if (pollData == null)
            throw BusinessException.badRequest("Poll data not found", "INVALID_POLL");

        // Check if poll is closed
        if (pollData.containsKey("closesAt") && pollData.get("closesAt") != null) {
            Instant closesAt = Instant.parse(pollData.get("closesAt").toString());
            if (Instant.now().isAfter(closesAt))
                throw BusinessException.badRequest("Poll is closed", "POLL_CLOSED");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");

        // Validate option IDs
        Set<Long> validOptionIds = options.stream()
                .map(opt -> ((Number) opt.get("id")).longValue())
                .collect(Collectors.toSet());

        for (Long optionId : request.getOptionIds())
            if (!validOptionIds.contains(optionId))
                throw BusinessException.badRequest("Invalid option ID: " + optionId, "INVALID_OPTION");

        // Check multiple votes
        Boolean allowMultiple = (Boolean) pollData.get("allowMultiple");
        if (!Boolean.TRUE.equals(allowMultiple) && request.getOptionIds().size() > 1)
            throw BusinessException.badRequest("Multiple votes not allowed", "MULTIPLE_VOTES_NOT_ALLOWED");

        // Remove user's previous votes
        for (Map<String, Object> option : options) {
            @SuppressWarnings("unchecked")
            List<Long> votes = (List<Long>) option.get("votes");
            votes.remove(userId);
        }

        // Add new votes
        for (Long optionId : request.getOptionIds()) {
            for (Map<String, Object> option : options) {
                if (((Number) option.get("id")).longValue() == optionId) {
                    @SuppressWarnings("unchecked")
                    List<Long> votes = (List<Long>) option.get("votes");
                    if (!votes.contains(userId))
                        votes.add(userId);
                }
            }
        }

        messageRepository.save(message);
        log.info("Poll vote recorded: messageId={}, userId={}, options={}", messageId, userId, request.getOptionIds());

        messageCache.invalidate(conversationId);

        MessageResponse response = messageMapper.toResponse(message);
        broadcastMessage(message, message.getConversation());

        return response;
    }

    // ==================== EVENT METHODS ====================

    @Transactional
    public MessageResponse createEvent(Long userId, Long conversationId, CreateEventRequest request) {
        log.debug("Creating event in conversation {} by user {}", conversationId, userId);

        Conversation conversation = validateAndGetConversation(conversationId, userId);
        // Events don't have specific permission - any participant can create
        // validateGroupPermission(conversation, userId, "create_events");

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // Build event metadata
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", request.getTitle());
        eventData.put("description", request.getDescription());
        eventData.put("startTime", request.getStartTime().toString());
        eventData.put("endTime", request.getEndTime().toString());
        eventData.put("location", request.getLocation());
        eventData.put("going", new ArrayList<Long>());
        eventData.put("maybe", new ArrayList<Long>());
        eventData.put("notGoing", new ArrayList<Long>());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("event", eventData);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getTitle())
                .type(MessageType.EVENT)
                .metadata(metadata)
                .build();

        messageRepository.save(message);
        log.info("Event created: messageId={}, conversationId={}", message.getId(), conversationId);

        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        participantRepository.incrementUnreadCountForOthers(conversationId, userId);
        invalidateCaches(conversationId, conversation.getParticipantIds());

        MessageResponse response = messageMapper.toResponse(message);
        broadcastMessage(message, conversation);
        broadcastConversationUpdate(conversation);

        return response;
    }

    @Transactional
    public MessageResponse respondToEvent(Long userId, Long conversationId, Long messageId, EventRsvpRequest request) {
        log.debug("User {} responding to event {} with status {}", userId, messageId, request.getStatus());

        Message message = validateAndGetMessage(messageId, conversationId);

        if (!message.isEventMessage())
            throw BusinessException.badRequest("Message is not an event", "INVALID_MESSAGE_TYPE");

        validateUserIsParticipant(conversationId, userId);

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) message.getMetadata().get("event");

        if (eventData == null)
            throw BusinessException.badRequest("Event data not found", "INVALID_EVENT");

        @SuppressWarnings("unchecked")
        List<Long> going = (List<Long>) eventData.get("going");
        @SuppressWarnings("unchecked")
        List<Long> maybe = (List<Long>) eventData.get("maybe");
        @SuppressWarnings("unchecked")
        List<Long> notGoing = (List<Long>) eventData.get("notGoing");

        // Remove user from all lists
        going.remove(userId);
        maybe.remove(userId);
        notGoing.remove(userId);

        // Add user to appropriate list
        switch (request.getStatus()) {
            case "GOING":
                if (!going.contains(userId)) going.add(userId);
                break;
            case "MAYBE":
                if (!maybe.contains(userId)) maybe.add(userId);
                break;
            case "NOT_GOING":
                if (!notGoing.contains(userId)) notGoing.add(userId);
                break;
            default:
                throw BusinessException.badRequest("Invalid RSVP status", "INVALID_STATUS");
        }

        messageRepository.save(message);
        log.info("Event RSVP recorded: messageId={}, userId={}, status={}", messageId, userId, request.getStatus());

        messageCache.invalidate(conversationId);

        MessageResponse response = messageMapper.toResponse(message);
        broadcastMessage(message, message.getConversation());

        return response;
    }

    // ==================== FORWARD MESSAGE ====================

    @Transactional
    public List<MessageResponse> forwardMessage(Long userId, Long conversationId, Long messageId, List<Long> conversationIds) {
        log.debug("User {} forwarding message {} to {} conversations", userId, messageId, conversationIds.size());

        Message originalMessage = validateAndGetMessage(messageId, conversationId);

        if (originalMessage.isDeleted())
            throw BusinessException.badRequest("Cannot forward deleted message");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        List<MessageResponse> forwardedMessages = new ArrayList<>();

        for (Long targetConversationId : conversationIds) {
            Conversation conversation = conversationRepository.findByIdWithParticipants(targetConversationId)
                    .orElseThrow(() -> BusinessException.notFound("Conversation not found: " + targetConversationId));

            validateUserIsParticipant(targetConversationId, userId);

            // Copy metadata from original message
            Map<String, Object> metadata = new HashMap<>(originalMessage.getMetadata());

            // Add forward info to metadata
            Map<String, Object> forwardInfo = new HashMap<>();
            forwardInfo.put("conversationId", originalMessage.getConversation().getId());
            forwardInfo.put("messageId", originalMessage.getId());
            forwardInfo.put("originalSenderId", originalMessage.getSender().getId());
            forwardInfo.put("originalSenderUsername", originalMessage.getSender().getUsername());
            metadata.put("forwardedFrom", forwardInfo);

            Message forwardedMessage = Message.builder()
                    .conversation(conversation)
                    .sender(user)
                    .content(originalMessage.getContent())
                    .type(originalMessage.getType())
                    .metadata(metadata)
                    .forwarded(true)
                    .originalMessage(originalMessage)
                    .build();

            messageRepository.save(forwardedMessage);
            log.info("Message forwarded: originalId={}, newId={}, toConversation={}", messageId, forwardedMessage.getId(), targetConversationId);

            participantRepository.incrementUnreadCountForOthers(targetConversationId, userId);

            conversation.setLastMessage(forwardedMessage);
            conversationRepository.save(conversation);

            invalidateCaches(targetConversationId, conversation.getParticipantIds());

            MessageResponse response = messageMapper.toResponse(forwardedMessage);
            broadcastMessage(forwardedMessage, conversation);
            broadcastConversationUpdate(conversation);

            forwardedMessages.add(response);
        }

        // Update forward count on original message
        originalMessage.setForwardCount(
                originalMessage.getForwardCount() != null
                        ? originalMessage.getForwardCount() + conversationIds.size()
                        : conversationIds.size()
        );
        messageRepository.save(originalMessage);

        return forwardedMessages;
    }

    // ==================== HELPER METHODS ====================

    private Conversation validateAndGetConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        return conversation;
    }

    private Message validateAndGetMessage(Long messageId, Long conversationId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.belongsToConversation(conversationId))
            throw BusinessException.badRequest("Message does not belong to this conversation");

        return message;
    }

    private void validateMessageOwnership(Message message, Long userId) {
        if (!message.isOwnedBy(userId))
            throw BusinessException.forbidden("You can only modify your own messages");
    }

    private void validateUserIsParticipant(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");
    }

    private void validateGroupPermission(Conversation conversation, Long userId, String permission) {
        if (!conversation.isGroupConversation())
            return;

        if (!groupPermissionsService.hasPermission(conversation.getId(), userId, permission))
            throw BusinessException.forbidden("You do not have permission to " + permission.replace("_", " "));
    }

    private void invalidateCaches(Long conversationId, Set<Long> participantIds) {
        messageCache.invalidate(conversationId);
        cacheManager.invalidateConversationCaches(conversationId, participantIds);
    }

    private void broadcastMessage(Message message, Conversation conversation) {
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);

        WebSocketMessage<OutgoingMessageDto> outgoingWebSocketMessage =
                new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        conversation.getParticipants()
                .forEach(p -> chatSessionService.sendMessageToUser(p.getUser().getId(), outgoingWebSocketMessage));

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

                WebSocketMessage<MentionEventDto> mentionMessage =
                        new WebSocketMessage<>(WebSocketEventType.MESSAGE_MENTION, mentionEvent);
                chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
            }
        }
    }

    private void broadcastConversationUpdate(Conversation conversation) {
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

        conversation.getParticipants()
                .forEach(p -> chatSessionService.sendMessageToUser(p.getUser().getId(), message));
    }
}
