package io.github.thomashtn.valoquests.shared.util;

import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;

/**
 * Validates the pagination parameters of a paged query before a page request is built.
 *
 * <p>{@code PageRequest.of} rejects a negative index or a non-positive size with an
 * {@link IllegalArgumentException}, which the global handler can only report as a 500 — a caller
 * mistake answered as a server fault. Worse, it accepts any positive size, so an unbounded
 * {@code ?size=} on a public endpoint turns one request into a full-table fetch. Both are caller
 * errors, so both are checked here and reported as {@link InvalidRequestException} (HTTP 400).</p>
 *
 * <p>Every paged query in this application shares {@link #MAXIMUM_PAGE_SIZE} on purpose: the cap is
 * a limit on what one request may cost the server, not a per-endpoint preference, and four
 * independent copies of it had already drifted apart in wording.</p>
 */
public final class PaginationGuard {

    /**
     * Largest page size any paged endpoint accepts.
     */
    public static final int MAXIMUM_PAGE_SIZE = 100;

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
