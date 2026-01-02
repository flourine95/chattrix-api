package com.chattrix.api.mappers;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.TypingUserDto;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface WebSocketMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "sender", source = "sender")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessage", source = "replyToMessage")
    @Mapping(target = "mentionedUsers", ignore = true)
    @Mapping(target = "mediaUrl", expression = "java(extractMetadataString(message, \"mediaUrl\"))")
    @Mapping(target = "thumbnailUrl", expression = "java(extractMetadataString(message, \"thumbnailUrl\"))")
    @Mapping(target = "fileName", expression = "java(extractMetadataString(message, \"fileName\"))")
    @Mapping(target = "fileSize", expression = "java(extractMetadataLong(message, \"fileSize\"))")
    @Mapping(target = "duration", expression = "java(extractMetadataInteger(message, \"duration\"))")
    @Mapping(target = "latitude", expression = "java(extractMetadataDouble(message, \"latitude\"))")
    @Mapping(target = "longitude", expression = "java(extractMetadataDouble(message, \"longitude\"))")
    @Mapping(target = "locationName", expression = "java(extractMetadataString(message, \"locationName\"))")
    OutgoingMessageDto toOutgoingMessageResponse(Message message);

    @Mapping(target = "userId", source = "id")
    TypingUserDto toTypingUserResponse(User user);

    UserResponse toUserResponse(User user);

    default String extractMetadataString(Message message, String key) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get(key);
        return value != null ? value.toString() : null;
    }

    default Long extractMetadataLong(Message message, String key) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default Integer extractMetadataInteger(Message message, String key) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default Double extractMetadataDouble(Message message, String key) {
        if (message.getMetadata() == null) return null;
        Object value = message.getMetadata().get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
