package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.shared.config.ApplicationProperties;
import io.github.thomashtn.valorant.tracker.synchronization.model.DeepSynchronizationScope;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Imports a player's match history using Henrik pagination.
 *
 * <p>By default, only matches belonging to the most recent season are
 * imported. The service can also be configured to import the complete
 * history exposed by Henrik.</p>
 */
@Service
public class PlayerDeepSynchronizationService {

    /**
     * Logger used to report synchronization progress and stop reasons.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PlayerDeepSynchronizationService.class);

    /**
     * Maximum number of matches requested from Henrik per page.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Safety guard preventing an accidental infinite pagination loop.
     */
    private static final int MAXIMUM_PAGE_COUNT = 1_000;

    /**
     * Repository used to load and persist tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Service used to resolve a missing Riot PUUID before synchronization.
     */
    private final PlayerAccountResolutionService accountResolutionService;

    /**
     * Henrik client used to retrieve paginated match history.
     */
    private final HenrikMatchClient matchClient;

    /**
     * Service responsible for idempotent match persistence.
     */
    private final MatchImportService matchImportService;

    /**
     * Typed application configuration used to select the import scope.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Clock used to produce deterministic completion timestamps.
     */
    private final Clock clock;

    /**
     * Creates the deep-synchronization service.
     *
     * @param playerRepository tracked-player repository
     * @param accountResolutionService Riot account resolution service
     * @param matchClient Henrik match-history client
     * @param matchImportService idempotent match-import service
     * @param applicationProperties application synchronization configuration
     * @param clock application clock
     */
    public PlayerDeepSynchronizationService(
        PlayerRepository playerRepository,
        PlayerAccountResolutionService accountResolutionService,
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        ApplicationProperties applicationProperties,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.accountResolutionService = accountResolutionService;
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.applicationProperties = applicationProperties;
        this.clock = clock;
    }

    /**
     * Imports a player's match history according to the configured scope.
     *
     * @param playerId internal player identifier
     * @return completed deep-synchronization result
     */
    public PlayerDeepSynchronizationResult synchronize(long playerId) {
        Player resolvedPlayer = resolvePlayer(playerId);
        DeepSynchronizationScope scope = resolveScope();

        LOGGER.info(
            "Starting deep synchronization for player {} with scope {}",
            resolvedPlayer.getId(),
            scope
        );

        DeepImportSummary summary = importHistory(resolvedPlayer, scope);
        Instant completedAt = clock.instant();
        Player savedPlayer = saveCompletion(resolvedPlayer, completedAt);

        LOGGER.info(
            "Completed deep synchronization for player {}: pagesFetched={} matchesImported={}",
            savedPlayer.getId(),
            summary.pagesFetched(),
            summary.matchesImported()
        );

        return new PlayerDeepSynchronizationResult(
            savedPlayer,
            summary.pagesFetched(),
            summary.matchesImported(),
            completedAt
        );
    }

    /**
     * Loads the player and resolves its Riot PUUID when necessary.
     *
     * @param playerId internal player identifier
     * @return player ready for Henrik requests
     */
    private Player resolvePlayer(long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
        return accountResolutionService.resolvePuuid(player);
    }

    /**
     * Reads the configured deep-synchronization scope.
     *
     * @return active synchronization scope
     */
    private DeepSynchronizationScope resolveScope() {
        return applicationProperties
            .scheduling()
            .deepSynchronizationScope();
    }

    /**
     * Imports all eligible Henrik pages for a player.
     *
     * @param player resolved tracked player
     * @param scope configured import scope
     * @return aggregate pagination result
     */
    private DeepImportSummary importHistory(
        Player player,
        DeepSynchronizationScope scope
    ) {
        DeepPaginationState state = new DeepPaginationState();

        while (!state.isComplete()) {
            verifyMaximumPageCount(state.pagesFetched());
            processNextPage(player, scope, state);
        }

        return new DeepImportSummary(
            state.pagesFetched(),
            state.matchesImported()
        );
    }

