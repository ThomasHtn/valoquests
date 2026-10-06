package io.github.thomashtn.valoquests.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

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

    /**
     * Wraps one page's mapped content with that page's position and totals.
     *
     * @param page    page as the repository returned it
     * @param content the page's rows, already mapped
     * @param <T>     mapped row type
     * @return the page response
     */
    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return new PageResponse<>(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

}
