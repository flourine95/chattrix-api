package com.chattrix.api.mappers;

import com.chattrix.api.entities.Contact;
import com.chattrix.api.responses.ContactResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ContactMapper {

    @Mapping(target = "contactUserId", source = "contactUser.id")
    @Mapping(target = "username", source = "contactUser.username")
    @Mapping(target = "fullName", source = "contactUser.fullName")
    @Mapping(target = "avatarUrl", source = "contactUser.avatarUrl")
    @Mapping(target = "isOnline", source = "contactUser.online")
    @Mapping(target = "lastSeen", source = "contactUser.lastSeen")
    @Mapping(target = "isFavorite", source = "favorite")
    ContactResponse toResponse(Contact contact);

    List<ContactResponse> toResponseList(List<Contact> contacts);
}
