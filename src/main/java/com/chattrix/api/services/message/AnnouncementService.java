package com.chattrix.api.services.message;

import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.CreateAnnouncementRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
@ApplicationScoped
public class AnnouncementService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MessageMapper messageMapper;

    @Inject
    private ChatSessionService chatSessionService;

    /**
     * Create announcement (admin only)
     */
    @Transactional
    public MessageResponse createAnnouncement(Long userId, Long conversationId, CreateAnnouncementRequest request) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Announcements can only be created in group conversations", "INVALID_CONVERSATION_TYPE");
        }

        // Check if user is admin
        if (!participantRepository.isUserAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only admins can create announcements");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Create announcement message
        Message announcement = new Message();
        announcement.setConversation(conversation);
        announcement.setSender(user);
        announcement.setContent(request.getContent());
        announcement.setType(MessageType.ANNOUNCEMENT);

        messageRepository.save(announcement);

        // Update conversation's lastMessage
        conversation.setLastMessage(announcement);
        conversationRepository.save(conversation);

        // Broadcast to all participants
        broadcastAnnouncement(announcement, conversation);

        return messageMapper.toResponse(announcement);
    }

    /**
     * Get all announcements for a conversation with cursor-based pagination
     */
    public CursorPaginatedResponse<MessageResponse> getAnnouncements(Long userId, Long conversationId, Long cursor, int limit) {
        // Validate limit
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100; // Cap at 100
        }
        
        // Check if user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this group");
        }

        // Fetch limit + 1 to determine if there's a next page
        List<Message> announcements = messageRepository.findAnnouncementsByCursor(conversationId, cursor, limit);
        
        // Determine if there's a next page
        boolean hasMore = announcements.size() > limit;
        if (hasMore) {
            announcements = announcements.subList(0, limit); // Remove the extra item
        }
        
        // Convert to responses
        List<MessageResponse> responses = announcements.stream()
                .map(messageMapper::toResponse)
                .toList();
        
        // Get nextCursor (ID of last item, or null if no more pages)
        Long nextCursor = null;
        if (hasMore && !responses.isEmpty()) {
            nextCursor = responses.get(responses.size() - 1).getId();
        }
        
        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    /**
     * Delete announcement (admin only)
     */
    @Transactional
    public void deleteAnnouncement(Long userId, Long conversationId, Long messageId) {
        // Get message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "MESSAGE_NOT_FOUND"));

        // Verify it's an announcement
        if (message.getType() != MessageType.ANNOUNCEMENT) {
            throw BusinessException.badRequest("Message is not an announcement", "INVALID_MESSAGE_TYPE");
        }

        // Verify it belongs to this conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Message not found in this conversation", "MESSAGE_NOT_FOUND");
        }

        // Check if user is admin or sender
        boolean isAdmin = participantRepository.isUserAdmin(conversationId, userId);
        boolean isSender = message.getSender().getId().equals(userId);

        if (!isAdmin && !isSender) {
            throw BusinessException.forbidden("Only admins or sender can delete announcements");
        }

        // Delete
        messageRepository.delete(message);

        // Broadcast deletion
        AnnouncementDeleteEventDto payload = AnnouncementDeleteEventDto.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .type("announcement")
                .build();
        WebSocketMessage<AnnouncementDeleteEventDto> wsMessage = new WebSocketMessage<>(WebSocketEventType.ANNOUNCEMENT_DELETED, payload);

        message.getConversation().getParticipants().forEach(participant -> {
            chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
        });
    }

    private void broadcastAnnouncement(Message announcement, Conversation conversation) {
        MessageResponse response = messageMapper.toResponse(announcement);

        AnnouncementEventDto payload = AnnouncementEventDto.builder()
                .announcement(response)
                .conversationId(conversation.getId())
                .build();

        WebSocketMessage<AnnouncementEventDto> wsMessage = new WebSocketMessage<>(WebSocketEventType.ANNOUNCEMENT_CREATED, payload);

        // Broadcast to all participants
        conversation.getParticipants().forEach(participant -> {
            chatSessionService.sendMessageToUser(participant.getUser().getId(), wsMessage);
        });
    }
}
