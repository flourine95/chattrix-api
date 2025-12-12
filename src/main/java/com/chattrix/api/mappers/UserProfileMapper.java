package com.chattrix.api.mappers;

import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface UserProfileMapper {

    UserProfileResponse toProfileResponse(User user);
}

