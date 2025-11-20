package com.chattrix.api.mappers;

import com.chattrix.api.entities.Call;
import com.chattrix.api.responses.CallResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Call entities and CallResponse DTOs.
 * Note: Caller and callee names/avatars must be set manually after mapping
 * as they are not stored in the Call entity.
 */
@Mapper(componentModel = "cdi")
public interface CallMapper {

    /**
     * Maps a Call entity to a CallResponse DTO.
     * Note: callerName, callerAvatar, calleeName, and calleeAvatar will be null
     * and must be populated separately by the service layer.
     *
     * @param call the Call entity to map
     * @return the mapped CallResponse DTO
     */
    @Mapping(target = "callId", source = "id")
    @Mapping(target = "callerName", ignore = true)
    @Mapping(target = "callerAvatar", ignore = true)
    @Mapping(target = "calleeName", ignore = true)
    @Mapping(target = "calleeAvatar", ignore = true)
    CallResponse toResponse(Call call);
}