    /**
     * Fetches, filters and imports the next Henrik page.
     *
     * @param player resolved tracked player
     * @param scope configured import scope
     * @param state mutable pagination state
     */
    private void processNextPage(
        Player player,
        DeepSynchronizationScope scope,
        DeepPaginationState state
    ) {
        HenrikMatchHistoryResponse response = matchClient.getMatches(
            player.getRiotPuuid(),
            state.startOffset(),
            PAGE_SIZE
        );
        List<HenrikMatchData> receivedMatches = response.data();

        if (receivedMatches.isEmpty()) {
            stopPagination(player, state, "empty-page");
            return;
        }

        state.incrementPagesFetched();
        initializeCurrentSeason(scope, state, receivedMatches);

        List<HenrikMatchData> eligibleMatches = filterMatchesByScope(
            receivedMatches,
            scope,
            state.currentSeasonId()
        );
        int importedOnPage = importEligibleMatches(
            player,
            response,
            eligibleMatches
        );
        state.addImportedMatches(importedOnPage);

        logImportedPage(
            player,
            state,
            receivedMatches.size(),
            eligibleMatches.size(),
            importedOnPage
        );
        updatePaginationState(player, scope, state, receivedMatches);
    }

    /**
     * Initializes the current season from the first valid page when required.
     *
     * @param scope configured import scope
     * @param state mutable pagination state
     * @param matches current Henrik page
     */
    private void initializeCurrentSeason(
        DeepSynchronizationScope scope,
        DeepPaginationState state,
        List<HenrikMatchData> matches
    ) {
        if (scope == DeepSynchronizationScope.CURRENT_SEASON
            && state.currentSeasonId() == null) {
            state.setCurrentSeasonId(resolveMostRecentSeasonId(matches));
        }
    }

    /**
     * Updates pagination after one non-empty Henrik page.
     *
     * @param player synchronized player
     * @param scope configured import scope
     * @param state mutable pagination state
     * @param matches current Henrik page
     */
    private void updatePaginationState(
        Player player,
        DeepSynchronizationScope scope,
        DeepPaginationState state,
        List<HenrikMatchData> matches
    ) {
        boolean seasonBoundaryReached =
            scope == DeepSynchronizationScope.CURRENT_SEASON
                && containsAnotherSeason(matches, state.currentSeasonId());
        boolean incompletePage = matches.size() < PAGE_SIZE;

        if (seasonBoundaryReached) {
            stopPagination(player, state, "season-boundary");
        } else if (incompletePage) {
            stopPagination(player, state, "incomplete-page");
        } else {
            state.advanceOffset(matches.size());
        }
    }

    /**
     * Persists the last successful synchronization timestamp.
     *
     * @param player synchronized player
     * @param completedAt completion timestamp
     * @return persisted player
     */
    private Player saveCompletion(Player player, Instant completedAt) {
        player.setLastSuccessfulSynchronizationAt(completedAt);
        return playerRepository.save(player);
    }

    /**
     * Imports a filtered page when at least one match is eligible.
     *
     * @param player synchronized player
     * @param sourceResponse original Henrik response
     * @param eligibleMatches matches retained by the synchronization scope
     * @return number of newly imported matches
     */
    private int importEligibleMatches(
        Player player,
        HenrikMatchHistoryResponse sourceResponse,
        List<HenrikMatchData> eligibleMatches
    ) {
        if (eligibleMatches.isEmpty()) {
            return 0;
        }

        HenrikMatchHistoryResponse filteredResponse =
            new HenrikMatchHistoryResponse(
                sourceResponse.status(),
                eligibleMatches
            );
        return matchImportService.importMatches(player, filteredResponse);
    }

    /**
     * Logs the metrics of one imported Henrik page.
     *
     * @param player synchronized player
     * @param state current pagination state
     * @param receivedMatchCount number of matches received
     * @param eligibleMatchCount number of matches retained
     * @param importedOnPage number of matches imported
     */
    private void logImportedPage(
        Player player,
        DeepPaginationState state,
        int receivedMatchCount,
        int eligibleMatchCount,
        int importedOnPage
    ) {
        LOGGER.info(
            "Imported deep synchronization page for player {}: page={} start={} received={} "
                + "eligible={} imported={} totalImported={} season={}",
            player.getId(),
            state.currentPageNumber(),
            state.startOffset(),
            receivedMatchCount,
            eligibleMatchCount,
            importedOnPage,
            state.matchesImported(),
            state.currentSeasonId()
        );
    }

    /**
     * Marks pagination as complete and logs the stop reason.
     *
     * @param player synchronized player
     * @param state current pagination state
     * @param reason machine-readable stop reason
     */
    private void stopPagination(
        Player player,
        DeepPaginationState state,
        String reason
    ) {
        state.complete();
        LOGGER.info(
            "Stopping deep synchronization for player {}: page={} start={} reason={} season={}",
            player.getId(),
            state.currentPageNumber(),
            state.startOffset(),
            reason,
            state.currentSeasonId()
        );
    }

