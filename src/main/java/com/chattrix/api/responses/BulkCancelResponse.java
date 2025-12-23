package com.chattrix.api.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkCancelResponse {
    private int cancelledCount;
    private List<Long> failedIds;
}
