package com.chattrix.api.mappers;

import com.chattrix.api.entities.Call;
import com.chattrix.api.entities.CallParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.responses.CallConnectionResponse;
import com.chattrix.api.responses.CallParticipantResponse;
import com.chattrix.api.responses.CallResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface CallMapper {

    @Mapping(target = "id", source = "call.id")
    @Mapping(target = "callerId", source = "call.callerId")
    @Mapping(target = "participants", expression = "java(mapParticipants(call))")
    CallResponse toResponse(Call call);

    default List<CallParticipantResponse> mapParticipants(Call call) {
        if (call.getParticipants() == null) return List.of();
        return call.getParticipants().stream()
                .map(p -> CallParticipantResponse.builder()
                        .userId(p.getUserId())
                        .status(p.getStatus())
                        .joinedAt(p.getJoinedAt())
                        .leftAt(p.getLeftAt())
                        .build())
                .collect(Collectors.toList());
    }

    CallConnectionResponse toConnectionResponse(CallResponse callInfo, String token);
}
