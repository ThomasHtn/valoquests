package io.github.thomashtn.valorant.tracker.synchronization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStopReason;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import java.time.Instant;
import java.util.List;

/**
 * Exposes one synchronization execution and its player-level outcomes.
 */
@Schema(description = "Detailed synchronization execution.")
public record SynchronizationDetailsResponse(

    Long id,
    SynchronizationType type,
    SynchronizationTrigger trigger,
    SynchronizationStatus status,
    Instant startedAt,
    Instant finishedAt,
    int playersProcessed,
    int failureCount,
    int matchesImported,
    String errorMessage,
    List<PlayerResultResponse> players
) {
    /**
     * Exposes one player's outcome within an execution.
     *
     * @param stopReason condition that ended the match-history walk, {@code null} when the player
     *     failed before completing one. Explains a short import without inspecting the logs.
     */
    public record PlayerResultResponse(

        Long playerId,
        String displayName,
        SynchronizationStatus status,
        int pagesFetched,
        int matchesImported,
        String errorMessage,
        SynchronizationStopReason stopReason
    ) {
    }

    /**
     * Creates an immutable synchronization-details response.
     */
    public SynchronizationDetailsResponse {
        players = List.copyOf(players);
    }

}
