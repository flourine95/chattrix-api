package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.*;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MediaResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.conversation.GroupPermissionsService;
import com.chattrix.api.services.notification.ChatSessionService;
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

    @Inject
    private MessageCache messageCache;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private GroupPermissionsService groupPermissionsService;

    @Transactional
    public CursorPaginatedResponse<MessageResponse> getMessages(Long userId, Long conversationId, Long cursor, int limit, String sort) {
        if (limit < 1) throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        if (limit > 100) limit = 100;

        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId).orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId)) throw BusinessException.badRequest("You do not have access to this conversation");

        List<Message> messages = messageRepository.findByConversationIdWithCursor(conversationId, cursor, limit, sort);

        boolean hasMore = messages.size() > limit;
        if (hasMore) messages = messages.subList(0, limit);

        List<MessageResponse> responses = messages.stream()
                .map(m -> mapMessageToResponse(m, userId))
                .toList();

        Long nextCursor = hasMore && !responses.isEmpty() ? responses.get(responses.size() - 1).getId() : null;

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    @Transactional
    public MessageResponse getMessage(Long userId, Long conversationId, Long messageId) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId))
            throw BusinessException.badRequest("You do not have access to this conversation");

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found");

        return mapMessageToResponse(message, userId);
    }

    @Transactional
    public MessageResponse sendMessage(Long userId, Long conversationId, ChatMessageRequest request) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId))
            throw BusinessException.badRequest("You do not have access to this conversation");

        // Check if user is muted (for group conversations)
        if (conversation.getType() == ConversationType.GROUP) {
            ConversationParticipant participant = conversation.getParticipant(userId).orElse(null);
            if (participant != null && participant.isCurrentlyMuted())
                throw BusinessException.forbidden("You are muted in this conversation");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (request.replyToMessageId() != null) {
            replyToMessage = messageRepository.findByIdSimple(request.replyToMessageId())
                    .orElseThrow(() -> BusinessException.notFound("Reply to message not found"));

            if (!replyToMessage.getConversation().getId().equals(conversationId))
                throw BusinessException.badRequest("Cannot reply to message from different conversation");
        }

        // Validate mentions if provided
        if (request.mentions() != null && !request.mentions().isEmpty()) {
            Set<Long> participantIds = conversation.getParticipantIds();
            for (Long mentionedUserId : request.mentions())
                if (!participantIds.contains(mentionedUserId))
                    throw BusinessException.badRequest("Cannot mention user who is not in this conversation");
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
                throw BusinessException.badRequest("Invalid message type: " + request.type());
            }
        }
        message.setType(messageType);

        // Build metadata from request fields
        Map<String, Object> metadata = new HashMap<>();
        if (request.mediaUrl() != null) metadata.put("mediaUrl", request.mediaUrl());
        if (request.thumbnailUrl() != null) metadata.put("thumbnailUrl", request.thumbnailUrl());
        if (request.fileName() != null) metadata.put("fileName", request.fileName());
        if (request.fileSize() != null) metadata.put("fileSize", request.fileSize());
        if (request.duration() != null) metadata.put("duration", request.duration());
        if (request.latitude() != null) metadata.put("latitude", request.latitude());
        if (request.longitude() != null) metadata.put("longitude", request.longitude());
        if (request.locationName() != null) metadata.put("locationName", request.locationName());
        message.setMetadata(metadata);

        message.setReplyToMessage(replyToMessage);
        message.setMentions(request.mentions());

        messageRepository.save(message);

        // Update conversation's lastMessage and updatedAt
        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        // Auto-unarchive for all participants who have archived this conversation
        conversation.getParticipants().forEach(participant -> {
            if (participant.isArchived()) {
                participant.setArchived(false);
                participant.setArchivedAt(null);
                participantRepository.save(participant);
            }
        });

        // Increment unread count for all participants except the sender
        participantRepository.incrementUnreadCountForOthers(conversationId, userId);

        // Invalidate caches
        invalidateCaches(conversationId, conversation.getParticipantIds());

        // Broadcast message to all participants via WebSocket
        broadcastMessage(message, conversation);
        broadcastConversationUpdate(conversation);

        return mapMessageToResponse(message, userId);
    }

    @Transactional
    public MessageResponse updateMessage(Long userId, Long conversationId, Long messageId, UpdateMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found in this conversation");

        if (!message.getSender().getId().equals(userId))
            throw BusinessException.forbidden("You can only edit your own messages");

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

        return mapMessageToResponse(message, userId);
    }

    public List<Map<String, Object>> getEditHistory(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found in this conversation");

        if (!message.getConversation().isUserParticipant(userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> editHistory = (List<Map<String, Object>>) message.getMetadata().get("editHistory");

        return editHistory != null ? editHistory : new ArrayList<>();
    }

    @Transactional
    public void deleteMessage(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found in this conversation");

        if (!message.getSender().getId().equals(userId))
            throw BusinessException.forbidden("You can only delete your own messages");

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


    private void invalidateCaches(Long conversationId, Set<Long> participantIds) {
        messageCache.invalidate(conversationId);
        cacheManager.invalidateConversationCaches(conversationId, participantIds);
    }

    private void broadcastMessage(Message message, Conversation conversation) {
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);

        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            List<User> mentionedUsers = userRepository.findByIds(message.getMentions());
            outgoingDto.setMentionedUsers(userMapper.toMentionedUserResponseList(mentionedUsers));
        }

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

    private MessageResponse mapMessageToResponse(Message message, Long userId) {
        MessageResponse response = messageMapper.toResponse(message);

        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            List<User> mentionedUsers = userRepository.findByIds(message.getMentions());
            response.setMentionedUsers(userMapper.toMentionedUserResponseList(mentionedUsers));
        }

        return response;
    }

    // ==================== CHAT INFO METHODS ====================

    @Transactional
    public CursorPaginatedResponse<MessageResponse> searchMessages(Long userId, Long conversationId, String query, String type, Long senderId, Long cursor, int limit, String sort) {
        if (limit < 1) throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        if (limit > 100) limit = 100;

        if (!participantRepository.isUserParticipant(conversationId, userId))
            throw BusinessException.badRequest("You do not have access to this conversation");

        List<Message> messages = messageRepository.searchMessagesByCursor(conversationId, query, type, senderId, cursor, limit, sort);

        boolean hasMore = messages.size() > limit;
        if (hasMore) messages = messages.subList(0, limit);

        List<MessageResponse> messageResponses = messages.stream()
                .map(m -> mapMessageToResponse(m, userId))
                .toList();

        Long nextCursor = hasMore && !messageResponses.isEmpty() ? messageResponses.get(messageResponses.size() - 1).getId() : null;

        return new CursorPaginatedResponse<>(messageResponses, nextCursor, limit);
    }

    public CursorPaginatedResponse<MediaResponse> getMediaFiles(Long userId, Long conversationId, String type, String startDate, String endDate, Long cursor, int limit) {
        if (limit < 1) throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        if (limit > 100) limit = 100;

        if (!participantRepository.isUserParticipant(conversationId, userId))
            throw BusinessException.badRequest("You do not have access to this conversation");

        Instant start = null;
        Instant end = null;
        try {
            if (startDate != null && !startDate.isEmpty()) start = Instant.parse(startDate);
            if (endDate != null && !endDate.isEmpty()) end = Instant.parse(endDate);
        } catch (Exception e) {
            throw BusinessException.badRequest("Invalid date format. Use ISO-8601 (e.g., 2023-01-01T00:00:00Z)", "INVALID_DATE");
        }

        List<Message> messages = messageRepository.findMediaByCursor(conversationId, type, start, end, cursor, limit);

        boolean hasMore = messages.size() > limit;
        if (hasMore) messages = messages.subList(0, limit);

        List<MediaResponse> mediaResponses = messages.stream()
                .map(this::mapMessageToMediaResponse)
                .toList();

        Long nextCursor = hasMore && !mediaResponses.isEmpty() ? mediaResponses.get(mediaResponses.size() - 1).getId() : null;

        return new CursorPaginatedResponse<>(mediaResponses, nextCursor, limit);
    }

    private MediaResponse mapMessageToMediaResponse(Message message) {
        MediaResponse response = new MediaResponse();
        response.setId(message.getId());
        response.setType(message.getType().name());
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setSentAt(message.getSentAt());
        return response;
    }

    // ==================== POLL METHODS ====================

    @Transactional
    public MessageResponse createPoll(Long userId, Long conversationId, CreatePollRequest request) {
        log.debug("Creating poll in conversation {} by user {}", conversationId, userId);

        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        if (conversation.getType() == ConversationType.GROUP)
            if (!groupPermissionsService.hasPermission(conversationId, userId, "create_polls"))
                throw BusinessException.forbidden("You don't have permission to create polls");

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

        MessageResponse response = mapMessageToResponse(message, userId);
        broadcastMessage(message, conversation);
        broadcastConversationUpdate(conversation);

        return response;
    }

    @Transactional
    public MessageResponse votePoll(Long userId, Long conversationId, Long messageId, VotePollRequest request) {
        log.debug("User {} voting on poll {} with options {}", userId, messageId, request.getOptionIds());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found in this conversation");

        if (message.getType() != MessageType.POLL)
            throw BusinessException.badRequest("Message is not a poll", "INVALID_MESSAGE_TYPE");

        if (!participantRepository.isUserParticipant(conversationId, userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

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

        MessageResponse response = mapMessageToResponse(message, userId);
        broadcastMessage(message, message.getConversation());

        return response;
    }

    // ==================== EVENT METHODS ====================

    @Transactional
    public MessageResponse createEvent(Long userId, Long conversationId, CreateEventRequest request) {
        log.debug("Creating event in conversation {} by user {}", conversationId, userId);

        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        if (conversation.getType() == ConversationType.GROUP)
            if (!groupPermissionsService.hasPermission(conversationId, userId, "create_events"))
                throw BusinessException.forbidden("You don't have permission to create events");

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

        MessageResponse response = mapMessageToResponse(message, userId);
        broadcastMessage(message, conversation);
        broadcastConversationUpdate(conversation);

        return response;
    }

    @Transactional
    public MessageResponse respondToEvent(Long userId, Long conversationId, Long messageId, EventRsvpRequest request) {
        log.debug("User {} responding to event {} with status {}", userId, messageId, request.getStatus());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!message.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found in this conversation");

        if (message.getType() != MessageType.EVENT)
            throw BusinessException.badRequest("Message is not an event", "INVALID_MESSAGE_TYPE");

        if (!participantRepository.isUserParticipant(conversationId, userId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

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

        MessageResponse response = mapMessageToResponse(message, userId);
        broadcastMessage(message, message.getConversation());

        return response;
    }

    // ==================== FORWARD MESSAGE ====================

    @Transactional
    public List<MessageResponse> forwardMessage(Long userId, Long conversationId, Long messageId, List<Long> conversationIds) {
        log.debug("User {} forwarding message {} to {} conversations", userId, messageId, conversationIds.size());

        Message originalMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found"));

        if (!originalMessage.getConversation().getId().equals(conversationId))
            throw BusinessException.notFound("Message not found in this conversation");

        if (originalMessage.isDeleted())
            throw BusinessException.badRequest("Cannot forward deleted message");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        List<MessageResponse> forwardedMessages = new ArrayList<>();

        for (Long targetConversationId : conversationIds) {
            Conversation conversation = conversationRepository.findByIdWithParticipants(targetConversationId)
                    .orElseThrow(() -> BusinessException.notFound("Conversation not found: " + targetConversationId));

            if (!participantRepository.isUserParticipant(targetConversationId, userId))
                throw BusinessException.badRequest("You are not a participant in conversation: " + targetConversationId);

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

            MessageResponse response = mapMessageToResponse(forwardedMessage, userId);
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
}
