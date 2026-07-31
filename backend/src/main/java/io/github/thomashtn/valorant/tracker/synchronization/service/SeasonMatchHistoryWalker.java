package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.model.MatchImportResult;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.match.service.SeasonResolutionService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.synchronization.model.MatchHistoryWalkResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStopReason;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Walks one player's Henrik match history backwards, season by season.
 *
 * <p>Henrik returns matches newest first, so the walk starts at offset zero and moves back in time.
 * Its scope is the season of the newest match: everything that season holds is imported, and the
 * walk stops when it crosses into an older one.
 *
 * <p>Two rules make the result trustworthy across interruptions and season changes:
 *
 * <ul>
 *   <li>A season is only marked complete once the walk has proved it reached its oldest match, by
 *       crossing into an older season or by exhausting the available history. Until then the stored
 *       history may have holes, so the next run re-walks the season in full rather than stopping at
 *       the first already-stored match.</li>
 *   <li>An older season is only walked when the player already has an unfinished state for it. That
 *       finishes what a previous run started, whether it was interrupted or overtaken by a season
 *       change, without ever widening the scope: on an empty database no older state exists, so the
 *       walk is bounded by the current season.</li>
 * </ul>
 *
 * <p>Stop conditions are evaluated on the raw Henrik page, never on the subset actually imported.
 * A page holding nothing but ignored game modes proves nothing about the history behind it and must
 * not read as a boundary.
 *
 * <p><strong>Known limitation.</strong> Seasons interleaved across a page boundary are not detected:
 * if the last match of a page belongs to an older season and the first match of the next page
 * belongs to the current one again, the walk stops early. This would require Riot to have tagged an
 * older match with a newer act, while Henrik orders matches strictly by descending start instant.
 *
 * <p><strong>This walk must not run inside a transaction.</strong> See
 * {@link SeasonSynchronizationStateService} for why the completion flag depends on each step
 * committing on its own.
 */
@Service
public class SeasonMatchHistoryWalker {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(SeasonMatchHistoryWalker.class);

    /**
     * Number of matches requested from Henrik per HTTP call, capped by the client.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Safety guard against a walk that never advances, for instance if Henrik repeats a page.
     *
     * <p>Sized well beyond a normal run: a heavy player reaches roughly ninety pages over an act,
     * and a run chaining two seasons about twice that. Reaching this limit signals an anomaly, not a
     * busy player, which is why it stops the walk instead of raising the limit.
     */
    private static final int MAXIMUM_PAGE_COUNT = 300;

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
     * Creates the season-scoped match history walker.
     */
    public SeasonMatchHistoryWalker(
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        SeasonResolutionService seasonResolutionService,
        SeasonSynchronizationStateService stateService
    ) {
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.seasonResolutionService = seasonResolutionService;
        this.stateService = stateService;
    }

    /**
     * Imports every match of the player's current season, resuming unfinished seasons on the way.
     *
     * @param player tracked player whose Riot identifier is already resolved
     * @return the pages retrieved, matches imported and the condition that ended the walk
     */
    public MatchHistoryWalkResult walk(Player player) {
        HenrikMatchHistoryResponse firstResponse = fetchPage(player, 0);
        if (firstResponse.data().isEmpty()) {
            logStop(player, 0, 0, SynchronizationStopReason.EMPTY_PAGE, null);
            return MatchHistoryWalkResult.empty();
        }

        Optional<HenrikMatchMetadata.HenrikSeason> targetSeason =
            firstResolvableSeason(firstResponse.data());
        if (targetSeason.isEmpty()) {
            LOGGER.warn(
                "Unable to determine the current season for player {}: no match of the first page "
                    + "carries a season identifier. Skipping match import.",
                player.getId()
            );
            return MatchHistoryWalkResult.empty();
        }

        return walkFrom(player, firstResponse, targetSeason.get());
    }

