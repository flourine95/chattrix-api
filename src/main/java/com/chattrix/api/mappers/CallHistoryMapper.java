package com.chattrix.api.mappers;

import com.chattrix.api.entities.CallHistory;
import com.chattrix.api.responses.CallHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface CallHistoryMapper {
    CallHistoryResponse toResponse(CallHistory callHistory);

    List<CallHistoryResponse> toResponseList(List<CallHistory> callHistories);
}
