package com.chattrix.api.mappers;

import com.chattrix.api.dto.MessageMetadata;
import com.chattrix.api.entities.Message;
import com.chattrix.api.responses.MessageDetailResponse;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.responses.ReplyMessageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {UserMapper.class, MessageMetadataMapper.class}
)
public interface MessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    MessageDetailResponse toDetailResponse(Message message);

    List<MessageDetailResponse> toDetailResponseList(List<Message> messages);

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "originalMessageId", source = "originalMessage.id")
    @Mapping(target = "pollId", ignore = true) // Poll data in JSONB metadata
    @Mapping(target = "poll", ignore = true) // Poll data in JSONB metadata
    @Mapping(target = "eventId", ignore = true) // Event data in JSONB metadata
    @Mapping(target = "event", ignore = true) // Event data in JSONB metadata
    @Mapping(target = "pinnedBy", source = "pinnedBy.id")
    @Mapping(target = "pinnedByUsername", source = "pinnedBy.username")
    @Mapping(target = "pinnedByFullName", source = "pinnedBy.fullName")
    @Mapping(target = "mentionedUsers", ignore = true)
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "scheduledStatus", expression = "java(message.getScheduledStatus() != null ? message.getScheduledStatus().name() : null)")
    // Extract metadata fields from JSONB
    @Mapping(target = "mediaUrl", expression = "java(extractMediaUrl(message))")
    @Mapping(target = "thumbnailUrl", expression = "java(extractThumbnailUrl(message))")
    @Mapping(target = "fileName", expression = "java(extractFileName(message))")
    @Mapping(target = "fileSize", expression = "java(extractFileSize(message))")
    @Mapping(target = "duration", expression = "java(extractDuration(message))")
    @Mapping(target = "latitude", expression = "java(extractLatitude(message))")
    @Mapping(target = "longitude", expression = "java(extractLongitude(message))")
    @Mapping(target = "locationName", expression = "java(extractLocationName(message))")
    @Mapping(target = "failedReason", expression = "java(extractFailedReason(message))")
    MessageResponse toResponse(Message message);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    ReplyMessageResponse toReplyMessageResponse(Message message);
    
    // Helper methods to extract metadata fields
    default String extractMediaUrl(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("mediaUrl");
        return value != null ? value.toString() : null;
    }
    
    default String extractThumbnailUrl(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("thumbnailUrl");
        return value != null ? value.toString() : null;
    }
    
    default String extractFileName(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("fileName");
        return value != null ? value.toString() : null;
    }
    
    default Long extractFileSize(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("fileSize");
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }
    
    default Integer extractDuration(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("duration");
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }
    
    default Double extractLatitude(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("latitude");
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }
    
    default Double extractLongitude(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("longitude");
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }
    
    default String extractLocationName(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("locationName");
        return value != null ? value.toString() : null;
    }
    
    default String extractFailedReason(Message message) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get("failedReason");
        return value != null ? value.toString() : null;
    }
}
