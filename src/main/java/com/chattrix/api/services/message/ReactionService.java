package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.ReactionResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.ReactionEventDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ReactionService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ChatSessionService chatSessionService;

    @Transactional
    public ReactionResponse addReaction(Long userId, Long messageId, String emoji) {
        // Validate emoji (basic validation)
        if (emoji == null || emoji.trim().isEmpty()) {
            throw BusinessException.badRequest("Emoji is required", "BAD_REQUEST");
        }

        // Get message
        Message message = messageRepository.findByIdSimple(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        // Verify user has access to conversation
        Conversation conversation = conversationRepository.findByIdWithParticipants(message.getConversation().getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        // Get user for event
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Get or initialize reactions map
        Map<String, List<Long>> reactions = message.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
        }

        // Toggle reaction logic
        List<Long> userIds = reactions.getOrDefault(emoji, new ArrayList<>());
        String action;

        if (userIds.contains(userId)) {
            // Remove reaction (toggle off)
            userIds.remove(userId);
            action = "remove";
            if (userIds.isEmpty()) {
                reactions.remove(emoji);
            } else {
                reactions.put(emoji, userIds);
            }
        } else {
            // Add reaction (toggle on)
            userIds.add(userId);
            reactions.put(emoji, userIds);
            action = "add";
        }

        message.setReactions(reactions);
        messageRepository.save(message);

        // Broadcast reaction event to all conversation participants
        ReactionEventDto reactionEvent = new ReactionEventDto();
        reactionEvent.setMessageId(messageId);
        reactionEvent.setUserId(userId);
        reactionEvent.setUserName(user.getFullName());
        reactionEvent.setEmoji(emoji);
        reactionEvent.setAction(action);
        reactionEvent.setReactions(reactions);
        reactionEvent.setTimestamp(java.time.Instant.now());

        WebSocketMessage<ReactionEventDto> wsMessage = new WebSocketMessage<>("message.reaction", reactionEvent);

        conversation.getParticipants().forEach(participant -> {
            Long participantId = participant.getUser().getId();
            chatSessionService.sendMessageToUser(participantId, wsMessage);
        });

        // Prepare response
        ReactionResponse response = new ReactionResponse();
        response.setMessageId(messageId);
        response.setReactions(reactions);

        return response;
    }

    @Transactional
    public ReactionResponse removeReaction(Long userId, Long messageId, String emoji) {
        // Get message
        Message message = messageRepository.findByIdSimple(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        // Verify user has access to conversation
        Conversation conversation = conversationRepository.findByIdWithParticipants(message.getConversation().getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        // Get user for event
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Get reactions map
        Map<String, List<Long>> reactions = message.getReactions();
        if (reactions != null && reactions.containsKey(emoji)) {
            List<Long> userIds = reactions.get(emoji);
            userIds.remove(userId);

            if (userIds.isEmpty()) {
                reactions.remove(emoji);
            } else {
                reactions.put(emoji, userIds);
            }

            message.setReactions(reactions);
            messageRepository.save(message);

            // Broadcast reaction event to all conversation participants
            ReactionEventDto reactionEvent = new ReactionEventDto();
            reactionEvent.setMessageId(messageId);
            reactionEvent.setUserId(userId);
            reactionEvent.setUserName(user.getFullName());
            reactionEvent.setEmoji(emoji);
            reactionEvent.setAction("remove");
            reactionEvent.setReactions(reactions);
            reactionEvent.setTimestamp(java.time.Instant.now());

            WebSocketMessage<ReactionEventDto> wsMessage = new WebSocketMessage<>("message.reaction", reactionEvent);

            conversation.getParticipants().forEach(participant -> {
                Long participantId = participant.getUser().getId();
                chatSessionService.sendMessageToUser(participantId, wsMessage);
            });
        }

        // Prepare response
        ReactionResponse response = new ReactionResponse();
        response.setMessageId(messageId);
        response.setReactions(reactions != null ? reactions : new HashMap<>());

        return response;
    }

    public ReactionResponse getReactions(Long userId, Long messageId) {
        // Get message
        Message message = messageRepository.findByIdSimple(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        // Verify user has access to conversation
        Conversation conversation = conversationRepository.findByIdWithParticipants(message.getConversation().getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw BusinessException.badRequest("You do not have access to this conversation", "BAD_REQUEST");
        }

        // Prepare response
        ReactionResponse response = new ReactionResponse();
        response.setMessageId(messageId);
        response.setReactions(message.getReactions() != null ? message.getReactions() : new HashMap<>());

        return response;
    }
}






