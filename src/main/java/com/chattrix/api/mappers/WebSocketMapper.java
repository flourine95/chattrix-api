package com.chattrix.api.mappers;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserResponse;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.TypingUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", uses = {MessageMapper.class})
public interface WebSocketMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "sender", source = "sender")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessage", source = "replyToMessage")
    @Mapping(target = "mentionedUsers", ignore = true)
    OutgoingMessageDto toOutgoingMessageResponse(Message message);

    @Mapping(target = "userId", source = "id")
    TypingUserDto toTypingUserResponse(User user);

    UserResponse toUserResponse(User user);
}
