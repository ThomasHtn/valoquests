package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.model.MatchImportResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.MatchImportService;
import io.github.thomashtn.valoquests.match.service.SeasonResolutionService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.synchronization.model.MatchHistoryWalkResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStopReason;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Walks one player's Henrik match history backwards, season by season.
 *
 * <p>Scope: the newest match's season and the previous one; an older season only if it has an unfinished
 * state. A season is marked complete only once its oldest match is proved reached, else it is re-walked
 * in full. Stops are judged on the raw page, never the imported subset. Must not run in a transaction.
 * Known limitation: seasons interleaved across a page boundary stop the walk early.
 */
@Service
public class SeasonMatchHistoryWalker {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SeasonMatchHistoryWalker.class);

    /**
     * Safety guard against a walk that never advances, for instance if Henrik repeats a page.
     *
     * <p>Far above a heavy player's act, so reaching it signals an anomaly; the per-page checkpoint
     * lets the next run resume near this point.
     */
    private static final int MAXIMUM_PAGE_COUNT = 1_000;

    /**
     * Number of seasons the walk may open on its own behind the season of the newest match.
     *
     * <p>An unfinished season is resumed regardless of the budget, but every crossing consumes it.
     */
    private static final int TRAILING_SEASON_BUDGET = 1;

    /**
     * Henrik client used to retrieve match-history pages.
     */
    private final HenrikMatchClient matchClient;

    /**
     * Service used to persist Henrik matches idempotently.
     */
    private final MatchImportService matchImportService;

    /**
     * Service used to resolve and persist match seasons.
     */
    private final SeasonResolutionService seasonResolutionService;

    /**
     * Service owning the per-player season completion state.
     */
    private final SeasonSynchronizationStateService stateService;

    /**
     * Repository telling how far back the player's stored history of a season reaches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Creates the season-scoped match history walker.
     *
     * @param matchClient             Henrik client returning pages of match history
     * @param matchImportService      service persisting the matches of one page
     * @param seasonResolutionService service resolving the season a match belongs to
     * @param stateService            service owning the per-player season completion state
     * @param playerMatchRepository   repository holding the player's stored matches
     */
    public SeasonMatchHistoryWalker(
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        SeasonResolutionService seasonResolutionService,
        SeasonSynchronizationStateService stateService,
        PlayerMatchRepository playerMatchRepository
    ) {
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.seasonResolutionService = seasonResolutionService;
        this.stateService = stateService;
        this.playerMatchRepository = playerMatchRepository;
    }

    /**
     * Imports the player's current and previous seasons, resuming unfinished seasons on the way.
     *
     * @param player tracked player whose Riot identifier is already resolved
     * @return the pages retrieved, matches imported and the condition that ended the walk
     */
    public MatchHistoryWalkResult walk(Player player) {
        HistoryPage firstPage = fetchPage(player, 0, 0);
        if (firstPage.matches().isEmpty()) {
            return stop(player, SynchronizationStopReason.EMPTY_PAGE, firstPage, 0, null);
        }

        Optional<HenrikMatchMetadata.HenrikSeason> targetSeason =
            firstResolvableSeason(firstPage.matches());
        if (targetSeason.isEmpty()) {
            LOGGER.warn(
                "Unable to determine the current season for player {}: no match of the first page "
                    + "carries a season identifier. Skipping match import.",
                player.getId()
            );
            return new MatchHistoryWalkResult(1, 0, SynchronizationStopReason.SEASON_UNRESOLVED);
        }

        return walkPages(player, firstPage, targetSeason.get());
    }

    /**
     * Walks every page from the first one, starting with the season it revealed.
     */
    private MatchHistoryWalkResult walkPages(
        Player player,
        HistoryPage firstPage,
        HenrikMatchMetadata.HenrikSeason targetSeason
    ) {
        Season season = seasonResolutionService.resolve(targetSeason);
        SeasonScope scope = startScope(player, season, targetSeason.id());
        // Only this season's checkpoint is read back: a crossed season never becomes the starting one.
        Long startingSeasonId = scope.seasonId();
        // Read before the first page is imported, so its new matches never pass for known history.
        Optional<Instant> newestStoredStart = newestStoredStart(player, scope);

        HistoryPage page = firstPage;
        int fromIndex = 0;
        int matchesImported = 0;

        while (true) {
            PageImport pageImport = importInScopeMatches(player, page, fromIndex, scope);
            matchesImported += pageImport.imported();

            int olderSeasonIndex = firstForeignSeasonIndex(page.matches(), scope.externalId(), fromIndex);
            if (olderSeasonIndex >= 0) {
                Optional<SeasonScope> crossedScope =
                    crossSeasonBoundary(player, scope, page.matches().get(olderSeasonIndex));
                if (crossedScope.isEmpty()) {
                    return stop(player, SynchronizationStopReason.SEASON_BOUNDARY, page,
                        matchesImported, scope.externalId());
                }
                // Replay this page from the boundary index, not zero, so newer matches don't stop it.
                scope = crossedScope.get();
                fromIndex = olderSeasonIndex;
                continue;
            }

            if (page.matches().size() < HenrikMatchClient.MAX_PAGE_SIZE) {
                stateService.markSeasonComplete(player.getId(), scope.seasonId());
                return stop(player, SynchronizationStopReason.END_OF_HISTORY, page,
                    matchesImported, scope.externalId());
            }

            if (scope.alreadyComplete() && pageImport.knownHistoryReached()) {
                return stop(player, SynchronizationStopReason.KNOWN_HISTORY_REACHED, page,
                    matchesImported, scope.externalId());
            }

            int nextOffset = nextPageOffset(player, scope, page, pageImport, newestStoredStart);

            // The page is already committed, so the checkpoint never passes what is stored.
            if (scope.seasonId().equals(startingSeasonId)) {
                stateService.recordProgress(player.getId(), scope.seasonId(), nextOffset);
            }

            if (page.pagesFetched() >= MAXIMUM_PAGE_COUNT) {
                // Not marked complete: freezing a truncated season would leave a permanent hole.
                LOGGER.warn(
                    "Match history walk reached the safety page limit for player {}: "
                        + "maximumPages={} start={} season={}",
                    player.getId(), MAXIMUM_PAGE_COUNT, nextOffset, scope.externalId()
                );
                return new MatchHistoryWalkResult(page.pagesFetched(), matchesImported,
                    SynchronizationStopReason.PAGE_LIMIT_REACHED);
            }

            page = fetchPage(player, page.number() + 1, nextOffset);
            fromIndex = 0;
            if (page.matches().isEmpty()) {
                stateService.markSeasonComplete(player.getId(), scope.seasonId());
                return stop(player, SynchronizationStopReason.EMPTY_PAGE, page,
                    matchesImported, scope.externalId());
            }
        }
    }

    /**
     * Decides where the walk continues after a page.
     *
     * <p>Only the first page may jump to a further checkpoint, and only once it reaches stored history
     * (a stored match, or an oldest start no later than the newest stored); else the checkpoint is
     * discarded, as newer matches may have shifted it.
     *
     * @param player     tracked player being walked
     * @param scope      season being walked
     * @param page       page just imported
     * @param pageImport outcome of importing that page
     * @param newestStoredStart start of the newest match stored for the season before this walk
     * @return the offset of the next page to fetch
     */
    private int nextPageOffset(
        Player player,
        SeasonScope scope,
        HistoryPage page,
        PageImport pageImport,
        Optional<Instant> newestStoredStart
    ) {
        int nextOffset = page.startOffset() + page.matches().size();
        int checkpointOffset = page.number() == 0 ? scope.resumeOffset() : 0;
        if (checkpointOffset <= nextOffset) {
            return nextOffset;
        }
        if (pageImport.alreadyKnown() > 0 || reachesBackTo(page, newestStoredStart)) {
            LOGGER.info(
                "Resuming match history walk for player {} from checkpoint: season={} "
                    + "offset={} instead of {}",
                player.getId(), scope.externalId(), checkpointOffset, nextOffset
            );
            return checkpointOffset;
        }

        LOGGER.info(
            "Ignoring the checkpoint of season {} for player {}: the first page does not reach "
                + "stored history, so matches played since it was written may extend past it",
            scope.externalId(), player.getId()
        );
        stateService.discardProgress(player.getId(), scope.seasonId());
        return nextOffset;
    }

    /**
     * Start of the newest match stored for the season, only looked up when a checkpoint may use it.
     */
    private Optional<Instant> newestStoredStart(Player player, SeasonScope scope) {
        if (scope.resumeOffset() <= 0) {
            return Optional.empty();
        }
        return playerMatchRepository.findNewestMatchStartInSeason(player.getId(), scope.seasonId());
    }

    /**
     * Tells whether the oldest match of a page started no later than a stored match.
     */
    private boolean reachesBackTo(HistoryPage page, Optional<Instant> newestStoredStart) {
        Optional<Instant> oldestOnPage = page.matches().stream()
            .filter(Objects::nonNull)
            .map(HenrikMatchData::metadata)
            .filter(Objects::nonNull)
            .map(HenrikMatchMetadata::startedAt)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder());
        return oldestOnPage.isPresent()
            && newestStoredStart.isPresent()
            && !oldestOnPage.get().isAfter(newestStoredStart.get());
    }

    /**
     * Closes the season the walk just left and decides whether it continues into the older one.
     *
     * <p>The left season is marked complete first. The older one is admitted if it has an unfinished
     * state, else if the trailing budget allows; every admission consumes the budget.
     *
     * @param player        tracked player being walked
     * @param leaving       season the walk just walked past the oldest match of
     * @param boundaryMatch first match of the older season on the current page
     * @return the scope to continue with, or empty when the walk must stop at this boundary
     */
    private Optional<SeasonScope> crossSeasonBoundary(
        Player player,
        SeasonScope leaving,
        HenrikMatchData boundaryMatch
    ) {
        stateService.markSeasonComplete(player.getId(), leaving.seasonId());

        String foreignSeasonId = seasonId(boundaryMatch);
        int remainingBudget = leaving.trailingSeasonBudget() - 1;
        Optional<Long> resumableSeasonId =
            stateService.findResumableSeasonId(player.getId(), foreignSeasonId);
        if (resumableSeasonId.isPresent()) {
            LOGGER.info(
                "Resuming an unfinished season for player {}: season={} externalId={}",
                player.getId(),
                resumableSeasonId.get(),
                foreignSeasonId
            );
            return Optional.of(
                new SeasonScope(resumableSeasonId.get(), foreignSeasonId, false, 0, remainingBudget)
            );
        }

        if (leaving.trailingSeasonBudget() <= 0) {
            return Optional.empty();
        }

        Season season = seasonResolutionService.resolve(boundaryMatch.metadata().season());
        // Its checkpoint is not applied: the walk already sits at its newest match, a jump would skip pages.
        boolean alreadyComplete = stateService.startSeason(player, season).complete();
        LOGGER.info(
            "Extending the walk into the previous season for player {}: season={} externalId={}",
            player.getId(),
            season.getId(),
            foreignSeasonId
        );
        return Optional.of(
            new SeasonScope(season.getId(), foreignSeasonId, alreadyComplete, 0, remainingBudget)
        );
    }

    /**
     * Declares the season being walked and reports whether it was already complete.
     */
    private SeasonScope startScope(Player player, Season season, String externalId) {
        SeasonSynchronizationStateService.SeasonWalkStart walkStart =
            stateService.startSeason(player, season);
        if (!walkStart.complete()) {
            LOGGER.info(
                "Season {} is not fully synchronized for player {}: walking it in full "
                    + "from checkpoint offset {}",
                externalId,
                player.getId(),
                walkStart.resumeOffset()
            );
        }
        return new SeasonScope(
            season.getId(),
            externalId,
            walkStart.complete(),
            walkStart.resumeOffset(),
            TRAILING_SEASON_BUDGET
        );
    }

    /**
     * Imports the matches of a page segment that belong to the season being walked.
     *
     * <p>Other seasons' matches are withheld: stored without a season state, they would skew it for good.
     */
    private PageImport importInScopeMatches(
        Player player,
        HistoryPage page,
        int fromIndex,
        SeasonScope scope
    ) {
        List<HenrikMatchData> inScope = page.matches().subList(fromIndex, page.matches().size()).stream()
            .filter(match -> isInScope(match, scope.externalId()))
            .toList();

        MatchImportResult importResult = matchImportService.importPage(player, inScope);

        LOGGER.info(
            "Imported Henrik match page for player {}: page={} start={} received={} inScope={} "
                + "imported={} alreadyKnown={} rejected={} skipped={} season={}",
            player.getId(),
            page.number(),
            page.startOffset(),
            page.matches().size(),
            inScope.size(),
            importResult.imported(),
            importResult.alreadyKnown(),
            importResult.rejected(),
            importResult.skipped(),
            scope.externalId()
        );
        return new PageImport(
            importResult.imported(),
            importResult.alreadyKnown(),
            importResult.knownHistoryReached()
        );
    }

    /**
     * Determines whether a match belongs to the season being walked.
     *
     * <p>A match without a season stays in scope so the import service rejects it in the counters.
     */
    private boolean isInScope(HenrikMatchData match, String seasonExternalId) {
        String matchSeasonId = seasonId(match);
        return matchSeasonId == null || matchSeasonId.equals(seasonExternalId);
    }

    /**
     * Finds where the page leaves the season being walked.
     *
     * @param page raw Henrik page, ordered from the newest match to the oldest
     * @param seasonExternalId Henrik identifier of the season being walked
     * @param fromIndex first index still to be examined
     * @return index of the first match of another season, or {@code -1} when the page stays in scope
     */
    private int firstForeignSeasonIndex(
        List<HenrikMatchData> page,
        String seasonExternalId,
        int fromIndex
    ) {
        for (int index = fromIndex; index < page.size(); index++) {
            String matchSeasonId = seasonId(page.get(index));
            if (matchSeasonId != null && !matchSeasonId.equals(seasonExternalId)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Finds the season of the newest match carrying one.
     */
    private Optional<HenrikMatchMetadata.HenrikSeason> firstResolvableSeason(
        List<HenrikMatchData> page
    ) {
        return page.stream()
            .filter(match -> seasonId(match) != null)
            .map(match -> match.metadata().season())
            .findFirst();
    }

    /**
     * Reads a match season identifier, tolerating a null entry in the page.
     */
    private String seasonId(HenrikMatchData match) {
        return match == null ? null : match.seasonId();
    }

    /**
     * Retrieves one match-history page, tolerating a null payload from Henrik.
     */
    private HistoryPage fetchPage(Player player, int number, int startOffset) {
        HenrikMatchHistoryResponse response = matchClient.getMatches(
            player.getRiotPuuid(),
            startOffset,
            HenrikMatchClient.MAX_PAGE_SIZE
        );
        return new HistoryPage(response == null ? List.of() : response.data(), number, startOffset);
    }

    /**
     * Logs why the walk stopped and reports it.
     *
     * @param player           tracked player being walked
     * @param stopReason       condition that ended the walk
     * @param lastPage         last page fetched
     * @param matchesImported  matches newly stored during the walk
     * @param seasonExternalId Henrik identifier of the season being walked, {@code null} before any
     * @return the walk's result
     */
    private MatchHistoryWalkResult stop(
        Player player,
        SynchronizationStopReason stopReason,
        HistoryPage lastPage,
        int matchesImported,
        String seasonExternalId
    ) {
        LOGGER.info(
            "Stopping match history walk for player {}: page={} start={} stopReason={} season={}",
            player.getId(),
            lastPage.number(),
            lastPage.startOffset(),
            stopReason,
            seasonExternalId
        );
        return new MatchHistoryWalkResult(lastPage.pagesFetched(), matchesImported, stopReason);
    }

    /**
     * Season currently being walked.
     *
     * @param seasonId local season identifier
     * @param externalId Henrik season identifier
     * @param alreadyComplete whether reaching known matches may stop the walk
     * @param resumeOffset checkpoint offset to apply once, right after the mandatory first page
     * @param trailingSeasonBudget seasons the walk may still open on its own behind this one
     */
    private record SeasonScope(
        Long seasonId,
        String externalId,
        boolean alreadyComplete,
        int resumeOffset,
        int trailingSeasonBudget
    ) {
    }

    /**
     * One page of Henrik match history, newest match first.
     *
     * @param matches     raw entries of the page, possibly holding {@code null} entries
     * @param number      zero-based position of the page in this walk
     * @param startOffset Henrik offset the page was fetched from
     */
    private record HistoryPage(List<HenrikMatchData> matches, int number, int startOffset) {

        /**
         * Number of pages fetched so far, this one included.
         */
        int pagesFetched() {
            return number + 1;
        }
    }

    /**
     * Outcome of importing one page segment.
     *
     * @param imported number of newly stored player matches
     * @param alreadyKnown number of in-scope matches that were already stored
     * @param knownHistoryReached whether every in-scope match of the segment was already stored
     */
    private record PageImport(int imported, int alreadyKnown, boolean knownHistoryReached) {
    }
}
