package io.github.thomashtn.valoquests.synchronization.dto;

import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStopReason;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Exposes one synchronization execution and its player-level outcomes.
 *
 * @param id               internal execution identifier
 * @param type             synchronization type
 * @param trigger          scheduled or manual origin
 * @param status           execution status
 * @param startedAt        start of the execution
 * @param finishedAt       end of the execution, {@code null} while it runs
 * @param playersProcessed players the execution covered
 * @param failureCount     players whose synchronization failed
 * @param matchesImported  matches imported across every player
 * @param errorMessage     execution-level error, {@code null} when none
 * @param players          one result per processed player
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
     * @param playerId        internal player identifier
     * @param displayName     name shown in the application
     * @param status          outcome of this player's synchronization
     * @param pagesFetched    Henrik match-history pages read
     * @param matchesImported matches imported for this player
     * @param errorMessage    failure description, {@code null} when the player succeeded
     * @param stopReason      why the walk ended, {@code null} when the player failed before
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
