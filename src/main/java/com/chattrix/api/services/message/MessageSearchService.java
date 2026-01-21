package com.chattrix.api.services.message;

import com.chattrix.api.entities.Message;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.GlobalSearchResultResponse;
import com.chattrix.api.responses.MediaSearchResponse;
import com.chattrix.api.responses.MessageContextResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.utils.PaginationHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

@ApplicationScoped
public class MessageSearchService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private MessageMapper messageMapper;

    /**
     * Global search - Search across all user's conversations with cursor-based pagination
     */
    public CursorPaginatedResponse<GlobalSearchResultResponse> globalSearch(Long userId, String query, String type, Long cursor, int limit) {
        // Bean Validation already checked: query is not blank

        limit = PaginationHelper.validateLimit(limit);

        List<Message> messages = messageRepository.globalSearchMessagesByCursor(userId, query, type, cursor, limit);

        return PaginationHelper.buildResponse(messages, limit, this::toGlobalSearchResult, GlobalSearchResultResponse::getMessageId);
    }

    /**
     * Get message context - messages around a specific message
     */
    public MessageContextResponse getMessageContext(Long userId, Long conversationId, Long messageId, int contextSize) {
        // Verify user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        // Verify message belongs to conversation
        Message targetMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "MESSAGE_NOT_FOUND"));

        if (!targetMessage.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Message not found in this conversation", "MESSAGE_NOT_FOUND");
        }

        // Get context messages
        List<Message> contextMessages = messageRepository.getMessageContext(messageId, conversationId, contextSize);

        List<MessageResponse> messageResponses = contextMessages.stream()
                .map(messageMapper::toResponse)
                .toList();

        // Find target message index
        int targetIndex = IntStream.range(0, messageResponses.size())
                .filter(i -> messageResponses.get(i).getId().equals(messageId))
                .findFirst()
                .orElse(-1);

        return MessageContextResponse.builder()
                .targetMessageId(messageId)
                .messages(messageResponses)
                .targetIndex(targetIndex)
                .build();
    }

    private GlobalSearchResultResponse toGlobalSearchResult(Message message) {
        return GlobalSearchResultResponse.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .type(message.getType().name())
                .sentAt(message.getSentAt())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderFullName(message.getSender().getFullName())
                .senderAvatarUrl(message.getSender().getAvatarUrl())
                .conversationId(message.getConversation().getId())
                .conversationName(message.getConversation().getName())
                .conversationType(message.getConversation().getType().name())
                .conversationAvatarUrl(message.getConversation().getAvatarUrl())
                .build();
    }
    
    /**
     * Search media files in a conversation with statistics
     */
    public MediaSearchResponse searchMedia(
            Long userId, Long conversationId, String type, 
            String startDate, String endDate, 
            Long cursor, int limit) {
        
        // Verify user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }
        
        // Parse dates
        Instant startInstant = null;
        Instant endInstant = null;
        
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                startInstant = Instant.parse(startDate);
            } catch (Exception e) {
                throw BusinessException.badRequest("Invalid startDate format. Use ISO-8601 format (e.g., 2026-01-01T00:00:00Z)");
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                endInstant = Instant.parse(endDate);
            } catch (Exception e) {
                throw BusinessException.badRequest("Invalid endDate format. Use ISO-8601 format (e.g., 2026-01-31T23:59:59Z)");
            }
        }
        
        limit = PaginationHelper.validateLimit(limit);
        
        // Get media messages
        List<Message> messages = messageRepository.findMediaByCursor(
            conversationId, type, startInstant, endInstant, cursor, limit);
        
        // Build pagination info
        boolean hasNextPage = messages.size() > limit;
        if (hasNextPage) {
            messages = messages.subList(0, limit);
        }
        
        Long nextCursor = null;
        if (hasNextPage && !messages.isEmpty()) {
            nextCursor = messages.get(messages.size() - 1).getId();
        }
        
        List<MessageResponse> messageResponses = messages.stream()
                .map(messageMapper::toResponse)
                .toList();
        
        // Get statistics
        java.util.Map<String, Long> stats = messageRepository.getMediaStatistics(conversationId);
        
        com.chattrix.api.responses.MediaStatisticsResponse statistics = 
            com.chattrix.api.responses.MediaStatisticsResponse.builder()
                .totalImages(stats.get("images"))
                .totalVideos(stats.get("videos"))
                .totalAudios(stats.get("audios"))
                .totalFiles(stats.get("files"))
                .totalLinks(stats.get("links"))
                .totalMedia(stats.get("total"))
                .totalSize(0L) // TODO: Calculate from metadata if needed
                .build();
        
        MediaSearchResponse.CursorPaginationInfo pagination =
            MediaSearchResponse.CursorPaginationInfo.builder()
                .nextCursor(nextCursor)
                .hasNextPage(hasNextPage)
                .pageSize(messageResponses.size())
                .build();
        
        return MediaSearchResponse.builder()
                .messages(messageResponses)
                .statistics(statistics)
                .pagination(pagination)
                .build();
    }
    
    /**
     * Get media statistics only (without messages)
     */
    public com.chattrix.api.responses.MediaStatisticsResponse getMediaStatistics(Long userId, Long conversationId) {
        // Verify user is participant
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }
        
        java.util.Map<String, Long> stats = messageRepository.getMediaStatistics(conversationId);
        
        return com.chattrix.api.responses.MediaStatisticsResponse.builder()
                .totalImages(stats.get("images"))
                .totalVideos(stats.get("videos"))
                .totalAudios(stats.get("audios"))
                .totalFiles(stats.get("files"))
                .totalLinks(stats.get("links"))
                .totalMedia(stats.get("total"))
                .totalSize(0L) // TODO: Calculate from metadata if needed
                .build();
    }
}
