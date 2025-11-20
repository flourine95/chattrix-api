package com.chattrix.api.responses;

import com.chattrix.api.entities.CallHistoryStatus;
import com.chattrix.api.entities.CallType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallStatisticsResponse {
    private Integer totalCalls;
    private Integer totalDurationMinutes;
    private Map<CallType, Integer> callsByType;
    private Map<CallHistoryStatus, Integer> callsByStatus;
    private Integer averageCallDuration;
}
