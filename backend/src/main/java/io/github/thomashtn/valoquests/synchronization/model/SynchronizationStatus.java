package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Describes the lifecycle state of a synchronization execution.
 */
public enum SynchronizationStatus {

    /**
     * Execution has been created but has not started yet.
     */
    PENDING,

    /**
     * Execution is currently processing data.
     */
    RUNNING,

    /**
     * Some players succeeded while other players failed.
     */
    PARTIAL,

    /**
     * Execution completed successfully.
     */
    COMPLETED,

    /**
     * Execution did not complete successfully.
     */
    FAILED,

    /**
     * Execution was explicitly cancelled.
     */
    CANCELLED;

    /**
     * Resolves the status of a batch from its player outcomes.
     *
     * @param playerCount       players the batch covered
     * @param successfulPlayers players synchronized without failure
     * @param failureCount      players whose synchronization failed
     * @return completed when nothing failed, failed when nothing succeeded, partial otherwise
     */
    public static SynchronizationStatus ofBatch(int playerCount, int successfulPlayers, int failureCount) {
        if (playerCount == 0 || failureCount == 0) {
            return COMPLETED;
        }

        if (successfulPlayers == 0) {
            return FAILED;
        }

        return PARTIAL;
    }
}
