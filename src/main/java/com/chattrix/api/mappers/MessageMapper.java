package com.chattrix.api.mappers;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.EventResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.responses.PollOptionResponse;
import com.chattrix.api.responses.PollResponse;
import com.chattrix.api.responses.ReplyMessageResponse;
import org.mapstruct.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface MessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    @Mapping(target = "senderAvatarUrl", source = "sender.avatarUrl")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessage", source = "replyToMessage")
    @Mapping(target = "originalMessageId", source = "originalMessage.id")
    @Mapping(target = "pinnedBy", source = "pinnedBy.id")
    @Mapping(target = "pinnedByUsername", source = "pinnedBy.username")
    @Mapping(target = "pinnedByFullName", source = "pinnedBy.fullName")
    @Mapping(target = "scheduledStatus", expression = "java(message.getScheduledStatus() != null ? message.getScheduledStatus().name() : null)")
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    MessageResponse toResponse(Message message);
    
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    ReplyMessageResponse toReplyMessageResponse(Message message);
    
    /**
     * Helper method to map User fields with a prefix.
     * This demonstrates how to create reusable mapping logic.
     */
    default void mapUserWithPrefix(User user, String prefix, MessageResponse target) {
        if (user == null) return;
        
        switch (prefix) {
            case "sender":
                target.setSenderId(user.getId());
                target.setSenderUsername(user.getUsername());
                target.setSenderFullName(user.getFullName());
                target.setSenderAvatarUrl(user.getAvatarUrl());
                break;
            case "pinnedBy":
                target.setPinnedBy(user.getId());
                target.setPinnedByUsername(user.getUsername());
                target.setPinnedByFullName(user.getFullName());
                break;
        }
    }
    
    /**
     * Convert Message to PollResponse
     * Extracts poll data from metadata
     */
    default PollResponse toPollResponse(Message message) {
        return toPollResponse(message, null);
    }
    
    /**
     * Convert Message to PollResponse with current user context
     * Extracts poll data from metadata and includes user-specific info
     */
    default PollResponse toPollResponse(Message message, Long currentUserId) {
        if (message == null || message.getMetadata() == null) return null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");
        if (pollData == null) return null;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> optionsList =
            (List<Map<String, Object>>) pollData.get("options");
        
        List<PollOptionResponse> options = new ArrayList<>();
        int totalVotes = 0;
        
        if (optionsList != null) {
            for (Map<String, Object> opt : optionsList) {
                @SuppressWarnings("unchecked")
                List<Object> votes = (List<Object>) opt.get("votes");
                
                // Convert votes to List<Long>
                List<Long> voterIds = new ArrayList<>();
                if (votes != null) {
                    for (Object id : votes) {
                        if (id instanceof Number) {
                            voterIds.add(((Number) id).longValue());
                        }
                    }
                }
                
                int voteCount = voterIds.size();
                totalVotes += voteCount;
                
                // Check if current user voted for this option
                Boolean hasVoted = null;
                if (currentUserId != null) {
                    hasVoted = voterIds.contains(currentUserId);
                }
                
                // Check if poll is anonymous
                Boolean anonymous = (Boolean) pollData.get("anonymous");
                List<Long> voterIdsToReturn = (Boolean.TRUE.equals(anonymous)) ? null : voterIds;
                
                options.add(PollOptionResponse.builder()
                    .id(((Number) opt.get("id")).longValue())
                    .text((String) opt.get("text"))
                    .voteCount(voteCount)
                    .voterIds(voterIdsToReturn)
                    .hasVoted(hasVoted)
                    .build());
            }
        }
        
        Instant closesAt = null;
        if (pollData.containsKey("closesAt") && pollData.get("closesAt") != null) {
            closesAt = Instant.parse(pollData.get("closesAt").toString());
        }
        
        boolean isClosed = closesAt != null && Instant.now().isAfter(closesAt);
        
        return PollResponse.builder()
            .messageId(message.getId())
            .question((String) pollData.get("question"))
            .options(options)
            .allowMultiple((Boolean) pollData.get("allowMultiple"))
            .anonymous((Boolean) pollData.get("anonymous"))
            .closesAt(closesAt)
            .isClosed(isClosed)
            .totalVotes(totalVotes)
            .createdBy(message.getSender().getId())
            .createdByUsername(message.getSender().getUsername())
            .createdAt(message.getCreatedAt())
            .build();
    }
    
    /**
     * Convert Message to EventResponse
     * Extracts event data from metadata
     */
    default EventResponse toEventResponse(Message message) {
        if (message == null || message.getMetadata() == null) return null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) message.getMetadata().get("event");
        if (eventData == null) return null;
        
        @SuppressWarnings("unchecked")
        List<Object> going = (List<Object>) eventData.get("going");
        @SuppressWarnings("unchecked")
        List<Object> maybe = (List<Object>) eventData.get("maybe");
        @SuppressWarnings("unchecked")
        List<Object> notGoing = (List<Object>) eventData.get("notGoing");
        
        List<Long> goingIds = going != null ? going.stream()
            .map(id -> ((Number) id).longValue()).toList() : new ArrayList<>();
        List<Long> maybeIds = maybe != null ? maybe.stream()
            .map(id -> ((Number) id).longValue()).toList() : new ArrayList<>();
        List<Long> notGoingIds = notGoing != null ? notGoing.stream()
            .map(id -> ((Number) id).longValue()).toList() : new ArrayList<>();
        
        Instant startTime = Instant.parse(eventData.get("startTime").toString());
        Instant endTime = Instant.parse(eventData.get("endTime").toString());
        boolean isPast = Instant.now().isAfter(endTime);
        
        return EventResponse.builder()
            .messageId(message.getId())
            .title((String) eventData.get("title"))
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
            .isPast(isPast)
            .createdBy(message.getSender().getId())
            .createdByUsername(message.getSender().getUsername())
            .createdAt(message.getCreatedAt())
            .build();
    }
}
