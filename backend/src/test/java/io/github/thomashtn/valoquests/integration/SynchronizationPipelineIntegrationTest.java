package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.henrik.client.HenrikAccountClient;
import io.github.thomashtn.valoquests.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valoquests.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valoquests.henrik.model.HenrikAccount;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.roster.dto.PlayerUpdateRequest;
import io.github.thomashtn.valoquests.roster.service.PlayerAdminService;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationQueryService;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the standard synchronization pipeline against PostgreSQL.
 *
 * <p>Only Henrik clients are mocked. Not {@code @Transactional} because the synchronization refuses to run in a
 * transaction, so {@link #tearDown()} removes the committed fixtures.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Import(
    SynchronizationPipelineIntegrationTest.FixedClockConfiguration.class
)
class SynchronizationPipelineIntegrationTest
    extends PostgreSqlIntegrationTest {

    /**
     * Monday identifying the week containing the imported matches.
     */
    private static final LocalDate WEEK_START =
        LocalDate.of(2026, 7, 20);

    /**
     * Deterministic completion time used by synchronization and calculations.
     */
    private static final Instant SYNCHRONIZATION_TIME =
        Instant.parse("2026-07-22T12:00:00Z");

    /**
     * Stable PUUID of the tracked integration-test player.
     */
    private static final String PLAYER_PUUID =
        "synchronization-pipeline-player";

    /**
     * Standard synchronization command service under test.
     */
    @Autowired
    private SynchronizationCommandService synchronizationCommandService;

    /**
     * Production challenge service that also recalculates the ranking.
     */
    @Autowired
    private ChallengeRecalculationService challengeRecalculationService;

    /**
     * Repository used to prepare and inspect the tracked player.
     */
    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Repository used to inspect imported shared match metadata.
     */
    @Autowired
    private ValorantMatchRepository valorantMatchRepository;

    /**
     * Repository used to inspect imported player statistics.
     */
    @Autowired
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Challenge catalogue repository used to create deterministic rules.
     */
    @Autowired
    private ChallengeRepository challengeRepository;

    /**
     * Weekly challenge repository used to create the deterministic pack.
     */
    @Autowired
    private ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Progress repository used to verify calculator output.
     */
    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Ranking repository used to verify score generation and idempotence.
     */
    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Synchronization repository used to verify global execution records.
     */
    @Autowired
    private SynchronizationRepository synchronizationRepository;

    /**
     * Repository used to verify the per-player execution records.
     */
    @Autowired
    private SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Service reading back the execution a synchronization recorded.
     */
    @Autowired
    private SynchronizationQueryService synchronizationQueryService;

    /**
     * Persistence context used to force fresh PostgreSQL reads.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Mocked because the test player already owns a known PUUID.
     */
    @MockitoBean
    private HenrikAccountClient accountClient;

    /**
     * Mocked remote client providing the current competitive rank.
     */
    @MockitoBean
    private HenrikMmrClient mmrClient;

    /**
     * Mocked remote client providing deterministic match-history payloads.
     */
    @MockitoBean
    private HenrikMatchClient matchClient;

    /**
     * Used to clean up committed fixture data, since nothing here rolls back.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Administration service editing a player while a synchronization runs.
     */
    @Autowired
    private PlayerAdminService playerAdminService;

    /**
     * Removes every row this test committed and restores seeded players.
     */
    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM player_challenge_progress");
        jdbcTemplate.update("DELETE FROM weekly_player_score");
        jdbcTemplate.update("DELETE FROM weekly_challenge");
        jdbcTemplate.update("DELETE FROM challenge WHERE code LIKE 'PIPELINE_%'");
        jdbcTemplate.update("DELETE FROM player_season_synchronization");
        jdbcTemplate.update("DELETE FROM synchronization_player_result");
        jdbcTemplate.update("DELETE FROM synchronization");
        jdbcTemplate.update("DELETE FROM player_match");
        jdbcTemplate.update("DELETE FROM valorant_match WHERE external_match_id LIKE 'pipeline-match-%'");
        jdbcTemplate.update("DELETE FROM season WHERE external_id = 'pipeline-season'");
        jdbcTemplate.update("DELETE FROM player WHERE riot_puuid = ?", PLAYER_PUUID);
        jdbcTemplate.update("DELETE FROM player WHERE game_name LIKE 'Pipeline%'");
        jdbcTemplate.update("UPDATE player SET status = ?", PlayerStatus.ACTIVE.name());
    }

    /**
     * Verifies first import, production calculations and repeated idempotent
     * synchronization of the same Henrik history.
     */
    @Test
    void shouldSynchronizeImportCalculateAndRemainIdempotent() {
        deactivateSeededPlayers();

        Player player = createPlayer();
        createChallengePack();

        HenrikMatchHistoryResponse historyResponse =
            createHistoryResponse();

        when(mmrClient.getCurrentMmr(PLAYER_PUUID))
            .thenReturn(createMmrResponse());

        when(matchClient.getMatches(PLAYER_PUUID, 0, 10))
            .thenReturn(historyResponse);

        synchronizationCommandService.synchronizePlayer(player.getId());
        SynchronizationResponse firstSynchronization = synchronizationQueryService.findLatest();

        // Importing triggers the recalculation; a second call would shift the previous position.
        flushAndClear();

        assertFirstSynchronization(firstSynchronization, player);
        assertImportedMatches(player);
        assertCalculatedProgress(player);
        assertCalculatedRanking(player, null);

        List<Long> firstMatchIds = valorantMatchRepository.findAll()
            .stream()
            .filter(match -> match.getExternalMatchId()
                .startsWith("pipeline-match-"))
            .map(ValorantMatch::getId)
            .toList();

        List<Long> firstPlayerMatchIds = playerMatchRepository.findAll()
            .stream()
            .filter(playerMatch -> playerMatch.getPlayer().getId()
                .equals(player.getId()))
            .map(PlayerMatch::getId)
            .toList();

        List<Long> firstProgressIds = loadProgress(player)
            .stream()
            .map(PlayerChallengeProgress::getId)
            .toList();

        Long firstScoreId = loadScore(player).getId();

        synchronizationCommandService.synchronizePlayer(player.getId());
        SynchronizationResponse secondSynchronization = synchronizationQueryService.findLatest();

        // The second sync skips recalculation; this call stands in for the admin repair and proves idempotence.
        challengeRecalculationService
            .drawAndRecalculateCurrentWeek();

        flushAndClear();

        assertSecondSynchronization(secondSynchronization, player);

        assertThat(valorantMatchRepository.findAll())
            .filteredOn(match -> match.getExternalMatchId()
                .startsWith("pipeline-match-"))
            .extracting(ValorantMatch::getId)
            .containsExactlyInAnyOrderElementsOf(firstMatchIds);

        assertThat(playerMatchRepository.findAll())
            .filteredOn(playerMatch -> playerMatch.getPlayer().getId()
                .equals(player.getId()))
            .extracting(PlayerMatch::getId)
            .containsExactlyInAnyOrderElementsOf(firstPlayerMatchIds);

        assertThat(loadProgress(player))
            .extracting(PlayerChallengeProgress::getId)
            .containsExactlyInAnyOrderElementsOf(firstProgressIds);

        WeeklyPlayerScore secondScore = loadScore(player);

        assertThat(secondScore.getId()).isEqualTo(firstScoreId);
        assertCalculatedRanking(player, 1);

        verify(accountClient, never())
            .getAccount(anyString(), anyString());

        // The quiet second pass keeps the stored rank instead of asking Henrik again.
        verify(mmrClient, org.mockito.Mockito.times(1))
            .getCurrentMmr(PLAYER_PUUID);

        verify(matchClient, org.mockito.Mockito.times(2))
            .getMatches(PLAYER_PUUID, 0, 10);
    }

    /**
     * Verifies that the final write of a synchronization does not undo an admin edit.
     *
     * <p>A full save of the copy loaded before the Henrik calls would restore the replaced name and portrait.
     */
    @Test
    @DisplayName("Keeps an admin edit made during the synchronization while storing the new rank")
    void shouldKeepAnAdminEditMadeDuringTheSynchronization() {
        Player player = createPlayer();
        when(mmrClient.getCurrentMmr(PLAYER_PUUID)).thenAnswer(invocation -> {
            playerAdminService.update(player.getId(), new PlayerUpdateRequest(
                "PipelinePlayer", "TEST", "Renamed meanwhile", "new-portrait"
            ));
            return createMmrResponse();
        });
        when(matchClient.getMatches(PLAYER_PUUID, 0, 10))
            .thenReturn(new HenrikMatchHistoryResponse(List.of()));

        synchronizationCommandService.synchronizePlayer(player.getId());

        Player stored = playerRepository.findById(player.getId()).orElseThrow();
        assertThat(stored.getDisplayName()).isEqualTo("Renamed meanwhile");
        assertThat(stored.getPortrait()).isEqualTo("new-portrait");
        assertThat(stored.getCompetitiveTier()).isEqualTo(CompetitiveTier.DIAMOND_2);
        assertThat(stored.getLastSuccessfulSynchronizationAt()).isEqualTo(SYNCHRONIZATION_TIME);
    }

    /**
     * Verifies that a resolved PUUID is stored without a full save of the player.
     */
    @Test
    @DisplayName("Stores the PUUID resolved for a player that had none")
    void shouldStoreTheResolvedPuuid() {
        Player player = createPlayerWithoutPuuid();
        when(accountClient.getAccount("PipelinePlayer", "TEST"))
            .thenReturn(new HenrikAccount(PLAYER_PUUID, "PipelinePlayer", "TEST"));
        when(mmrClient.getCurrentMmr(PLAYER_PUUID)).thenReturn(createMmrResponse());
        when(matchClient.getMatches(PLAYER_PUUID, 0, 10))
            .thenReturn(new HenrikMatchHistoryResponse(List.of()));

        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(playerRepository.findById(player.getId()).orElseThrow().getRiotPuuid())
            .isEqualTo(PLAYER_PUUID);
    }

    /**
     * Verifies that a PUUID resolved for a Riot identity the administrator replaced is dropped.
     *
     * <p>Stored anyway, it would import the matches of an account the player no longer names.
     */
    @Test
    @DisplayName("Drops a PUUID resolved for a Riot identity changed during the lookup")
    void shouldDropAPuuidResolvedForAnIdentityChangedMeanwhile() {
        Player player = createPlayerWithoutPuuid();
        when(accountClient.getAccount("PipelinePlayer", "TEST")).thenAnswer(invocation -> {
            playerAdminService.update(player.getId(), new PlayerUpdateRequest(
                "PipelineRenamed", "TEST", "PipelinePlayer#TEST", null
            ));
            return new HenrikAccount(PLAYER_PUUID, "PipelinePlayer", "TEST");
        });

        synchronizationCommandService.synchronizePlayer(player.getId());

        assertThat(synchronizationQueryService.findLatest().status()).isEqualTo(SynchronizationStatus.FAILED);
        Player stored = playerRepository.findById(player.getId()).orElseThrow();
        assertThat(stored.getGameName()).isEqualTo("PipelineRenamed");
        assertThat(stored.getRiotPuuid()).isNull();
    }

    /**
     * Verifies the persisted summary and updated player after the first run.
     */
    private void assertFirstSynchronization(
        SynchronizationResponse response,
        Player originalPlayer
    ) {
        assertSynchronizationResponse(response, 2);

        Player synchronizedPlayer = playerRepository.findById(
            originalPlayer.getId()
        ).orElseThrow();

        assertThat(synchronizedPlayer.getCompetitiveTier())
            .isEqualTo(CompetitiveTier.DIAMOND_2);
        assertThat(synchronizedPlayer.getRankRating())
            .isEqualTo(73);
        assertThat(
            synchronizedPlayer
                .getLastSuccessfulSynchronizationAt()
        ).isEqualTo(SYNCHRONIZATION_TIME);

        assertThat(synchronizationRepository.findById(response.id()))
            .isPresent()
            .get()
            .satisfies(synchronization -> {
                assertThat(synchronization.getStatus())
                    .isEqualTo(SynchronizationStatus.COMPLETED);
                assertThat(synchronization.getMatchesImported())
                    .isEqualTo(2);
            });

        assertThat(
            playerResultRepository
                .findAllBySynchronizationIdOrderByPlayerIdAsc(
                    response.id()
                )
        )
            .singleElement()
            .satisfies(result -> {
                assertThat(result.getPlayer().getId())
                    .isEqualTo(originalPlayer.getId());
                assertThat(result.getStatus())
                    .isEqualTo(SynchronizationStatus.COMPLETED);
                assertThat(result.getPagesFetched())
                    .isEqualTo(1);
                assertThat(result.getMatchesImported())
                    .isEqualTo(2);
            });
    }

    /**
     * Verifies that the second execution reports no new match association.
     */
    private void assertSecondSynchronization(
        SynchronizationResponse response,
        Player player
    ) {
        assertSynchronizationResponse(response, 0);

        assertThat(
            playerResultRepository
                .findAllBySynchronizationIdOrderByPlayerIdAsc(
                    response.id()
                )
        )
            .singleElement()
            .satisfies(result -> {
                assertThat(result.getPlayer().getId())
                    .isEqualTo(player.getId());
                assertThat(result.getPagesFetched())
                    .isEqualTo(1);
                assertThat(result.getMatchesImported())
                    .isZero();
            });
    }

    /**
     * Verifies common synchronization response fields.
     */
    private void assertSynchronizationResponse(
        SynchronizationResponse response,
        int expectedMatchesImported
    ) {
        assertThat(response.id()).isNotNull();
        assertThat(response.type())
            .isEqualTo(SynchronizationType.STANDARD);
        assertThat(response.trigger())
            .isEqualTo(SynchronizationTrigger.MANUAL);
        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(response.startedAt())
            .isEqualTo(SYNCHRONIZATION_TIME);
        assertThat(response.finishedAt())
            .isEqualTo(SYNCHRONIZATION_TIME);
        assertThat(response.lastSuccessfulSynchronizationAt())
            .isEqualTo(SYNCHRONIZATION_TIME);
        assertThat(response.playersProcessed()).isEqualTo(1);
        assertThat(response.failureCount()).isZero();
        assertThat(response.matchesImported())
            .isEqualTo(expectedMatchesImported);
        assertThat(response.errorMessage()).isNull();
    }

    /**
     * Verifies shared metadata and tracked-player statistics mapped from Henrik.
     */
    private void assertImportedMatches(Player player) {
        List<ValorantMatch> importedMatches =
            valorantMatchRepository.findAll()
                .stream()
                .filter(match -> match.getExternalMatchId()
                    .startsWith("pipeline-match-"))
                .toList();

        assertThat(importedMatches)
            .hasSize(2)
            .allSatisfy(match -> {
                assertThat(match.getGameMode())
                    .isEqualTo(GameMode.COMPETITIVE);
                assertThat(match.getMapName())
                    .isEqualTo("Ascent");
                assertThat(match.getDurationSeconds())
                    .isEqualTo(2_400);
            });

        List<PlayerMatch> playerMatches =
            playerMatchRepository
                .findAllByPlayerIdOrderByMatchStartedAtDesc(
                    player.getId()
                );

        assertThat(playerMatches)
            .hasSize(2)
            .extracting(PlayerMatch::getResult)
            .containsExactly(
                MatchResult.LOSS,
                MatchResult.WIN
            );

        assertThat(playerMatches)
            .extracting(PlayerMatch::getKills)
            .containsExactly(20, 30);

        assertThat(playerMatches)
            .extracting(PlayerMatch::getDamageDealt)
            .containsExactly(2_000, 3_000);
    }

    /**
     * Verifies progress generated by real calculators from imported matches.
     */
    private void assertCalculatedProgress(Player player) {
        Map<String, PlayerChallengeProgress> progressByCode =
            loadProgress(player).stream()
                .collect(
                    Collectors.toMap(
                        progress -> progress
                            .getSelection()
                            .getChallenge()
                            .getCode(),
                        Function.identity()
                    )
                );

        // The five weekly challenges, plus the daily one the recalculation drew for today.
        assertThat(progressByCode).hasSize(6);

        assertProgress(
            progressByCode.get("PIPELINE_KILLS"),
            "50.0000",
            true
        );
        assertProgress(
            progressByCode.get("PIPELINE_DAMAGE"),
            "5000.0000",
            true
        );
        assertProgress(
            progressByCode.get("PIPELINE_WINS"),
            "1.0000",
            true
        );
        assertProgress(
            progressByCode.get("PIPELINE_KD"),
            "2.0000",
            true
        );
        assertProgress(
            progressByCode.get("PIPELINE_PLAY_DAYS"),
            "2.0000",
            true
        );
    }

    /**
     * Verifies the single-player ranking produced from the imported matches and calculated progress.
     *
     * <p>The second day carries the 2 % streak bonus: 500 + 350 × 1.02 = 857 damage. The five weekly
     * challenges pay 29 points at the 2 000 floor, plus the day's challenge if validated.
     */
    private void assertCalculatedRanking(
        Player player,
        Integer previousPosition
    ) {
        WeeklyPlayerScore score = loadScore(player);
        int dailyPoints = dailyPoints(player);

        assertThat(score.getWeekStart())
            .isEqualTo(WEEK_START);
        assertThat(score.getGuardianDamage())
            .isEqualTo(857);
        assertThat(score.getMatchCount())
            .isEqualTo(2);
        assertThat(score.getPlayedDays())
            .isEqualTo(2);
        assertThat(score.getChallengePoints())
            .isEqualTo(78 + dailyPoints);
        assertThat(score.getTotalPoints())
            .isEqualTo(857 + 78 + dailyPoints);
        assertThat(score.getCompletedChallenges())
            .isEqualTo(5);
        assertThat(score.getCompletedDailyChallenges())
            .isEqualTo(completedDailies(player));
        assertThat(score.getPosition())
            .isEqualTo(1);
        assertThat(score.getPreviousPosition())
            .isEqualTo(previousPosition);
        assertThat(score.getCalculatedAt())
            .isEqualTo(SYNCHRONIZATION_TIME);
        assertThat(score.getFinalizedAt()).isNull();
    }

    /**
     * Prices the daily challenges this player validated this week.
     *
     * <p>The daily is drawn from the pool, so its validation is read back. Outside a campaign it pays 24 points.
     *
     * @param player player whose dailies are priced
     * @return the points those dailies add
     */
    private int dailyPoints(Player player) {
        return completedDailies(player) * 6;
    }

    /**
     * Counts the daily challenges this player validated this week.
     *
     * @param player player whose dailies are counted
     * @return validated daily challenges
     */
    private int completedDailies(Player player) {
        return (int) progressRepository
            .findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(WEEK_START)
            .stream()
            .filter(progress -> progress.getPlayer().getId().equals(player.getId()))
            .filter(progress -> progress.getSelection().getCadence() == ChallengeCadence.DAILY)
            .filter(PlayerChallengeProgress::isCompleted)
            .count();
    }

    /**
     * Verifies one persisted challenge-progress row.
     */
    private void assertProgress(
        PlayerChallengeProgress progress,
        String expectedCurrentValue,
        boolean expectedCompleted
    ) {
        assertThat(progress).isNotNull();
        assertThat(progress.getCurrentValue())
            .isEqualByComparingTo(expectedCurrentValue);
        assertThat(progress.isCompleted())
            .isEqualTo(expectedCompleted);
        assertThat(progress.getCalculatedAt())
            .isEqualTo(SYNCHRONIZATION_TIME);

        if (expectedCompleted) {
            assertThat(progress.getCompletedAt())
                .isEqualTo(SYNCHRONIZATION_TIME);
        } else {
            assertThat(progress.getCompletedAt()).isNull();
        }
    }

    /**
     * Retrieves every progress row belonging to the test player and week.
     */
    private List<PlayerChallengeProgress> loadProgress(Player player) {
        return progressRepository
            .findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(
                WEEK_START
            )
            .stream()
            .filter(progress -> progress.getPlayer().getId()
                .equals(player.getId()))
            .toList();
    }

    /**
     * Retrieves the only score generated for the test player and week.
     */
    private WeeklyPlayerScore loadScore(Player player) {
        return scoreRepository
            .findAllByWeekStartOrderByPositionAscPlayerIdAsc(WEEK_START)
            .stream()
            .filter(score -> score.getPlayer().getId()
                .equals(player.getId()))
            .findFirst()
            .orElseThrow();
    }

    /**
     * Creates one active player whose PUUID is already resolved.
     */
    private Player createPlayer() {
        Player player = new Player();
        player.setRiotPuuid(PLAYER_PUUID);
        player.setGameName("PipelinePlayer");
        player.setTagLine("TEST");
        player.setDisplayName("PipelinePlayer#TEST");
        player.setPortrait("default");
        player.setStatus(PlayerStatus.ACTIVE);
        return playerRepository.save(player);
    }

    /**
     * Creates one active player whose PUUID is still to be resolved.
     */
    private Player createPlayerWithoutPuuid() {
        Player player = createPlayer();
        player.setRiotPuuid(null);
        return playerRepository.save(player);
    }

    /**
     * Marks migration-seeded players inactive for deterministic calculations.
     */
    private void deactivateSeededPlayers() {
        List<Player> players = playerRepository.findAll();
        players.forEach(
            player -> player.setStatus(PlayerStatus.INACTIVE)
        );
        playerRepository.saveAll(players);
    }

    /**
     * Creates five deterministic challenges covering every supported mode.
     */
    private void createChallengePack() {
        List<Challenge> challenges = challengeRepository.saveAll(
            List.of(
                createChallenge(
                    "PIPELINE_KILLS",
                    ChallengeTier.EASY,
                    ProgressMode.SUM,
                    "KILLS",
                    "50",
                    null
                ),
                createChallenge(
                    "PIPELINE_DAMAGE",
                    ChallengeTier.NORMAL,
                    ProgressMode.SUM,
                    "DAMAGE_DEALT",
                    "5000",
                    null
                ),
                createChallenge(
                    "PIPELINE_WINS",
                    ChallengeTier.MEDIUM,
                    ProgressMode.SUM,
                    "MATCHES_WON",
                    "1",
                    null
                ),
                createChallenge(
                    "PIPELINE_KD",
                    ChallengeTier.HARD,
                    ProgressMode.RATIO,
                    "KD",
                    "2",
                    "\"minimumMatches\": 2,"
                ),
                createChallenge(
                    "PIPELINE_PLAY_DAYS",
                    ChallengeTier.VERY_HARD,
                    ProgressMode.DISTINCT_COUNT,
                    "PLAY_DAY",
                    "2",
                    "\"groupBy\": \"PLAY_DAY\","
                )
            )
        );

        challengeSelectionRepository.saveAll(
            challenges.stream()
                .map(this::createSelection)
                .toList()
        );
    }

    /**
     * Creates one challenge catalogue entry using the production JSON schema.
     */
    private Challenge createChallenge(
        String code,
        ChallengeTier tier,
        ProgressMode progressMode,
        String metric,
        String target,
        String additionalCondition
    ) {
        String additionalJson = additionalCondition == null
            ? ""
            : additionalCondition;

        Challenge challenge = new Challenge();
        challenge.setCode(code);
        challenge.setName(code);
        challenge.setDescription(
            "Synchronization pipeline challenge " + code
        );
        challenge.setTier(tier);
        challenge.setCategory(ChallengeCategory.OTHER);
        challenge.setProgressMode(progressMode);
        challenge.setAmateurConditionsJson(
            """
                [
                  {
                    "metric": "%s",
                    "operator": "GTE",
                    "target": %s,
                    %s
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """.formatted(
                metric,
                target,
                additionalJson
            )
        );
        challenge.setProConditionsJson(challenge.getAmateurConditionsJson());
        challenge.setEnabled(true);
        challenge.setSchemaVersion(3);
        return challenge;
    }

    /**
     * Associates one deterministic challenge with the current week.
     */
    private ChallengeSelection createSelection(
        Challenge challenge
    ) {
        ChallengeSelection selection = new ChallengeSelection();
        selection.setWeekStart(WEEK_START);
        selection.setChallenge(challenge);
        selection.setResolvedConditionsJson(challenge.getAmateurConditionsJson());
        selection.setSelectedAt(
            SYNCHRONIZATION_TIME.minusSeconds(3_600)
        );
        return selection;
    }

    /**
     * Creates the current MMR response returned by the mocked Henrik client.
     */
    private HenrikMmrResponse createMmrResponse() {
        return new HenrikMmrResponse(
            new HenrikMmrResponse.HenrikMmrData(
                new HenrikMmrResponse.HenrikCurrentMmr(
                    new HenrikMmrResponse.HenrikTier("Diamond 2"),
                    73
                )
            )
        );
    }

    /**
     * Creates two completed competitive matches returned by Henrik.
     */
    private HenrikMatchHistoryResponse createHistoryResponse() {
        return new HenrikMatchHistoryResponse(
            List.of(
                createMatch(
                    "pipeline-match-1",
                    "2026-07-21T18:00:00Z",
                    true,
                    30,
                    10,
                    8,
                    3_000
                ),
                createMatch(
                    "pipeline-match-2",
                    "2026-07-22T10:00:00Z",
                    false,
                    20,
                    15,
                    5,
                    2_000
                )
            )
        );
    }

    /**
     * Creates one complete Henrik match payload for the tracked player.
     */
    private HenrikMatchData createMatch(
        String matchId,
        String startedAt,
        boolean won,
        int kills,
        int deaths,
        int assists,
        int damageDealt
    ) {
        String playerTeam = "Blue";

        HenrikMatchMetadata metadata = new HenrikMatchMetadata(
            matchId,
            new HenrikMatchMetadata.HenrikMap(
                "ascent",
                "Ascent"
            ),
            2_400_000L,
            Instant.parse(startedAt),
            true,
            new HenrikMatchMetadata.HenrikQueue(
                "competitive",
                "Competitive",
                "Competitive"
            ),
            new HenrikMatchMetadata.HenrikSeason(
                "pipeline-season",
                "E9A4"
            )
        );

        HenrikMatchPlayer trackedPlayer = new HenrikMatchPlayer(
            PLAYER_PUUID,
            playerTeam,
            new HenrikMatchPlayer.HenrikAgent(
                "omen",
                "Omen"
            ),
            new HenrikMatchPlayer.HenrikPlayerStats(
                kills * 250,
                kills,
                deaths,
                assists,
                kills,
                kills * 2,
                0,
                new HenrikMatchPlayer.HenrikDamage(
                    damageDealt)
            ),
            new HenrikMatchPlayer.HenrikTier("Diamond 2")
        );

        HenrikMatchPlayer opponent = new HenrikMatchPlayer(
            "pipeline-opponent-" + matchId,
            "Red",
            new HenrikMatchPlayer.HenrikAgent(
                "jett",
                "Jett"
            ),
            new HenrikMatchPlayer.HenrikPlayerStats(
                1_000,
                5,
                20,
                2,
                2,
                10,
                0,
                new HenrikMatchPlayer.HenrikDamage(
                    1_000)
            ),
            new HenrikMatchPlayer.HenrikTier("Diamond 1")
        );

        HenrikMatchTeam blueTeam = new HenrikMatchTeam(
            "Blue",
            won,
            new HenrikMatchTeam.HenrikRounds(
                won ? 13 : 10,
                won ? 10 : 13
            )
        );

        HenrikMatchTeam redTeam = new HenrikMatchTeam(
            "Red",
            !won,
            new HenrikMatchTeam.HenrikRounds(
                won ? 10 : 13,
                won ? 13 : 10
            )
        );

        return new HenrikMatchData(
            metadata,
            List.of(trackedPlayer, opponent),
            List.of(redTeam, blueTeam)
        );
    }

    /**
     * Clears managed entity state so the next read hits PostgreSQL.
     *
     * <p>No flush needed: this test is not transactional, so every write is already committed.
     */
    private void flushAndClear() {
        entityManager.clear();
    }

    /**
     * Supplies a deterministic primary application clock.
     */
    @TestConfiguration
    static class FixedClockConfiguration {

        /**
         * Creates the fixed UTC clock used by every production service.
         */
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                SYNCHRONIZATION_TIME,
                ZoneOffset.UTC
            );
        }
    }
}
