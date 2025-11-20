package com.chattrix.api.mappers;

import com.chattrix.api.entities.CallHistory;
import com.chattrix.api.responses.CallHistoryResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting between CallHistory entities and CallHistoryResponse DTOs.
 */
@Mapper(componentModel = "cdi")
public interface CallHistoryMapper {

    /**
     * Maps a CallHistory entity to a CallHistoryResponse DTO.
     *
     * @param callHistory the CallHistory entity to map
     * @return the mapped CallHistoryResponse DTO
     */
    CallHistoryResponse toResponse(CallHistory callHistory);

    /**
     * Maps a list of CallHistory entities to a list of CallHistoryResponse DTOs.
     *
     * @param callHistories the list of CallHistory entities to map
     * @return the list of mapped CallHistoryResponse DTOs
     */
    List<CallHistoryResponse> toResponseList(List<CallHistory> callHistories);
}
