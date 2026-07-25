package io.github.thomashtn.valorant.tracker.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Generic immutable representation of a paginated API result.
 */
@Schema(description = "Paginated API response.")
public record PageResponse<T>(

    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    /**
     * Creates an immutable page response.
     */
    public PageResponse {
        content = List.copyOf(content);
    }

}
