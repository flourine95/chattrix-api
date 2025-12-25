package com.chattrix.api.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Cursor-based paginated response wrapper for API endpoints.
 * Uses cursor (ID) instead of page number for better performance.
 *
 * @param <T> the type of data in the response
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorPaginatedResponse<T> {
    private List<T> items;
    private PaginationMeta meta;

    /**
     * Creates a cursor-based paginated response with Long cursor.
     *
     * @param items       the list of data items
     * @param nextCursor  the cursor for the next page (null if no more pages)
     * @param limit       the number of items per page
     */
    public CursorPaginatedResponse(List<T> items, Long nextCursor, int limit) {
        this.items = items;
        this.meta = new PaginationMeta(nextCursor, nextCursor != null, limit);
    }

    /**
     * Creates a cursor-based paginated response with String cursor.
     *
     * @param items       the list of data items
     * @param nextCursor  the cursor for the next page (null if no more pages)
     * @param limit       the number of items per page
     */
    public CursorPaginatedResponse(List<T> items, String nextCursor, int limit) {
        this.items = items;
        this.meta = new PaginationMeta(nextCursor, nextCursor != null, limit);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class PaginationMeta {
        private Object nextCursor;  // Can be Long or String
        private boolean hasNextPage;
        private int itemsPerPage;
    }
}
