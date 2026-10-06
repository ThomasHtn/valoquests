package io.github.thomashtn.valoquests.match.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MatchImportResult}.
 */
class MatchImportResultTest {

    /**
     * Existing valid matches form a safe incremental-history boundary.
     */
    @Test
    void shouldDetectKnownHistoryBoundary() {
        MatchImportResult result = new MatchImportResult(10, 0, 8, 2, 0);

        assertThat(result.knownHistoryReached()).isTrue();
    }

    /**
     * Rejected-only pages must not stop pagination.
     */
    @Test
    void shouldNotTreatRejectedPageAsKnownHistory() {
        MatchImportResult result = new MatchImportResult(10, 0, 0, 10, 0);

        assertThat(result.knownHistoryReached()).isFalse();
    }

    /**
     * Verifies that a page holding only ignored game modes does not stop pagination.
     *
     * <p>The matches that matter may sit on the next page, so a boundary here would truncate the season.
     */
    @Test
    void shouldNotTreatSkippedPageAsKnownHistory() {
        MatchImportResult result = new MatchImportResult(10, 0, 0, 0, 10);

        assertThat(result.knownHistoryReached()).isFalse();
    }

    /**
     * Skipped matches never mask an existing-history boundary.
     */
    @Test
    void shouldDetectKnownHistoryBoundaryDespiteSkippedMatches() {
        MatchImportResult result = new MatchImportResult(10, 0, 3, 0, 7);

        assertThat(result.knownHistoryReached()).isTrue();
    }

    /**
     * Newly imported matches always require pagination to continue.
     */
    @Test
    void shouldNotStopWhenPageContainsNewMatches() {
        MatchImportResult result = new MatchImportResult(10, 1, 8, 1, 0);

        assertThat(result.knownHistoryReached()).isFalse();
    }

    /**
     * Inconsistent counters are rejected immediately.
     */
    @Test
    void shouldRejectInconsistentCounters() {
        assertThatThrownBy(() -> new MatchImportResult(10, 3, 3, 3, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("received count");
    }

    /**
     * Negative counters are rejected immediately.
     */
    @Test
    void shouldRejectNegativeCounters() {
        assertThatThrownBy(() -> new MatchImportResult(10, 10, 0, 0, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be negative");
    }

    @Test
    @DisplayName("Counts each outcome into its counter, an absent outcome counting zero")
    void shouldBuildCountersFromOutcomeCounts() {
        MatchImportResult result = MatchImportResult.of(6, Map.of(
            MatchImportOutcome.IMPORTED, 3,
            MatchImportOutcome.REJECTED, 1,
            MatchImportOutcome.SKIPPED, 2
        ));

        assertThat(result).isEqualTo(new MatchImportResult(6, 3, 0, 1, 2));
    }
}
