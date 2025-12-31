package com.chattrix.api.services.message;

import com.chattrix.api.enums.MessageType;
import com.chattrix.api.entities.*;
import com.chattrix.api.enums.ScheduledStatus;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.*;
import com.chattrix.api.requests.ScheduleMessageRequest;
import com.chattrix.api.requests.UpdateScheduledMessageRequest;
import com.chattrix.api.responses.BulkCancelResponse;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.ConversationUpdateDto;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
@ApplicationScoped
public class ScheduledMessageService {

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
    private ChatSessionService chatSessionService;

    @Transactional
    public MessageResponse scheduleMessage(Long userId, Long conversationId, ScheduleMessageRequest request) {
        // Validate scheduled time
        Instant now = Instant.now();
        if (request.scheduledTime().isBefore(now)) {
            throw BusinessException.badRequest("Scheduled time must be in the future", "VALIDATION_ERROR");
        }

        Instant oneYearFromNow = now.plus(365, ChronoUnit.DAYS);
        if (request.scheduledTime().isAfter(oneYearFromNow)) {
            throw BusinessException.badRequest("Scheduled time cannot be more than 1 year in the future", "VALIDATION_ERROR");
        }

        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.unauthorized("You don't have permission to schedule messages in this conversation");
        }

        // Get sender
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Validate reply to message if provided
        Message replyToMessage = null;
        if (request.replyToMessageId() != null) {
            replyToMessage = messageRepository.findByIdSimple(request.replyToMessageId())
                    .orElseThrow(() -> BusinessException.notFound("Reply to message not found", "RESOURCE_NOT_FOUND"));

            if (!replyToMessage.getConversation().getId().equals(conversationId)) {
                throw BusinessException.badRequest("Cannot reply to message from different conversation", "BAD_REQUEST");
            }
        }

        // Parse message type
        MessageType messageType = MessageType.TEXT;
        if (request.type() != null) {
            try {
                messageType = MessageType.valueOf(request.type().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw BusinessException.badRequest("Invalid message type: " + request.type(), "BAD_REQUEST");
            }
        }

        // Create scheduled message
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.content());
        message.setType(messageType);
        // TODO: Use MessageMetadata for media fields
        // message.setMediaUrl(request.mediaUrl());
        // message.setThumbnailUrl(request.thumbnailUrl());
        // message.setFileName(request.fileName());
        // message.setFileSize(request.fileSize());
        // message.setDuration(request.duration());
        message.setReplyToMessage(replyToMessage);
        
        // Set scheduled fields
        message.setScheduled(true);
        message.setScheduledTime(request.scheduledTime());
        message.setScheduledStatus(ScheduledStatus.PENDING);

        messageRepository.save(message);

