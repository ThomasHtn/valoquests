package io.github.thomashtn.valoquests.synchronization.dto;

import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Exposes the summary of a synchronization execution.
 *
 * @param id internal synchronization identifier
 * @param type synchronization type
 * @param trigger origin of the execution
 * @param status current or final execution status
 * @param startedAt execution start timestamp
 * @param finishedAt execution completion timestamp
 * @param lastAttemptAt timestamp of the represented attempt
 * @param lastSuccessfulSynchronizationAt latest successful player
 *                                                synchronization
 * @param playersProcessed number of processed players
 * @param failureCount number of failed player synchronizations
 * @param matchesImported number of newly imported player-match associations
 * @param errorMessage execution error when the synchronization failed
 */
@Schema(
    description = "Summary of a synchronization execution."
)
public record SynchronizationResponse(

    Long id,
    SynchronizationType type,
    SynchronizationTrigger trigger,
    SynchronizationStatus status,
    Instant startedAt,
    Instant finishedAt,
    Instant lastAttemptAt,
    Instant lastSuccessfulSynchronizationAt,
    int playersProcessed,
    int failureCount,
    int matchesImported,
    String errorMessage
) {
}
