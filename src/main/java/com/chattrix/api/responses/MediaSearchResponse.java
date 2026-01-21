package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class MediaSearchResponse {
    private List<MessageResponse> messages;
    private MediaStatisticsResponse statistics;
    private CursorPaginationInfo pagination;
    
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class CursorPaginationInfo {
        private Long nextCursor;
        private Boolean hasNextPage;
        private Integer pageSize;
    }
}
