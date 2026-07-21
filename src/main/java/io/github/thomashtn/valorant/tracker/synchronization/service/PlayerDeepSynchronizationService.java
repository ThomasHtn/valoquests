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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Imports a player's Henrik match history page by page.
 *
 * <p>The service orchestrates account resolution, pagination and completion persistence. Match
 * storage remains delegated to {@link MatchImportService}, which preserves import idempotence.</p>
 */
@Service
public class PlayerDeepSynchronizationService {

    /**
     * Logger used to report synchronization progress and stop decisions.
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
     * @param playerRepository         tracked-player repository
     * @param accountResolutionService Riot account resolution service
     * @param matchClient              Henrik match-history client
     * @param matchImportService       idempotent match-import service
     * @param applicationProperties    application synchronization configuration
     * @param clock                    application clock
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
        Player player = resolvePlayer(playerId);
        DeepSynchronizationScope scope = resolveScope();

        LOGGER.info(
            "Starting deep synchronization for player {} with scope {}",
            player.getId(),
            scope
        );

        DeepImportSummary summary = importHistory(player, scope);
        Instant completedAt = clock.instant();
        Player savedPlayer = saveCompletion(player, completedAt);

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
        return applicationProperties.deepSynchronization().scope();
    }

    /**
     * Imports eligible pages until Henrik returns no more useful data.
     *
     * @param player resolved tracked player
     * @param scope  configured import scope
     * @return aggregate pagination result
     */
    private DeepImportSummary importHistory(Player player, DeepSynchronizationScope scope) {
        DeepImportSummary summary = DeepImportSummary.empty();
        String currentSeasonId = null;
        int startOffset = 0;

        for (int pageNumber = 0; pageNumber < MAXIMUM_PAGE_COUNT; pageNumber++) {
            HenrikMatchHistoryResponse response = fetchPage(player, startOffset);
            List<HenrikMatchData> receivedMatches = response.data();

            if (receivedMatches.isEmpty()) {
                logStop(player, pageNumber, startOffset, "empty-page", currentSeasonId);
                return summary;
            }

            currentSeasonId = resolveCurrentSeasonId(scope, currentSeasonId, receivedMatches);
            PageImportResult pageResult = importPage(player, response, scope, currentSeasonId);
            summary = summary.addPage(pageResult.importedMatchCount());

            logPage(player, pageNumber, startOffset, receivedMatches.size(), pageResult, summary, currentSeasonId);

            String stopReason = resolveStopReason(receivedMatches, scope, currentSeasonId);
            if (stopReason != null) {
                logStop(player, pageNumber, startOffset, stopReason, currentSeasonId);
                return summary;
            }

            startOffset += receivedMatches.size();
        }

        throw new IllegalStateException("Deep synchronization exceeded the maximum page count");
    }

    /**
     * Retrieves one Henrik page for the resolved player.
     *
     * @param player      synchronized player
     * @param startOffset item offset sent to Henrik
     * @return Henrik match-history response
     */
    private HenrikMatchHistoryResponse fetchPage(Player player, int startOffset) {
        return matchClient.getMatches(player.getRiotPuuid(), startOffset, PAGE_SIZE);
    }

