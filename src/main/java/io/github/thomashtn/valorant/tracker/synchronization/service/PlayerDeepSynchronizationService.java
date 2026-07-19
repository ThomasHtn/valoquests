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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Imports a player's match history using Henrik pagination.
 *
 * <p>By default, the synchronization imports only matches belonging to the
 * season of the most recent match returned by Henrik. It can also be
 * configured to import the complete available history.</p>
 */
@Service
public class PlayerDeepSynchronizationService {

    /**
     * Maximum page size accepted by the current Henrik match client.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Safety limit preventing an accidental infinite pagination loop.
     */
    private static final int MAXIMUM_PAGE_COUNT = 1_000;

    private final PlayerRepository playerRepository;
    private final PlayerAccountResolutionService accountResolutionService;
    private final HenrikMatchClient matchClient;
    private final MatchImportService matchImportService;
    private final ApplicationProperties applicationProperties;
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
     * <p>For {@link DeepSynchronizationScope#CURRENT_SEASON}, the season of
     * the most recent valid match is considered the current season. Pagination
     * stops as soon as a match from another season is encountered.</p>
     *
     * <p>For {@link DeepSynchronizationScope#ALL_HISTORY}, pagination
     * continues until Henrik returns an empty or incomplete page.</p>
     *
     * @param playerId internal player identifier
     * @return completed deep-synchronization result
     */
    public PlayerDeepSynchronizationResult synchronize(long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        Player resolvedPlayer =
            accountResolutionService.resolvePuuid(player);

        DeepSynchronizationScope scope =
            applicationProperties
                .scheduling()
                .deepSynchronizationScope();

        int start = 0;
        int pagesFetched = 0;
        int matchesImported = 0;

        String currentSeasonId = null;

        while (true) {
            verifyMaximumPageCount(pagesFetched);

            HenrikMatchHistoryResponse response =
                matchClient.getMatches(
                    resolvedPlayer.getRiotPuuid(),
                    start,
                    PAGE_SIZE
                );

            List<HenrikMatchData> receivedMatches =
                response.data();

            int receivedMatchCount = receivedMatches.size();

            if (receivedMatchCount == 0) {
                break;
            }

            pagesFetched++;

            if (scope == DeepSynchronizationScope.CURRENT_SEASON
                && currentSeasonId == null) {
                currentSeasonId =
                    resolveMostRecentSeasonId(receivedMatches);
            }

            List<HenrikMatchData> matchesToImport =
                filterMatchesByScope(
                    receivedMatches,
                    scope,
                    currentSeasonId
                );

            if (!matchesToImport.isEmpty()) {
                HenrikMatchHistoryResponse filteredResponse =
                    new HenrikMatchHistoryResponse(
                        response.status(),
                        matchesToImport
                    );

                matchesImported += matchImportService.importMatches(
                    resolvedPlayer,
                    filteredResponse
                );
            }

            boolean seasonBoundaryReached =
                scope == DeepSynchronizationScope.CURRENT_SEASON
                    && containsAnotherSeason(
                    receivedMatches,
                    currentSeasonId
                );

            if (seasonBoundaryReached
                || receivedMatchCount < PAGE_SIZE) {
                break;
            }

            /*
             * Henrik's start parameter is an item offset, not a page number.
             */
            start += receivedMatchCount;
        }

        Instant completedAt = clock.instant();

        resolvedPlayer.setLastSuccessfulSynchronizationAt(
            completedAt
        );

        Player savedPlayer =
            playerRepository.save(resolvedPlayer);

        return new PlayerDeepSynchronizationResult(
            savedPlayer,
            pagesFetched,
            matchesImported,
            completedAt
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
     * <p>Henrik returns match history from the newest match to the oldest.
     * Therefore, the first available season identifier represents the current
     * season for the synchronized player.</p>
     *
     * @param matches matches returned by Henrik
     * @return identifier of the most recent season
     */
    private String resolveMostRecentSeasonId(
        List<HenrikMatchData> matches
    ) {
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
            .filter(match ->
                currentSeasonId.equals(extractSeasonId(match))
            )
            .toList();
    }

    /**
     * Determines whether a page contains a match from an older season.
     *
     * <p>Matches without usable season metadata are ignored here because the
     * import service already rejects malformed matches.</p>
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
            .anyMatch(seasonId ->
                !currentSeasonId.equals(seasonId)
            );
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
}
