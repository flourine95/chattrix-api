package com.chattrix.api.mappers;

import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}



