package io.github.thomashtn.valorant.tracker.synchronization.service;

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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
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
 * <p>These cover the rules that make the walk safe across interruptions and season changes: what is
 * imported, when a season may be declared complete, and when stopping at already-stored matches is
 * trustworthy. Getting any of them wrong leaves a silent hole in a player's history.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeasonMatchHistoryWalkerTest {

    /**
     * Number of matches Henrik returns for a full page.
     */
    private static final int PAGE_SIZE = 10;

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
     * Local identifier of the previous season.
     */
    private static final long PREVIOUS_SEASON_ID = 19L;

    @Mock
    private HenrikMatchClient matchClient;

    @Mock
    private MatchImportService matchImportService;

    @Mock
    private SeasonResolutionService seasonResolutionService;

    @Mock
    private SeasonSynchronizationStateService stateService;

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
            stateService
        );

        player = new Player();
        player.setId(1L);
        player.setRiotPuuid(PUUID);

        when(seasonResolutionService.resolve(any())).thenAnswer(invocation -> {
            HenrikMatchMetadata.HenrikSeason source = invocation.getArgument(0);
            Season season = new Season();
            season.setExternalId(source.id());
            season.setId(CURRENT_SEASON.equals(source.id())
                ? CURRENT_SEASON_ID
                : PREVIOUS_SEASON_ID);
            return season;
        });
        when(stateService.startSeason(any(), any()))
            .thenAnswer(invocation -> new SeasonSynchronizationStateService.SeasonWalkStart(
                ((Season) invocation.getArgument(1)).getId(),
                0
            ));
        when(stateService.isComplete(anyLong(), anyLong())).thenReturn(false);
        when(stateService.findResumableSeasonId(anyLong(), anyString()))
            .thenReturn(Optional.empty());
        when(matchImportService.importMatchesWithSummary(any(), any()))
            .thenAnswer(invocation -> allImported(invocation.getArgument(1)));
    }

    /**
     * Verifies the nominal first walk of a season on an empty database.
     *
     * <p>The older season the walk crosses into was never targeted, so it must be left untouched:
     * declaring it would make the next run walk the player's whole history one season at a time.
     */
    @Test
    void shouldWalkTheCurrentSeasonAndStopAtItsBoundary() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(6)
        );

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        assertThat(result.pagesFetched()).isEqualTo(3);
        assertThat(result.matchesImported()).isEqualTo(26);

        verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
        verify(stateService, never()).markSeasonComplete(1L, PREVIOUS_SEASON_ID);
        verify(stateService, never()).startSeason(any(), argThatSeasonIs(PREVIOUS_SEASON));
    }

    /**
     * Verifies that a straddling page only hands the current season's matches to the import.
     *
     * <p>Importing the older season's matches without declaring that season would leave it holding a
     * handful of matches and no state saying it is unfinished, skewing every statistic filtered on
     * it, forever.
     */
    @Test
    void shouldNotImportMatchesOfASeasonItDoesNotWalk() {
        givenPages(straddlingPage(6));

        walker.walk(player);

        assertThat(importedSeasonIds())
            .containsOnly(CURRENT_SEASON);
    }

    /**
     * Verifies that a player with no history is reported rather than failed.
     */
    @Test
    void shouldReportAPlayerWithoutAnyMatch() {
        givenPages(List.of());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result).isEqualTo(MatchHistoryWalkResult.empty());
        verifyNoInteractions(seasonResolutionService, matchImportService);
        verify(stateService, never()).startSeason(any(), any());
    }

    /**
     * Verifies that an unclassifiable first page ends the walk without an exception.
     *
     * <p>The season cannot be determined, so nothing can be imported safely. Failing here would mark
     * the player failed on every single run.
     */
    @Test
    void shouldStopWhenNoMatchCarriesASeason() {
        givenPages(seasonlessPage(PAGE_SIZE));

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.EMPTY_PAGE);
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
        assertThat(result.pagesFetched()).isEqualTo(1);
        verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
    }

    /**
     * Verifies that a completed season stops as soon as known matches are reached.
     *
     * <p>This is what keeps a routine run down to a single Henrik call per player.
     */
    @Test
    void shouldStopAtKnownHistoryWhenTheSeasonIsComplete() {
        when(stateService.isComplete(1L, CURRENT_SEASON_ID)).thenReturn(true);
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE)
        );
        doAnswer(invocation -> allKnown(invocation.getArgument(1)))
            .when(matchImportService).importMatchesWithSummary(any(), any());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.KNOWN_HISTORY_REACHED);
        verify(matchClient, times(1)).getMatches(eq(PUUID), anyInt(), anyInt());
    }

    /**
     * Verifies that an unfinished season is walked in full despite already-stored matches.
     *
     * <p>The guarantee that an interrupted run never leaves a permanent hole: stopping at the first
     * known match would leave everything behind the interruption point missing forever.
     */
    @Test
    void shouldIgnoreKnownHistoryWhileTheSeasonIsIncomplete() {
        when(stateService.isComplete(1L, CURRENT_SEASON_ID)).thenReturn(false);
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(4)
        );
        doAnswer(invocation -> allKnown(invocation.getArgument(1)))
            .when(matchImportService).importMatchesWithSummary(any(), any());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        verify(matchClient, times(3)).getMatches(eq(PUUID), anyInt(), anyInt());
        verify(stateService).markSeasonComplete(1L, CURRENT_SEASON_ID);
    }

    /**
     * Verifies that an unfinished previous season is caught up after a season change.
     *
     * <p>Riot rolling the act over must not abandon a season the player was still catching up. The
     * older season's matches sitting on the straddling page have to be imported too, otherwise that
     * season starts with a hole at its newest end and is then declared complete.
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
     * <p>Refetching it would burn a rate-limited request; skipping it would drop the older season's
     * matches that page carries.
     */
    @Test
    void shouldReplayTheBoundaryPageWithoutRefetchingIt() {
        when(stateService.findResumableSeasonId(1L, PREVIOUS_SEASON))
            .thenReturn(Optional.of(PREVIOUS_SEASON_ID));
        givenPages(straddlingPage(4));

        walker.walk(player);

        verify(matchClient, times(1)).getMatches(PUUID, 0, PAGE_SIZE);
        verify(matchImportService, times(2)).importMatchesWithSummary(any(), any());
        assertThat(importedSeasonIds()).contains(CURRENT_SEASON, PREVIOUS_SEASON);
    }

    /**
     * Verifies that a page holding only ignored game modes does not stop the walk.
     *
     * <p>Such a page proves nothing about the history behind it: the matches that matter may all be
     * on the next one.
     */
    @Test
    void shouldContinueThroughAPageOfIgnoredGameModes() {
        givenPages(
            page(CURRENT_SEASON, PAGE_SIZE),
            straddlingPage(4)
        );
        doAnswer(invocation -> allSkipped(invocation.getArgument(1)))
            .when(matchImportService).importMatchesWithSummary(any(), any());

        MatchHistoryWalkResult result = walker.walk(player);

        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);
        verify(matchClient, times(2)).getMatches(eq(PUUID), anyInt(), anyInt());
    }

    /**
     * Verifies that null entries count towards the raw page size.
     *
     * <p>Henrik does return them. Letting them shorten the page would read as the end of the
     * history and truncate the season.
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
     * Verifies that the safety limit stops the walk without declaring the season complete, but still
     * leaves a checkpoint so the next run resumes near this point instead of from the season's start.
     *
     * <p>The season is truncated: marking it complete would freeze the truncation into a permanent
     * hole that no later run would ever repair.
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
            .when(matchImportService).importMatchesWithSummary(any(), any());

        assertThatThrownBy(() -> walker.walk(player))
            .isInstanceOf(IllegalStateException.class);

        verify(stateService, never()).markSeasonComplete(anyLong(), anyLong());
    }

    /**
     * Verifies that a fresh execution resuming an incomplete season skips straight to the persisted
     * checkpoint instead of re-walking every page a previous execution already confirmed.
     *
     * <p>This is what keeps a heavy player's season from perpetually restarting at offset zero on
     * every retry: only the mandatory first page and the pages beyond the checkpoint cost a Henrik
     * call.
     */
    @Test
    void shouldResumeFromThePersistedCheckpointInsteadOfOffsetZero() {
        doAnswer(invocation -> new SeasonSynchronizationStateService.SeasonWalkStart(
            ((Season) invocation.getArgument(1)).getId(),
            30
        )).when(stateService).startSeason(any(), any());
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
        return new HenrikMatchHistoryResponse(200, matches);
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
        List<HenrikMatchData> matches = new ArrayList<>(page(CURRENT_SEASON, currentSeasonMatches));
        matches.addAll(page(PREVIOUS_SEASON, PAGE_SIZE - currentSeasonMatches));
        return matches;
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
                Instant.parse("2026-07-20T18:00:00Z"),
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
    private MatchImportResult allImported(HenrikMatchHistoryResponse response) {
        int size = response.data().size();
        return new MatchImportResult(size, size, 0, 0, 0);
    }

    /**
     * Reports every match of the submitted page as already stored.
     */
    private MatchImportResult allKnown(HenrikMatchHistoryResponse response) {
        int size = response.data().size();
        return new MatchImportResult(size, 0, size, 0, 0);
    }

    /**
     * Reports every match of the submitted page as an ignored game mode.
     */
    private MatchImportResult allSkipped(HenrikMatchHistoryResponse response) {
        int size = response.data().size();
        return new MatchImportResult(size, 0, 0, 0, size);
    }

    /**
     * Collects the seasons of every match handed to the import service.
     */
    private List<String> importedSeasonIds() {
        ArgumentCaptor<HenrikMatchHistoryResponse> captor =
            ArgumentCaptor.forClass(HenrikMatchHistoryResponse.class);
        verify(matchImportService, atLeastOnce())
            .importMatchesWithSummary(any(), captor.capture());

        return captor.getAllValues().stream()
            .flatMap(response -> response.data().stream())
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