        return messageMapper.toResponse(message);
    }

    public CursorPaginatedResponse<MessageResponse> getScheduledMessages(Long userId, Long conversationId, String status, Long cursor, int limit) {
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100;
        }

        ScheduledStatus scheduledStatus = status != null ? parseStatus(status) : null;

        List<Message> messages = messageRepository.findScheduledMessagesByCursor(
                userId, conversationId, scheduledStatus, cursor, limit);

        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = messages.subList(0, limit);
        }

        List<MessageResponse> responses = messages.stream()
                .map(messageMapper::toResponse)
                .toList();

        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    public MessageResponse getScheduledMessage(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Scheduled message not found", "RESOURCE_NOT_FOUND"));

        if (!message.isScheduled()) {
            throw BusinessException.notFound("Message is not a scheduled message", "RESOURCE_NOT_FOUND");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw BusinessException.notFound("Scheduled message not found", "RESOURCE_NOT_FOUND");
        }

        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Scheduled message not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        return messageMapper.toResponse(message);
    }

    @Transactional
    public MessageResponse updateScheduledMessage(Long userId, Long conversationId, Long messageId, UpdateScheduledMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Scheduled message not found", "RESOURCE_NOT_FOUND"));

        if (!message.isScheduled()) {
            throw BusinessException.notFound("Message is not a scheduled message", "RESOURCE_NOT_FOUND");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw BusinessException.notFound("Scheduled message not found", "RESOURCE_NOT_FOUND");
        }

        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Scheduled message not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        if (message.getScheduledStatus() != ScheduledStatus.PENDING) {
            throw BusinessException.badRequest("Cannot edit scheduled message: Message has already been " +
                    message.getScheduledStatus().name().toLowerCase(), "BAD_REQUEST");
        }

        // Update fields if provided
        if (request.content() != null) {
            message.setContent(request.content());
        }

        if (request.scheduledTime() != null) {
            Instant now = Instant.now();
            if (request.scheduledTime().isBefore(now)) {
                throw BusinessException.badRequest("Scheduled time must be in the future", "VALIDATION_ERROR");
            }
            message.setScheduledTime(request.scheduledTime());
        }

        // TODO: Use MessageMetadata for media fields
        /*
        if (request.mediaUrl() != null) {
            message.setMediaUrl(request.mediaUrl());
        }

        if (request.thumbnailUrl() != null) {
            message.setThumbnailUrl(request.thumbnailUrl());
        }

        if (request.fileName() != null) {
            message.setFileName(request.fileName());
        }
        */

        messageRepository.save(message);

        return messageMapper.toResponse(message);
    }

    @Transactional
    public void cancelScheduledMessage(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Scheduled message not found", "RESOURCE_NOT_FOUND"));

        if (!message.isScheduled()) {
            throw BusinessException.notFound("Message is not a scheduled message", "RESOURCE_NOT_FOUND");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw BusinessException.notFound("Scheduled message not found", "RESOURCE_NOT_FOUND");
        }

        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Scheduled message not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        if (message.getScheduledStatus() == ScheduledStatus.SENT) {
            throw BusinessException.badRequest("Cannot cancel scheduled message: Message has already been sent", "BAD_REQUEST");
        }

        message.setScheduledStatus(ScheduledStatus.CANCELLED);
        messageRepository.save(message);
    }

    @Transactional
    public BulkCancelResponse bulkCancelScheduledMessages(Long userId, Long conversationId, List<Long> messageIds) {
        int cancelledCount = 0;
        List<Long> failedIds = new ArrayList<>();

        for (Long messageId : messageIds) {
            try {
                Optional<Message> optionalMessage = messageRepository.findById(messageId);

                if (optionalMessage.isEmpty()) {
                    failedIds.add(messageId);
                    continue;
                }

                Message message = optionalMessage.get();

                if (!message.isScheduled()) {
                    failedIds.add(messageId);
                    continue;
                }

                if (!message.getSender().getId().equals(userId)) {
                    failedIds.add(messageId);
                    continue;
                }

                if (!message.getConversation().getId().equals(conversationId)) {
                    failedIds.add(messageId);
                    continue;
                }

                if (message.getScheduledStatus() == ScheduledStatus.SENT) {
                    failedIds.add(messageId);
                    continue;
                }

                message.setScheduledStatus(ScheduledStatus.CANCELLED);
                messageRepository.save(message);
                cancelledCount++;

            } catch (Exception e) {
                failedIds.add(messageId);
            }
        }

        return new BulkCancelResponse(cancelledCount, failedIds);
    }

    @Transactional
    public void processScheduledMessages() {
        Instant now = Instant.now();

        List<Message> dueMessages = messageRepository.findScheduledMessagesDue(now);

        for (Message scheduledMsg : dueMessages) {
            try {
                // Verify sender is still a participant
                boolean isStillParticipant = participantRepository.isUserParticipant(
                        scheduledMsg.getConversation().getId(),
                        scheduledMsg.getSender().getId()
                );

                if (!isStillParticipant) {
                    scheduledMsg.setScheduledStatus(ScheduledStatus.FAILED);
                    // scheduledMsg.setFailedReason("User has left the conversation"); // TODO: Add failedReason field
                    messageRepository.save(scheduledMsg);
                    sendFailureNotification(scheduledMsg, "User has left the conversation");
                    continue;
                }

                // Update message to mark as sent
                scheduledMsg.setScheduledStatus(ScheduledStatus.SENT);
                scheduledMsg.setSentAt(Instant.now());
                // Keep scheduled=true to maintain history that this was a scheduled message
                messageRepository.save(scheduledMsg);

                // Update conversation's lastMessage
                Conversation conversation = scheduledMsg.getConversation();
                conversation.setLastMessage(scheduledMsg);
                conversationRepository.save(conversation);

                // Increment unread count for all participants except the sender
                participantRepository.incrementUnreadCountForOthers(
                        scheduledMsg.getConversation().getId(),
                        scheduledMsg.getSender().getId()
                );

                // Send success notification (includes chat.message and scheduled.message.sent events)
                sendSuccessNotification(scheduledMsg);

                // Broadcast conversation update
                broadcastConversationUpdate(scheduledMsg.getConversation());

            } catch (Exception e) {
                scheduledMsg.setScheduledStatus(ScheduledStatus.FAILED);
                // TODO: Add failedReason field to Message entity
                // scheduledMsg.setFailedReason(e.getMessage());
                messageRepository.save(scheduledMsg);
                sendFailureNotification(scheduledMsg, e.getMessage());
            }
        }
    }

    private void sendSuccessNotification(Message message) {
        try {
            OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);

            // 1. Send regular chat.message event so message appears in conversation real-time
            WebSocketMessage<OutgoingMessageDto> chatMessage = new WebSocketMessage<>("chat.message", outgoingDto);
            message.getConversation().getParticipants().forEach(participant -> {
                chatSessionService.sendMessageToUser(participant.getUser().getId(), chatMessage);
            });

            // 2. Send scheduled.message.sent event for scheduled message notification
            Map<String, Object> payload = new HashMap<>();
            payload.put("scheduledMessageId", message.getId());
            payload.put("message", outgoingDto);

            WebSocketMessage<Map<String, Object>> scheduledNotification = new WebSocketMessage<>("scheduled.message.sent", payload);
            message.getConversation().getParticipants().forEach(participant -> {
                chatSessionService.sendMessageToUser(participant.getUser().getId(), scheduledNotification);
            });

        } catch (Exception e) {
            System.err.println("Failed to send success notification: " + e.getMessage());
        }
    }

    private void sendFailureNotification(Message message, String failedReason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("scheduledMessageId", message.getId());
            payload.put("conversationId", message.getConversation().getId());
            payload.put("failedReason", failedReason);
            payload.put("failedAt", Instant.now().toString());

            WebSocketMessage<Map<String, Object>> wsMessage = new WebSocketMessage<>("scheduled.message.failed", payload);

            chatSessionService.sendMessageToUser(message.getSender().getId(), wsMessage);

        } catch (Exception e) {
            System.err.println("Failed to send failure notification: " + e.getMessage());
        }
    }

    private void broadcastConversationUpdate(Conversation conversation) {
        try {
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
                chatSessionService.sendMessageToUser(participant.getUser().getId(), message);
            });

        } catch (Exception e) {
            System.err.println("Failed to broadcast conversation update: " + e.getMessage());
        }
    }

    private ScheduledStatus parseStatus(String status) {
        try {
            return ScheduledStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Invalid status: " + status, "BAD_REQUEST");
        }
    }
}
