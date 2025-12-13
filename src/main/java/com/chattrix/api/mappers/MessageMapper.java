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
    @Mapping(target = "senderName", source = "sender.fullName")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "originalMessageId", source = "originalMessage.id")
    @Mapping(target = "mentionedUsers", ignore = true)
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    MessageResponse toResponse(Message message);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    ReplyMessageResponse toReplyMessageResponse(Message message);
}