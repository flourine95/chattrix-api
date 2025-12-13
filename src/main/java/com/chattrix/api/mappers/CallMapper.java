package com.chattrix.api.mappers;

import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.CallConnectionResponse;
import com.chattrix.api.responses.CallResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface CallMapper {

    @Mapping(target = "callerId", source = "caller.id")
    @Mapping(target = "callerName", source = "caller.fullName")
    @Mapping(target = "callerAvatar", source = "caller.avatarUrl")

    @Mapping(target = "calleeId", source = "callee.id")
    @Mapping(target = "calleeName", source = "callee.fullName")
    @Mapping(target = "calleeAvatar", source = "callee.avatarUrl")

    @Mapping(target = "id", source = "call.id")
    @Mapping(target = "createdAt", source = "call.createdAt")
    CallResponse toResponse(Call call, User caller, User callee);

    CallConnectionResponse toConnectionResponse(CallResponse callInfo, String token);
}