package io.github.thomashtn.valorant.tracker.match.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for {@link MatchImportResult}. */
class MatchImportResultTest {

    /** Existing valid matches form a safe incremental-history boundary. */
    @Test
    void shouldDetectKnownHistoryBoundary() {
        MatchImportResult result = new MatchImportResult(10, 0, 8, 2);

        assertThat(result.knownHistoryReached()).isTrue();
    }

    /** Rejected-only pages must not stop pagination. */
    @Test
    void shouldNotTreatRejectedPageAsKnownHistory() {
        MatchImportResult result = new MatchImportResult(10, 0, 0, 10);

        assertThat(result.knownHistoryReached()).isFalse();
    }

    /** Newly imported matches always require pagination to continue. */
    @Test
    void shouldNotStopWhenPageContainsNewMatches() {
        MatchImportResult result = new MatchImportResult(10, 1, 8, 1);

        assertThat(result.knownHistoryReached()).isFalse();
    }

    /** Inconsistent counters are rejected immediately. */
    @Test
    void shouldRejectInconsistentCounters() {
        assertThatThrownBy(() -> new MatchImportResult(10, 3, 3, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("received count");
    }
}
