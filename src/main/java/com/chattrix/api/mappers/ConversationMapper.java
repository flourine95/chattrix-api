package com.chattrix.api.mappers;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.responses.ConversationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ConversationMapper {

    @Mapping(target = "unreadCount", ignore = true)
    ConversationResponse toResponse(Conversation conversation);

    List<ConversationResponse> toResponseList(List<Conversation> conversations);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "online", source = "user.online")
    @Mapping(target = "lastSeen", source = "user.lastSeen")
    ConversationResponse.ParticipantResponse toParticipantResponse(ConversationParticipant participant);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderFullName", source = "sender.fullName")
    @Mapping(target = "senderAvatarUrl", source = "sender.avatarUrl")
    @Mapping(target = "readCount", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    ConversationResponse.MessageResponse toMessageResponse(Message message);
}