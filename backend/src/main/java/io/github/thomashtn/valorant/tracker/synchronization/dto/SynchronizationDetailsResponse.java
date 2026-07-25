package io.github.thomashtn.valorant.tracker.synchronization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
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
    public record PlayerResultResponse(

        Long playerId,
        String displayName,
        SynchronizationStatus status,
        int pagesFetched,
        int matchesImported,
        String errorMessage
    ) {
    }

    /**
     * Creates an immutable synchronization-details response.
     */
    public SynchronizationDetailsResponse {
        players = List.copyOf(players);
    }

}
