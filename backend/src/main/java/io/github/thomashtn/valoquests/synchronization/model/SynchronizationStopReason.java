package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Describes why a match-history walk stopped.
 *
 * <p>Reported alongside every synchronization result so an operator can tell a healthy incremental
 * run from a truncated one without reading through the logs.
 */
public enum SynchronizationStopReason {

    /**
     * Henrik returned no match at all, either for a player with no history or past its end.
     */
    EMPTY_PAGE,

    /**
     * Henrik returned a partial page, which only happens at the end of the available history.
     */
    END_OF_HISTORY,

    /**
     * The walk reached a season outside its scope, having completed the seasons it targeted.
     */
    SEASON_BOUNDARY,

    /**
     * Every match of a page was already stored, which is only trusted for a completed season.
     */
    KNOWN_HISTORY_REACHED,

    /**
     * The safety page limit stopped the walk. The season is truncated and stays incomplete.
     */
    PAGE_LIMIT_REACHED
}
