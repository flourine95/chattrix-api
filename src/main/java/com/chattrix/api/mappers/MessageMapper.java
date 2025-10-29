package com.chattrix.api.mappers;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.*;
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
    @Mapping(target = "senderName", source = "sender.fullName")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    @Mapping(target = "replyToMessageId", source = "replyToMessage.id")
    @Mapping(target = "replyToMessage", source = "replyToMessage")
    @Mapping(target = "mentionedUsers", ignore = true)
    MessageResponse toResponse(Message message);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "senderName", source = "sender.fullName")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    ReplyMessageResponse toReplyMessageResponse(Message message);

    @Mapping(target = "name", source = "fullName")
    MentionedUserResponse toMentionedUserResponse(User user);

    List<MentionedUserResponse> toMentionedUserResponseList(List<User> users);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "status", expression = "java(user.isOnline() ? \"ONLINE\" : \"OFFLINE\")")
    ConversationMemberResponse toConversationMemberResponse(User user);

    List<ConversationMemberResponse> toConversationMemberResponseList(List<User> users);
}

