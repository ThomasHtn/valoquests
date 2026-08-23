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
    CANCELLED
}