    /**
     * Resolves the season retained by current-season synchronization.
     *
     * @param scope           configured import scope
     * @param currentSeasonId previously resolved season identifier
     * @param matches         current Henrik page
     * @return retained season identifier, or {@code null} for all-history mode
     */
    private String resolveCurrentSeasonId(
        DeepSynchronizationScope scope,
        String currentSeasonId,
        List<HenrikMatchData> matches
    ) {
        if (scope == DeepSynchronizationScope.ALL_HISTORY || currentSeasonId != null) {
            return currentSeasonId;
        }

        return matches.stream()
            .map(this::extractSeasonId)
            .filter(this::hasText)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Unable to determine the current season from Henrik matches"
            ));
    }

    /**
     * Filters and imports one non-empty Henrik page.
     *
     * @param player          synchronized player
     * @param response        original Henrik response
     * @param scope           configured synchronization scope
     * @param currentSeasonId season retained by current-season mode
     * @return metrics produced for the page
     */
    private PageImportResult importPage(
        Player player,
        HenrikMatchHistoryResponse response,
        DeepSynchronizationScope scope,
        String currentSeasonId
    ) {
        List<HenrikMatchData> eligibleMatches = filterMatchesByScope(
            response.data(),
            scope,
            currentSeasonId
        );

        if (eligibleMatches.isEmpty()) {
            return new PageImportResult(0, 0);
        }

        HenrikMatchHistoryResponse filteredResponse = new HenrikMatchHistoryResponse(
            response.status(),
            eligibleMatches
        );
        int importedMatchCount = matchImportService.importMatches(player, filteredResponse);

        return new PageImportResult(eligibleMatches.size(), importedMatchCount);
    }

    /**
     * Determines whether pagination must stop after the current page.
     *
     * @param matches         complete Henrik page
     * @param scope           configured synchronization scope
     * @param currentSeasonId season retained by current-season mode
     * @return machine-readable stop reason, or {@code null} when pagination continues
     */
    private String resolveStopReason(
        List<HenrikMatchData> matches,
        DeepSynchronizationScope scope,
        String currentSeasonId
    ) {
        if (scope == DeepSynchronizationScope.CURRENT_SEASON
            && containsAnotherSeason(matches, currentSeasonId)) {
            return "season-boundary";
        }
        if (matches.size() < PAGE_SIZE) {
            return "incomplete-page";
        }
        return null;
    }

    /**
     * Filters matches according to the configured deep-synchronization scope.
     *
     * @param matches         matches returned by Henrik
     * @param scope           configured synchronization scope
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
     * @param matches         complete Henrik page
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
        if (match == null || match.metadata() == null || match.metadata().season() == null) {
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
     * Persists the last successful synchronization timestamp.
     *
     * @param player      synchronized player
     * @param completedAt completion timestamp
     * @return persisted player
     */
    private Player saveCompletion(Player player, Instant completedAt) {
        player.setLastSuccessfulSynchronizationAt(completedAt);
        return playerRepository.save(player);
    }

    /**
     * Logs the metrics produced by one Henrik page.
     */
    private void logPage(
        Player player,
        int pageNumber,
        int startOffset,
        int receivedMatchCount,
        PageImportResult pageResult,
        DeepImportSummary summary,
        String currentSeasonId
    ) {
        LOGGER.info(
            "Imported deep synchronization page for player {}: page={} start={} received={} "
                + "eligible={} imported={} totalImported={} season={}",
            player.getId(),
            pageNumber,
            startOffset,
            receivedMatchCount,
            pageResult.eligibleMatchCount(),
            pageResult.importedMatchCount(),
            summary.matchesImported(),
            currentSeasonId
        );
    }

    /**
     * Logs why pagination has stopped.
     */
    private void logStop(
        Player player,
        int pageNumber,
        int startOffset,
        String reason,
        String currentSeasonId
    ) {
        LOGGER.info(
            "Stopping deep synchronization for player {}: page={} start={} reason={} season={}",
            player.getId(),
            pageNumber,
            startOffset,
            reason,
            currentSeasonId
        );
    }

    /**
     * Metrics produced while processing one Henrik page.
     */
    private record PageImportResult(int eligibleMatchCount, int importedMatchCount) {
    }

    /**
     * Aggregate result produced by the pagination loop.
     */
    private record DeepImportSummary(int pagesFetched, int matchesImported) {

        /**
         * @return empty pagination summary
         */
        private static DeepImportSummary empty() {
            return new DeepImportSummary(0, 0);
        }

        /**
         * Returns the summary after processing one additional page.
         *
         * @param importedMatchCount number of matches imported from the page
         * @return updated immutable summary
         */
        private DeepImportSummary addPage(int importedMatchCount) {
            return new DeepImportSummary(pagesFetched + 1, matchesImported + importedMatchCount);
        }
    }
}
