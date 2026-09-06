package io.github.thomashtn.valoquests.synchronization.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
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
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes and records scheduled and manual synchronizations.
 *
 * <p>The service deliberately keeps external API calls outside a global
 * database transaction. Each execution is persisted before processing starts,
 * and every player outcome is recorded independently so partial failures remain
 * visible without blocking the remaining players.</p>
 *
 * <p>The absence of a surrounding transaction is also what the per-season completion flag depends
 * on: see {@link SeasonSynchronizationStateService}. Making this service transactional would defer
 * every commit to the end of the batch and let a rollback erase the state that says a season is
 * still being caught up. {@link
 * io.github.thomashtn.valoquests.shared.util.NonTransactionalGuard} enforces this at the
 * entry of {@link PlayerSynchronizationService#synchronize}, called once per player below.</p>
 *
 * <p>Importing matches is only half of the workflow: challenge progress and the weekly ranking are
 * derived from the stored matches and stay stale until they are rebuilt. Every execution that
 * actually imported something therefore ends with a challenge recalculation, which is what keeps the
 * ranking live between two scheduled runs.</p>
 */
@Service
public class DefaultSynchronizationCommandService
    implements SynchronizationCommandService {

    /**
     * Logger used for synchronization lifecycle and failure diagnostics.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultSynchronizationCommandService.class);

    /**
     * Maximum size accepted by synchronization error columns.
     */
    private static final int MAXIMUM_ERROR_MESSAGE_LENGTH = 2_000;

    /**
     * Service used to synchronize one player.
     */
    private final PlayerSynchronizationService playerSynchronizationService;

    /**
     * Repository used to retrieve active tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to persist global synchronization executions.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Repository used to persist one outcome per processed player.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Service used to rebuild challenge progress and the weekly ranking after an import.
     */
    private final ChallengeRecalculationService challengeRecalculationService;

    /**
     * Service used to replay the campaign after an import, so a day's gains show up the same day.
     */
    private final CampaignReplayService campaignReplayService;

    /**
     * Clock used to generate deterministic execution timestamps.
     */
    private final Clock clock;

    /**
     * Creates the synchronization command service.
     *
     * @param playerSynchronizationService     player synchronization service
     * @param playerRepository                 tracked-player repository
     * @param synchronizationRepository        global execution repository
     * @param playerResultRepository           per-player result repository
     * @param challengeRecalculationService    challenge progress recalculation service
     * @param campaignReplayService            campaign replay service
     * @param clock                            application clock
     */
    public DefaultSynchronizationCommandService(
        PlayerSynchronizationService playerSynchronizationService,
        PlayerRepository playerRepository,
        SynchronizationRepository synchronizationRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        ChallengeRecalculationService challengeRecalculationService,
        CampaignReplayService campaignReplayService,
        Clock clock
    ) {
        this.playerSynchronizationService = playerSynchronizationService;
        this.playerRepository = playerRepository;
        this.synchronizationRepository = synchronizationRepository;
        this.playerResultRepository = playerResultRepository;
        this.challengeRecalculationService = challengeRecalculationService;
        this.campaignReplayService = campaignReplayService;
        this.clock = clock;
    }

    /**
     * Executes a synchronization for every active player.
     *
     * @param trigger synchronization trigger
     * @return persisted synchronization summary
     */
    @Override
    public SynchronizationResponse synchronizeAllPlayers(
        SynchronizationTrigger trigger
    ) {
        Synchronization synchronization = startSynchronization(trigger);
        List<Player> players =
            playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED);
        SynchronizationBatchSummary summary = SynchronizationBatchSummary.empty();

        LOGGER.info(
            "Starting synchronization for {} tracked players",
            players.size()
        );

        for (Player player : players) {
            summary = synchronizeOnePlayer(synchronization, player, summary).summary();
        }

        completeBatchSynchronization(
            synchronization,
            players.size(),
            summary
        );

        LOGGER.info(
            "Synchronization completed with status {}, {} failures and {} imported matches",
            synchronization.getStatus(),
            synchronization.getFailureCount(),
            synchronization.getMatchesImported()
        );

        refreshChallengeProgress(summary.matchesImported());

        return toResponse(
            synchronization,
            summary.lastSuccessfulSynchronizationAt()
        );
    }

    /**
     * Executes a synchronization for one player and records its outcome.
     *
     * <p>Treated as a batch of one: the same per-player step, completion and status derivation as
     * {@link #synchronizeAllPlayers(SynchronizationTrigger)} apply here, so a failure is recorded
     * with a {@link SynchronizationPlayerResult} row exactly like a batch failure would be. The
     * original runtime exception is then deliberately re-thrown, since unlike the batch path this
     * method reports the outcome of the one player its caller asked for. Preserving the same
     * exception instance keeps its concrete type, stack trace and diagnostic context.</p>
     *
     * @param playerId tracked player identifier
     * @return persisted synchronization summary
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     */
    @Override
    @SuppressFBWarnings(
        value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION",
        justification = """
            The original synchronization exception is deliberately propagated
            after the failed execution has been persisted. Preserving the original
            instance keeps its concrete type, stack trace and diagnostic context.
            """
    )
    public SynchronizationResponse synchronizePlayer(long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        Synchronization synchronization =
            startSynchronization(SynchronizationTrigger.MANUAL);

        LOGGER.info("Starting synchronization for player {}", playerId);

        PlayerSynchronizationOutcome outcome = synchronizeOnePlayer(
            synchronization,
            player,
            SynchronizationBatchSummary.empty()
        );

        completeBatchSynchronization(synchronization, 1, outcome.summary());
        refreshChallengeProgress(outcome.summary().matchesImported());

        if (outcome.failure() != null) {
            throw outcome.failure();
        }

        LOGGER.info(
            "Synchronization completed for player {} with {} imported matches",
            playerId,
            outcome.summary().matchesImported()
        );

        return toResponse(
            synchronization,
            outcome.summary().lastSuccessfulSynchronizationAt()
        );
    }

    /**
     * Executes one player and returns the updated batch aggregate alongside the original failure,
     * if any.
     *
     * <p>Shared by the batch loop, which discards the failure and moves on to the next player, and
     * by {@link #synchronizePlayer(long)}, which re-throws it once completion has been recorded.</p>
     *
     * @param synchronization global execution
     * @param player          player to process
     * @param summary         current batch summary
     * @return updated batch summary and the original exception, {@code null} on success
     */
    private PlayerSynchronizationOutcome synchronizeOnePlayer(
        Synchronization synchronization,
        Player player,
        SynchronizationBatchSummary summary
    ) {
        try {
            PlayerSynchronizationResult result =
                playerSynchronizationService.synchronize(player.getId());
            saveSuccessfulPlayerResult(synchronization, result);
            return new PlayerSynchronizationOutcome(
                summary.withSuccess(result),
                null
            );
        } catch (RuntimeException exception) {
            String errorMessage = safeErrorMessage(exception);

            LOGGER.error(
                "Synchronization failed for player {}",
                player.getId(),
                exception
            );

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

            return new PlayerSynchronizationOutcome(
                summary.withFailure(player, errorMessage),
                exception
            );
        }
    }

    /**
     * Rebuilds challenge progress and the weekly ranking from the newly imported matches.
     *
     * <p>Skipped when nothing was imported: progress is derived exclusively from stored matches, so
     * an execution that added none can only recompute the very same values.
     *
     * <p>A recalculation failure is logged instead of propagated. The matches are already committed
     * and the execution genuinely succeeded, so failing it here would misreport the import and, on
     * the batch path, discard the summary of every player that was processed. Progress is rebuilt
     * from scratch on the next run, which makes a transient failure self-healing.
     *
     * @param matchesImported number of matches imported by the execution
     */
    private void refreshChallengeProgress(int matchesImported) {
        if (matchesImported == 0) {
            LOGGER.debug(
                "No match imported: challenge progress is left untouched"
            );
            return;
        }

        try {
            challengeRecalculationService.recalculateCurrentWeekProgress();
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Challenge progress recalculation failed after importing {} match(es). "
                    + "Progress and ranking stay stale until the next synchronization.",
                matchesImported,
                exception
            );
        }

        replayCampaign(matchesImported);
    }

    /**
     * Replays the campaign over the matches that were just imported.
     *
     * <p>Runs after the challenge recalculation because the campaign credits the wounded those
     * challenges rescued, and this is what makes a day's gains show up on the day itself rather
     * than at the next nightly tick.
     *
     * <p>Caught and logged like the recalculation above: the replay is idempotent and the scheduled
     * tick will redo it, so a stale base must never fail a synchronization that did import matches.
     *
     * @param matchesImported number of matches imported by the execution
     */
    private void replayCampaign(int matchesImported) {
        try {
            campaignReplayService.replayRunningCampaign();
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Campaign replay failed after importing {} match(es). The base stays stale until the "
                    + "next synchronization or daily tick.",
                matchesImported,
                exception
            );
        }
    }

    /**
     * Creates and persists a running synchronization execution.
     *
     * @param trigger synchronization trigger
     * @return persisted execution
     */
    private Synchronization startSynchronization(
        SynchronizationTrigger trigger
    ) {
        Synchronization synchronization = new Synchronization();

        synchronization.setType(SynchronizationType.STANDARD);
        synchronization.setTrigger(trigger);
        synchronization.setStatus(SynchronizationStatus.RUNNING);
        synchronization.setStartedAt(clock.instant());
        synchronization.setPlayersProcessed(0);
        synchronization.setFailureCount(0);
        synchronization.setMatchesImported(0);
        synchronization.setErrorMessage(null);

        return synchronizationRepository.save(synchronization);
    }

    /**
     * Marks a batch execution as complete and persists its aggregate values.
     *
     * @param synchronization global execution
     * @param playerCount     selected player count
     * @param summary         aggregated outcomes
     */
    private void completeBatchSynchronization(
        Synchronization synchronization,
        int playerCount,
        SynchronizationBatchSummary summary
    ) {
        synchronization.setStatus(
            determineGlobalStatus(
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
            nullableTruncatedErrorMessage(summary.errorMessages())
        );

        synchronizationRepository.save(synchronization);
    }

    /**
     * Persists a successful player outcome.
     *
     * @param synchronization global execution
     * @param result          successful player outcome
     */
    private void saveSuccessfulPlayerResult(
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
        result.setErrorMessage(
            errorMessage == null
                ? null
                : truncateErrorMessage(errorMessage)
        );
        result.setStopReason(stopReason);

        playerResultRepository.save(result);
    }

    /**
     * Determines the final status of a multi-player execution.
     *
     * @param playerCount       selected player count
     * @param successfulPlayers successful player count
     * @param failureCount      failed player count
     * @return global synchronization status
     */
    private SynchronizationStatus determineGlobalStatus(
        int playerCount,
        int successfulPlayers,
        int failureCount
    ) {
        if (playerCount == 0 || failureCount == 0) {
            return SynchronizationStatus.COMPLETED;
        }

        if (successfulPlayers == 0) {
            return SynchronizationStatus.FAILED;
        }

        return SynchronizationStatus.PARTIAL;
    }

    /**
     * Returns a non-empty diagnostic message for an exception.
     *
     * @param exception synchronization exception
     * @return safe error description
     */
    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }

    /**
     * Maps a persisted execution to its API representation.
     *
     * @param synchronization                 persisted execution
     * @param lastSuccessfulSynchronizationAt latest successful player timestamp
     * @return API response
     */
    private SynchronizationResponse toResponse(
        Synchronization synchronization,
        Instant lastSuccessfulSynchronizationAt
    ) {
        return new SynchronizationResponse(
            synchronization.getId(),
            synchronization.getType(),
            synchronization.getTrigger(),
            synchronization.getStatus(),
            synchronization.getStartedAt(),
            synchronization.getFinishedAt(),
            synchronization.getStartedAt(),
            lastSuccessfulSynchronizationAt,
            synchronization.getPlayersProcessed(),
            synchronization.getFailureCount(),
            synchronization.getMatchesImported(),
            synchronization.getErrorMessage()
        );
    }

    /**
     * Returns {@code null} for an empty aggregate or a truncated value.
     *
     * @param message aggregated errors
     * @return nullable persisted value
     */
    private String nullableTruncatedErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        return truncateErrorMessage(message);
    }

    /**
     * Restricts an error message to the database column size.
     *
     * @param message original message
     * @return non-empty persisted message
     */
    private String truncateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Synchronization failed";
        }

        if (message.length() <= MAXIMUM_ERROR_MESSAGE_LENGTH) {
            return message;
        }

        return message.substring(0, MAXIMUM_ERROR_MESSAGE_LENGTH);
    }
}
