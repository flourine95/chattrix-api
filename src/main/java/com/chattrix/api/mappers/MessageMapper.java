package com.chattrix.api.mappers;

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
        uses = {UserMapper.class}
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
    @Mapping(target = "pollId", source = "poll.id")
    @Mapping(target = "poll", source = "poll")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "pinnedBy", source = "pinnedBy.id")
    @Mapping(target = "pinnedByUsername", source = "pinnedBy.username")
    @Mapping(target = "pinnedByFullName", source = "pinnedBy.fullName")
    @Mapping(target = "mentionedUsers", ignore = true)
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "scheduledStatus", expression = "java(message.getScheduledStatus() != null ? message.getScheduledStatus().name() : null)")
    MessageResponse toResponse(Message message);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    ReplyMessageResponse toReplyMessageResponse(Message message);
}
