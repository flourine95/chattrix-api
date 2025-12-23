package com.chattrix.api.responses;

import lombok.*;

import java.util.List;

/**
 * Generic paginated response wrapper for API endpoints.
 * Contains the data list and pagination metadata.
 *
 * @param <T> the type of data in the response
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginatedResponse<T> {
    private List<T> data;
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private boolean hasNextPage;
    private boolean hasPrevPage;

    /**
     * Creates a paginated response with calculated metadata.
     *
     * @param data  the list of data items
     * @param page  the current page number (0-indexed)
     * @param size  the page size
     * @param total the total number of items
     */
    public PaginatedResponse(List<T> data, int page, int size, long total) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNextPage = page < totalPages - 1;
        this.hasPrevPage = page > 0;
    }
}
