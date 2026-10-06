package io.github.thomashtn.valoquests.shared.util;

import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;

/**
 * Validates the pagination parameters of a paged query before a page request is built.
 *
 * <p>{@code PageRequest.of} would reject a bad index or size with an {@link IllegalArgumentException},
 * reported as a 500, and accepts any positive size, so an unbounded {@code ?size=} would fetch a
 * whole table. Both are caller errors, reported here as {@link InvalidRequestException} (HTTP 400).</p>
 *
 * <p>Every paged query shares {@link #MAXIMUM_PAGE_SIZE} on purpose: the cap is a limit on what one
 * request may cost the server, not a per-endpoint preference.</p>
 */
public final class PaginationGuard {

    /**
     * Largest page size any paged endpoint accepts.
     */
    public static final int MAXIMUM_PAGE_SIZE = 100;

    /**
     * Not instantiable: static helpers only.
     */
    private PaginationGuard() {
    }

    /**
     * Fails fast when a page index or page size is outside the accepted range.
     *
     * @param page zero-based page index
     * @param size number of elements requested in one page
     * @throws InvalidRequestException when {@code page} is negative, or {@code size} is below 1 or
     *     above {@link #MAXIMUM_PAGE_SIZE}
     */
    public static void assertValidPageRequest(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new InvalidRequestException("size must be between 1 and " + MAXIMUM_PAGE_SIZE);
        }
    }
}
