package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
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
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
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
 * <p>Only Henrik clients are mocked. Account resolution, rank mapping, match
 * import, season resolution, challenge calculation, ranking calculation and
 * synchronization persistence use production components and the real migrated
 * PostgreSQL schema.</p>
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}: match creation races are resolved by
 * committing in their own transaction (see {@code MatchImportService}), which cannot see the
 * uncommitted fixture data an ambient test transaction would otherwise hold. Fixture rows are
 * committed for real and removed in {@link #tearDown()} instead.
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
    private WeeklyChallengeRepository weeklyChallengeRepository;

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

        SynchronizationResponse firstSynchronization =
            synchronizationCommandService.synchronizePlayer(
                player.getId()
            );

        // No explicit recalculation: importing matches is what triggers it. Calling it here as well
        // would rebuild the ranking twice and shift the previous position on the very first run.
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

        SynchronizationResponse secondSynchronization =
            synchronizationCommandService.synchronizePlayer(
                player.getId()
            );

        // The second synchronization imports nothing, so it deliberately skips the recalculation.
        // This call stands in for the administrative repair route and proves the calculation is
        // idempotent: it rewrites the same values in place and only shifts the previous position.
        challengeRecalculationService
            .recalculateCurrentWeekProgress();

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

        verify(mmrClient, org.mockito.Mockito.times(2))
            .getCurrentMmr(PLAYER_PUUID);

        verify(matchClient, org.mockito.Mockito.times(2))
            .getMatches(PLAYER_PUUID, 0, 10);
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
        assertThat(response.lastAttemptAt())
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
                            .getWeeklyChallenge()
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
     * <p>The two imported matches fall on two consecutive days, so the second carries the 2 % streak
     * bonus: WIN 500 + LOSS 350 × 1.02 = 357, for 857 of guardian damage. The five weekly challenges
     * pay 20 + 34 + 54 + 78 + 108 = 294 points at the 2 000 floor, plus the day's challenge when the
     * player validated it.
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
        assertThat(score.getActiveDays())
            .isEqualTo(2);
        assertThat(score.getStreakDays())
            .isEqualTo(2);
        assertThat(score.getChallengePoints())
            .isEqualTo(294 + dailyPoints);
        assertThat(score.getTotalPoints())
            .isEqualTo(857 + 294 + dailyPoints);
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
     * <p>The day's challenge is drawn from the pool, so whether this player validated it is read
     * back rather than assumed. Outside any campaign a validated daily pays 24 points: weight 1.2
     * at the 2 000 floor.
     *
     * @param player player whose dailies are priced
     * @return the points those dailies add
     */
    private int dailyPoints(Player player) {
        return completedDailies(player) * 24;
    }

    /**
     * Counts the daily challenges this player validated this week.
     *
     * @param player player whose dailies are counted
     * @return validated daily challenges
     */
    private int completedDailies(Player player) {
        return (int) progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(WEEK_START)
            .stream()
            .filter(progress -> progress.getPlayer().getId().equals(player.getId()))
            .filter(progress -> progress.getWeeklyChallenge().getCadence() == ChallengeCadence.DAILY)
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
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
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
            .findAllByWeekStartOrderByPositionAsc(WEEK_START)
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
                    ChallengeDifficulty.EASY,
                    ProgressMode.SUM,
                    "KILLS",
                    "50",
                    null
                ),
                createChallenge(
                    "PIPELINE_DAMAGE",
                    ChallengeDifficulty.NORMAL,
                    ProgressMode.SUM,
                    "DAMAGE_DEALT",
                    "5000",
                    null
                ),
                createChallenge(
                    "PIPELINE_WINS",
                    ChallengeDifficulty.MEDIUM,
                    ProgressMode.SUM,
                    "MATCHES_WON",
                    "1",
                    null
                ),
                createChallenge(
                    "PIPELINE_KD",
                    ChallengeDifficulty.HARD,
                    ProgressMode.RATIO,
                    "KD",
                    "2",
                    "\"minimumMatches\": 2,"
                ),
                createChallenge(
                    "PIPELINE_PLAY_DAYS",
                    ChallengeDifficulty.VERY_HARD,
                    ProgressMode.DISTINCT_COUNT,
                    "PLAY_DAY",
                    "2",
                    "\"groupBy\": \"PLAY_DAY\","
                )
            )
        );

        weeklyChallengeRepository.saveAll(
            challenges.stream()
                .map(this::createWeeklyChallenge)
                .toList()
        );
    }

    /**
     * Creates one challenge catalogue entry using the production JSON schema.
     */
    private Challenge createChallenge(
        String code,
        ChallengeDifficulty difficulty,
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
        challenge.setDifficulty(difficulty);
        challenge.setCategory(ChallengeCategory.OTHER);
        challenge.setProgressMode(progressMode);
        challenge.setConditionsJson(
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
        challenge.setEnabled(true);
        challenge.setSchemaVersion(3);
        return challenge;
    }

    /**
     * Associates one deterministic challenge with the current week.
     */
    private WeeklyChallenge createWeeklyChallenge(
        Challenge challenge
    ) {
        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setResolvedConditionsJson(challenge.getConditionsJson());
        weeklyChallenge.setSelectedAt(
            SYNCHRONIZATION_TIME.minusSeconds(3_600)
        );
        return weeklyChallenge;
    }

    /**
     * Creates the current MMR response returned by the mocked Henrik client.
     */
    private HenrikMmrResponse createMmrResponse() {
        return new HenrikMmrResponse(
            200,
            new HenrikMmrResponse.HenrikMmrData(
                new HenrikMmrResponse.HenrikCurrentMmr(
                    new HenrikMmrResponse.HenrikTier(
                        22,
                        "Diamond 2"
                    ),
                    73,
                    1_873
                )
            )
        );
    }

    /**
     * Creates two completed competitive matches returned by Henrik.
     */
    private HenrikMatchHistoryResponse createHistoryResponse() {
        return new HenrikMatchHistoryResponse(
            200,
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
            "PipelinePlayer",
            "TEST",
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
                    damageDealt,
                    2_500
                )
            ),
            new HenrikMatchPlayer.HenrikTier(
                22,
                "Diamond 2"
            )
        );

        HenrikMatchPlayer opponent = new HenrikMatchPlayer(
            "pipeline-opponent-" + matchId,
            "Opponent",
            "TEST",
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
                    1_000,
                    damageDealt
                )
            ),
            new HenrikMatchPlayer.HenrikTier(
                21,
                "Diamond 1"
            )
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
     * <p>No explicit flush: every write already commits on its own, through the repository call or
     * service transaction that issued it, since this test is deliberately not wrapped in one.
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