    /**
     * Walks every page from the first one, starting with the season it revealed.
     */
    private MatchHistoryWalkResult walkFrom(
        Player player,
        HenrikMatchHistoryResponse firstResponse,
        HenrikMatchMetadata.HenrikSeason targetSeason
    ) {
        Season season = seasonResolutionService.resolve(targetSeason);
        SeasonScope scope = startScope(player, season, targetSeason.id());

        HenrikMatchHistoryResponse response = firstResponse;
        int fromIndex = 0;
        int startOffset = 0;
        int pagesFetched = 1;
        int matchesImported = 0;

        while (true) {
            List<HenrikMatchData> page = response.data();
            PageImport pageImport =
                importSegment(player, response, fromIndex, scope, pagesFetched, startOffset);
            matchesImported += pageImport.imported();

            int foreignIndex = firstForeignSeasonIndex(page, scope.externalId(), fromIndex);
            if (foreignIndex >= 0) {
                stateService.markSeasonComplete(player.getId(), scope.seasonId());

                String foreignSeasonId = seasonId(page.get(foreignIndex));
                Optional<Long> resumableSeasonId =
                    stateService.findResumableSeasonId(player.getId(), foreignSeasonId);
                if (resumableSeasonId.isPresent()) {
                    // The same page is replayed from the boundary for the admitted season: its
                    // matches sit on this page and would otherwise be the hole at that season's
                    // most recent end. Resuming at the boundary index rather than at zero keeps the
                    // already-walked newer matches out of scope, which would otherwise read as a
                    // boundary again and stop the walk immediately.
                    scope = new SeasonScope(resumableSeasonId.get(), foreignSeasonId, false);
                    fromIndex = foreignIndex;
                    LOGGER.info(
                        "Resuming an unfinished season for player {}: season={} externalId={}",
                        player.getId(),
                        scope.seasonId(),
                        foreignSeasonId
                    );
                    continue;
                }

                logStop(player, pagesFetched - 1, startOffset,
                    SynchronizationStopReason.SEASON_BOUNDARY, scope.externalId());
                return new MatchHistoryWalkResult(pagesFetched, matchesImported,
                    SynchronizationStopReason.SEASON_BOUNDARY);
            }

            if (page.size() < PAGE_SIZE) {
                stateService.markSeasonComplete(player.getId(), scope.seasonId());
                logStop(player, pagesFetched - 1, startOffset,
                    SynchronizationStopReason.END_OF_HISTORY, scope.externalId());
                return new MatchHistoryWalkResult(pagesFetched, matchesImported,
                    SynchronizationStopReason.END_OF_HISTORY);
            }

            if (scope.earlyStopAllowed() && pageImport.knownHistoryReached()) {
                logStop(player, pagesFetched - 1, startOffset,
                    SynchronizationStopReason.KNOWN_HISTORY_REACHED, scope.externalId());
                return new MatchHistoryWalkResult(pagesFetched, matchesImported,
                    SynchronizationStopReason.KNOWN_HISTORY_REACHED);
            }

            startOffset += page.size();

            if (pagesFetched >= MAXIMUM_PAGE_COUNT) {
                // Deliberately not marked complete: the season is truncated, and freezing it here
                // would turn the truncation into a permanent hole.
                LOGGER.warn(
                    "Match history walk reached the safety page limit for player {}: "
                        + "maximumPages={} start={} season={}",
                    player.getId(), MAXIMUM_PAGE_COUNT, startOffset, scope.externalId()
                );
                return new MatchHistoryWalkResult(pagesFetched, matchesImported,
                    SynchronizationStopReason.PAGE_LIMIT_REACHED);
            }

            response = fetchPage(player, startOffset);
            fromIndex = 0;
            if (response.data().isEmpty()) {
                stateService.markSeasonComplete(player.getId(), scope.seasonId());
                logStop(player, pagesFetched, startOffset,
                    SynchronizationStopReason.EMPTY_PAGE, scope.externalId());
                return new MatchHistoryWalkResult(pagesFetched, matchesImported,
                    SynchronizationStopReason.EMPTY_PAGE);
            }
            pagesFetched++;
        }
    }

