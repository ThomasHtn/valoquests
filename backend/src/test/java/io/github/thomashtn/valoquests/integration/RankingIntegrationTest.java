package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
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
 *
 * <p>Challenge damage is resolved from {@code ScoringRulesetV1} by difficulty tier (EASY=1500,
 * NORMAL=2500, MEDIUM=4000, HARD=6000), not from the legacy {@code Challenge.damage} column, which this
 * feature supersedes for scoring. No match is ever persisted by these fixtures, so match damage and the
 * regularity bonus are always zero here; only challenge damage and, where two players complete the same
 * weekly challenge, the team bonus contribute to the total.</p>
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
     * Verifies damage aggregation, incomplete-progress exclusion and zero-score
     * ranking entries.
     */
    @Test
    void shouldAggregateCompletedChallengesAndRankEveryActivePlayer() {
        removeSeededPlayers();

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

        WeeklyChallenge challengeEasy =
            createWeeklyChallenge(
                "RANKING_NOMINAL_EASY",
                ChallengeDifficulty.EASY
            );

        WeeklyChallenge challengeNormal =
            createWeeklyChallenge(
                "RANKING_NOMINAL_NORMAL",
                ChallengeDifficulty.NORMAL
            );

        WeeklyChallenge challengeHard =
            createWeeklyChallenge(
                "RANKING_NOMINAL_HARD",
                ChallengeDifficulty.HARD
            );

        createProgress(
            alpha,
            challengeEasy,
            true
        );
        createProgress(
            alpha,
            challengeNormal,
            true
        );
        createProgress(
            alpha,
            challengeHard,
            false
        );

        createProgress(
            bravo,
            challengeHard,
            true
        );

        createProgress(
            charlie,
            challengeNormal,
            false
        );

        rankingRecalculationService.recalculateWeek(
            WEEK_START
        );

        List<WeeklyPlayerScore> scores =
            loadScores();

        assertThat(scores).hasSize(3);

        // bravo: HARD (6000) alone. alpha: EASY (1500) + NORMAL (2500) = 4000. Neither challenge is
        // completed by more than one player here, so the team bonus stays at zero throughout.
        assertScore(
            scores.get(0),
            bravo,
            6_000,
            6_000,
            1,
            1,
            null
        );

        assertScore(
            scores.get(1),
            alpha,
            4_000,
            4_000,
            2,
            2,
            null
        );

        assertScore(
            scores.get(2),
            charlie,
            0,
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
        removeSeededPlayers();

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

        WeeklyChallenge challengeEasy =
            createWeeklyChallenge(
                "RANKING_VARIATION_EASY",
                ChallengeDifficulty.EASY
            );

        WeeklyChallenge challengeNormal =
            createWeeklyChallenge(
                "RANKING_VARIATION_NORMAL",
                ChallengeDifficulty.NORMAL
            );

        WeeklyChallenge challengeMedium =
            createWeeklyChallenge(
                "RANKING_VARIATION_MEDIUM",
                ChallengeDifficulty.MEDIUM
            );

        PlayerChallengeProgress alphaProgress =
            createProgress(
                alpha,
                challengeMedium,
                true
            );

        createProgress(
            bravo,
            challengeNormal,
            true
        );

        PlayerChallengeProgress bravoBonusProgress =
            createProgress(
                bravo,
                challengeMedium,
                false
            );

        createProgress(
            charlie,
            challengeEasy,
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

        // First pass ordered alpha (MEDIUM=4000) > bravo (NORMAL=2500) > charlie (EASY=1500), which
        // seeds the previous positions asserted below. The second pass flips completion: bravo now
        // also completes the MEDIUM challenge (NORMAL+MEDIUM=6500) while alpha completes nothing.
        assertScore(
            scores.get(0),
            bravo,
            6_500,
            6_500,
            2,
            1,
            2
        );

        assertScore(
            scores.get(1),
            charlie,
            1_500,
            1_500,
            1,
            2,
            3
        );

        assertScore(
            scores.get(2),
            alpha,
            0,
            0,
            0,
            3,
            1
        );
    }

    /**
     * Verifies ranking tie breakers in their documented order:
     * total damage, then completed challenges, then player identifier.
     */
    @Test
    void shouldResolveTiesDeterministically() {
        removeSeededPlayers();

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

        // bravo reaches 4000 (EASY 1500 + NORMAL 2500) across two challenges; alpha and charlie each
        // reach the same 4000 total through a single, separate MEDIUM challenge. All three tie on total
        // damage, so the tie is resolved by completed-challenge count (bravo wins with 2), then by
        // player identifier for the remaining alpha/charlie tie.
        WeeklyChallenge bravoEasy =
            createWeeklyChallenge(
                "RANKING_TIE_BRAVO_EASY",
                ChallengeDifficulty.EASY
            );

        WeeklyChallenge bravoNormal =
            createWeeklyChallenge(
                "RANKING_TIE_BRAVO_NORMAL",
                ChallengeDifficulty.NORMAL
            );

        WeeklyChallenge alphaMedium =
            createWeeklyChallenge(
                "RANKING_TIE_ALPHA_MEDIUM",
                ChallengeDifficulty.MEDIUM
            );

        WeeklyChallenge charlieMedium =
            createWeeklyChallenge(
                "RANKING_TIE_CHARLIE_MEDIUM",
                ChallengeDifficulty.MEDIUM
            );

        createProgress(
            alpha,
            alphaMedium,
            true
        );

        createProgress(
            bravo,
            bravoEasy,
            true
        );

        createProgress(
            bravo,
            bravoNormal,
            true
        );

        createProgress(
            charlie,
            charlieMedium,
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
            4_000,
            4_000,
            2,
            1,
            null
        );

        assertScore(
            scores.get(1),
            alpha,
            4_000,
            4_000,
            1,
            2,
            null
        );

        assertScore(
            scores.get(2),
            charlie,
            4_000,
            4_000,
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
        removeSeededPlayers();

        Player alpha = createPlayer(
            "ranking-idempotent-alpha",
            "Alpha"
        );
        Player bravo = createPlayer(
            "ranking-idempotent-bravo",
            "Bravo"
        );

        WeeklyChallenge challengeEasy =
            createWeeklyChallenge(
                "RANKING_IDEMPOTENT_EASY",
                ChallengeDifficulty.EASY
            );

        WeeklyChallenge challengeNormal =
            createWeeklyChallenge(
                "RANKING_IDEMPOTENT_NORMAL",
                ChallengeDifficulty.NORMAL
            );

        createProgress(
            alpha,
            challengeNormal,
            true
        );

        createProgress(
            bravo,
            challengeEasy,
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
            2_500,
            2_500,
            1,
            1,
            1
        );

        assertScore(
            secondScores.get(1),
            bravo,
            1_500,
            1_500,
            1,
            2,
            2
        );
    }

    /**
     * Verifies that a player who becomes inactive mid-week keeps their score (for display), but
     * their already-completed challenge stops counting toward the team bonus of players who are
     * still active.
     */
    @Test
    void shouldExcludeInactivePlayerFromTeamBonusWhileKeepingTheirScore() {
        removeSeededPlayers();

        Player activePlayer = createPlayer(
            "ranking-active-player",
            "Active"
        );
        Player futureInactivePlayer = createPlayer(
            "ranking-inactive-player",
            "FutureInactive"
        );

        WeeklyChallenge challengeEasy =
            createWeeklyChallenge(
                "RANKING_INACTIVE_EASY",
                ChallengeDifficulty.EASY
            );

        createProgress(
            activePlayer,
            challengeEasy,
            true
        );

        createProgress(
            futureInactivePlayer,
            challengeEasy,
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

        assertThat(scores).hasSize(2);

        // The now-inactive player's progress row still exists (the roster is assumed fixed for a
        // given week; deactivating mid-week is an edge case outside that assumption), but it no
        // longer counts toward the team bonus tier: only one player is still active, so the "two
        // completions" tier no longer applies.
        Long inactivePlayerId = futureInactivePlayer.getId();

        WeeklyPlayerScore activeScore = scores.stream()
            .filter(score -> score.getPlayer().getId().equals(activePlayer.getId()))
            .findFirst()
            .orElseThrow();
        WeeklyPlayerScore inactiveScore = scores.stream()
            .filter(score -> score.getPlayer().getId().equals(inactivePlayerId))
            .findFirst()
            .orElseThrow();

        assertScore(
            activeScore,
            activePlayer,
            1_500,
            1_500,
            1,
            1,
            1
        );

        assertThat(inactiveScore.getChallengeDamage()).isZero();
        assertThat(inactiveScore.getTotalDamage()).isZero();
        assertThat(inactiveScore.getCompletedChallenges()).isEqualTo(1);
        assertThat(inactiveScore.getPosition()).isNull();
    }

    /**
     * Verifies that an inactive player still gets a weekly score built with their real
     * completed-challenge count, but with zero damage and no ranking slot, and does not push an
     * active player behind it down a position.
     */
    @Test
    void shouldExcludeInactivePlayerFromRankingSlot() {
        removeSeededPlayers();

        Player pro = createNonCompetitivePlayer(
            "ranking-pro",
            "Pro"
        );
        Player regular = createPlayer(
            "ranking-regular",
            "Regular"
        );

        WeeklyChallenge challengeHard =
            createWeeklyChallenge(
                "RANKING_NON_COMPETITIVE_HARD",
                ChallengeDifficulty.HARD
            );
        WeeklyChallenge challengeEasy =
            createWeeklyChallenge(
                "RANKING_NON_COMPETITIVE_EASY",
                ChallengeDifficulty.EASY
            );

        createProgress(pro, challengeHard, true);
        createProgress(regular, challengeEasy, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(2);

        // loadScores() orders by `position ASC`, and PostgreSQL's default NULLS LAST puts the pro
        // player's null position after the regular player's real one, regardless of the HARD
        // challenge he completed: an inactive player never deals damage, so he never competes for
        // a slot in the first place - the regular player still gets position 1, not 2.
        WeeklyPlayerScore regularScore = scores.get(0);
        WeeklyPlayerScore proScore = scores.get(1);

        assertThat(proScore.getPlayer().getId()).isEqualTo(pro.getId());
        assertThat(proScore.getTotalDamage()).isZero();
        assertThat(proScore.getCompletedChallenges()).isEqualTo(1);
        assertThat(proScore.getPosition()).isNull();

        assertScore(
            regularScore,
            regular,
            1_500,
            1_500,
            1,
            1,
            null
        );
    }

    /**
     * Removes Flyway-seeded players so every test controls the complete tracked-player
     * population. Marking them inactive is not enough: an inactive player still gets a weekly
     * score built for display, so they would still show up in the ranking assertions below.
     */
    private void removeSeededPlayers() {
        playerRepository.deleteAll();
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
     * Creates one inactive ranking participant, e.g. a showcased pro player who must never occupy
     * a ranking slot.
     *
     * @param riotPuuid   unique Riot account identifier
     * @param displayName player display name
     * @return persisted player
     */
    private Player createNonCompetitivePlayer(
        String riotPuuid,
        String displayName
    ) {
        Player player = createPlayer(riotPuuid, displayName);
        player.setStatus(PlayerStatus.INACTIVE);

        return playerRepository.save(player);
    }

    /**
     * Creates a reusable challenge and selects it for the test week.
     *
     * @param code       stable unique challenge code
     * @param difficulty difficulty tier, which resolves the damage awarded on completion
     * @return persisted weekly challenge
     */
    private WeeklyChallenge createWeeklyChallenge(
        String code,
        ChallengeDifficulty difficulty
    ) {
        Challenge challenge =
            createChallenge(
                code,
                difficulty
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
     * @param code       stable unique code
     * @param difficulty difficulty tier, which resolves the damage awarded on completion
     * @return unsaved challenge
     */
    private Challenge createChallenge(
        String code,
        ChallengeDifficulty difficulty
    ) {
        Challenge challenge = new Challenge();

        challenge.setCode(code);
        challenge.setName(code);
        challenge.setDescription(
            "Ranking integration challenge " + code
        );
        challenge.setDifficulty(difficulty);
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
     * @param expectedChallengeDamage     expected challenge damage ({@code challengeDamage})
     * @param expectedTotalDamage         expected total damage, including any team bonus
     * @param expectedCompletedChallenges expected completed challenge count
     * @param expectedPosition            expected current position
     * @param expectedPreviousPosition    expected former position
     */
    private void assertScore(
        WeeklyPlayerScore score,
        Player expectedPlayer,
        int expectedChallengeDamage,
        int expectedTotalDamage,
        int expectedCompletedChallenges,
        int expectedPosition,
        Integer expectedPreviousPosition
    ) {
        assertThat(score.getPlayer().getId())
            .isEqualTo(expectedPlayer.getId());

        assertThat(score.getWeekStart())
            .isEqualTo(WEEK_START);

        assertThat(score.getChallengeDamage())
            .isEqualTo(expectedChallengeDamage);

        assertThat(score.getTotalDamage())
            .isEqualTo(expectedTotalDamage);

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
