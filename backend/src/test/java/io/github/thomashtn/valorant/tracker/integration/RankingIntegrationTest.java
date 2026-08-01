package io.github.thomashtn.valorant.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCategory;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valorant.tracker.ranking.service.RankingRecalculationService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies weekly ranking recalculation against a real PostgreSQL database.
 *
 * <p>The tests persist actual players, weekly challenges and challenge
 * progress before executing the production ranking service. They validate
 * aggregation, ordering, position history, idempotence and cleanup.</p>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Import(RankingIntegrationTest.FixedClockConfiguration.class)
@Transactional
class RankingIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Monday identifying the deterministic integration-test week.
     */
    private static final LocalDate WEEK_START =
        LocalDate.of(2026, 7, 20);

    /**
     * Fixed instant used as the ranking calculation timestamp.
     */
    private static final Instant CALCULATION_TIME =
        Instant.parse("2026-07-24T12:00:00Z");

    /**
     * Player repository used to prepare ranking participants.
     */
    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Challenge repository used to create deterministic rewards.
     */
    @Autowired
    private ChallengeRepository challengeRepository;

    /**
     * Weekly challenge repository used to select challenges for the week.
     */
    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Progress repository used to persist completed challenge states.
     */
    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Score repository used to inspect generated ranking snapshots.
     */
    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Production ranking recalculation service under test.
     */
    @Autowired
    private RankingRecalculationService rankingRecalculationService;

    /**
     * Persistence context used to force database reads between recalculations.
     */
    @Autowired
    private EntityManager entityManager;

    /**
     * Verifies point aggregation, incomplete-progress exclusion and zero-score
     * ranking entries.
     */
    @Test
    void shouldAggregateCompletedChallengesAndRankEveryActivePlayer() {
        deactivateSeededPlayers();

        Player alpha = createPlayer(
            "ranking-alpha",
            "Alpha"
        );
        Player bravo = createPlayer(
            "ranking-bravo",
            "Bravo"
        );
        Player charlie = createPlayer(
            "ranking-charlie",
            "Charlie"
        );

        WeeklyChallenge challenge100 =
            createWeeklyChallenge(
                "RANKING_NOMINAL_100",
                100
            );

        WeeklyChallenge challenge200 =
            createWeeklyChallenge(
                "RANKING_NOMINAL_200",
                200
            );

        WeeklyChallenge challenge400 =
            createWeeklyChallenge(
                "RANKING_NOMINAL_400",
                400
            );

        createProgress(
            alpha,
            challenge100,
            true
        );
        createProgress(
            alpha,
            challenge200,
            true
        );
        createProgress(
            alpha,
            challenge400,
            false
        );

        createProgress(
            bravo,
            challenge400,
            true
        );

        createProgress(
            charlie,
            challenge200,
            false
        );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        List<WeeklyPlayerScore> scores =
            loadScores();

        assertThat(scores).hasSize(3);

        assertScore(
            scores.get(0),
            bravo,
            400,
            1,
            1,
            null
        );

        assertScore(
            scores.get(1),
            alpha,
            300,
            2,
            2,
            null
        );

        assertScore(
            scores.get(2),
            charlie,
            0,
            0,
            3,
            null
        );
    }

    /**
     * Verifies that recalculation stores each former position before applying
     * the new ranking order.
     */
    @Test
    void shouldPreservePreviousPositionsWhenRankingChanges() {
        deactivateSeededPlayers();

        Player alpha = createPlayer(
            "ranking-variation-alpha",
            "Alpha"
        );
        Player bravo = createPlayer(
            "ranking-variation-bravo",
            "Bravo"
        );
        Player charlie = createPlayer(
            "ranking-variation-charlie",
            "Charlie"
        );

        WeeklyChallenge challenge100 =
            createWeeklyChallenge(
                "RANKING_VARIATION_100",
                100
            );

        WeeklyChallenge challenge200 =
            createWeeklyChallenge(
                "RANKING_VARIATION_200",
                200
            );

        WeeklyChallenge challenge300 =
            createWeeklyChallenge(
                "RANKING_VARIATION_300",
                300
            );

        PlayerChallengeProgress alphaProgress =
            createProgress(
                alpha,
                challenge300,
                true
            );

        createProgress(
            bravo,
            challenge200,
            true
        );

        PlayerChallengeProgress bravoBonusProgress =
            createProgress(
                bravo,
                challenge300,
                false
            );

        createProgress(
            charlie,
            challenge100,
            true
        );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        flushAndClear();

        alphaProgress = progressRepository.findById(
            alphaProgress.getId()
        ).orElseThrow();

        bravoBonusProgress = progressRepository.findById(
            bravoBonusProgress.getId()
        ).orElseThrow();

        alphaProgress.setCompleted(false);
        alphaProgress.setCompletedAt(null);

        bravoBonusProgress.setCompleted(true);
        bravoBonusProgress.setCompletedAt(
            CALCULATION_TIME
        );

        progressRepository.saveAll(
            List.of(
                alphaProgress,
                bravoBonusProgress
            )
        );

        flushAndClear();

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        List<WeeklyPlayerScore> scores =
            loadScores();

        assertThat(scores).hasSize(3);

        assertScore(
            scores.get(0),
            bravo,
            500,
            2,
            1,
            2
        );

        assertScore(
            scores.get(1),
            charlie,
            100,
            1,
            2,
            3
        );

        assertScore(
            scores.get(2),
            alpha,
            0,
            0,
            3,
            1
        );
    }

    /**
     * Verifies ranking tie breakers in their documented order:
     * completed challenges, then player identifier.
     */
    @Test
    void shouldResolveTiesDeterministically() {
        deactivateSeededPlayers();

        Player alpha = createPlayer(
            "ranking-tie-alpha",
            "Alpha"
        );
        Player bravo = createPlayer(
            "ranking-tie-bravo",
            "Bravo"
        );
        Player charlie = createPlayer(
            "ranking-tie-charlie",
            "Charlie"
        );

        WeeklyChallenge challenge100A =
            createWeeklyChallenge(
                "RANKING_TIE_100_A",
                100
            );

        WeeklyChallenge challenge100B =
            createWeeklyChallenge(
                "RANKING_TIE_100_B",
                100
            );

        WeeklyChallenge challenge200 =
            createWeeklyChallenge(
                "RANKING_TIE_200",
                200
            );

        createProgress(
            alpha,
            challenge200,
            true
        );

        createProgress(
            bravo,
            challenge100A,
            true
        );

        createProgress(
            bravo,
            challenge100B,
            true
        );

        createProgress(
            charlie,
            challenge200,
            true
        );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        List<WeeklyPlayerScore> scores =
            loadScores();

        assertThat(scores).hasSize(3);

        assertScore(
            scores.get(0),
            bravo,
            200,
            2,
            1,
            null
        );

        assertScore(
            scores.get(1),
            alpha,
            200,
            1,
            2,
            null
        );

        assertScore(
            scores.get(2),
            charlie,
            200,
            1,
            3,
            null
        );

        assertThat(alpha.getId())
            .isLessThan(charlie.getId());
    }

    /**
     * Verifies that repeated recalculation updates existing rows instead of
     * creating duplicates.
     */
    @Test
    void shouldRemainIdempotentWhenProgressDoesNotChange() {
        deactivateSeededPlayers();

        Player alpha = createPlayer(
            "ranking-idempotent-alpha",
            "Alpha"
        );
        Player bravo = createPlayer(
            "ranking-idempotent-bravo",
            "Bravo"
        );

        WeeklyChallenge challenge100 =
            createWeeklyChallenge(
                "RANKING_IDEMPOTENT_100",
                100
            );

        WeeklyChallenge challenge200 =
            createWeeklyChallenge(
                "RANKING_IDEMPOTENT_200",
                200
            );

        createProgress(
            alpha,
            challenge200,
            true
        );

        createProgress(
            bravo,
            challenge100,
            true
        );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        flushAndClear();

        Map<Long, Long> firstScoreIds =
            loadScores()
                .stream()
                .collect(
                    Collectors.toMap(
                        score -> score
                            .getPlayer()
                            .getId(),
                        WeeklyPlayerScore::getId
                    )
                );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        flushAndClear();

        List<WeeklyPlayerScore> secondScores =
            loadScores();

        assertThat(secondScores).hasSize(2);

        assertThat(
            scoreRepository.count()
        ).isEqualTo(2);

        assertThat(secondScores)
            .allSatisfy(score -> {
                assertThat(score.getId())
                    .isEqualTo(
                        firstScoreIds.get(
                            score.getPlayer().getId()
                        )
                    );

                assertThat(score.getPreviousPosition())
                    .isEqualTo(score.getPosition());

                assertThat(score.getCalculatedAt())
                    .isEqualTo(CALCULATION_TIME);
            });

        assertScore(
            secondScores.get(0),
            alpha,
            200,
            1,
            1,
            1
        );

        assertScore(
            secondScores.get(1),
            bravo,
            100,
            1,
            2,
            2
        );
    }

    /**
     * Verifies that an obsolete score is deleted when its player becomes
     * inactive.
     */
    @Test
    void shouldRemoveScoresBelongingToInactivePlayers() {
        deactivateSeededPlayers();

        Player activePlayer = createPlayer(
            "ranking-active-player",
            "Active"
        );
        Player futureInactivePlayer = createPlayer(
            "ranking-inactive-player",
            "FutureInactive"
        );

        WeeklyChallenge challenge100 =
            createWeeklyChallenge(
                "RANKING_INACTIVE_100",
                100
            );

        createProgress(
            activePlayer,
            challenge100,
            true
        );

        createProgress(
            futureInactivePlayer,
            challenge100,
            true
        );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        flushAndClear();

        futureInactivePlayer = playerRepository.findById(
            futureInactivePlayer.getId()
        ).orElseThrow();

        futureInactivePlayer.setStatus(
            PlayerStatus.INACTIVE
        );

        playerRepository.save(
            futureInactivePlayer
        );

        flushAndClear();

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        List<WeeklyPlayerScore> scores =
            loadScores();

        assertThat(scores)
            .singleElement()
            .satisfies(score ->
                assertScore(
                    score,
                    activePlayer,
                    100,
                    1,
                    1,
                    1
                )
            );
    }

    /**
     * Marks Flyway-seeded players inactive so every test controls the complete
     * active-player population.
     */
    private void deactivateSeededPlayers() {
        List<Player> players = playerRepository.findAll();

        players.forEach(
            player -> player.setStatus(
                PlayerStatus.INACTIVE
            )
        );

        playerRepository.saveAll(players);
        flushAndClear();
    }

    /**
     * Creates one active ranking participant.
     *
     * @param riotPuuid   unique Riot account identifier
     * @param displayName player display name
     * @return persisted player
     */
    private Player createPlayer(
        String riotPuuid,
        String displayName
    ) {
        Player player = new Player();

        player.setRiotPuuid(riotPuuid);
        player.setGameName(displayName);
        player.setTagLine("TEST");
        player.setDisplayName(
            displayName + "#TEST"
        );
        player.setPortrait("default");
        player.setStatus(PlayerStatus.ACTIVE);

        return playerRepository.save(player);
    }

    /**
     * Creates a reusable challenge and selects it for the test week.
     *
     * @param code   stable unique challenge code
     * @param points points awarded when completed
     * @return persisted weekly challenge
     */
    private WeeklyChallenge createWeeklyChallenge(
        String code,
        int points
    ) {
        Challenge challenge =
            createChallenge(
                code,
                points
            );

        challenge = challengeRepository.save(
            challenge
        );

        WeeklyChallenge weeklyChallenge =
            new WeeklyChallenge();

        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(
            CALCULATION_TIME.minusSeconds(3_600)
        );

        return weeklyChallengeRepository.save(
            weeklyChallenge
        );
    }

    /**
     * Creates one complete challenge catalogue entry.
     *
     * @param code   stable unique code
     * @param points completion reward
     * @return unsaved challenge
     */
    private Challenge createChallenge(
        String code,
        int points
    ) {
        Challenge challenge = new Challenge();

        challenge.setCode(code);
        challenge.setName(code);
        challenge.setDescription(
            "Ranking integration challenge " + code
        );
        challenge.setDifficulty(
            ChallengeDifficulty.EASY
        );
        challenge.setPoints(points);
        challenge.setCategory(
            ChallengeCategory.OTHER
        );
        challenge.setRuleType(
            ChallengeRuleType.SINGLE
        );
        challenge.setProgressMode(
            ProgressMode.SUM
        );
        challenge.setConditionsJson(
            """
                [
                  {
                    "metric": "KILLS",
                    "operator": "GTE",
                    "target": 1,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );
        challenge.setEnabled(true);
        challenge.setSchemaVersion(3);

        return challenge;
    }

    /**
     * Creates one persisted player challenge-progress row.
     *
     * @param player          evaluated player
     * @param weeklyChallenge evaluated weekly challenge
     * @param completed       completion state
     * @return persisted progress
     */
    private PlayerChallengeProgress createProgress(
        Player player,
        WeeklyChallenge weeklyChallenge,
        boolean completed
    ) {
        PlayerChallengeProgress progress =
            new PlayerChallengeProgress();

        progress.setPlayer(player);
        progress.setWeeklyChallenge(
            weeklyChallenge
        );
        progress.setCurrentValue(
            completed
                ? BigDecimal.ONE
                : BigDecimal.ZERO
        );
        progress.setTargetValue(
            BigDecimal.ONE
        );
        progress.setCompleted(completed);
        progress.setCompletedAt(
            completed
                ? CALCULATION_TIME.minusSeconds(60)
                : null
        );
        progress.setCalculatedAt(
            CALCULATION_TIME.minusSeconds(60)
        );

        return progressRepository.save(progress);
    }

    /**
     * Retrieves the test-week ranking from PostgreSQL.
     *
     * @return scores ordered by current position
     */
    private List<WeeklyPlayerScore> loadScores() {
        flushAndClear();

        return scoreRepository
            .findAllByWeekStartOrderByPositionAsc(
                WEEK_START
            );
    }

    /**
     * Forces pending statements to PostgreSQL and clears managed entities.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Verifies one complete weekly score.
     *
     * @param score                       persisted score
     * @param expectedPlayer              expected player
     * @param expectedPoints              expected points
     * @param expectedCompletedChallenges expected completed challenge count
     * @param expectedPosition            expected current position
     * @param expectedPreviousPosition    expected former position
     */
    private void assertScore(
        WeeklyPlayerScore score,
        Player expectedPlayer,
        int expectedPoints,
        int expectedCompletedChallenges,
        int expectedPosition,
        Integer expectedPreviousPosition
    ) {
        assertThat(score.getPlayer().getId())
            .isEqualTo(expectedPlayer.getId());

        assertThat(score.getWeekStart())
            .isEqualTo(WEEK_START);

        assertThat(score.getPoints())
            .isEqualTo(expectedPoints);

        assertThat(score.getCompletedChallenges())
            .isEqualTo(
                expectedCompletedChallenges
            );

        assertThat(score.getPosition())
            .isEqualTo(expectedPosition);

        assertThat(score.getPreviousPosition())
            .isEqualTo(
                expectedPreviousPosition
            );

        assertThat(score.getCalculatedAt())
            .isEqualTo(CALCULATION_TIME);

        assertThat(score.getFinalizedAt())
            .isNull();
    }

    /**
     * Overrides the production clock with a deterministic UTC clock.
     */
    @TestConfiguration
    static class FixedClockConfiguration {

        /**
         * Provides the clock used by ranking services.
         *
         * @return fixed UTC clock
         */
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                CALCULATION_TIME,
                ZoneOffset.UTC
            );
        }
    }
}