    /**
     * Declares the season being walked and reports whether an early stop may be trusted.
     */
    private SeasonScope startScope(Player player, Season season, String externalId) {
        Long seasonId = stateService.startSeason(player, season);
        boolean earlyStopAllowed = stateService.isComplete(player.getId(), seasonId);
        if (!earlyStopAllowed) {
            LOGGER.info(
                "Season {} is not fully synchronized for player {}: walking it in full",
                externalId,
                player.getId()
            );
        }
        return new SeasonScope(seasonId, externalId, earlyStopAllowed);
    }

    /**
     * Imports the matches of a page segment that belong to the season being walked.
     *
     * <p>Matches of another season are withheld on purpose: importing them without declaring their
     * season would leave that season holding a handful of matches with no state to say it is
     * unfinished, permanently skewing the statistics and challenges filtered on it.
     */
    private PageImport importSegment(
        Player player,
        HenrikMatchHistoryResponse response,
        int fromIndex,
        SeasonScope scope,
        int pageNumber,
        int startOffset
    ) {
        List<HenrikMatchData> page = response.data();
        List<HenrikMatchData> inScope = page.subList(fromIndex, page.size()).stream()
            .filter(match -> isInScope(match, scope.externalId()))
            .toList();

        MatchImportResult importResult = matchImportService.importMatchesWithSummary(
            player,
            new HenrikMatchHistoryResponse(response.status(), inScope)
        );

        LOGGER.info(
            "Imported Henrik match page for player {}: page={} start={} received={} inScope={} "
                + "imported={} alreadyKnown={} rejected={} skipped={} season={}",
            player.getId(),
            pageNumber - 1,
            startOffset,
            page.size(),
            inScope.size(),
            importResult.imported(),
            importResult.alreadyKnown(),
            importResult.rejected(),
            importResult.skipped(),
            scope.externalId()
        );
        return new PageImport(importResult.imported(), importResult.knownHistoryReached());
    }

    /**
     * Determines whether a match belongs to the season being walked.
     *
     * <p>A match with no usable season identifier stays in scope so the import service classifies
     * it, and rejects it, rather than having it silently disappear from the counters.
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
     * Reads a match season identifier, tolerating every level of missing payload.
     */
    private String seasonId(HenrikMatchData match) {
        if (match == null || match.metadata() == null || match.metadata().season() == null) {
            return null;
        }
        String id = match.metadata().season().id();
        return id == null || id.isBlank() ? null : id;
    }

    /**
     * Retrieves one match-history page, tolerating a null payload from Henrik.
     */
    private HenrikMatchHistoryResponse fetchPage(Player player, int startOffset) {
        HenrikMatchHistoryResponse response = matchClient.getMatches(
            player.getRiotPuuid(),
            startOffset,
            PAGE_SIZE
        );
        return response == null
            ? new HenrikMatchHistoryResponse(null, List.of())
            : response;
    }

    /**
     * Logs why the walk stopped.
     */
    private void logStop(
        Player player,
        int pageNumber,
        int startOffset,
        SynchronizationStopReason stopReason,
        String seasonExternalId
    ) {
        LOGGER.info(
            "Stopping match history walk for player {}: page={} start={} stopReason={} season={}",
            player.getId(),
            pageNumber,
            startOffset,
            stopReason,
            seasonExternalId
        );
    }

    /**
     * Season currently being walked.
     *
     * @param seasonId local season identifier
     * @param externalId Henrik season identifier
     * @param earlyStopAllowed whether the stored history of this season is known to be contiguous
     */
    private record SeasonScope(Long seasonId, String externalId, boolean earlyStopAllowed) {
    }

    /**
     * Outcome of importing one page segment.
     *
     * @param imported number of newly stored player matches
     * @param knownHistoryReached whether every in-scope match of the segment was already stored
     */
    private record PageImport(int imported, boolean knownHistoryReached) {
    }
}
