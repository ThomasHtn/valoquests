package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valoquests.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStopReason;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Writes the audit trail of a synchronization execution: its start, each player's outcome, its end.
 *
 * <p>Every write commits on its own, so an execution and its partial outcomes stay visible even when
 * the batch is interrupted.
 */
@Component
public class SynchronizationRecorder {

    /**
     * Repository used to persist global synchronization executions.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Repository used to persist one outcome per processed player.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Clock used to generate deterministic execution timestamps.
     */
    private final Clock clock;

    /**
     * Creates the synchronization recorder.
     *
     * @param synchronizationRepository global execution repository
     * @param playerResultRepository    per-player result repository
     * @param clock                     application clock
     */
    public SynchronizationRecorder(
        SynchronizationRepository synchronizationRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        Clock clock
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.playerResultRepository = playerResultRepository;
        this.clock = clock;
    }

    /**
     * Creates and persists a running synchronization execution.
     *
     * @param trigger synchronization trigger
     * @return persisted execution
     */
    public Synchronization start(SynchronizationTrigger trigger) {
        Synchronization synchronization = new Synchronization();

        synchronization.setType(SynchronizationType.STANDARD);
        synchronization.setTrigger(trigger);
        synchronization.setStatus(SynchronizationStatus.RUNNING);
        synchronization.setStartedAt(clock.instant());

        return synchronizationRepository.save(synchronization);
    }

    /**
     * Marks a batch execution as complete and persists its aggregate values.
     *
     * @param synchronization global execution
     * @param playerCount     selected player count
     * @param summary         aggregated outcomes
     */
    public void complete(
        Synchronization synchronization,
        int playerCount,
        SynchronizationBatchSummary summary
    ) {
        synchronization.setStatus(
            SynchronizationStatus.ofBatch(
                playerCount,
                summary.successfulPlayers(),
                summary.failureCount()
            )
        );
        synchronization.setFinishedAt(clock.instant());
        synchronization.setPlayersProcessed(playerCount);
        synchronization.setFailureCount(summary.failureCount());
        synchronization.setMatchesImported(summary.matchesImported());
        synchronization.setErrorMessage(
            SynchronizationErrorMessage.truncateOrNull(summary.errorMessages())
        );

        synchronizationRepository.save(synchronization);
    }

    /**
     * Persists a successful player outcome.
     *
     * @param synchronization global execution
     * @param result          successful player outcome
     */
    public void recordSuccess(
        Synchronization synchronization,
        PlayerSynchronizationResult result
    ) {
        savePlayerResult(
            synchronization,
            result.player(),
            SynchronizationStatus.COMPLETED,
            result.pagesFetched(),
            result.matchesImported(),
            null,
            result.stopReason()
        );
    }

    /**
     * Persists a failed player outcome.
     *
     * @param synchronization global execution
     * @param player          player whose synchronization failed
     * @param errorMessage    failure description
     */
    public void recordFailure(Synchronization synchronization, Player player, String errorMessage) {
        // No stop reason: the walk never reached a stop condition of its own.
        savePlayerResult(
            synchronization,
            player,
            SynchronizationStatus.FAILED,
            0,
            0,
            errorMessage,
            null
        );
    }

    /**
     * Persists one player outcome within a global execution.
     *
     * @param synchronization global execution
     * @param player          processed player
     * @param status          outcome status
     * @param pagesFetched    retrieved page count
     * @param matchesImported imported match count
     * @param errorMessage    optional failure description
     * @param stopReason      condition that ended the walk, {@code null} when none completed
     */
    private void savePlayerResult(
        Synchronization synchronization,
        Player player,
        SynchronizationStatus status,
        int pagesFetched,
        int matchesImported,
        String errorMessage,
        SynchronizationStopReason stopReason
    ) {
        SynchronizationPlayerResult result =
            new SynchronizationPlayerResult();

        result.setSynchronization(synchronization);
        result.setPlayer(player);
        result.setStatus(status);
        result.setPagesFetched(pagesFetched);
        result.setMatchesImported(matchesImported);
        result.setErrorMessage(SynchronizationErrorMessage.truncateOrNull(errorMessage));
        result.setStopReason(stopReason);

        playerResultRepository.save(result);
    }
}
