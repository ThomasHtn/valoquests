package io.github.thomashtn.valorant.tracker.match.model;

/**
 * Summarizes the processing of one Henrik match-history response.
 *
 * @param received number of entries returned by Henrik
 * @param imported number of new player-match associations inserted
 * @param alreadyKnown number of valid associations already stored
 * @param rejected number of malformed, incomplete or unrelated entries
 */
public record MatchImportResult(
    int received,
    int imported,
    int alreadyKnown,
    int rejected
) {

    /** Validates all counters. */
    public MatchImportResult {
        if (received < 0 || imported < 0 || alreadyKnown < 0 || rejected < 0) {
            throw new IllegalArgumentException(
                "match import counters must not be negative"
            );
        }
        if (imported + alreadyKnown + rejected != received) {
            throw new IllegalArgumentException(
                "match import counters must equal the received count"
            );
        }
    }

    /**
     * Indicates that every valid tracked-player match from the page was already
     * persisted, which is a safe incremental-pagination boundary.
     */
    public boolean knownHistoryReached() {
        int validMatches = imported + alreadyKnown;
        return validMatches > 0
            && imported == 0
            && alreadyKnown == validMatches;
    }
}
