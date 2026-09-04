package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valoquests.match.service.MatchImportService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads a whole calibration window out of Henrik, once, before a campaign is opened.
 *
 * <p>The ordinary synchronization walks the current act and the one before it, which is all a live
 * campaign ever needs. A calibration needs nine months, and it needs them before it can be trusted:
 * a reference measured on the two acts already stored would count every earlier week as a zero and
 * hand the squad a floor it never deserved.
 *
 * <p>Walks by raw offset rather than by season, because the question here is a date, not an act.
 * Stops the moment a page ends before the window starts, so an operator running it twice in a row
 * pays only for the pages that reach back that far.
 */
@Service
public class HistoryBackfillService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryBackfillService.class);

    /**
     * Matches requested per Henrik call, capped by the client.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Pages one player's walk may fetch before it is treated as an anomaly.
     *
     * <p>Nine months of a heavy player is a few thousand matches; well past that, a walk that keeps
     * going is a Henrik that keeps repeating a page, not a player who keeps playing.
     */
    private static final int MAXIMUM_PAGE_COUNT = 600;

    /**
     * Henrik client returning pages of match history.
     */
    private final HenrikMatchClient matchClient;

    /**
     * Service persisting the matches of one page.
     */
    private final MatchImportService matchImportService;

    /**
     * Repository resolving the players to walk.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository recording the execution.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Clock stamping the execution.
     */
    private final Clock clock;

    /**
     * Creates the history backfill service.
     *
     * @param matchClient               Henrik match client
     * @param matchImportService        match import service
     * @param playerRepository          player repository
     * @param synchronizationRepository synchronization repository
     * @param clock                     clock
     */
    public HistoryBackfillService(
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        PlayerRepository playerRepository,
        SynchronizationRepository synchronizationRepository,
        Clock clock
    ) {
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.playerRepository = playerRepository;
        this.synchronizationRepository = synchronizationRepository;
        this.clock = clock;
    }

    /**
     * Walks every active player's history back to the calibration window.
     *
     * @return the execution row, already finished
     */
    public Synchronization backfill() {
        Instant windowStart = clock.instant()
            .minus(CampaignRuleset.CALIBRATION_WINDOW_MONTHS * 30L, ChronoUnit.DAYS);
        List<Player> roster = playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE);

        Synchronization execution = start(roster.size());
        int imported = 0;
        int failures = 0;

        for (Player player : roster) {
            try {
                imported += walk(player, windowStart);
            } catch (RuntimeException exception) {
                failures++;
                LOGGER.error("History backfill failed for player {}", player.getId(), exception);
            }
        }

        return finish(execution, imported, failures);
    }

    /**
     * Walks one player's history until it reaches past the window.
     *
     * @param player      player to walk
     * @param windowStart instant the walk may stop at
     * @return the matches imported for that player
     */
    private int walk(Player player, Instant windowStart) {
        int imported = 0;

        for (int page = 0; page < MAXIMUM_PAGE_COUNT; page++) {
            HenrikMatchHistoryResponse response =
                matchClient.getMatches(player.getRiotPuuid(), page * PAGE_SIZE, PAGE_SIZE);

            if (response.data().isEmpty()) {
                LOGGER.info("History backfill reached the end of player {}'s history.", player.getId());

                return imported;
            }

            imported += matchImportService.importMatchesWithSummary(player, response).imported();

            if (reachesPast(response, windowStart)) {
                LOGGER.info(
                    "History backfill covered player {} back to {} in {} page(s).",
                    player.getId(),
                    windowStart,
                    page + 1
                );

                return imported;
            }
        }

        LOGGER.warn(
            "History backfill stopped for player {} after {} pages without reaching {}.",
            player.getId(),
            MAXIMUM_PAGE_COUNT,
            windowStart
        );

        return imported;
    }

    /**
     * Determines whether one page already reaches past the window's first instant.
     *
     * @param response    page returned by Henrik
     * @param windowStart instant the walk may stop at
     * @return {@code true} when the page holds a match older than the window
     */
    private boolean reachesPast(HenrikMatchHistoryResponse response, Instant windowStart) {
        return response.data().stream()
            .map(match -> match.metadata().startedAt())
            .filter(Objects::nonNull)
            .anyMatch(startedAt -> startedAt.isBefore(windowStart));
    }

    /**
     * Records the start of the execution.
     *
     * @param playerCount players the walk will attempt
     * @return the execution row
     */
    private Synchronization start(int playerCount) {
        Synchronization execution = new Synchronization();
        execution.setType(SynchronizationType.HISTORY_BACKFILL);
        execution.setTrigger(SynchronizationTrigger.MANUAL);
        execution.setStatus(SynchronizationStatus.RUNNING);
        execution.setStartedAt(clock.instant());
        execution.setPlayersProcessed(playerCount);

        return synchronizationRepository.save(execution);
    }

    /**
     * Records the end of the execution.
     *
     * @param execution execution row
     * @param imported  matches imported
     * @param failures  players the walk could not finish
     * @return the finished execution row
     */
    private Synchronization finish(Synchronization execution, int imported, int failures) {
        execution.setStatus(failures == 0 ? SynchronizationStatus.COMPLETED : SynchronizationStatus.FAILED);
        execution.setFinishedAt(clock.instant());
        execution.setMatchesImported(imported);
        execution.setFailureCount(failures);

        if (failures > 0) {
            execution.setErrorMessage(failures + " player(s) could not be walked back to the window.");
        }

        LOGGER.info("History backfill finished: {} match(es) imported, {} failure(s).", imported, failures);

        return synchronizationRepository.save(execution);
    }
}