    /**
     * Ensures that the pagination safety limit has not been exceeded.
     *
     * @param pagesFetched number of pages already fetched
     */
    private void verifyMaximumPageCount(int pagesFetched) {
        if (pagesFetched >= MAXIMUM_PAGE_COUNT) {
            throw new IllegalStateException(
                "Deep synchronization exceeded the maximum page count"
            );
        }
    }

    /**
     * Resolves the season identifier from the most recent valid match.
     *
     * @param matches matches returned by Henrik
     * @return identifier of the most recent season
     */
    private String resolveMostRecentSeasonId(List<HenrikMatchData> matches) {
        return matches.stream()
            .map(this::extractSeasonId)
            .filter(this::hasText)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Unable to determine the current season from Henrik matches"
            ));
    }

    /**
     * Filters matches according to the configured deep-synchronization scope.
     *
     * @param matches matches returned by Henrik
     * @param scope configured synchronization scope
     * @param currentSeasonId current season identifier
     * @return matches eligible for import
     */
    private List<HenrikMatchData> filterMatchesByScope(
        List<HenrikMatchData> matches,
        DeepSynchronizationScope scope,
        String currentSeasonId
    ) {
        if (scope == DeepSynchronizationScope.ALL_HISTORY) {
            return matches;
        }

        return matches.stream()
            .filter(match -> currentSeasonId.equals(extractSeasonId(match)))
            .toList();
    }

    /**
     * Determines whether a page contains a match from another season.
     *
     * @param matches complete Henrik page
     * @param currentSeasonId current season identifier
     * @return {@code true} when another season is present
     */
    private boolean containsAnotherSeason(
        List<HenrikMatchData> matches,
        String currentSeasonId
    ) {
        return matches.stream()
            .map(this::extractSeasonId)
            .filter(this::hasText)
            .anyMatch(seasonId -> !currentSeasonId.equals(seasonId));
    }

    /**
     * Extracts the season identifier from one Henrik match.
     *
     * @param match Henrik match
     * @return season identifier, or {@code null} when unavailable
     */
    private String extractSeasonId(HenrikMatchData match) {
        if (match == null
            || match.metadata() == null
            || match.metadata().season() == null) {
            return null;
        }
        return match.metadata().season().id();
    }

    /**
     * Tests whether a string contains non-whitespace characters.
     *
     * @param value value to inspect
     * @return {@code true} when the value contains text
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Aggregate result produced by the pagination loop.
     */
    private record DeepImportSummary(int pagesFetched, int matchesImported) {}

    /**
     * Mutable state kept private to one deep-synchronization execution.
     */
    private static final class DeepPaginationState {

        /**
         * Offset sent to Henrik for the next request.
         */
        private int startOffset;

        /**
         * Number of non-empty pages fetched so far.
         */
        private int pagesFetched;

        /**
         * Total number of newly imported matches.
         */
        private int matchesImported;

        /**
         * Season retained when the scope is {@code CURRENT_SEASON}.
         */
        private String currentSeasonId;

        /**
         * Indicates whether the pagination loop must stop.
         */
        private boolean complete;

        /**
         * @return offset used for the next Henrik request
         */
        private int startOffset() {
            return startOffset;
        }

        /**
         * @return number of non-empty pages fetched
         */
        private int pagesFetched() {
            return pagesFetched;
        }

        /**
         * @return total number of newly imported matches
         */
        private int matchesImported() {
            return matchesImported;
        }

        /**
         * @return current season identifier, or {@code null} when unresolved
         */
        private String currentSeasonId() {
            return currentSeasonId;
        }

        /**
         * @return zero-based page number used in logs
         */
        private int currentPageNumber() {
            return Math.max(0, pagesFetched - 1);
        }

        /**
         * @return whether pagination has completed
         */
        private boolean isComplete() {
            return complete;
        }

        /**
         * Increments the fetched-page counter.
         */
        private void incrementPagesFetched() {
            pagesFetched++;
        }

        /**
         * Adds newly imported matches to the aggregate count.
         *
         * @param importedOnPage number imported from the current page
         */
        private void addImportedMatches(int importedOnPage) {
            matchesImported += importedOnPage;
        }

        /**
         * Advances the Henrik offset after a complete page.
         *
         * @param receivedMatchCount number of matches returned by Henrik
         */
        private void advanceOffset(int receivedMatchCount) {
            startOffset += receivedMatchCount;
        }

        /**
         * Stores the season inferred from the most recent valid match.
         *
         * @param seasonId season identifier
         */
        private void setCurrentSeasonId(String seasonId) {
            currentSeasonId = seasonId;
        }

        /**
         * Marks the pagination loop as complete.
         */
        private void complete() {
            complete = true;
        }
    }
}
