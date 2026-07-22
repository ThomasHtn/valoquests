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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

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
        PaginationState state = PaginationState.initial();

        while (state.shouldContinue()) {
            HenrikMatchHistoryResponse response = matchClient.getMatches(
                player.getRiotPuuid(),
                state.startOffset(),
                MATCH_PAGE_SIZE
            );
            state = processPage(player, response, state);
        }

        if (state.pageLimitReached()) {
            LOGGER.warn(
                "Standard synchronization reached the safety page limit for player {}: maximumPages={}",
                player.getId(),
                MAXIMUM_PAGE_COUNT
            );
        }

        return new MatchImportSummary(
            state.pagesFetched(),
            state.totalImported()
        );
    }

    /**
     * Imports one match-history page and advances pagination state.
     *
     * @param player   synchronized player
     * @param response Henrik match-history response
     * @param state    current pagination state
     * @return updated pagination state
     */
    private PaginationState processPage(
        Player player,
        HenrikMatchHistoryResponse response,
        PaginationState state
    ) {
        int received = response.data().size();

        if (received == 0) {
            logStandardStop(
                player,
                state.pagesFetched(),
                state.startOffset(),
                "empty-page"
            );
            return state.markComplete();
        }

        MatchImportResult importResult =
            matchImportService.importMatchesWithSummary(player, response);
        int totalImported = state.totalImported() + importResult.imported();

        logImportedPage(player, state, received, importResult, totalImported);

        int pagesFetched = state.pagesFetched() + 1;
        boolean incompletePage = received < MATCH_PAGE_SIZE;
        boolean knownHistoryReached = importResult.knownHistoryReached();

        if (incompletePage || knownHistoryReached) {
            String reason = incompletePage
                ? "incomplete-page"
                : "existing-history-reached";
            logStandardStop(
                player,
                pagesFetched - 1,
                state.startOffset(),
                reason
            );
            return new PaginationState(
                state.startOffset(),
                pagesFetched,
                totalImported,
                true
            );
        }

        return new PaginationState(
            state.startOffset() + received,
            pagesFetched,
            totalImported,
            false
        );
    }

    /**
     * Logs the result of one imported Henrik match-history page.
     *
     * @param player        synchronized player
     * @param state         pagination state before importing the page
     * @param received      number of matches returned by Henrik
     * @param importResult  persisted import result
     * @param totalImported cumulative imported match count
     */
    private void logImportedPage(
        Player player,
        PaginationState state,
        int received,
        MatchImportResult importResult,
        int totalImported
    ) {
        LOGGER.info(
            "Imported Henrik match page for player {}: page={} start={} requested={} received={} "
                + "imported={} alreadyKnown={} rejected={} totalImported={}",
            player.getId(),
            state.pagesFetched(),
            state.startOffset(),
            MATCH_PAGE_SIZE,
            received,
            importResult.imported(),
            importResult.alreadyKnown(),
            importResult.rejected(),
            totalImported
        );
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
     * Immutable state used while traversing standard synchronization pages.
     *
     * @param startOffset   next Henrik result offset
     * @param pagesFetched  number of non-empty pages processed
     * @param totalImported cumulative number of imported matches
     * @param complete      whether pagination reached a terminal condition
     */
    private record PaginationState(
        int startOffset,
        int pagesFetched,
        int totalImported,
        boolean complete
    ) {

        /**
         * Creates the initial pagination state.
         *
         * @return initial state
         */
        private static PaginationState initial() {
            return new PaginationState(0, 0, 0, false);
        }

        /**
         * Determines whether another Henrik page may be requested.
         *
         * @return {@code true} when pagination should continue
         */
        private boolean shouldContinue() {
            return !complete && pagesFetched < MAXIMUM_PAGE_COUNT;
        }

        /**
         * Determines whether pagination stopped on the safety limit.
         *
         * @return {@code true} when the maximum page count was reached
         */
        private boolean pageLimitReached() {
            return !complete && pagesFetched >= MAXIMUM_PAGE_COUNT;
        }

        /**
         * Returns an equivalent terminal state.
         *
         * @return completed pagination state
         */
        private PaginationState markComplete() {
            return new PaginationState(
                startOffset,
                pagesFetched,
                totalImported,
                true
            );
        }
    }

    /**
     * Internal pagination result kept private to the orchestration service.
     *
     * @param pagesFetched    number of processed non-empty pages
     * @param matchesImported total number of imported matches
     */
    private record MatchImportSummary(int pagesFetched, int matchesImported) {
    }
}

