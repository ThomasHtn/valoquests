package io.github.thomashtn.valoquests.match.model;

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
     * Indicates that every valid tracked-player match from the page was already
     * persisted, which is a safe incremental-pagination boundary.
     *
     * <p>Skipped entries are deliberately excluded from the valid count: a page holding nothing but
     * modes the tracker ignores proves nothing about the history behind it, and must not be read as
     * a boundary.
     */
    public boolean knownHistoryReached() {
        int validMatches = imported + alreadyKnown;
        return validMatches > 0
            && imported == 0
            && alreadyKnown == validMatches;
    }
}
