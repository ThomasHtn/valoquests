package io.github.thomashtn.valoquests.shared.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link PaginationGuard}.
 */
class PaginationGuardTest {

    /**
     * Verifies that a request inside the accepted range passes.
     */
    @Test
    void shouldAcceptAPageRequestInsideTheAcceptedRange() {
        assertThatCode(() -> PaginationGuard.assertValidPageRequest(0, 10))
            .doesNotThrowAnyException();
    }

    /**
     * Verifies that both ends of the accepted size range are allowed, so the cap itself is
     * reachable rather than off by one.
     */
    @Test
    void shouldAcceptTheBoundariesOfTheAcceptedRange() {
        assertThatCode(() -> PaginationGuard.assertValidPageRequest(0, 1))
            .doesNotThrowAnyException();
        assertThatCode(() ->
            PaginationGuard.assertValidPageRequest(0, PaginationGuard.MAXIMUM_PAGE_SIZE))
            .doesNotThrowAnyException();
    }

    /**
     * Verifies that a negative page index is reported as a caller error.
     */
    @Test
    void shouldRejectANegativePageIndex() {
        assertThatThrownBy(() -> PaginationGuard.assertValidPageRequest(-1, 10))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("page");
    }

    /**
     * Verifies that a size outside the accepted range is reported as a caller error rather than
     * reaching {@code PageRequest.of}, which would answer a 500, or the database, which would
     * fetch the whole table.
     *
     * @param size rejected page size
     */
    @ParameterizedTest
    @ValueSource(ints = {0, -1, PaginationGuard.MAXIMUM_PAGE_SIZE + 1, Integer.MAX_VALUE})
    void shouldRejectAPageSizeOutsideTheAcceptedRange(int size) {
        assertThatThrownBy(() -> PaginationGuard.assertValidPageRequest(0, size))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("size must be between 1 and "
                + PaginationGuard.MAXIMUM_PAGE_SIZE);
    }
}
