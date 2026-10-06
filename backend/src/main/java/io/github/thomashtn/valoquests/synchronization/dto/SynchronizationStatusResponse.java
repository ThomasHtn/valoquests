package io.github.thomashtn.valoquests.synchronization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Exposes whether the public data is being rebuilt and when it last was.
 *
 * @param inProgress      whether a synchronization is running, recalculation included
 * @param lastCompletedAt end of the last synchronization that completed or partially succeeded,
 *                        {@code null} when none ever did
 */
@Schema(description = "Public synchronization status.")
public record SynchronizationStatusResponse(
    boolean inProgress,
    Instant lastCompletedAt
) {
}
