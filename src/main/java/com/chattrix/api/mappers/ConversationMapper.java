package com.chattrix.api.mappers;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.responses.ConversationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface ConversationMapper {

    @Mapping(target = "type", expression = "java(conversation.getType().name())")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "lastMessage", source = "lastMessage")
    ConversationResponse toResponse(Conversation conversation);

    List<ConversationResponse> toResponseList(List<Conversation> conversations);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "role", expression = "java(participant.getRole().name())")
    ConversationResponse.ParticipantResponse toParticipantResponse(ConversationParticipant participant);

    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "sentAt", source = "sentAt")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    ConversationResponse.MessageResponse toMessageResponse(Message message);
}
