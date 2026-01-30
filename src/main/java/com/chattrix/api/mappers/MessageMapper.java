package com.chattrix.api.mappers;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.MessageResponse;
import org.mapstruct.*;

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
    com.chattrix.api.responses.ReplyMessageResponse toReplyMessageResponse(Message message);
    
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
    default com.chattrix.api.responses.PollResponse toPollResponse(Message message) {
        return toPollResponse(message, null);
    }
    
    /**
     * Convert Message to PollResponse with current user context
     * Extracts poll data from metadata and includes user-specific info
     */
    default com.chattrix.api.responses.PollResponse toPollResponse(Message message, Long currentUserId) {
        if (message == null || message.getMetadata() == null) return null;
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> pollData = (java.util.Map<String, Object>) message.getMetadata().get("poll");
        if (pollData == null) return null;
        
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> optionsList = 
            (java.util.List<java.util.Map<String, Object>>) pollData.get("options");
        
        java.util.List<com.chattrix.api.responses.PollOptionResponse> options = new java.util.ArrayList<>();
        int totalVotes = 0;
        
        if (optionsList != null) {
            for (java.util.Map<String, Object> opt : optionsList) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> votes = (java.util.List<Object>) opt.get("votes");
                
                // Convert votes to List<Long>
                java.util.List<Long> voterIds = new java.util.ArrayList<>();
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
                java.util.List<Long> voterIdsToReturn = (Boolean.TRUE.equals(anonymous)) ? null : voterIds;
                
                options.add(com.chattrix.api.responses.PollOptionResponse.builder()
                    .id(((Number) opt.get("id")).longValue())
                    .text((String) opt.get("text"))
                    .voteCount(voteCount)
                    .voterIds(voterIdsToReturn)
                    .hasVoted(hasVoted)
                    .build());
            }
        }
        
        java.time.Instant closesAt = null;
        if (pollData.containsKey("closesAt") && pollData.get("closesAt") != null) {
            closesAt = java.time.Instant.parse(pollData.get("closesAt").toString());
        }
        
        boolean isClosed = closesAt != null && java.time.Instant.now().isAfter(closesAt);
        
        return com.chattrix.api.responses.PollResponse.builder()
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
    default com.chattrix.api.responses.EventResponse toEventResponse(Message message) {
        if (message == null || message.getMetadata() == null) return null;
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> eventData = (java.util.Map<String, Object>) message.getMetadata().get("event");
        if (eventData == null) return null;
        
        @SuppressWarnings("unchecked")
        java.util.List<Object> going = (java.util.List<Object>) eventData.get("going");
        @SuppressWarnings("unchecked")
        java.util.List<Object> maybe = (java.util.List<Object>) eventData.get("maybe");
        @SuppressWarnings("unchecked")
        java.util.List<Object> notGoing = (java.util.List<Object>) eventData.get("notGoing");
        
        java.util.List<Long> goingIds = going != null ? going.stream()
            .map(id -> ((Number) id).longValue()).toList() : new java.util.ArrayList<>();
        java.util.List<Long> maybeIds = maybe != null ? maybe.stream()
            .map(id -> ((Number) id).longValue()).toList() : new java.util.ArrayList<>();
        java.util.List<Long> notGoingIds = notGoing != null ? notGoing.stream()
            .map(id -> ((Number) id).longValue()).toList() : new java.util.ArrayList<>();
        
        java.time.Instant startTime = java.time.Instant.parse(eventData.get("startTime").toString());
        java.time.Instant endTime = java.time.Instant.parse(eventData.get("endTime").toString());
        boolean isPast = java.time.Instant.now().isAfter(endTime);
        
        return com.chattrix.api.responses.EventResponse.builder()
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
