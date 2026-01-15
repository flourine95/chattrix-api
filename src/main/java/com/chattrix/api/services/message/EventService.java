package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.UpdateEventRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.EventResponse;
import com.chattrix.api.responses.EventRsvpResponse;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class EventService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MessageMapper messageMapper;

    @Inject
    private MessageCache messageCache;

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private com.chattrix.api.mappers.WebSocketMapper webSocketMapper;

    @Inject
    private com.chattrix.api.mappers.UserMapper userMapper;

    /**
     * List events in conversation with filters
     */
    public CursorPaginatedResponse<EventResponse> listEvents(
            Long userId, Long conversationId, String status, Long cursor, int limit) {
        
        log.debug("Listing events: conversationId={}, status={}, cursor={}, limit={}", 
                conversationId, status, cursor, limit);

        validateUserIsParticipant(conversationId, userId);

        if (limit < 1 || limit > 100) {
            limit = 20;
        }

        List<Message> messages;
        if (cursor == null) {
            messages = messageRepository.findByConversationAndType(
                    conversationId, MessageType.EVENT, limit + 1);
        } else {
            messages = messageRepository.findByConversationAndTypeWithCursor(
                    conversationId, MessageType.EVENT, cursor, limit + 1);
        }

        // Filter by status
        Instant now = Instant.now();
        List<Message> filteredMessages = messages.stream()
                .filter(msg -> {
                    if ("all".equals(status)) return true;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> eventData = (Map<String, Object>) msg.getMetadata().get("event");
                    if (eventData == null) return false;

                    boolean isPast = isEventPast(eventData, now);
                    
                    if ("upcoming".equals(status)) return !isPast;
                    if ("past".equals(status)) return isPast;
                    
                    return true;
                })
                .collect(Collectors.toList());

        boolean hasMore = filteredMessages.size() > limit;
        if (hasMore) {
            filteredMessages = filteredMessages.subList(0, limit);
        }

        List<EventResponse> events = filteredMessages.stream()
                .map(msg -> buildEventResponse(msg, userId))
                .collect(Collectors.toList());

        Long nextCursor = hasMore && !events.isEmpty() 
                ? filteredMessages.get(filteredMessages.size() - 1).getId() 
                : null;

        return new CursorPaginatedResponse<EventResponse>(events, nextCursor, limit);
    }

    /**
     * Get event detail
     */
    public EventResponse getEvent(Long userId, Long conversationId, Long messageId) {
        log.debug("Getting event: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetEvent(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        return buildEventResponse(message, userId);
    }

    /**
     * Update event
     */
    @Transactional
    public EventResponse updateEvent(Long userId, Long conversationId, Long messageId, UpdateEventRequest request) {
        log.debug("Updating event: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetEvent(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        // Only creator or admin can update
        if (!message.isSentBy(userId) && !isAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only event creator or admin can update event");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) message.getMetadata().get("event");

        // Update fields
        if (request.getTitle() != null) {
            eventData.put("title", request.getTitle());
            message.setContent(request.getTitle());
        }

        if (request.getDescription() != null) {
            eventData.put("description", request.getDescription());
        }

        if (request.getStartTime() != null) {
            eventData.put("startTime", request.getStartTime().toString());
        }

        if (request.getEndTime() != null) {
            eventData.put("endTime", request.getEndTime().toString());
        }

        if (request.getLocation() != null) {
            eventData.put("location", request.getLocation());
        }

        messageRepository.save(message);
        messageCache.invalidate(conversationId);

        log.info("Event updated: messageId={}", messageId);

        broadcastEventUpdate(message);

        return buildEventResponse(message, userId);
    }

    /**
     * Delete event
     */
    @Transactional
    public void deleteEvent(Long userId, Long conversationId, Long messageId) {
        log.debug("Deleting event: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetEvent(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        if (!message.isSentBy(userId) && !isAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only event creator or admin can delete event");
        }

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        messageRepository.save(message);
        messageCache.invalidate(conversationId);

        log.info("Event deleted: messageId={}", messageId);

        broadcastEventUpdate(message);
    }

    /**
     * Get event RSVPs
     */
    public List<EventRsvpResponse> getEventRsvps(Long userId, Long conversationId, Long messageId, String status) {
        log.debug("Getting event RSVPs: messageId={}, status={}", messageId, status);

        Message message = validateAndGetEvent(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) message.getMetadata().get("event");

        @SuppressWarnings("unchecked")
        List<Object> going = (List<Object>) eventData.get("going");
        @SuppressWarnings("unchecked")
        List<Object> maybe = (List<Object>) eventData.get("maybe");
        @SuppressWarnings("unchecked")
        List<Object> notGoing = (List<Object>) eventData.get("notGoing");

        List<EventRsvpResponse> rsvps = new ArrayList<>();

        if ("all".equals(status) || "going".equals(status)) {
            rsvps.addAll(buildRsvpResponses(going, "GOING"));
        }
        if ("all".equals(status) || "maybe".equals(status)) {
            rsvps.addAll(buildRsvpResponses(maybe, "MAYBE"));
        }
        if ("all".equals(status) || "not_going".equals(status)) {
            rsvps.addAll(buildRsvpResponses(notGoing, "NOT_GOING"));
        }

        return rsvps;
    }

    // ==================== HELPER METHODS ====================

    private Message validateAndGetEvent(Long messageId, Long conversationId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Event not found"));

        if (!message.belongsToConversation(conversationId)) {
            throw BusinessException.notFound("Event not found in this conversation");
        }

        if (!message.isEventMessage()) {
            throw BusinessException.badRequest("Message is not an event");
        }

        if (message.isDeleted()) {
            throw BusinessException.notFound("Event has been deleted");
        }

        return message;
    }

    private void validateUserIsParticipant(Long conversationId, Long userId) {
        // Use findByIdWithParticipants to fetch participants eagerly
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }
    }

    private boolean isAdmin(Long conversationId, Long userId) {
        return false;
    }

    private boolean isEventPast(Map<String, Object> eventData, Instant now) {
        if (eventData.containsKey("endTime") && eventData.get("endTime") != null) {
            try {
                Instant endTime = Instant.parse(eventData.get("endTime").toString());
                return now.isAfter(endTime);
            } catch (Exception e) {
                log.warn("Failed to parse endTime");
            }
        }
        return false;
    }

    private EventResponse buildEventResponse(Message message, Long currentUserId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) message.getMetadata().get("event");

        if (eventData == null) {
            throw BusinessException.badRequest("Invalid event data");
        }

        @SuppressWarnings("unchecked")
        List<Object> going = (List<Object>) eventData.get("going");
        @SuppressWarnings("unchecked")
        List<Object> maybe = (List<Object>) eventData.get("maybe");
        @SuppressWarnings("unchecked")
        List<Object> notGoing = (List<Object>) eventData.get("notGoing");

        List<Long> goingIds = convertToLongList(going);
        List<Long> maybeIds = convertToLongList(maybe);
        List<Long> notGoingIds = convertToLongList(notGoing);

        String currentUserStatus = null;
        if (goingIds.contains(currentUserId)) currentUserStatus = "GOING";
        else if (maybeIds.contains(currentUserId)) currentUserStatus = "MAYBE";
        else if (notGoingIds.contains(currentUserId)) currentUserStatus = "NOT_GOING";

        Instant startTime = null;
        Instant endTime = null;
        try {
            if (eventData.get("startTime") != null) {
                startTime = Instant.parse(eventData.get("startTime").toString());
            }
            if (eventData.get("endTime") != null) {
                endTime = Instant.parse(eventData.get("endTime").toString());
            }
        } catch (Exception e) {
            log.warn("Failed to parse event times");
        }

        boolean isPast = isEventPast(eventData, Instant.now());

        return EventResponse.builder()
                .messageId(message.getId())
                .title(eventData.get("title").toString())
                .description((String) eventData.get("description"))
                .startTime(startTime)
                .endTime(endTime)
                .location((String) eventData.get("location"))
                .goingUserIds(goingIds)
                .maybeUserIds(maybeIds)
                .notGoingUserIds(notGoingIds)
                .goingCount(goingIds.size())
                .maybeCount(maybeIds.size())
                .notGoingCount(notGoingIds.size())
                .currentUserStatus(currentUserStatus)
                .isPast(isPast)
                .createdBy(message.getSender().getId())
                .createdByUsername(message.getSender().getUsername())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private List<Long> convertToLongList(List<Object> list) {
        if (list == null) return new ArrayList<>();
        return list.stream()
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).longValue())
                .collect(Collectors.toList());
    }

    private List<EventRsvpResponse> buildRsvpResponses(List<Object> userIds, String status) {
        List<Long> ids = convertToLongList(userIds);
        List<User> users = userRepository.findByIds(ids);

        return users.stream()
                .map(user -> EventRsvpResponse.builder()
                        .id(user.getId())
                        .user(userMapper.toResponse(user))
                        .status(status)
                        .createdAt(Instant.now())
                        .build())
                .collect(Collectors.toList());
    }

    private void broadcastEventUpdate(Message message) {
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);
        WebSocketMessage<OutgoingMessageDto> wsMessage =
                new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        // Use findByIdWithParticipants to fetch participants eagerly
        Conversation conversation = conversationRepository.findByIdWithParticipants(message.getConversation().getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        conversation.getParticipants()
                .forEach(p -> chatSessionService.sendMessageToUser(p.getUser().getId(), wsMessage));
    }
}
