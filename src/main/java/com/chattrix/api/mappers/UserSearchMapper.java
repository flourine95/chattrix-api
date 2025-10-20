package com.chattrix.api.mappers;

import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface UserSearchMapper {

    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "hasConversation", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    UserSearchResponse toSearchResponse(User user);

    List<UserSearchResponse> toSearchResponseList(List<User> users);
}

