package io.github.thomashtn.valoquests.match.model;

import java.util.Map;

/**
 * Summarizes the processing of one Henrik match-history response.
 *
 * @param received number of entries returned by Henrik
 * @param imported number of new player-match associations inserted
 * @param alreadyKnown number of valid associations already stored
 * @param rejected number of malformed, incomplete or unrelated entries
 * @param skipped number of valid entries whose game mode is not imported
 */
public record MatchImportResult(

    int received,
    int imported,
    int alreadyKnown,
    int rejected,
    int skipped
) {

    /**
     * Validates all counters.
     */
    public MatchImportResult {
        if (received < 0 || imported < 0 || alreadyKnown < 0 || rejected < 0 || skipped < 0) {
            throw new IllegalArgumentException(
                "match import counters must not be negative"
            );
        }
        if (imported + alreadyKnown + rejected + skipped != received) {
            throw new IllegalArgumentException(
                "match import counters must equal the received count"
            );
        }
    }

    /**
     * Builds the result from the outcome of each received entry.
     *
     * @param received number of entries returned by Henrik
     * @param counts   number of entries per outcome, an absent outcome counting zero
     * @return the import counters
     */
    public static MatchImportResult of(int received, Map<MatchImportOutcome, Integer> counts) {
        return new MatchImportResult(
            received,
            counts.getOrDefault(MatchImportOutcome.IMPORTED, 0),
            counts.getOrDefault(MatchImportOutcome.ALREADY_KNOWN, 0),
            counts.getOrDefault(MatchImportOutcome.REJECTED, 0),
            counts.getOrDefault(MatchImportOutcome.SKIPPED, 0)
        );
    }

    /**
     * Indicates that every valid match of the page was already stored, a safe pagination boundary.
     *
     * <p>Skipped entries do not count: a page of ignored modes proves nothing about older history.
     */
    public boolean knownHistoryReached() {
        int validMatches = imported + alreadyKnown;
        return validMatches > 0
            && imported == 0
            && alreadyKnown == validMatches;
    }
}
