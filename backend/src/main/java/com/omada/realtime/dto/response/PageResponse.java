package com.omada.realtime.dto.response;

import java.util.List;

/**
 * Generic envelope for a page of results. Kept independent of Spring's
 * Page so the JSON shape ({content, page, size, totalElements, totalPages})
 * is stable and doesn't change with Spring Data internals.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages) {
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0);
    }
}
