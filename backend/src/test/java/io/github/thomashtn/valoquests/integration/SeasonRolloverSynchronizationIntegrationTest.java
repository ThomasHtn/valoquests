package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies season-scoped synchronization end to end against PostgreSQL.
 *
 * <p>Not {@code @Transactional}: each step must commit on its own for the completion flag to be observable.
 * Only the Henrik clients are mocked.
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
    private static final int PAGE_SIZE = HenrikMatchClient.MAX_PAGE_SIZE;

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
            new HenrikMmrResponse(new HenrikMmrResponse.HenrikMmrData(
                new HenrikMmrResponse.HenrikCurrentMmr(
                    new HenrikMmrResponse.HenrikTier("Diamond 2"), 73
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
     * <p>The older season gets no state row; ignored queues are skipped and unclassified ones keep their raw slug.
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
            .doesNotContain(GameMode.NEW_MAP, GameMode.CUSTOM);
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
     * <p>An interrupted run must never leave a permanent hole, so the next run cannot stop at a stored match.
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
     * <p>Guards against wrapping the walk in a transaction, which would roll back the matches with the state row.
     */
    @Test
    void shouldKeepTheSeasonUnfinishedWhenTheWalkFails() {
        when(matchClient.getMatches(eq(PUUID), eq(0), anyInt()))
            .thenReturn(response(fullPage(SEASON_A, "competitive")));
        when(matchClient.getMatches(eq(PUUID), eq(PAGE_SIZE), anyInt()))
            .thenThrow(new IllegalStateException("Henrik unavailable"));

        // The failure is recorded on the execution, not thrown.
        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(importedMatchCount()).isEqualTo(10);
        assertThat(isSeasonComplete(SEASON_A)).isFalse();
        assertThat(seasonStateCount(SEASON_A)).isEqualTo(1);
    }

    /**
     * Verifies that a heavy Deathmatch history spanning dozens of pages survives an interruption and resumes.
     *
     * <p>Imported pages stay committed and the next run resumes from the checkpoint instead of refetching them.
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

        // The failure is recorded on the execution, not thrown.
        synchronizationCommandService.synchronizePlayer(player.getId());

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
     * Verifies that a first page of custom games, which are never stored, keeps the checkpoint usable
     * when it reaches back to the season's newest stored match.
     */
    @Test
    @DisplayName("Resumes from the checkpoint when the first page holds only ignored modes reaching stored history")
    void shouldResumeFromTheCheckpointWhenTheFirstPageHoldsOnlyIgnoredModes() {
        List<HenrikMatchData> secondPage = fullPage(SEASON_A, "competitive");
        when(matchClient.getMatches(eq(PUUID), anyInt(), anyInt()))
            .thenAnswer(invocation -> switch ((int) invocation.getArgument(1)) {
                case 0 -> response(fullPage(SEASON_A, "competitive"));
                case 10 -> response(secondPage);
                default -> throw new IllegalStateException("Henrik unavailable");
            });
        synchronizationCommandService.synchronizePlayer(player.getId());
        assertThat(seasonNextStartOffset(SEASON_A)).isEqualTo(20);

        doAnswer(invocation -> switch ((int) invocation.getArgument(1)) {
            case 0 -> response(fullPage(SEASON_A, "custom"));
            case 10 -> response(secondPage);
            default -> response(boundaryPage(SEASON_A, OLDER_SEASON, 5));
        }).when(matchClient).getMatches(eq(PUUID), anyInt(), anyInt());
        synchronizationCommandService.synchronizePlayer(player.getId());

        verify(matchClient, times(1)).getMatches(PUUID, 10, PAGE_SIZE);
        assertThat(isSeasonComplete(SEASON_A)).isTrue();
    }

    /**
     * Verifies that a match Henrik repeats on the next page, after the window shifted, is imported once.
     *
     * <p>Offset pagination shifts when matches are played mid-walk, so a page can repeat the previous page's tail.
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
     * Verifies that two players sharing a match, synchronized one after the other, share one
     * {@link ValorantMatch} row.
     *
     * <p>The sequential counterpart of {@code MatchImportConcurrencyIntegrationTest}.
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
            new HenrikMmrResponse(new HenrikMmrResponse.HenrikMmrData(
                new HenrikMmrResponse.HenrikCurrentMmr(
                    new HenrikMmrResponse.HenrikTier("Diamond 2"), 73
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
        return new HenrikMatchHistoryResponse(matches);
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
        matches.add(match(SEASON_A, "newmap"));
        matches.add(match(SEASON_A, "custom"));
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
                    "Red",
                    new HenrikMatchPlayer.HenrikAgent("agent-1", "Jett"),
                    new HenrikMatchPlayer.HenrikPlayerStats(
                        4000, 20, 12, 3, 10, 25, 2,
                        new HenrikMatchPlayer.HenrikDamage(3200)
                    ),
                    new HenrikMatchPlayer.HenrikTier("Diamond 2")
                ),
                new HenrikMatchPlayer(
                    secondPuuid,
                    "Blue",
                    new HenrikMatchPlayer.HenrikAgent("agent-2", "Sova"),
                    new HenrikMatchPlayer.HenrikPlayerStats(
                        3500, 18, 14, 5, 9, 20, 3,
                        new HenrikMatchPlayer.HenrikDamage(2900)
                    ),
                    new HenrikMatchPlayer.HenrikTier("Platinum 3")
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
                "Red",
                new HenrikMatchPlayer.HenrikAgent("agent-1", "Jett"),
                new HenrikMatchPlayer.HenrikPlayerStats(
                    4000, 20, 12, 3, 10, 25, 2,
                    new HenrikMatchPlayer.HenrikDamage(3200)
                ),
                new HenrikMatchPlayer.HenrikTier("Diamond 2")
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
     * <p>Includes challenge and ranking rows, which synchronization recalculates whenever it imports a match.
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
