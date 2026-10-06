package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationStatusResponse;

/**
 * Provides read-only access to persisted synchronization executions.
 */
public interface SynchronizationQueryService {

    /**
     * Returns the most recently started synchronization execution.
     *
     * @return the latest execution summary
     * @throws ResourceNotFoundException when no execution has ever been recorded
     */
    SynchronizationResponse findLatest();

    /**
     * Returns whether a synchronization is running and when the last successful one finished.
     *
     * @return the public synchronization status
     */
    SynchronizationStatusResponse findStatus();

    /**
     * Returns synchronization history ordered from newest to oldest.
     *
     * @param page zero-based page index
     * @param size number of executions returned per page
     * @return the requested page of execution summaries
     */
    PageResponse<SynchronizationResponse> findHistory(int page, int size);

    /**
     * Returns one execution and every persisted per-player result.
     *
     * @param synchronizationId internal synchronization identifier
     * @return the execution with one detailed result per processed player
     * @throws ResourceNotFoundException when no execution carries that identifier
     */
    SynchronizationDetailsResponse findById(long synchronizationId);
}
