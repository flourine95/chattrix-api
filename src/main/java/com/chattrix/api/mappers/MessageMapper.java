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
}
