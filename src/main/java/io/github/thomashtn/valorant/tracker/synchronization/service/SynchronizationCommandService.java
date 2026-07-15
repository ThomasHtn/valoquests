package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;

/** Defines administrative synchronization commands and monitoring queries. */
public interface SynchronizationCommandService {

    /** @return completed standard synchronization summary */
    SynchronizationResponse synchronizeAllPlayers();

    /** @param playerId player to synchronize @return synchronization summary */
    SynchronizationResponse synchronizePlayer(long playerId);

    /** @return accepted deep-synchronization request */
    SynchronizationResponse requestDeepSynchronizationForAllPlayers();

    /** @param playerId player to import deeply @return accepted synchronization request */
    SynchronizationResponse requestDeepSynchronizationForPlayer(long playerId);

    /** @return latest synchronization attempt and latest successful execution */
    SynchronizationResponse findLatest();

    /** @param page zero-based page @param size page size @return synchronization history */
    PageResponse<SynchronizationResponse> findHistory(int page, int size);

    /** @param synchronizationId synchronization identifier @return detailed execution result */
    SynchronizationDetailsResponse findById(long synchronizationId);
}
