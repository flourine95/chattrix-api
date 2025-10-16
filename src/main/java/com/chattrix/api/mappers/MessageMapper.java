package com.chattrix.api.mappers;

import com.chattrix.api.entities.Message;
import com.chattrix.api.responses.MessageDetailResponse;
import com.chattrix.api.responses.MessageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi", uses = {UserMapper.class})
public interface MessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    MessageDetailResponse toDetailResponse(Message message);

    List<MessageDetailResponse> toDetailResponseList(List<Message> messages);

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    MessageResponse toResponse(Message message);
}

