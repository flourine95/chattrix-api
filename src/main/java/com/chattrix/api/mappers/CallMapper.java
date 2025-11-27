package com.chattrix.api.mappers;

import com.chattrix.api.entities.Call;
import com.chattrix.api.responses.CallConnectionResponse;
import com.chattrix.api.responses.CallResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface CallMapper {

    @Mapping(target = "callerName", ignore = true)
    @Mapping(target = "callerAvatar", ignore = true)
    @Mapping(target = "calleeName", ignore = true)
    @Mapping(target = "calleeAvatar", ignore = true)
    CallResponse toResponse(Call call);

    default CallConnectionResponse toConnectionResponse(CallResponse info, String token) {
        if (info == null) return null;
        return CallConnectionResponse.builder()
                .callInfo(info)
                .token(token)
                .build();
    }
}