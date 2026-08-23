package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Contains the outcome of one player's match-history walk.
 *
 * @param pagesFetched number of Henrik match-history pages retrieved
 * @param matchesImported number of newly imported player matches
 * @param stopReason condition that ended the walk
 */
public record MatchHistoryWalkResult(

    int pagesFetched,
    int matchesImported,
    SynchronizationStopReason stopReason
) {

    /**
     * Validates the immutable result.
     */
    public MatchHistoryWalkResult {
        if (pagesFetched < 0) {
            throw new IllegalArgumentException(
                "pagesFetched must not be negative"
            );
        }
        if (matchesImported < 0) {
            throw new IllegalArgumentException(
                "matchesImported must not be negative"
            );
        }
        if (stopReason == null) {
            throw new IllegalArgumentException("stopReason must not be null");
        }
    }

    /**
     * Creates the result of a walk that found no match to process.
     *
     * @return an empty walk result
     */
    public static MatchHistoryWalkResult empty() {
        return new MatchHistoryWalkResult(
            0,
            0,
            SynchronizationStopReason.EMPTY_PAGE
        );
    }
}
