package com.chattrix.api.mappers;

import com.chattrix.api.entities.User;
import com.chattrix.api.requests.RegisterRequest;
import com.chattrix.api.responses.ConversationMemberResponse;
import com.chattrix.api.responses.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface UserMapper {
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterRequest request);

    ConversationMemberResponse toConversationMemberResponse(User user);

    List<ConversationMemberResponse> toConversationMemberResponseList(List<User> users);
}