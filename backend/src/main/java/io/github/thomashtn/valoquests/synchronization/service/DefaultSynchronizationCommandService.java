package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.util.NonTransactionalGuard;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valoquests.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Executes and records scheduled and manual synchronizations.
 *
 * <p>Deliberately not transactional, see {@link SeasonSynchronizationStateService}: each execution
 * and every player outcome commit on their own, so partial failures stay visible without blocking the
 * remaining players. {@link NonTransactionalGuard} sits at the entry of both commands because, checked
 * deeper, its failure would be recorded as an ordinary player failure instead of failing fast.</p>
 *
 * <p>Importing matches is only half of the workflow: challenge progress and the weekly ranking are
 * derived from the stored matches and stay stale until they are rebuilt. Every execution that
 * actually imported something therefore ends with a challenge recalculation, which is what keeps the
 * ranking live between two scheduled runs.</p>
 *
 * <p>An execution is only marked finished once that recalculation and the campaign replay are done:
 * the public synchronization status reads a finished execution as "the screens are up to date".</p>
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
     * Service used to synchronize one player.
     */
    private final PlayerSynchronizationService playerSynchronizationService;

    /**
     * Repository used to retrieve active tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Writes the execution row and one outcome row per processed player.
     */
    private final SynchronizationRecorder recorder;

    /**
     * Service used to rebuild challenge progress and the weekly ranking after an import.
     */
    private final ChallengeRecalculationService challengeRecalculationService;

    /**
     * Service used to replay the campaign after an import, so a day's gains show up the same day.
     */
    private final CampaignReplayService campaignReplayService;

    /**
     * Creates the synchronization command service.
     *
     * @param playerSynchronizationService     player synchronization service
     * @param playerRepository                 tracked-player repository
     * @param recorder                         execution and per-player result writer
     * @param challengeRecalculationService    challenge progress recalculation service
     * @param campaignReplayService            campaign replay service
     */
    public DefaultSynchronizationCommandService(
        PlayerSynchronizationService playerSynchronizationService,
        PlayerRepository playerRepository,
        SynchronizationRecorder recorder,
        ChallengeRecalculationService challengeRecalculationService,
        CampaignReplayService campaignReplayService
    ) {
        this.playerSynchronizationService = playerSynchronizationService;
        this.playerRepository = playerRepository;
        this.recorder = recorder;
        this.challengeRecalculationService = challengeRecalculationService;
        this.campaignReplayService = campaignReplayService;
    }

    /**
     * Executes a synchronization for every active player.
     *
     * @param trigger synchronization trigger
     */
    @Override
    public void synchronizeAllPlayers(
        SynchronizationTrigger trigger
    ) {
        NonTransactionalGuard.assertNoActiveTransaction("Synchronization");

        runBatch(trigger, playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED));
    }

    /**
     * Executes a synchronization for one player and records its outcome.
     *
     * <p>Treated as a batch of one: the same per-player step, completion and status derivation as
     * {@link #synchronizeAllPlayers(SynchronizationTrigger)} apply here, so a failure is logged once
     * and recorded with a {@link SynchronizationPlayerResult} row exactly like a batch failure.</p>
     *
     * @param playerId tracked player identifier
     * @throws PlayerNotFoundException when no tracked player owns the identifier
     */
    @Override
    public void synchronizePlayer(long playerId) {
        NonTransactionalGuard.assertNoActiveTransaction("Synchronization");

        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        runBatch(SynchronizationTrigger.MANUAL, List.of(player));
    }

    /**
     * Records one execution over a set of players: start, each player in turn, rebuild, finish.
     *
     * @param trigger synchronization trigger
     * @param players players to synchronize, in order
     */
    private void runBatch(SynchronizationTrigger trigger, List<Player> players) {
        Synchronization synchronization = recorder.start(trigger);
        SynchronizationBatchSummary summary = SynchronizationBatchSummary.empty();

        LOGGER.info(
            "Starting synchronization for {} tracked players",
            players.size()
        );

        for (Player player : players) {
            summary = synchronizeOnePlayer(synchronization, player, summary);
        }

        rebuildDerivedState(summary.matchesImported());
        recorder.complete(synchronization, players.size(), summary);

        LOGGER.info(
            "Synchronization completed with status {}, {} failures and {} imported matches",
            synchronization.getStatus(),
            synchronization.getFailureCount(),
            synchronization.getMatchesImported()
        );
    }

    /**
     * Executes one player and returns the batch aggregate updated with its outcome.
     *
     * <p>A failure is logged and recorded here, then the caller moves on: nothing is re-thrown.
     *
     * @param synchronization global execution
     * @param player          player to process
     * @param summary         current batch summary
     * @return updated batch summary
     */
    private SynchronizationBatchSummary synchronizeOnePlayer(
        Synchronization synchronization,
        Player player,
        SynchronizationBatchSummary summary
    ) {
        try {
            PlayerSynchronizationResult result =
                playerSynchronizationService.synchronize(player.getId());
            recorder.recordSuccess(synchronization, result);
            return summary.withSuccess(result);
        } catch (RuntimeException exception) {
            String errorMessage = SynchronizationErrorMessage.of(exception);

            LOGGER.error(
                "Synchronization failed for player {}",
                player.getId(),
                exception
            );

            recorder.recordFailure(synchronization, player, errorMessage);

            return summary.withFailure(player, errorMessage);
        }
    }

    /**
     * Rebuilds everything derived from the newly imported matches: challenge progress, the weekly
     * ranking and the campaign.
     *
     * <p>Skipped when nothing was imported: all of it is derived exclusively from stored matches, so
     * an execution that added none can only recompute the very same values.
     *
     * @param matchesImported number of matches imported by the execution
     */
    private void rebuildDerivedState(int matchesImported) {
        if (matchesImported == 0) {
            LOGGER.debug(
                "No match imported: progress, ranking and campaign are left untouched"
            );
            return;
        }

        recalculateChallengeProgress(matchesImported);
        replayCampaign(matchesImported);
    }

    /**
     * Rebuilds challenge progress and the weekly ranking from the newly imported matches.
     *
     * <p>A recalculation failure is logged instead of propagated. The matches are already committed
     * and the execution genuinely succeeded, so failing it here would misreport the import and, on
     * the batch path, discard the summary of every player that was processed. Progress is rebuilt
     * from scratch on the next run, which makes a transient failure self-healing.
     *
     * @param matchesImported number of matches imported by the execution
     */
    private void recalculateChallengeProgress(int matchesImported) {
        try {
            challengeRecalculationService.drawAndRecalculateCurrentWeek();
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Challenge progress recalculation failed after importing {} match(es). "
                    + "Progress and ranking stay stale until the next synchronization.",
                matchesImported,
                exception
            );
        }
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
}
