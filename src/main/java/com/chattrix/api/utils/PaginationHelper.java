package com.chattrix.api.utils;

import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.responses.CursorPaginatedResponse;

import java.util.List;
import java.util.function.Function;

/**
 * Helper class for cursor-based pagination logic
 */
public class PaginationHelper {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 20;

    /**
     * Validate and normalize pagination limit
     * @param limit requested limit
     * @return normalized limit (between MIN_LIMIT and MAX_LIMIT)
     * @throws BusinessException if limit < MIN_LIMIT
     */
    public static int validateLimit(int limit) {
        if (limit < MIN_LIMIT)
            throw BusinessException.badRequest("Limit must be at least " + MIN_LIMIT, "INVALID_LIMIT");
        
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * Build cursor paginated response from a list of items
     * Repository should fetch limit+1 items to determine if there are more
     * 
     * @param items list of items (should be limit+1 size from repository)
     * @param limit the requested limit
     * @param mapper function to map entity to response DTO
     * @param <T> entity type
     * @param <R> response DTO type
     * @return CursorPaginatedResponse with mapped items and next cursor
     */
    public static <T, R> CursorPaginatedResponse<R> buildResponse(
            List<T> items,
            int limit,
            Function<T, R> mapper,
            Function<R, Long> cursorExtractor) {
        
        boolean hasMore = items.size() > limit;
        if (hasMore)
            items = items.subList(0, limit);

        List<R> responses = items.stream()
                .map(mapper)
                .toList();

        Long nextCursor = hasMore && !responses.isEmpty() 
                ? cursorExtractor.apply(responses.get(responses.size() - 1))
                : null;

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    /**
     * Build cursor paginated response when items are already mapped to DTOs
     * 
     * @param mappedItems list of already mapped items (should be limit+1 size)
     * @param limit the requested limit
     * @param cursorExtractor function to extract cursor from response DTO
     * @param <R> response DTO type
     * @return CursorPaginatedResponse with items and next cursor
     */
    public static <R> CursorPaginatedResponse<R> buildResponse(
            List<R> mappedItems,
            int limit,
            Function<R, Long> cursorExtractor) {
        
        boolean hasMore = mappedItems.size() > limit;
        if (hasMore)
            mappedItems = mappedItems.subList(0, limit);

        Long nextCursor = hasMore && !mappedItems.isEmpty()
                ? cursorExtractor.apply(mappedItems.get(mappedItems.size() - 1))
                : null;

        return new CursorPaginatedResponse<>(mappedItems, nextCursor, limit);
    }

    /**
     * Process list for pagination - trim to limit and determine if there are more items
     * Returns a record with the trimmed list and hasMore flag
     * 
     * @param items list of items (should be limit+1 size from repository)
     * @param limit the requested limit
     * @param <T> item type
     * @return PaginationResult with trimmed list and hasMore flag
     */
    public static <T> PaginationResult<T> processForPagination(List<T> items, int limit) {
        boolean hasMore = items.size() > limit;
        if (hasMore)
            items = items.subList(0, limit);
        
        return new PaginationResult<>(items, hasMore);
    }

    /**
     * Result of pagination processing
     */
    public record PaginationResult<T>(List<T> items, boolean hasMore) {}
}
