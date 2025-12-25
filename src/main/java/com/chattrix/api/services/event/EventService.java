package com.chattrix.api.services.event;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Event;
import com.chattrix.api.entities.EventRsvp;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.EventMapper;
import com.chattrix.api.repositories.*;
import com.chattrix.api.requests.CreateEventRequest;
import com.chattrix.api.requests.EventRsvpRequest;
import com.chattrix.api.requests.UpdateEventRequest;
import com.chattrix.api.responses.EventResponse;
import com.chattrix.api.responses.EventRsvpResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.EventEventDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventService {

    @Inject
    private EventRepository eventRepository;

    @Inject
    private EventRsvpRepository rsvpRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private EventMapper eventMapper;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ChatSessionService chatSessionService;

    /**
     * Get all events for a conversation
     */
    @Transactional
    public List<EventResponse> getEvents(Long userId, Long conversationId) {
        // Verify user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        List<Event> events = eventRepository.findByConversationId(conversationId);

        return events.stream()
                .map(event -> enrichEventResponse(event, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific event by ID
     */
    @Transactional
    public EventResponse getEvent(Long userId, Long conversationId, Long eventId) {
        // Verify user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> BusinessException.notFound("Event not found", "RESOURCE_NOT_FOUND"));

        // Verify event belongs to conversation
        if (!event.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Event not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        return enrichEventResponse(event, userId);
    }

    /**
     * Create a new event
     */
    @Transactional
    public EventResponse createEvent(Long userId, Long conversationId, CreateEventRequest request) {
        // Verify conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw BusinessException.badRequest("Events can only be created in group conversations", "BAD_REQUEST");
        }

        // Verify user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        // Validate times
        if (request.getEndTime() != null && request.getEndTime().isBefore(request.getStartTime())) {
            throw BusinessException.badRequest("End time must be after start time", "BAD_REQUEST");
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        Event event = Event.builder()
                .conversation(conversation)
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .build();

        eventRepository.save(event);

        // Create a message for the event
        Message eventMessage = new Message();
        eventMessage.setConversation(conversation);
        eventMessage.setSender(creator);
        eventMessage.setType(Message.MessageType.EVENT);
        eventMessage.setEvent(event);
        eventMessage.setContent(event.getTitle()); // Store title as content
        eventMessage.setSentAt(Instant.now());
        messageRepository.save(eventMessage);

        // Send WebSocket notification
        EventResponse eventResponse = enrichEventResponse(event, userId);
        sendEventNotification(conversationId, "EVENT_CREATED", eventResponse);

        return eventResponse;
    }

    /**
     * Update an event
     */
    @Transactional
    public EventResponse updateEvent(Long userId, Long conversationId, Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> BusinessException.notFound("Event not found", "RESOURCE_NOT_FOUND"));

        // Verify event belongs to conversation
        if (!event.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Event not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        // Only creator or admin can update
        boolean isCreator = event.getCreator().getId().equals(userId);
        boolean isAdmin = participantRepository.isUserAdmin(conversationId, userId);

        if (!isCreator && !isAdmin) {
            throw BusinessException.forbidden("Only event creator or group admin can update this event");
        }

        // Update fields
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }

        // Validate times
        if (event.getEndTime() != null && event.getEndTime().isBefore(event.getStartTime())) {
            throw BusinessException.badRequest("End time must be after start time", "BAD_REQUEST");
        }

        eventRepository.save(event);

        // Send WebSocket notification
        EventResponse eventResponse = enrichEventResponse(event, userId);
        sendEventNotification(conversationId, "EVENT_UPDATED", eventResponse);

        return eventResponse;
    }

    /**
     * Delete an event
     */
    @Transactional
    public void deleteEvent(Long userId, Long conversationId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> BusinessException.notFound("Event not found", "RESOURCE_NOT_FOUND"));

        // Verify event belongs to conversation
        if (!event.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Event not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        // Only creator or admin can delete
        boolean isCreator = event.getCreator().getId().equals(userId);
        boolean isAdmin = participantRepository.isUserAdmin(conversationId, userId);

        if (!isCreator && !isAdmin) {
            throw BusinessException.forbidden("Only event creator or group admin can delete this event");
        }

        // Send WebSocket notification before deletion
        EventResponse eventResponse = enrichEventResponse(event, userId);
        sendEventNotification(conversationId, "EVENT_DELETED", eventResponse);

        // Delete messages that reference this event
        messageRepository.deleteByEventId(eventId);

        // Delete event (cascade will delete RSVPs)
        eventRepository.delete(event);
    }

    /**
     * RSVP to an event
     */
    @Transactional
    public EventResponse rsvpToEvent(Long userId, Long conversationId, Long eventId, EventRsvpRequest request) {
        // Verify user is participant first
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        Event event = eventRepository.findByIdWithDetails(eventId)
                .orElseThrow(() -> BusinessException.notFound("Event not found", "RESOURCE_NOT_FOUND"));

        // Verify event belongs to conversation
        if (!event.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Event not found in this conversation", "RESOURCE_NOT_FOUND");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Find or create RSVP
        EventRsvp rsvp = rsvpRepository.findByEventIdAndUserId(eventId, userId)
                .orElse(EventRsvp.builder()
                        .event(event)
                        .user(user)
                        .build());

        rsvp.setStatus(EventRsvp.RsvpStatus.valueOf(request.getStatus()));
        rsvpRepository.save(rsvp);

        // Detach event from persistence context and reload fresh
        eventRepository.detach(event);
        event = eventRepository.findByIdWithDetails(eventId).orElseThrow();

        // Send WebSocket notification
        EventResponse eventResponse = enrichEventResponse(event, userId);
        sendEventNotification(conversationId, "EVENT_RSVP_UPDATED", eventResponse);

        return eventResponse;
    }

    /**
     * Enrich event response with RSVP statistics and current user's RSVP
     */
    private EventResponse enrichEventResponse(Event event, Long userId) {
        EventResponse response = eventMapper.toResponse(event);

        // Calculate RSVP counts
        long goingCount = event.getRsvps().stream()
                .filter(r -> r.getStatus() == EventRsvp.RsvpStatus.GOING)
                .count();
        long maybeCount = event.getRsvps().stream()
                .filter(r -> r.getStatus() == EventRsvp.RsvpStatus.MAYBE)
                .count();
        long notGoingCount = event.getRsvps().stream()
                .filter(r -> r.getStatus() == EventRsvp.RsvpStatus.NOT_GOING)
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
        List<EventRsvpResponse> rsvpResponses = event.getRsvps().stream()
                .map(eventMapper::toRsvpResponse)
                .collect(Collectors.toList());
        response.setRsvps(rsvpResponses);

        return response;
    }

    /**
     * Send WebSocket notification for event changes
     */
    private void sendEventNotification(Long conversationId, String eventType, EventResponse eventResponse) {
        try {
            EventEventDto eventEvent = EventEventDto.builder()
                    .type(eventType)
                    .event(eventResponse)
                    .build();

            WebSocketMessage<EventEventDto> message = new WebSocketMessage<>("event.event", eventEvent);

            List<Long> participantIds = participantRepository
                    .findByConversationId(conversationId)
                    .stream()
                    .map(cp -> cp.getUser().getId())
                    .toList();

            for (Long participantId : participantIds) {
                try {
                    chatSessionService.sendDirectMessage(participantId, message);
                } catch (Exception e) {
                    // Log but continue
                    System.err.println("Failed to send event notification to user " + participantId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send event notification: " + e.getMessage());
        }
    }
}
