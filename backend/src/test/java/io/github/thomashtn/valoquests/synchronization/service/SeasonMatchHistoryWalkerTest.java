package io.github.thomashtn.valoquests.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link SeasonMatchHistoryWalker}.
 *
 * <p>A wrong rule on import, season completion or early stop leaves a silent hole in a player's history.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeasonMatchHistoryWalkerTest {

    /**
     * Number of matches Henrik returns for a full page.
     */
    private static final int PAGE_SIZE = HenrikMatchClient.MAX_PAGE_SIZE;

    /**
     * Riot identifier of the walked player.
     */
    private static final String PUUID = "puuid-1";

    /**
     * Henrik identifier of the current season.
     */
    private static final String CURRENT_SEASON = "season-current";

    /**
     * Henrik identifier of the season preceding the current one.
     */
    private static final String PREVIOUS_SEASON = "season-previous";

    /**
     * Local identifier of the current season.
     */
    private static final long CURRENT_SEASON_ID = 20L;

    /**
     * Henrik identifier of the season preceding the previous one, outside the walked scope.
     */
    private static final String OLDER_SEASON = "season-older";

    /**
     * Local identifier of the previous season.
     */
    private static final long PREVIOUS_SEASON_ID = 19L;

    /**
     * Local identifier of the season preceding the previous one.
     */
    private static final long OLDER_SEASON_ID = 18L;

    /**
     * Start instant shared by every scripted match.
     */
    private static final Instant MATCH_START = Instant.parse("2026-07-20T18:00:00Z");

    @Mock
    private HenrikMatchClient matchClient;

    @Mock
    private MatchImportService matchImportService;

    @Mock
    private SeasonResolutionService seasonResolutionService;

    @Mock
    private SeasonSynchronizationStateService stateService;

    @Mock
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Walker under test.
     */
    private SeasonMatchHistoryWalker walker;

    /**
     * Player being walked.
     */
    private Player player;

    /**
     * Prepares the walker and the default collaborator behaviour.
     */
    @BeforeEach
    void setUp() {
        walker = new SeasonMatchHistoryWalker(
            matchClient,
            matchImportService,
            seasonResolutionService,
            stateService,
            playerMatchRepository
        );

        player = new Player();
        player.setId(1L);
        player.setRiotPuuid(PUUID);

        when(seasonResolutionService.resolve(any())).thenAnswer(invocation -> {
            HenrikMatchMetadata.HenrikSeason source = invocation.getArgument(0);
            Season season = new Season();
            season.setExternalId(source.id());
            season.setId(localSeasonId(source.id()));
            return season;
        });
        when(stateService.startSeason(any(), any()))
            .thenReturn(new SeasonSynchronizationStateService.SeasonWalkStart(0, false));
        when(stateService.findResumableSeasonId(anyLong(), anyString()))
            .thenReturn(Optional.empty());
        when(matchImportService.importPage(any(), any()))
            .thenAnswer(invocation -> allImported(invocation.getArgument(1)));
    }

    /**
     * Verifies the nominal first walk: the current season, then the previous one, then a stop.
     *
     * <p>Older seasons were never targeted; walking them would crawl the whole history one season per run.
     */
    @Test
    void shouldWalkTheCurrentAndPreviousSeasonsThenStop() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(6),
            page(PREVIOUS_SEASON, PAGE_SIZE),
            straddlingPage(PREVIOUS_SEASON, OLDER_SEASON, 4)
        );

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        assertThat(result.pagesFetched()).isEqualTo(4);
        assertThat(result.matchesImported()).isEqualTo(10 + 6 + 4 + 10 + 4);

        InOrder ordered = inOrder(stateService);
        ordered.verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
        ordered.verify(stateService).markSeasonComplete(1L, PREVIOUS_SEASON_ID);
        verify(stateService).startSeason(any(), argThatSeasonIs(PREVIOUS_SEASON));
        verify(stateService, never()).startSeason(any(), argThatSeasonIs(OLDER_SEASON));
        verify(stateService, never()).markSeasonComplete(1L, OLDER_SEASON_ID);
    }

    /**
     * Verifies that a repair of an unfinished season still consumes the trailing budget.
     *
     * <p>Otherwise a chain of unfinished seasons would widen the walk past its target, one boundary at a time.
     */
    @Test
    void shouldNotWidenTheScopeBeyondASeasonItResumed() {
        when(stateService.findResumableSeasonId(1L, PREVIOUS_SEASON))
            .thenReturn(Optional.of(PREVIOUS_SEASON_ID));
        givenPages(
            straddlingPage(6),
            straddlingPage(PREVIOUS_SEASON, OLDER_SEASON, 4)
        );

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        verify(stateService, never()).startSeason(any(), argThatSeasonIs(OLDER_SEASON));
        assertThat(importedSeasonIds()).doesNotContain(OLDER_SEASON);
    }

    /**
     * Verifies that a straddling page only hands the walked seasons' matches to the import.
     *
     * <p>An out-of-scope season with imported matches but no state would skew every statistic filtered on it.
     */
    @Test
    void shouldNotImportMatchesOfASeasonItDoesNotWalk() {
        givenPages(
            straddlingPage(6),
            straddlingPage(PREVIOUS_SEASON, OLDER_SEASON, 4)
        );

        walker.walk(player);

        assertThat(importedSeasonIds())
            .containsOnly(CURRENT_SEASON, PREVIOUS_SEASON);
    }

    /**
     * Verifies that a player with no history is reported rather than failed.
     */
    @Test
    void shouldReportAPlayerWithoutAnyMatch() {
        givenPages(List.of());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result).isEqualTo(
            new MatchHistoryWalkResult(1, 0, SynchronizationStopReason.EMPTY_PAGE)
        );
        verifyNoInteractions(seasonResolutionService, matchImportService);
        verify(stateService, never()).startSeason(any(), any());
    }

    /**
     * Verifies that an unclassifiable first page ends the walk without an exception.
     *
     * <p>Throwing would fail the player on every run; reporting an empty page would hide a full one.
     */
    @Test
    @DisplayName("Reports an unresolved season, not an empty page, when no match carries a season")
    void shouldStopWhenNoMatchCarriesASeason() {
        givenPages(seasonlessPage(PAGE_SIZE));

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_UNRESOLVED);
        assertThat(result.pagesFetched()).isEqualTo(1);
        verifyNoInteractions(seasonResolutionService, matchImportService);
    }

    /**
     * Verifies that the season is taken from the newest match that carries one.
     */
    @Test
    void shouldResolveTheSeasonFromTheFirstMatchThatCarriesOne() {
        List<HenrikMatchData> firstPage = new ArrayList<>();
        firstPage.add(match(null));
        firstPage.addAll(page(CURRENT_SEASON, 5));

        givenPages(firstPage);

        walker.walk(player);

        verify(stateService).startSeason(any(), argThatSeasonIs(CURRENT_SEASON));
    }

    /**
     * Verifies that exhausting the available history completes the season.
     */
    @Test
    void shouldCompleteTheSeasonWhenHistoryEnds() {
        givenPages(page(CURRENT_SEASON, 4));

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.END_OF_HISTORY);
        verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
    }

    /**
     * Verifies that an empty page following a full one completes the season.
     */
    @Test
    void shouldCompleteTheSeasonWhenTheNextPageIsEmpty() {
        givenPages(page(CURRENT_SEASON, PAGE_SIZE), List.of());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.EMPTY_PAGE);
        assertThat(result.pagesFetched()).isEqualTo(2);
        verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
    }

    /**
     * Verifies that a completed season stops as soon as known matches are reached.
     *
     * <p>This is what keeps a routine run down to a single Henrik call per player.
     */
    @Test
    void shouldStopAtKnownHistoryWhenTheSeasonIsComplete() {
        when(stateService.startSeason(any(), argThatSeasonIs(CURRENT_SEASON)))
            .thenReturn(new SeasonSynchronizationStateService.SeasonWalkStart(0, true));
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE)
        );
        doAnswer(invocation -> allKnown(invocation.getArgument(1)))
            .when(matchImportService).importPage(any(), any());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.KNOWN_HISTORY_REACHED);
        verify(matchClient, times(1)).getMatches(eq(PUUID), anyInt(), anyInt());
    }

    /**
     * Verifies that an unfinished season is walked in full despite already-stored matches.
     *
     * <p>Stopping at the first known match would leave everything behind the interruption missing forever.
     */
    @Test
    void shouldIgnoreKnownHistoryWhileTheSeasonIsIncomplete() {
        when(stateService.startSeason(any(), argThatSeasonIs(CURRENT_SEASON)))
            .thenReturn(new SeasonSynchronizationStateService.SeasonWalkStart(0, false));
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(4),
            straddlingPage(PREVIOUS_SEASON, OLDER_SEASON, 4)
        );
        doAnswer(invocation -> allKnown(invocation.getArgument(1)))
            .when(matchImportService).importPage(any(), any());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        verify(matchClient, times(4)).getMatches(eq(PUUID), anyInt(), anyInt());
        verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
    }

    /**
     * Verifies that an unfinished previous season is caught up after a season change.
     *
     * <p>The older season's matches on the straddling page must be imported too, or it is closed with a hole.
     */
    @Test
    void shouldResumeAnUnfinishedPreviousSeasonAfterASeasonChange() {
        when(stateService.findResumableSeasonId(1L, PREVIOUS_SEASON))
            .thenReturn(Optional.of(PREVIOUS_SEASON_ID));
        givenPages(
            straddlingPage(4),
            page(PREVIOUS_SEASON, PAGE_SIZE),
            page(PREVIOUS_SEASON, 3)
        );

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.END_OF_HISTORY);
        assertThat(importedSeasonIds())
            .contains(CURRENT_SEASON, PREVIOUS_SEASON);

        InOrder ordered = inOrder(stateService);
        ordered.verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
        ordered.verify(stateService).markSeasonComplete(1L, PREVIOUS_SEASON_ID);
    }

    /**
     * Verifies that resuming an older season replays the boundary page without a new Henrik call.
     *
     * <p>Refetching wastes a rate-limited call; skipping drops the older season's matches on that page.
     */
    @Test
    void shouldReplayTheBoundaryPageWithoutRefetchingIt() {
        when(stateService.findResumableSeasonId(1L, PREVIOUS_SEASON))
            .thenReturn(Optional.of(PREVIOUS_SEASON_ID));
        givenPages(straddlingPage(4));

        walker.walk(player);

        verify(matchClient, times(1)).getMatches(PUUID, 0, PAGE_SIZE);
        verify(matchImportService, times(2)).importPage(any(), any());
        assertThat(importedSeasonIds()).contains(CURRENT_SEASON, PREVIOUS_SEASON);
    }

    /**
     * Verifies that a page holding only ignored game modes does not stop the walk.
     *
     * <p>The matches that matter may all be on the next page.
     */
    @Test
    void shouldContinueThroughAPageOfIgnoredGameModes() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(4),
            straddlingPage(PREVIOUS_SEASON, OLDER_SEASON, 4)
        );
        doAnswer(invocation -> allSkipped(invocation.getArgument(1)))
            .when(matchImportService).importPage(any(), any());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        verify(matchClient, times(3)).getMatches(eq(PUUID), anyInt(), anyInt());
    }

    /**
     * Verifies that null entries count towards the raw page size.
     *
     * <p>Henrik does return them; a shorter page would read as the end of history and truncate the season.
     */
    @Test
    void shouldCountNullEntriesTowardsThePageSize() {
        List<HenrikMatchData> firstPage = new ArrayList<>(page(CURRENT_SEASON, PAGE_SIZE - 1));
        firstPage.add(null);

        givenPages(firstPage, page(CURRENT_SEASON, 2));

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.pagesFetched()).isEqualTo(2);
        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.END_OF_HISTORY);
    }

    /**
     * Verifies that the safety limit stops the walk without completing the season, but leaves a checkpoint.
     *
     * <p>Marking the truncated season complete would freeze a permanent hole.
     */
    @Test
    void shouldStopOnTheSafetyLimitWithoutCompletingTheSeason() {
        when(matchClient.getMatches(eq(PUUID), anyInt(), anyInt()))
            .thenAnswer(invocation -> response(page(CURRENT_SEASON, PAGE_SIZE)));

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.PAGE_LIMIT_REACHED);
        assertThat(result.pagesFetched()).isEqualTo(1_000);
        verify(stateService, never()).markSeasonComplete(anyLong(), anyLong());
        verify(stateService).recordProgress(1L, CURRENT_SEASON_ID, 1_000 * PAGE_SIZE);
    }

    /**
     * Verifies that a failure mid-walk never leaves a season falsely marked complete.
     */
    @Test
    void shouldNotCompleteTheSeasonWhenImportFails() {
        givenPages(page(CURRENT_SEASON, PAGE_SIZE), page(CURRENT_SEASON, PAGE_SIZE));
        doAnswer(invocation -> allImported(invocation.getArgument(1)))
            .doThrow(new IllegalStateException("database unavailable"))
            .when(matchImportService).importPage(any(), any());

        assertThatThrownBy(() -> walker.walk(player))
            .isInstanceOf(IllegalStateException.class);

        verify(stateService, never()).markSeasonComplete(anyLong(), anyLong());
    }

    /**
     * Verifies that a fresh execution resuming an incomplete season jumps straight to the persisted checkpoint.
     *
     * <p>Only the mandatory first page and the pages beyond the checkpoint cost a Henrik call.
     */
    @Test
    void shouldResumeFromThePersistedCheckpointInsteadOfOffsetZero() {
        givenCheckpoint(30);
        doAnswer(invocation -> allKnown(invocation.getArgument(1)))
            .doAnswer(invocation -> allImported(invocation.getArgument(1)))
            .when(matchImportService).importPage(any(), any());
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, 4)
        );

        walker.walk(player);

        verify(matchClient, times(1)).getMatches(PUUID, 0, PAGE_SIZE);
        verify(matchClient, never()).getMatches(PUUID, 10, PAGE_SIZE);
        verify(matchClient, never()).getMatches(PUUID, 20, PAGE_SIZE);
        verify(matchClient, times(1)).getMatches(PUUID, 30, PAGE_SIZE);
    }

    /**
     * Verifies that matches played since an interrupted run are not skipped by the checkpoint jump.
     *
     * <p>Offsets count from the newest match, so jumping past more than a page of new matches leaves a hole.
     */
    @Test
    @DisplayName("Walks on page by page and discards the checkpoint when the first page is all new")
    void shouldNotJumpToTheCheckpointWhenTheFirstPageHoldsOnlyNewMatches() {
        givenCheckpoint(30);
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, 4)
        );

        MatchHistoryWalkResult result = walker.walk(player);

        verify(matchClient).getMatches(PUUID, 10, PAGE_SIZE);
        verify(matchClient).getMatches(PUUID, 20, PAGE_SIZE);
        verify(stateService).discardProgress(1L, CURRENT_SEASON_ID);
        assertThat(result.pagesFetched()).isEqualTo(5);
        assertThat(result.matchesImported()).isEqualTo(44);
    }

    /**
     * Verifies that a first page of ignored game modes does not cost a valid checkpoint.
     *
     * <p>Custom games are never stored; only reaching the newest stored match proves nothing new lies beyond.
     */
    @Test
    @DisplayName("Jumps to the checkpoint when a first page of ignored modes reaches back to stored history")
    void shouldJumpToTheCheckpointWhenTheFirstPageOfSkippedModesReachesStoredHistory() {
        givenCheckpoint(30);
        when(playerMatchRepository.findNewestMatchStartInSeason(1L, CURRENT_SEASON_ID))
            .thenReturn(Optional.of(MATCH_START.plusSeconds(60)));
        doAnswer(invocation -> allSkipped(invocation.getArgument(1)))
            .doAnswer(invocation -> allImported(invocation.getArgument(1)))
            .when(matchImportService).importPage(any(), any());
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, 4)
        );

        walker.walk(player);

        verify(matchClient, never()).getMatches(PUUID, 10, PAGE_SIZE);
        verify(matchClient).getMatches(PUUID, 30, PAGE_SIZE);
        verify(stateService, never()).discardProgress(anyLong(), anyLong());
    }

    /**
     * Verifies that a first page of ignored game modes all newer than the stored history keeps the
     * checkpoint from being applied.
     */
    @Test
    @DisplayName("Walks on page by page when a first page of ignored modes is newer than stored history")
    void shouldNotJumpToTheCheckpointWhenTheSkippedFirstPageIsNewerThanStoredHistory() {
        givenCheckpoint(30);
        when(playerMatchRepository.findNewestMatchStartInSeason(1L, CURRENT_SEASON_ID))
            .thenReturn(Optional.of(MATCH_START.minusSeconds(60)));
        doAnswer(invocation -> allSkipped(invocation.getArgument(1)))
            .doAnswer(invocation -> allImported(invocation.getArgument(1)))
            .when(matchImportService).importPage(any(), any());
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, 4)
        );

        walker.walk(player);

        verify(matchClient).getMatches(PUUID, 10, PAGE_SIZE);
        verify(stateService).discardProgress(1L, CURRENT_SEASON_ID);
    }

    /**
     * Verifies that only the starting season gets a checkpoint.
     *
     * <p>A season entered by crossing a boundary never starts a walk, so its checkpoint would never be read.
     */
    @Test
    @DisplayName("Records checkpoints for the starting season only")
    void shouldNotRecordProgressForACrossedSeason() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(4),
            page(PREVIOUS_SEASON, PAGE_SIZE),
            page(PREVIOUS_SEASON, 2)
        );

        walker.walk(player);

        verify(stateService).recordProgress(1L, CURRENT_SEASON_ID, 10);
        verify(stateService, never()).recordProgress(eq(1L), eq(PREVIOUS_SEASON_ID), anyInt());
    }

    /**
     * Verifies that unfinished seasons are resumed even once the trailing budget is spent.
     *
     * <p>Their state proves an earlier run targeted them; the budget only bounds seasons the walk starts itself.
     */
    @Test
    @DisplayName("Resumes an unfinished older season even after the trailing budget is spent")
    void shouldResumeAnUnfinishedSeasonBeyondTheTrailingBudget() {
        when(stateService.findResumableSeasonId(1L, PREVIOUS_SEASON))
            .thenReturn(Optional.of(PREVIOUS_SEASON_ID));
        when(stateService.findResumableSeasonId(1L, OLDER_SEASON))
            .thenReturn(Optional.of(OLDER_SEASON_ID));
        givenPages(
            straddlingPage(6),
            straddlingPage(PREVIOUS_SEASON, OLDER_SEASON, 4),
            page(OLDER_SEASON, 3)
        );

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.END_OF_HISTORY);
        verify(stateService).markSeasonComplete(1L, OLDER_SEASON_ID);
        verify(stateService, never()).startSeason(any(), argThatSeasonIs(OLDER_SEASON));
    }

    /**
     * Verifies that the checkpoint is only ever advanced, never applied a second time once a season
     * boundary is crossed within the same run.
     */
    @Test
    void shouldRecordProgressAfterEachDurablyImportedPage() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(4)
        );

        walker.walk(player);

        InOrder ordered = inOrder(stateService);
        ordered.verify(stateService).recordProgress(1L, CURRENT_SEASON_ID, 10);
        ordered.verify(stateService).recordProgress(1L, CURRENT_SEASON_ID, 20);
        ordered.verify(stateService, never())
            .recordProgress(anyLong(), eq(CURRENT_SEASON_ID), eq(30));
    }

    /**
     * Verifies that pagination advances by the raw page size.
     */
    @Test
    void shouldAdvanceTheOffsetByTheRawPageSize() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, 2)
        );

        walker.walk(player);

        InOrder ordered = inOrder(matchClient);
        ordered.verify(matchClient).getMatches(PUUID, 0, PAGE_SIZE);
        ordered.verify(matchClient).getMatches(PUUID, 10, PAGE_SIZE);
        ordered.verify(matchClient).getMatches(PUUID, 20, PAGE_SIZE);
    }

    /**
     * Gives every season a checkpoint left by a previous, interrupted run.
     */
    private void givenCheckpoint(int offset) {
        doReturn(new SeasonSynchronizationStateService.SeasonWalkStart(offset, false))
            .when(stateService).startSeason(any(), any());
    }

    /**
     * Scripts the pages Henrik returns, in order.
     */
    @SafeVarargs
    private void givenPages(List<HenrikMatchData>... pages) {
        when(matchClient.getMatches(eq(PUUID), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                int startOffset = invocation.getArgument(1);
                int index = startOffset / PAGE_SIZE;
                return response(index < pages.length ? pages[index] : List.of());
            });
    }

    /**
     * Wraps matches in a Henrik response.
     */
    private HenrikMatchHistoryResponse response(List<HenrikMatchData> matches) {
        return new HenrikMatchHistoryResponse(matches);
    }

    /**
     * Creates a page holding matches of one season.
     */
    private List<HenrikMatchData> page(String seasonId, int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> match(seasonId))
            .toList();
    }

    /**
     * Creates a full page whose newest matches belong to the current season and the rest to the
     * previous one, as Henrik returns at a season boundary.
     */
    private List<HenrikMatchData> straddlingPage(int currentSeasonMatches) {
        return straddlingPage(CURRENT_SEASON, PREVIOUS_SEASON, currentSeasonMatches);
    }

    /**
     * Creates a full page straddling the boundary between two given seasons.
     */
    private List<HenrikMatchData> straddlingPage(
        String newerSeason,
        String olderSeason,
        int newerSeasonMatches
    ) {
        List<HenrikMatchData> matches = new ArrayList<>(page(newerSeason, newerSeasonMatches));
        matches.addAll(page(olderSeason, PAGE_SIZE - newerSeasonMatches));
        return matches;
    }

    /**
     * Maps a Henrik season identifier to its local identifier.
     */
    private long localSeasonId(String externalId) {
        return switch (externalId) {
            case CURRENT_SEASON -> CURRENT_SEASON_ID;
            case PREVIOUS_SEASON -> PREVIOUS_SEASON_ID;
            default -> OLDER_SEASON_ID;
        };
    }

    /**
     * Creates a page of matches Henrik returned without a season.
     */
    private List<HenrikMatchData> seasonlessPage(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> match(null))
            .toList();
    }

    /**
     * Creates a match belonging to the given season, or to none.
     */
    private HenrikMatchData match(String seasonId) {
        return new HenrikMatchData(
            new HenrikMatchMetadata(
                "match-" + System.nanoTime(),
                null,
                null,
                MATCH_START,
                true,
                new HenrikMatchMetadata.HenrikQueue("competitive", null, null),
                seasonId == null ? null : new HenrikMatchMetadata.HenrikSeason(seasonId, "V26")
            ),
            List.of(),
            List.of()
        );
    }

    /**
     * Reports every match of the submitted page as newly imported.
     */
    private MatchImportResult allImported(List<HenrikMatchData> page) {
        int size = page.size();
        return new MatchImportResult(size, size, 0, 0, 0);
    }

    /**
     * Reports every match of the submitted page as already stored.
     */
    private MatchImportResult allKnown(List<HenrikMatchData> page) {
        int size = page.size();
        return new MatchImportResult(size, 0, size, 0, 0);
    }

    /**
     * Reports every match of the submitted page as an ignored game mode.
     */
    private MatchImportResult allSkipped(List<HenrikMatchData> page) {
        int size = page.size();
        return new MatchImportResult(size, 0, 0, 0, size);
    }

    /**
     * Collects the seasons of every match handed to the import service.
     */
    private List<String> importedSeasonIds() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HenrikMatchData>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchImportService, atLeastOnce())
            .importPage(any(), captor.capture());

        return captor.getAllValues().stream()
            .flatMap(List::stream)
            .map(match -> match.metadata().season() == null
                ? null
                : match.metadata().season().id())
            .distinct()
            .toList();
    }

    /**
     * Matches a season by its Henrik identifier.
     */
    private Season argThatSeasonIs(String externalId) {
        return argThat(season -> season != null && externalId.equals(season.getExternalId()));
    }
}
