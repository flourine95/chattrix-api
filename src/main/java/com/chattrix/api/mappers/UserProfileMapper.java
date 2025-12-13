package com.chattrix.api.mappers;

import com.chattrix.api.entities.User;
import com.chattrix.api.responses.UserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface UserProfileMapper {

    UserProfileResponse toProfileResponse(User user);
}

