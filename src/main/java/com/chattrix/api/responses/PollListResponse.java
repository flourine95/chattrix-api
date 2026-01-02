package com.chattrix.api.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollListResponse {
    private List<PollResponse> polls;
    private Integer totalCount;
    private Integer activeCount;
    private Integer closedCount;
}
