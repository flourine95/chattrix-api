package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.requests.UpdatePollRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.responses.PollOptionResponse;
import com.chattrix.api.responses.PollResponse;
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
public class PollService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private MessageMapper messageMapper;

    @Inject
    private MessageCache messageCache;

    @Inject
    private ChatSessionService chatSessionService;

    @Inject
    private WebSocketMapper webSocketMapper;

    /**
     * List polls in conversation with filters
     */
    public CursorPaginatedResponse<PollResponse> listPolls(
            Long userId, Long conversationId, String status, Long cursor, int limit) {
        
        log.debug("Listing polls: conversationId={}, status={}, cursor={}, limit={}", 
                conversationId, status, cursor, limit);

        // Validate user is participant
        validateUserIsParticipant(conversationId, userId);

        // Validate limit
        if (limit < 1 || limit > 100) {
            limit = 20;
        }

        // Build query
        List<Message> messages;
        if (cursor == null) {
            messages = messageRepository.findByConversationAndType(
                    conversationId, MessageType.POLL, limit + 1);
        } else {
            messages = messageRepository.findByConversationAndTypeWithCursor(
                    conversationId, MessageType.POLL, cursor, limit + 1);
        }

        // Filter by status
        Instant now = Instant.now();
        List<Message> filteredMessages = messages.stream()
                .filter(msg -> {
                    if ("all".equals(status)) return true;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pollData = (Map<String, Object>) msg.getMetadata().get("poll");
                    if (pollData == null) return false;

                    boolean isClosed = isPollClosed(pollData, now);
                    
                    if ("active".equals(status)) return !isClosed;
                    if ("closed".equals(status)) return isClosed;
                    
                    return true;
                })
                .collect(Collectors.toList());

        // Check if has more
        boolean hasMore = filteredMessages.size() > limit;
        if (hasMore) {
            filteredMessages = filteredMessages.subList(0, limit);
        }

        // Convert to PollResponse
        List<PollResponse> polls = filteredMessages.stream()
                .map(msg -> buildPollResponse(msg, userId))
                .collect(Collectors.toList());

        // Get next cursor
        Long nextCursor = hasMore && !polls.isEmpty() 
                ? filteredMessages.get(filteredMessages.size() - 1).getId() 
                : null;

        return new CursorPaginatedResponse<PollResponse>(polls, nextCursor, limit);
    }

    /**
     * Get poll detail
     */
    public PollResponse getPoll(Long userId, Long conversationId, Long messageId) {
        log.debug("Getting poll: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetPoll(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        return buildPollResponse(message, userId);
    }

    /**
     * Update poll
     */
    @Transactional
    public PollResponse updatePoll(Long userId, Long conversationId, Long messageId, UpdatePollRequest request) {
        log.debug("Updating poll: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetPoll(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        // Only creator or admin can update
        if (!message.isSentBy(userId) && !isAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only poll creator or admin can update poll");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");

        // Check if poll has votes
        boolean hasVotes = hasAnyVotes(pollData);

        // If has votes, only allow closing poll or changing expiry
        if (hasVotes) {
            if (request.getQuestion() != null || request.getOptions() != null || request.getAllowMultipleVotes() != null) {
                throw BusinessException.badRequest("Cannot change question or options after votes have been cast");
            }
        }

        // Update fields
        if (request.getQuestion() != null) {
            pollData.put("question", request.getQuestion());
            message.setContent(request.getQuestion());
        }

        if (request.getOptions() != null && !hasVotes) {
            List<Map<String, Object>> newOptions = new ArrayList<>();
            for (int i = 0; i < request.getOptions().size(); i++) {
                Map<String, Object> option = new HashMap<>();
                option.put("id", (long) i);
                option.put("text", request.getOptions().get(i));
                option.put("votes", new ArrayList<>());
                newOptions.add(option);
            }
            pollData.put("options", newOptions);
        }

        if (request.getAllowMultipleVotes() != null && !hasVotes) {
            pollData.put("allowMultiple", request.getAllowMultipleVotes());
        }

        if (request.getExpiresAt() != null) {
            pollData.put("closesAt", request.getExpiresAt().toString());
        }

        if (Boolean.TRUE.equals(request.getClosed())) {
            pollData.put("closedAt", Instant.now().toString());
            pollData.put("closedManually", true);
        }

        messageRepository.save(message);
        messageCache.invalidate(conversationId);

        log.info("Poll updated: messageId={}", messageId);

        // Broadcast update
        broadcastPollUpdate(message);

        return buildPollResponse(message, userId);
    }

    /**
     * Close poll manually
     */
    @Transactional
    public PollResponse closePoll(Long userId, Long conversationId, Long messageId) {
        log.debug("Closing poll: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetPoll(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        // Only creator or admin can close
        if (!message.isSentBy(userId) && !isAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only poll creator or admin can close poll");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");

        pollData.put("closedAt", Instant.now().toString());
        pollData.put("closedManually", true);

        messageRepository.save(message);
        messageCache.invalidate(conversationId);

        log.info("Poll closed: messageId={}", messageId);

        // Broadcast update
        broadcastPollUpdate(message);

        return buildPollResponse(message, userId);
    }

    /**
     * Delete poll
     */
    @Transactional
    public void deletePoll(Long userId, Long conversationId, Long messageId) {
        log.debug("Deleting poll: messageId={}, conversationId={}", messageId, conversationId);

        Message message = validateAndGetPoll(messageId, conversationId);
        validateUserIsParticipant(conversationId, userId);

        // Only creator or admin can delete
        if (!message.isSentBy(userId) && !isAdmin(conversationId, userId)) {
            throw BusinessException.forbidden("Only poll creator or admin can delete poll");
        }

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        messageRepository.save(message);
        messageCache.invalidate(conversationId);

        log.info("Poll deleted: messageId={}", messageId);

        // Broadcast deletion
        broadcastPollDeletion(message);
    }

    // ==================== HELPER METHODS ====================

    private Message validateAndGetPoll(Long messageId, Long conversationId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found"));

        if (!message.belongsToConversation(conversationId)) {
            throw BusinessException.notFound("Poll not found in this conversation");
        }

        if (!message.isPollMessage()) {
            throw BusinessException.badRequest("Message is not a poll");
        }

        if (message.isDeleted()) {
            throw BusinessException.notFound("Poll has been deleted");
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
        // TODO: Implement admin check based on conversation participant role
        return false;
    }

    private boolean isPollClosed(Map<String, Object> pollData, Instant now) {
        // Check if manually closed
        if (pollData.containsKey("closedManually") && Boolean.TRUE.equals(pollData.get("closedManually"))) {
            return true;
        }

        // Check if expired
        if (pollData.containsKey("closesAt") && pollData.get("closesAt") != null) {
            try {
                Instant closesAt = Instant.parse(pollData.get("closesAt").toString());
                return now.isAfter(closesAt);
            } catch (Exception e) {
                log.warn("Failed to parse closesAt: {}", pollData.get("closesAt"));
            }
        }

        return false;
    }

    private boolean hasAnyVotes(Map<String, Object> pollData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");
        
        if (options == null) return false;

        return options.stream()
                .anyMatch(opt -> {
                    @SuppressWarnings("unchecked")
                    List<Object> votes = (List<Object>) opt.get("votes");
                    return votes != null && !votes.isEmpty();
                });
    }

    private PollResponse buildPollResponse(Message message, Long currentUserId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");

        if (pollData == null) {
            throw BusinessException.badRequest("Invalid poll data");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");

        // Calculate total votes and build options
        int totalVotes = 0;
        List<PollOptionResponse> optionResponses = new ArrayList<>();

        for (Map<String, Object> option : options) {
            @SuppressWarnings("unchecked")
            List<Object> votes = (List<Object>) option.get("votes");
            
            List<Long> voterIds = votes.stream()
                    .filter(v -> v instanceof Number)
                    .map(v -> ((Number) v).longValue())
                    .collect(Collectors.toList());

            totalVotes += voterIds.size();

            boolean hasVoted = voterIds.contains(currentUserId);

            optionResponses.add(PollOptionResponse.builder()
                    .id(((Number) option.get("id")).longValue())
                    .text(option.get("text").toString())
                    .voteCount(voterIds.size())
                    .voterIds(voterIds)
                    .hasVoted(hasVoted)
                    .build());
        }

        Instant closesAt = null;
        if (pollData.containsKey("closesAt") && pollData.get("closesAt") != null) {
            try {
                closesAt = Instant.parse(pollData.get("closesAt").toString());
            } catch (Exception e) {
                log.warn("Failed to parse closesAt");
            }
        }

        boolean isClosed = isPollClosed(pollData, Instant.now());

        return PollResponse.builder()
                .messageId(message.getId())
                .question(pollData.get("question").toString())
                .options(optionResponses)
                .allowMultiple((Boolean) pollData.getOrDefault("allowMultiple", false))
                .anonymous((Boolean) pollData.getOrDefault("anonymous", false))
                .closesAt(closesAt)
                .isClosed(isClosed)
                .totalVotes(totalVotes)
                .createdBy(message.getSender().getId())
                .createdByUsername(message.getSender().getUsername())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private void broadcastPollUpdate(Message message) {
        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);
        WebSocketMessage<OutgoingMessageDto> wsMessage =
                new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        // Use findByIdWithParticipants to fetch participants eagerly
        Conversation conversation = conversationRepository.findByIdWithParticipants(message.getConversation().getId())
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        conversation.getParticipants()
                .forEach(p -> chatSessionService.sendMessageToUser(p.getUser().getId(), wsMessage));
    }

    private void broadcastPollDeletion(Message message) {
        // Similar to broadcastPollUpdate but with deletion flag
        broadcastPollUpdate(message);
    }
}
