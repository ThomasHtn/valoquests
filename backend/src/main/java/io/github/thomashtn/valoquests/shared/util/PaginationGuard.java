package io.github.thomashtn.valoquests.shared.util;

import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;

/**
 * Validates the pagination parameters of a paged query before a page request is built.
 *
 * <p>Reports bad values as a 400 instead of the 500 {@code PageRequest.of} would raise, and caps the size
 * so one request cannot fetch a whole table.
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
