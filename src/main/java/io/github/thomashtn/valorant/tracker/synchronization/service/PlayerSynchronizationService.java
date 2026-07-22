package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMmrMapper;
import io.github.thomashtn.valorant.tracker.match.model.MatchImportResult;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the standard synchronization of one tracked player.
 */
@Service
public class PlayerSynchronizationService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PlayerSynchronizationService.class);

    /**
     * Number of matches requested from Henrik per HTTP call.
     */
    private static final int MATCH_PAGE_SIZE = 10;

    /**
     * Safety guard preventing an infinite loop if Henrik repeats pages.
     */
    private static final int MAXIMUM_PAGE_COUNT = 100;

    /**
     * Repository used to load and persist tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Service used to resolve missing Riot account identifiers.
     */
    private final PlayerAccountResolutionService accountResolutionService;

    /**
     * Henrik client used to retrieve the current competitive rank.
     */
    private final HenrikMmrClient mmrClient;

    /**
     * Mapper used to apply Henrik rank data to tracked players.
     */
    private final HenrikMmrMapper mmrMapper;

    /**
     * Henrik client used to retrieve match-history pages.
     */
    private final HenrikMatchClient matchClient;

    /**
     * Service used to persist Henrik matches idempotently.
     */
    private final MatchImportService matchImportService;

    /**
     * Clock used to produce deterministic timestamps.
     */
    private final Clock clock;

    /**
     * Creates the standard player synchronization service.
     */
    public PlayerSynchronizationService(
        PlayerRepository playerRepository,
        PlayerAccountResolutionService accountResolutionService,
        HenrikMmrClient mmrClient,
        HenrikMmrMapper mmrMapper,
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.accountResolutionService = accountResolutionService;
        this.mmrClient = mmrClient;
        this.mmrMapper = mmrMapper;
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.clock = clock;
    }

    /**
     * Synchronizes the Riot account, current rank and every new recent match.
     *
     * <p>Pagination stops when Henrik returns an empty or incomplete page, or
     * when a complete page contains no new player-match association. The last
     * condition makes repeated standard synchronizations efficient while still
     * importing more than the first ten matches on a clean database.</p>
     *
     * @param playerId internal player identifier
     * @return synchronization result
     */
    public PlayerSynchronizationResult synchronize(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
        Player resolvedPlayer = accountResolutionService.resolvePuuid(player);

        LOGGER.info(
            "Starting standard synchronization for player {} ({})",
            resolvedPlayer.getId(),
            resolvedPlayer.getDisplayName()
        );

        HenrikMmrResponse mmrResponse = mmrClient.getCurrentMmr(
            resolvedPlayer.getRiotPuuid()
        );
        mmrMapper.updatePlayer(mmrResponse, resolvedPlayer);

        MatchImportSummary importSummary = importRecentMatches(resolvedPlayer);
        Instant completedAt = clock.instant();
        resolvedPlayer.setLastSuccessfulSynchronizationAt(completedAt);
        Player savedPlayer = playerRepository.save(resolvedPlayer);

        LOGGER.info(
            "Completed standard synchronization for player {}: importedMatches={}",
            savedPlayer.getId(),
            importSummary.matchesImported()
        );

        return new PlayerSynchronizationResult(
            savedPlayer,
            importSummary.pagesFetched(),
            importSummary.matchesImported(),
            completedAt
        );
    }

    /**
     * Imports consecutive match-history pages until existing data is reached.
     */
    private MatchImportSummary importRecentMatches(Player player) {
        int startOffset = 0;
        int pagesFetched = 0;
        int totalImported = 0;
        boolean paginationComplete = false;

        while (!paginationComplete && pagesFetched < MAXIMUM_PAGE_COUNT) {
            HenrikMatchHistoryResponse response = matchClient.getMatches(
                player.getRiotPuuid(),
                startOffset,
                MATCH_PAGE_SIZE
            );
            int received = response.data().size();

            if (received == 0) {
                logStandardStop(player, pagesFetched, startOffset, "empty-page");
                paginationComplete = true;
            } else {
                MatchImportResult importResult =
                    matchImportService.importMatchesWithSummary(player, response);
                totalImported += importResult.imported();

                LOGGER.info(
                    "Imported Henrik match page for player {}: page={} start={} requested={} received={} "
                        + "imported={} alreadyKnown={} rejected={} totalImported={}",
                    player.getId(), pagesFetched, startOffset, MATCH_PAGE_SIZE,
                    received, importResult.imported(), importResult.alreadyKnown(),
                    importResult.rejected(), totalImported
                );

                pagesFetched++;
                paginationComplete = received < MATCH_PAGE_SIZE
                    || importResult.knownHistoryReached();

                if (paginationComplete) {
                    String reason = received < MATCH_PAGE_SIZE
                        ? "incomplete-page"
                        : "existing-history-reached";
                    logStandardStop(player, pagesFetched - 1, startOffset, reason);
                } else {
                    startOffset += received;
                }
            }
        }

        if (pagesFetched >= MAXIMUM_PAGE_COUNT) {
            LOGGER.warn(
                "Standard synchronization reached the safety page limit for player {}: maximumPages={}",
                player.getId(), MAXIMUM_PAGE_COUNT
            );
        }

        return new MatchImportSummary(pagesFetched, totalImported);
    }

    /**
     * Logs the reason why standard match pagination stopped.
     */
    private void logStandardStop(
        Player player,
        int pageNumber,
        int startOffset,
        String reason
    ) {
        LOGGER.info(
            "Stopping standard synchronization for player {}: page={} start={} reason={}",
            player.getId(), pageNumber, startOffset, reason
        );
    }

    /**
     * Internal pagination result kept private to the orchestration service.
     */
    private record MatchImportSummary(int pagesFetched, int matchesImported) {
    }
}

