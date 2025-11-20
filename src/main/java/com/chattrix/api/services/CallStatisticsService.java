package com.chattrix.api.services;

import com.chattrix.api.entities.CallHistory;
import com.chattrix.api.entities.CallHistoryStatus;
import com.chattrix.api.entities.CallType;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.repositories.CallHistoryRepository;
import com.chattrix.api.responses.CallStatisticsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for calculating call statistics and analytics.
 * Handles aggregation of call data by period, type, and status.
 * Validates: Requirements 15.1, 15.2, 15.3, 15.4, 15.5
 */
@ApplicationScoped
public class CallStatisticsService {

    private static final Logger LOGGER = Logger.getLogger(CallStatisticsService.class.getName());

    @Inject
    CallHistoryRepository callHistoryRepository;

    /**
     * Calculates call statistics for a user over a specified period.
     * Validates: Requirements 15.1, 15.2, 15.3, 15.4, 15.5
     *
     * @param userId the ID of the user
     * @param period the time period (day, week, month, year)
     * @return call statistics response with aggregated data
     * @throws BadRequestException if period parameter is invalid
     */
    public CallStatisticsResponse getStatistics(String userId, String period) {
        LOGGER.log(Level.INFO, "Calculating statistics for user {0}, period {1}",
                new Object[]{userId, period});

        // Parse period parameter and calculate date range
        Instant[] dateRange = calculateDateRange(period);
        Instant startDate = dateRange[0];
        Instant endDate = dateRange[1];

        LOGGER.log(Level.INFO, "Date range: {0} to {1}",
                new Object[]{startDate, endDate});

        // Query call history for period
        List<CallHistory> callHistories = callHistoryRepository.findByUserIdAndDateRange(
                userId,
                startDate,
                endDate
        );

        // Calculate total calls
        int totalCalls = callHistories.size();

        // Aggregate by type
        Map<CallType, Integer> callsByType = new HashMap<>();
        callsByType.put(CallType.AUDIO, 0);
        callsByType.put(CallType.VIDEO, 0);

        // Aggregate by status
        Map<CallHistoryStatus, Integer> callsByStatus = new HashMap<>();
        callsByStatus.put(CallHistoryStatus.COMPLETED, 0);
        callsByStatus.put(CallHistoryStatus.MISSED, 0);
        callsByStatus.put(CallHistoryStatus.REJECTED, 0);
        callsByStatus.put(CallHistoryStatus.FAILED, 0);

        // Calculate total duration
        int totalDurationSeconds = 0;

        for (CallHistory history : callHistories) {
            // Aggregate by type
            callsByType.merge(history.getCallType(), 1, Integer::sum);

            // Aggregate by status
            callsByStatus.merge(history.getStatus(), 1, Integer::sum);

            // Sum duration
            if (history.getDurationSeconds() != null) {
                totalDurationSeconds += history.getDurationSeconds();
            }
        }

        // Convert total duration to minutes
        int totalDurationMinutes = totalDurationSeconds / 60;

        // Calculate average call duration in minutes
        int averageCallDuration = totalCalls > 0 ? totalDurationMinutes / totalCalls : 0;

        LOGGER.log(Level.INFO, "Statistics calculated: totalCalls={0}, totalDurationMinutes={1}, averageCallDuration={2}",
                new Object[]{totalCalls, totalDurationMinutes, averageCallDuration});

        return new CallStatisticsResponse(
                totalCalls,
                totalDurationMinutes,
                callsByType,
                callsByStatus,
                averageCallDuration
        );
    }

    /**
     * Calculates the date range based on the period parameter.
     *
     * @param period the time period (day, week, month, year)
     * @return array with start and end dates [startDate, endDate]
     * @throws BadRequestException if period is invalid
     */
    private Instant[] calculateDateRange(String period) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startDateTime;
        ZonedDateTime endDateTime = now;

        switch (period.toLowerCase()) {
            case "day":
                startDateTime = now.truncatedTo(ChronoUnit.DAYS);
                break;
            case "week":
                startDateTime = now.truncatedTo(ChronoUnit.DAYS).minusDays(now.getDayOfWeek().getValue() - 1);
                break;
            case "month":
                startDateTime = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                break;
            case "year":
                startDateTime = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
                break;
            default:
                throw new BadRequestException("Invalid period parameter. Must be one of: day, week, month, year");
        }

        return new Instant[]{startDateTime.toInstant(), endDateTime.toInstant()};
    }
}
