package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.player.entity.Player;

import java.time.Instant;

/**
 * Immutable aggregate of all player outcomes in one synchronization batch.
 *
 * @param successfulPlayers               successful player count
 * @param failureCount                    failed player count
 * @param matchesImported                 total imported match count
 * @param lastSuccessfulSynchronizationAt latest successful timestamp
 * @param errorMessages                   aggregated failure descriptions
 */
record SynchronizationBatchSummary(
    int successfulPlayers,
    int failureCount,
    int matchesImported,
    Instant lastSuccessfulSynchronizationAt,
    String errorMessages
) {

    /**
     * Creates an empty summary.
     *
     * @return empty summary
     */
    static SynchronizationBatchSummary empty() {
        return new SynchronizationBatchSummary(0, 0, 0, null, null);
    }

    /**
     * Adds a successful player outcome.
     *
     * @param result successful outcome
     * @return updated summary
     */
    SynchronizationBatchSummary withSuccess(
        SynchronizationExecutionResult result
    ) {
        return new SynchronizationBatchSummary(
            successfulPlayers + 1,
            failureCount,
            matchesImported + result.matchesImported(),
            latestInstant(
                lastSuccessfulSynchronizationAt,
                result.completedAt()
            ),
            errorMessages
        );
    }

    /**
     * Adds a failed player outcome.
     *
     * @param player       failed player
     * @param errorMessage failure description
     * @return updated summary
     */
    SynchronizationBatchSummary withFailure(
        Player player,
        String errorMessage
    ) {
        String playerError = "Player "
            + player.getId()
            + ": "
            + errorMessage;
        String updatedErrors = errorMessages == null
            ? playerError
            : errorMessages + System.lineSeparator() + playerError;

        return new SynchronizationBatchSummary(
            successfulPlayers,
            failureCount + 1,
            matchesImported,
            lastSuccessfulSynchronizationAt,
            updatedErrors
        );
    }

    /**
     * Returns the most recent non-null timestamp.
     *
     * @param current   retained timestamp
     * @param candidate candidate timestamp
     * @return latest timestamp
     */
    private static Instant latestInstant(
        Instant current,
        Instant candidate
    ) {
        if (current == null) {
            return candidate;
        }

        if (candidate == null) {
            return current;
        }

        return candidate.isAfter(current)
            ? candidate
            : current;
    }
}
