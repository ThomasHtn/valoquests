package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.henrik.client.HenrikAccountClient;
import io.github.thomashtn.valoquests.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valoquests.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies season-scoped synchronization end to end against PostgreSQL.
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}. The per-season completion flag is
 * only trustworthy because each step commits on its own: a test transaction wrapping the whole walk
 * would hide exactly the property being verified, and would make the interrupted-run case
 * unobservable.
 *
 * <p>Only the Henrik clients are mocked. Match import, season resolution, completion tracking and
 * synchronization persistence run against the real migrated schema.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
class SeasonRolloverSynchronizationIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Number of matches Henrik returns for a full page.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Stable PUUID of the tracked test player.
     */
    private static final String PUUID = "season-rollover-player";

    /**
     * Henrik identifier of the season being played.
     */
    private static final String SEASON_A = "season-rollover-a";

    /**
     * Henrik identifier of the season Riot releases during the test.
     */
    private static final String SEASON_B = "season-rollover-b";

    /**
     * Henrik identifier of the season preceding every walk.
     */
    private static final String OLDER_SEASON = "season-rollover-older";

    /**
     * Henrik identifier of the season sitting below the walked scope.
     */
    private static final String OUT_OF_SCOPE_SEASON = "season-rollover-oldest";

    @Autowired
    private SynchronizationCommandService synchronizationCommandService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private HenrikAccountClient accountClient;

    @MockitoBean
    private HenrikMmrClient mmrClient;

    @MockitoBean
    private HenrikMatchClient matchClient;

    /**
     * Tracked player under test.
     */
    private Player player;

    /**
     * Creates an isolated player over an empty match history.
     */
    @BeforeEach
    void setUp() {
        cleanDerivedData();

        jdbcTemplate.update(
            "UPDATE player SET status = ? WHERE riot_puuid <> ?",
            PlayerStatus.INACTIVE.name(),
            PUUID
        );

        Player tracked = new Player();
        tracked.setRiotPuuid(PUUID);
        tracked.setGameName("Rollover");
        tracked.setTagLine("EUW");
        tracked.setDisplayName("Rollover");
        tracked.setStatus(PlayerStatus.ACTIVE);
        tracked.setCompetitiveTier(CompetitiveTier.UNRANKED);
        player = playerRepository.save(tracked);

        when(mmrClient.getCurrentMmr(PUUID)).thenReturn(
            new HenrikMmrResponse(200, new HenrikMmrResponse.HenrikMmrData(
                new HenrikMmrResponse.HenrikCurrentMmr(
                    new HenrikMmrResponse.HenrikTier(22, "Diamond 2"), 73, 1_873
                )
            ))
        );
    }

    /**
     * Removes the data this test committed, since no transaction rolls it back.
     */
    @AfterEach
    void tearDown() {
        cleanDerivedData();
        jdbcTemplate.update("DELETE FROM player WHERE riot_puuid = ?", PUUID);
        jdbcTemplate.update("DELETE FROM player WHERE riot_puuid = ?", "season-rollover-player-2");
        jdbcTemplate.update(
            "UPDATE player SET status = ?",
            PlayerStatus.ACTIVE.name()
        );
    }

    /**
     * Verifies the first walk of the two seasons in scope, then that a second run stops immediately.
     *
     * <p>The season below them was never targeted and must be left without a state row, otherwise
     * the next run would walk the player's whole history one season at a time.
     *
     * <p>Also pins the mode filter: an ignored queue never reaches the match tables, while a queue
     * the application cannot classify is stored with its raw slug so nothing is lost.
     */
    @Test
    void shouldWalkTheCurrentAndPreviousSeasonsThenStopAtKnownHistory() {
        givenHistory(
            fullPage(SEASON_A, "competitive"),
            mixedPage(),
            boundaryPage(SEASON_A, OLDER_SEASON, 4),
            boundaryPage(OLDER_SEASON, OUT_OF_SCOPE_SEASON, 5)
        );

        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(importedMatchCount()).isEqualTo(10 + 8 + 4 + 6 + 5);
        assertThat(isSeasonComplete(SEASON_A)).isTrue();
        assertThat(isSeasonComplete(OLDER_SEASON)).isTrue();
        assertThat(seasonStateCount(OUT_OF_SCOPE_SEASON)).isZero();

        assertThat(importedGameModes())
            .doesNotContain(GameMode.SWIFTPLAY, GameMode.ESCALATION);
        assertThat(importedGameModes()).contains(GameMode.OTHER);
        assertThat(rawQueueIds()).contains("valorant_royale");

        verify(matchClient, times(4)).getMatches(eq(PUUID), anyInt(), anyInt());

        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(importedMatchCount()).isEqualTo(33);
        verify(matchClient, times(5)).getMatches(eq(PUUID), anyInt(), anyInt());
    }

    /**
     * Verifies that a new season is walked on its own and leaves the previous one alone.
     */
    @Test
    void shouldWalkANewSeasonWithoutTouchingTheCompletedOne() {
        givenHistory(
            fullPage(SEASON_A, "competitive"),
            boundaryPage(SEASON_A, OLDER_SEASON, 3)
        );
        synchronizationCommandService.synchronizePlayer(player.getId());

        Instant seasonACompletedAt = seasonCompletedAt(SEASON_A);
        assertThat(seasonACompletedAt).isNotNull();

        givenHistory(
            fullPage(SEASON_B, "competitive"),
            boundaryPage(SEASON_B, SEASON_A, 5)
        );
        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(isSeasonComplete(SEASON_B)).isTrue();
        assertThat(isSeasonComplete(SEASON_A)).isTrue();
        assertThat(seasonCompletedAt(SEASON_A)).isEqualTo(seasonACompletedAt);
    }

    /**
     * Verifies that a season left unfinished is walked again in full.
     *
     * <p>The guarantee behind the whole design: an interrupted run must never leave a permanent
     * hole, so the next run may not stop at the first already-stored match.
     */
    @Test
    void shouldRewalkASeasonLeftUnfinished() {
        givenHistory(
            fullPage(SEASON_A, "competitive"),
            boundaryPage(SEASON_A, OLDER_SEASON, 3)
        );
        synchronizationCommandService.synchronizePlayer(player.getId());

        long importedMatches = importedMatchCount();
        markSeasonIncomplete(SEASON_A);

        synchronizationCommandService.synchronizePlayer(player.getId());

        // Both pages walked again rather than stopping on page one, and no duplicate created.
        verify(matchClient, times(5)).getMatches(eq(PUUID), anyInt(), anyInt());
        assertThat(importedMatchCount()).isEqualTo(importedMatches);
        assertThat(isSeasonComplete(SEASON_A)).isTrue();
    }

    /**
     * Verifies that a failure mid-walk commits the retrieved pages and leaves the season unfinished.
     *
     * <p>This is the regression guard against wrapping the walk in a transaction: doing so would
     * roll back the imported matches together with the state row, and the run would look as if it
     * had never happened.
     */
    @Test
    void shouldKeepTheSeasonUnfinishedWhenTheWalkFails() {
        when(matchClient.getMatches(eq(PUUID), eq(0), anyInt()))
            .thenReturn(response(fullPage(SEASON_A, "competitive")));
        when(matchClient.getMatches(eq(PUUID), eq(PAGE_SIZE), anyInt()))
            .thenThrow(new IllegalStateException("Henrik unavailable"));

        assertThatThrownBy(
            () -> synchronizationCommandService.synchronizePlayer(player.getId())
        ).isInstanceOf(IllegalStateException.class);

        assertThat(importedMatchCount()).isEqualTo(10);
        assertThat(isSeasonComplete(SEASON_A)).isFalse();
        assertThat(seasonStateCount(SEASON_A)).isEqualTo(1);
    }

    /**
     * Regression test for a heavy Deathmatch account whose season-history walk spans dozens of
     * pages, reproducing what live Henrik data showed for the player this pipeline was rebuilt for:
     * hundreds of Deathmatch matches within a single act, discovered through direct API pagination
     * during the investigation of missing match history.
     *
     * <p>Verifies both halves of the fix: an interruption mid-walk leaves every already-imported page
     * committed and the season correctly unfinished, and the next run resumes from the persisted
     * checkpoint instead of re-fetching the pages a previous run already confirmed. Without the
     * checkpoint, resuming a heavy account like this would cost the full rate-limited pagination again
     * on every retry, making it statistically less likely to ever finish.
     */
    @Test
    void shouldResumeAHeavyDeathmatchAccountAfterAnInterruptionWithoutRewalkingConfirmedPages() {
        List<List<HenrikMatchData>> pages = new ArrayList<>();
        IntStream.range(0, 20).forEach(index -> pages.add(fullPage(SEASON_A, "deathmatch")));
        pages.add(boundaryPage(SEASON_A, OLDER_SEASON, 5));

        when(matchClient.getMatches(eq(PUUID), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                int offset = invocation.getArgument(1);
                if (offset == 150) {
                    throw new IllegalStateException("Henrik unavailable");
                }
                int index = offset / PAGE_SIZE;
                return response(index < pages.size() ? pages.get(index) : List.of());
            });

        assertThatThrownBy(
            () -> synchronizationCommandService.synchronizePlayer(player.getId())
        ).isInstanceOf(IllegalStateException.class);

        // Pages 0-14 (offsets 0-140) committed before the failure at offset 150.
        assertThat(importedMatchCount()).isEqualTo(150);
        assertThat(isSeasonComplete(SEASON_A)).isFalse();
        assertThat(seasonNextStartOffset(SEASON_A)).isEqualTo(150);
        verify(matchClient, times(1)).getMatches(PUUID, 50, PAGE_SIZE);
        verify(matchClient, times(1)).getMatches(PUUID, 150, PAGE_SIZE);

        // Henrik recovers: the same offset now succeeds.
        doAnswer(invocation -> {
            int offset = invocation.getArgument(1);
            int index = offset / PAGE_SIZE;
            return response(index < pages.size() ? pages.get(index) : List.of());
        }).when(matchClient).getMatches(eq(PUUID), anyInt(), anyInt());

        synchronizationCommandService.synchronizePlayer(player.getId());

        // 200 for season A's pages, then the boundary page split between A and the previous season.
        assertThat(importedMatchCount()).isEqualTo(200 + 5 + 5);
        assertThat(isSeasonComplete(SEASON_A)).isTrue();
        assertThat(isSeasonComplete(OLDER_SEASON)).isTrue();

        // The confirmed prefix is never re-fetched: still exactly the one call from before the retry.
        verify(matchClient, times(1)).getMatches(PUUID, 50, PAGE_SIZE);
        verify(matchClient, times(1)).getMatches(PUUID, 100, PAGE_SIZE);
        // Offset 150 is fetched once more, past the checkpoint, this time successfully.
        verify(matchClient, times(2)).getMatches(PUUID, 150, PAGE_SIZE);
        verify(matchClient, times(1)).getMatches(PUUID, 190, PAGE_SIZE);
        verify(matchClient, times(1)).getMatches(PUUID, 200, PAGE_SIZE);
    }

    /**
     * Verifies that a match Henrik returns again on the following page, because a new match pushed
     * the whole window forward between the two requests, is imported exactly once.
     *
     * <p>Offset-based pagination is not stable under concurrent writes: two matches played while the
     * walk was already in progress shift every later page by two positions, so the newest two entries
     * of the next page are ones the previous page already delivered.
     */
    @Test
    void shouldDedupAMatchReturnedAgainOnAnOverlappingPage() {
        List<HenrikMatchData> firstPage = fullPage(SEASON_A, "deathmatch");
        HenrikMatchData repeatedFromFirstPage1 = firstPage.get(8);
        HenrikMatchData repeatedFromFirstPage2 = firstPage.get(9);

        List<HenrikMatchData> overlappingSecondPage = new ArrayList<>();
        overlappingSecondPage.add(repeatedFromFirstPage1);
        overlappingSecondPage.add(repeatedFromFirstPage2);
        IntStream.range(0, PAGE_SIZE - 2)
            .forEach(index -> overlappingSecondPage.add(match(SEASON_A, "deathmatch")));

        givenHistory(firstPage, overlappingSecondPage, boundaryPage(SEASON_A, OLDER_SEASON, 4));

        synchronizationCommandService.synchronizePlayer(player.getId());

        // 10 unique on the first page, 8 new on the overlapping one, 4 + 6 on the boundary page.
        assertThat(importedMatchCount()).isEqualTo(10 + 8 + 4 + 6);
        assertThat(isSeasonComplete(SEASON_A)).isTrue();

        long distinctMatches = valorantMatchRepository.findAll().stream()
            .filter(match -> match.getExternalMatchId().startsWith("rollover-"))
            .map(ValorantMatch::getExternalMatchId)
            .distinct()
            .count();
        assertThat(distinctMatches).isEqualTo(28);
    }

    /**
     * Verifies that two tracked players who both took part in the same match end up sharing one
     * {@link ValorantMatch} row when synchronized one after the other.
     *
     * <p>Complements {@code MatchImportConcurrencyIntegrationTest}, which covers the same guarantee
     * under genuine concurrent execution: this is the far more common sequential case, where the
     * second player's synchronization simply finds the row the first one already created.
     */
    @Test
    void shouldShareOneMatchRowWhenTwoPlayersAreSynchronizedSequentially() {
        Player secondPlayer = new Player();
        secondPlayer.setRiotPuuid("season-rollover-player-2");
        secondPlayer.setGameName("RolloverTwo");
        secondPlayer.setTagLine("EUW");
        secondPlayer.setDisplayName("RolloverTwo");
        secondPlayer.setStatus(PlayerStatus.ACTIVE);
        secondPlayer.setCompetitiveTier(CompetitiveTier.UNRANKED);
        secondPlayer = playerRepository.save(secondPlayer);

        when(mmrClient.getCurrentMmr("season-rollover-player-2")).thenReturn(
            new HenrikMmrResponse(200, new HenrikMmrResponse.HenrikMmrData(
                new HenrikMmrResponse.HenrikCurrentMmr(
                    new HenrikMmrResponse.HenrikTier(22, "Diamond 2"), 73, 1_873
                )
            ))
        );

        HenrikMatchData sharedMatch = sharedMatch(SEASON_A, "season-rollover-player-2");
        when(matchClient.getMatches(eq(PUUID), anyInt(), anyInt()))
            .thenAnswer(invocation -> (int) invocation.getArgument(1) == 0
                ? response(List.of(sharedMatch))
                : response(List.of()));
        when(matchClient.getMatches(eq("season-rollover-player-2"), anyInt(), anyInt()))
            .thenAnswer(invocation -> (int) invocation.getArgument(1) == 0
                ? response(List.of(sharedMatch))
                : response(List.of()));

        synchronizationCommandService.synchronizePlayer(player.getId());
        synchronizationCommandService.synchronizePlayer(secondPlayer.getId());

        List<ValorantMatch> storedMatches = valorantMatchRepository.findAll().stream()
            .filter(m -> m.getExternalMatchId().equals(sharedMatch.metadata().matchId()))
            .toList();
        assertThat(storedMatches).hasSize(1);

        long associationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM player_match WHERE match_id = ?",
            Long.class,
            storedMatches.get(0).getId()
        );
        assertThat(associationCount).isEqualTo(2);
    }

    /**
     * Verifies that a player with no history succeeds without recording a season.
     */
    @Test
    void shouldSucceedForAPlayerWithoutAnyMatch() {
        givenHistory(List.of());

        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(importedMatchCount()).isZero();
        assertThat(playerRepository.findById(player.getId()).orElseThrow()
            .getLastSuccessfulSynchronizationAt()).isNotNull();
    }

    /**
     * Scripts the pages Henrik returns, keyed by the requested offset.
     */
    @SafeVarargs
    private void givenHistory(List<HenrikMatchData>... pages) {
        when(matchClient.getMatches(eq(PUUID), anyInt(), anyInt()))
            .thenAnswer(invocation -> {
                int index = (int) invocation.getArgument(1) / PAGE_SIZE;
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
     * Creates a full page of one season and one queue.
     */
    private List<HenrikMatchData> fullPage(String seasonId, String queueId) {
        return IntStream.range(0, PAGE_SIZE)
            .mapToObj(index -> match(seasonId, queueId))
            .toList();
    }

    /**
     * Creates a full page mixing imported, ignored and unclassifiable queues.
     */
    private List<HenrikMatchData> mixedPage() {
        List<HenrikMatchData> matches = new ArrayList<>();
        matches.add(match(SEASON_A, "swiftplay"));
        matches.add(match(SEASON_A, "ggteam"));
        matches.add(match(SEASON_A, "valorant_royale"));
        IntStream.range(0, PAGE_SIZE - 3)
            .forEach(index -> matches.add(match(SEASON_A, "deathmatch")));
        return matches;
    }

    /**
     * Creates the full page on which the walk crosses into an older season.
     */
    private List<HenrikMatchData> boundaryPage(
        String currentSeason,
        String olderSeason,
        int currentSeasonMatches
    ) {
        List<HenrikMatchData> matches = new ArrayList<>();
        IntStream.range(0, currentSeasonMatches)
            .forEach(index -> matches.add(match(currentSeason, "competitive")));
        IntStream.range(0, PAGE_SIZE - currentSeasonMatches)
            .forEach(index -> matches.add(match(olderSeason, "competitive")));
        return matches;
    }

    /**
     * Creates a completed Henrik match two tracked players both took part in.
     */
    private HenrikMatchData sharedMatch(String seasonId, String secondPuuid) {
        return new HenrikMatchData(
            new HenrikMatchMetadata(
                "rollover-" + java.util.UUID.randomUUID(),
                new HenrikMatchMetadata.HenrikMap("map-1", "Ascent"),
                1_800_000L,
                Instant.parse("2026-07-21T18:00:00Z"),
                true,
                new HenrikMatchMetadata.HenrikQueue("competitive", null, null),
                new HenrikMatchMetadata.HenrikSeason(seasonId, seasonId)
            ),
            List.of(
                new HenrikMatchPlayer(
                    PUUID,
                    "Rollover",
                    "EUW",
                    "Red",
                    new HenrikMatchPlayer.HenrikAgent("agent-1", "Jett"),
                    new HenrikMatchPlayer.HenrikPlayerStats(
                        4000, 20, 12, 3, 10, 25, 2,
                        new HenrikMatchPlayer.HenrikDamage(3200, 2800)
                    ),
                    new HenrikMatchPlayer.HenrikTier(22, "Diamond 2")
                ),
                new HenrikMatchPlayer(
                    secondPuuid,
                    "RolloverTwo",
                    "EUW",
                    "Blue",
                    new HenrikMatchPlayer.HenrikAgent("agent-2", "Sova"),
                    new HenrikMatchPlayer.HenrikPlayerStats(
                        3500, 18, 14, 5, 9, 20, 3,
                        new HenrikMatchPlayer.HenrikDamage(2900, 2600)
                    ),
                    new HenrikMatchPlayer.HenrikTier(20, "Platinum 3")
                )
            ),
            List.of(
                new HenrikMatchTeam("Red", true, new HenrikMatchTeam.HenrikRounds(13, 7)),
                new HenrikMatchTeam("Blue", false, new HenrikMatchTeam.HenrikRounds(7, 13))
            )
        );
    }

    /**
     * Creates a completed Henrik match the tracked player took part in.
     */
    private HenrikMatchData match(String seasonId, String queueId) {
        return new HenrikMatchData(
            new HenrikMatchMetadata(
                "rollover-" + java.util.UUID.randomUUID(),
                new HenrikMatchMetadata.HenrikMap("map-1", "Ascent"),
                1_800_000L,
                Instant.parse("2026-07-21T18:00:00Z"),
                true,
                new HenrikMatchMetadata.HenrikQueue(queueId, null, null),
                new HenrikMatchMetadata.HenrikSeason(seasonId, seasonId)
            ),
            List.of(new HenrikMatchPlayer(
                PUUID,
                "Rollover",
                "EUW",
                "Red",
                new HenrikMatchPlayer.HenrikAgent("agent-1", "Jett"),
                new HenrikMatchPlayer.HenrikPlayerStats(
                    4000, 20, 12, 3, 10, 25, 2,
                    new HenrikMatchPlayer.HenrikDamage(3200, 2800)
                ),
                new HenrikMatchPlayer.HenrikTier(22, "Diamond 2")
            )),
            List.of(
                new HenrikMatchTeam("Red", true, new HenrikMatchTeam.HenrikRounds(13, 7)),
                new HenrikMatchTeam("Blue", false, new HenrikMatchTeam.HenrikRounds(7, 13))
            )
        );
    }

    /**
     * Counts the matches stored for the tracked player.
     */
    private long importedMatchCount() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM player_match WHERE player_id = ?",
            Long.class,
            player.getId()
        );
    }

    /**
     * Lists the game modes stored for the tracked player.
     */
    private List<GameMode> importedGameModes() {
        return valorantMatchRepository.findAll().stream()
            .filter(match -> match.getExternalMatchId().startsWith("rollover-"))
            .map(ValorantMatch::getGameMode)
            .distinct()
            .toList();
    }

    /**
     * Lists the raw Henrik queue slugs preserved on stored matches.
     */
    private List<String> rawQueueIds() {
        return valorantMatchRepository.findAll().stream()
            .filter(match -> match.getExternalMatchId().startsWith("rollover-"))
            .map(ValorantMatch::getQueueId)
            .distinct()
            .toList();
    }

    /**
     * Reports whether a season was walked back to its oldest match.
     */
    private boolean isSeasonComplete(String seasonExternalId) {
        Boolean complete = jdbcTemplate.query(
            """
                SELECT pss.complete
                FROM player_season_synchronization pss
                JOIN season s ON s.id = pss.season_id
                WHERE pss.player_id = ? AND s.external_id = ?
                """,
            resultSet -> resultSet.next() ? resultSet.getBoolean(1) : null,
            player.getId(),
            seasonExternalId
        );
        return Boolean.TRUE.equals(complete);
    }

    /**
     * Reads the instant a season was declared complete.
     */
    private Instant seasonCompletedAt(String seasonExternalId) {
        return jdbcTemplate.query(
            """
                SELECT pss.completed_at
                FROM player_season_synchronization pss
                JOIN season s ON s.id = pss.season_id
                WHERE pss.player_id = ? AND s.external_id = ?
                """,
            resultSet -> resultSet.next() && resultSet.getTimestamp(1) != null
                ? resultSet.getTimestamp(1).toInstant()
                : null,
            player.getId(),
            seasonExternalId
        );
    }

    /**
     * Reads the persisted pagination checkpoint of a season.
     */
    private int seasonNextStartOffset(String seasonExternalId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT pss.next_start_offset
                FROM player_season_synchronization pss
                JOIN season s ON s.id = pss.season_id
                WHERE pss.player_id = ? AND s.external_id = ?
                """,
            Integer.class,
            player.getId(),
            seasonExternalId
        );
    }

    /**
     * Counts the state rows recorded for a season.
     */
    private int seasonStateCount(String seasonExternalId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM player_season_synchronization pss
                JOIN season s ON s.id = pss.season_id
                WHERE pss.player_id = ? AND s.external_id = ?
                """,
            Integer.class,
            player.getId(),
            seasonExternalId
        );
    }

    /**
     * Simulates a season whose walk was interrupted.
     */
    private void markSeasonIncomplete(String seasonExternalId) {
        jdbcTemplate.update(
            """
                UPDATE player_season_synchronization
                SET complete = false, completed_at = NULL
                WHERE player_id = ?
                  AND season_id = (SELECT id FROM season WHERE external_id = ?)
                """,
            player.getId(),
            seasonExternalId
        );
    }

    /**
     * Removes every row this test may have committed.
     *
     * <p>Includes the challenge and ranking rows: synchronization recalculates the current week
     * whenever it imports a match, so a walk leaves progress behind even though this test never
     * asserts on it.
     */
    private void cleanDerivedData() {
        jdbcTemplate.update("DELETE FROM player_challenge_progress");
        jdbcTemplate.update("DELETE FROM weekly_player_score");
        jdbcTemplate.update("DELETE FROM weekly_challenge");
        jdbcTemplate.update("DELETE FROM player_season_synchronization");
        jdbcTemplate.update("DELETE FROM synchronization_player_result");
        jdbcTemplate.update("DELETE FROM synchronization");
        jdbcTemplate.update("DELETE FROM player_match");
        jdbcTemplate.update("DELETE FROM valorant_match");
        jdbcTemplate.update("DELETE FROM season");
    }
}
