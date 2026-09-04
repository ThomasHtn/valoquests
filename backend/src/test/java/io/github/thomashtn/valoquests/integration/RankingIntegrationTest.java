package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the weekly ranking against a real PostgreSQL database: how validated challenges are
 * priced and ordered, and who takes a slot.
 *
 * <p>No campaign is ever opened here, so every challenge pays at the 2 000 floor: 24 points for
 * the day's challenge, then 20 / 34 / 54 / 78 / 108 by difficulty. No match is stored either, so
 * the guardian damage is zero throughout and the order is decided by the challenges alone.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0"
    }
)
@Transactional
class RankingIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Monday identifying the deterministic integration-test week.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Fixed instant used as the ranking calculation timestamp.
     */
    private static final Instant CALCULATION_TIME = Instant.parse("2026-07-24T12:00:00Z");

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Autowired
    private PlayerChallengeProgressRepository progressRepository;

    @Autowired
    private WeeklyPlayerScoreRepository scoreRepository;

    @Autowired
    private RankingRecalculationService rankingRecalculationService;

    @Autowired
    private EntityManager entityManager;

    /**
     * Verifies challenge pricing, incomplete-progress exclusion and zero-score ranking entries.
     */
    @Test
    void shouldPriceCompletedChallengesAndRankEveryActivePlayer() {
        removeSeededPlayers();
        Player alpha = createPlayer("ranking-alpha", "Alpha");
        Player bravo = createPlayer("ranking-bravo", "Bravo");
        Player charlie = createPlayer("ranking-charlie", "Charlie");

        WeeklyChallenge easy = createWeeklyChallenge("RANKING_NOMINAL_EASY", ChallengeDifficulty.EASY);
        WeeklyChallenge normal = createWeeklyChallenge("RANKING_NOMINAL_NORMAL", ChallengeDifficulty.NORMAL);
        WeeklyChallenge hard = createWeeklyChallenge("RANKING_NOMINAL_HARD", ChallengeDifficulty.HARD);

        createProgress(alpha, easy, true);
        createProgress(alpha, normal, true);
        createProgress(alpha, hard, false);
        createProgress(bravo, hard, true);
        createProgress(charlie, normal, false);

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(3);
        // bravo: HARD 78 alone. alpha: EASY 20 + NORMAL 34 = 54. charlie validated nothing.
        assertScore(scores.get(0), bravo, 78, 1, 0, 1, null);
        assertScore(scores.get(1), alpha, 54, 2, 0, 2, null);
        assertScore(scores.get(2), charlie, 0, 0, 0, 3, null);
    }

    /**
     * Verifies that the day's challenge pays its own points and is counted apart from the pack.
     */
    @Test
    void shouldPriceTheDailyChallengeAndCountItApart() {
        removeSeededPlayers();
        Player alpha = createPlayer("ranking-daily-alpha", "Alpha");
        Player bravo = createPlayer("ranking-daily-bravo", "Bravo");

        WeeklyChallenge easy = createWeeklyChallenge("RANKING_DAILY_EASY", ChallengeDifficulty.EASY);
        WeeklyChallenge daily = createDailyChallenge("RANKING_DAILY_DAY", WEEK_START.plusDays(3));

        createProgress(alpha, daily, true);
        createProgress(bravo, easy, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(2);
        // The daily weighs 1.2 against EASY's 1.0, so it pays more: 24 against 20.
        assertScore(scores.get(0), alpha, 24, 0, 1, 1, null);
        assertScore(scores.get(1), bravo, 20, 1, 0, 2, null);
    }

    /**
     * Verifies that recalculation stores each former position before applying the new order.
     */
    @Test
    void shouldPreservePreviousPositionsWhenRankingChanges() {
        removeSeededPlayers();
        Player alpha = createPlayer("ranking-variation-alpha", "Alpha");
        Player bravo = createPlayer("ranking-variation-bravo", "Bravo");
        Player charlie = createPlayer("ranking-variation-charlie", "Charlie");

        WeeklyChallenge easy = createWeeklyChallenge("RANKING_VARIATION_EASY", ChallengeDifficulty.EASY);
        WeeklyChallenge normal = createWeeklyChallenge("RANKING_VARIATION_NORMAL", ChallengeDifficulty.NORMAL);
        WeeklyChallenge medium = createWeeklyChallenge("RANKING_VARIATION_MEDIUM", ChallengeDifficulty.MEDIUM);

        PlayerChallengeProgress alphaProgress = createProgress(alpha, medium, true);
        createProgress(bravo, normal, true);
        PlayerChallengeProgress bravoBonusProgress = createProgress(bravo, medium, false);
        createProgress(charlie, easy, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);
        flushAndClear();

        alphaProgress = progressRepository.findById(alphaProgress.getId()).orElseThrow();
        bravoBonusProgress = progressRepository.findById(bravoBonusProgress.getId()).orElseThrow();
        alphaProgress.setCompleted(false);
        alphaProgress.setCompletedAt(null);
        bravoBonusProgress.setCompleted(true);
        bravoBonusProgress.setCompletedAt(CALCULATION_TIME);
        progressRepository.saveAll(List.of(alphaProgress, bravoBonusProgress));
        flushAndClear();

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(3);
        // First pass: alpha (MEDIUM 54) > bravo (NORMAL 34) > charlie (EASY 20), which seeds the
        // previous positions. Second pass: bravo also validates MEDIUM (88) while alpha loses hers.
        assertScore(scores.get(0), bravo, 88, 2, 0, 1, 2);
        assertScore(scores.get(1), charlie, 20, 1, 0, 2, 3);
        assertScore(scores.get(2), alpha, 0, 0, 0, 3, 1);
    }

    /**
     * Verifies ranking tie breakers in their documented order: total points, guardian damage,
     * validated challenges, then player identifier.
     */
    @Test
    void shouldResolveTiesDeterministically() {
        removeSeededPlayers();
        Player alpha = createPlayer("ranking-tie-alpha", "Alpha");
        Player bravo = createPlayer("ranking-tie-bravo", "Bravo");
        Player charlie = createPlayer("ranking-tie-charlie", "Charlie");

        // bravo reaches 54 through EASY 20 + NORMAL 34; alpha and charlie each reach 54 through one
        // MEDIUM. Nobody dealt damage, so bravo wins on validations and alpha on identifier.
        WeeklyChallenge bravoEasy = createWeeklyChallenge("RANKING_TIE_BRAVO_EASY", ChallengeDifficulty.EASY);
        WeeklyChallenge bravoNormal = createWeeklyChallenge("RANKING_TIE_BRAVO_NORMAL", ChallengeDifficulty.NORMAL);
        WeeklyChallenge alphaMedium = createWeeklyChallenge("RANKING_TIE_ALPHA_MEDIUM", ChallengeDifficulty.MEDIUM);
        WeeklyChallenge charlieMedium = createWeeklyChallenge("RANKING_TIE_CHARLIE_MEDIUM", ChallengeDifficulty.MEDIUM);

        createProgress(alpha, alphaMedium, true);
        createProgress(bravo, bravoEasy, true);
        createProgress(bravo, bravoNormal, true);
        createProgress(charlie, charlieMedium, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(3);
        assertScore(scores.get(0), bravo, 54, 2, 0, 1, null);
        assertScore(scores.get(1), alpha, 54, 1, 0, 2, null);
        assertScore(scores.get(2), charlie, 54, 1, 0, 3, null);
        assertThat(alpha.getId()).isLessThan(charlie.getId());
    }

    /**
     * Verifies that repeated recalculation updates existing rows instead of creating duplicates.
     */
    @Test
    void shouldRemainIdempotentWhenProgressDoesNotChange() {
        removeSeededPlayers();
        Player alpha = createPlayer("ranking-idempotent-alpha", "Alpha");
        Player bravo = createPlayer("ranking-idempotent-bravo", "Bravo");

        WeeklyChallenge easy = createWeeklyChallenge("RANKING_IDEMPOTENT_EASY", ChallengeDifficulty.EASY);
        WeeklyChallenge normal = createWeeklyChallenge("RANKING_IDEMPOTENT_NORMAL", ChallengeDifficulty.NORMAL);

        createProgress(alpha, normal, true);
        createProgress(bravo, easy, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);
        flushAndClear();

        Map<Long, Long> firstScoreIds = loadScores().stream()
            .collect(Collectors.toMap(score -> score.getPlayer().getId(), WeeklyPlayerScore::getId));

        rankingRecalculationService.recalculateWeek(WEEK_START);
        flushAndClear();

        List<WeeklyPlayerScore> secondScores = loadScores();

        assertThat(secondScores).hasSize(2);
        assertThat(scoreRepository.count()).isEqualTo(2);
        assertThat(secondScores).allSatisfy(score -> {
            assertThat(score.getId()).isEqualTo(firstScoreIds.get(score.getPlayer().getId()));
            assertThat(score.getPreviousPosition()).isEqualTo(score.getPosition());
            assertThat(score.getCalculatedAt()).isEqualTo(CALCULATION_TIME);
        });
        assertScore(secondScores.get(0), alpha, 34, 1, 0, 1, 1);
        assertScore(secondScores.get(1), bravo, 20, 1, 0, 2, 2);
    }

    /**
     * Verifies that a player deactivated mid-week keeps their validation count for display but
     * loses their points and their slot, without pushing the active player down.
     */
    @Test
    void shouldDropPointsAndSlotOfAPlayerDeactivatedMidWeek() {
        removeSeededPlayers();
        Player activePlayer = createPlayer("ranking-active-player", "Active");
        Player futureInactivePlayer = createPlayer("ranking-inactive-player", "FutureInactive");

        WeeklyChallenge easy = createWeeklyChallenge("RANKING_INACTIVE_EASY", ChallengeDifficulty.EASY);
        createProgress(activePlayer, easy, true);
        createProgress(futureInactivePlayer, easy, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);
        flushAndClear();

        futureInactivePlayer = playerRepository.findById(futureInactivePlayer.getId()).orElseThrow();
        futureInactivePlayer.setStatus(PlayerStatus.INACTIVE);
        playerRepository.save(futureInactivePlayer);
        flushAndClear();

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(2);
        Long inactivePlayerId = futureInactivePlayer.getId();
        WeeklyPlayerScore activeScore = scores.stream()
            .filter(score -> score.getPlayer().getId().equals(activePlayer.getId()))
            .findFirst()
            .orElseThrow();
        WeeklyPlayerScore inactiveScore = scores.stream()
            .filter(score -> score.getPlayer().getId().equals(inactivePlayerId))
            .findFirst()
            .orElseThrow();

        assertScore(activeScore, activePlayer, 20, 1, 0, 1, 1);
        assertThat(inactiveScore.getChallengePoints()).isZero();
        assertThat(inactiveScore.getTotalPoints()).isZero();
        assertThat(inactiveScore.getCompletedChallenges()).isEqualTo(1);
        assertThat(inactiveScore.getPosition()).isNull();
        assertThat(inactiveScore.getPreviousPosition()).isEqualTo(2);
    }

    /**
     * Verifies that an inactive player gets a row with their real validation count, no points and
     * no slot, and never pushes an active player down a position.
     */
    @Test
    void shouldExcludeInactivePlayerFromRankingSlot() {
        removeSeededPlayers();
        Player pro = createNonCompetitivePlayer("ranking-pro", "Pro");
        Player regular = createPlayer("ranking-regular", "Regular");

        WeeklyChallenge hard = createWeeklyChallenge("RANKING_NON_COMPETITIVE_HARD", ChallengeDifficulty.HARD);
        WeeklyChallenge easy = createWeeklyChallenge("RANKING_NON_COMPETITIVE_EASY", ChallengeDifficulty.EASY);
        createProgress(pro, hard, true);
        createProgress(regular, easy, true);

        rankingRecalculationService.recalculateWeek(WEEK_START);

        List<WeeklyPlayerScore> scores = loadScores();

        assertThat(scores).hasSize(2);
        // Ordered by position, NULLS LAST: the pro's null position comes after the regular's real
        // one, whatever the HARD challenge would have paid a competing player.
        WeeklyPlayerScore regularScore = scores.get(0);
        WeeklyPlayerScore proScore = scores.get(1);

        assertThat(proScore.getPlayer().getId()).isEqualTo(pro.getId());
        assertThat(proScore.getTotalPoints()).isZero();
        assertThat(proScore.getCompletedChallenges()).isEqualTo(1);
        assertThat(proScore.getPosition()).isNull();
        assertScore(regularScore, regular, 20, 1, 0, 1, null);
    }

    /**
     * Removes Flyway-seeded players so every test controls the complete tracked-player population.
     * Marking them inactive is not enough: an inactive player still gets a row built for display.
     */
    private void removeSeededPlayers() {
        playerRepository.deleteAll();
        flushAndClear();
    }

    private Player createPlayer(String riotPuuid, String displayName) {
        Player player = new Player();
        player.setRiotPuuid(riotPuuid);
        player.setGameName(displayName);
        player.setTagLine("TEST");
        player.setDisplayName(displayName + "#TEST");
        player.setPortrait("default");
        player.setStatus(PlayerStatus.ACTIVE);

        return playerRepository.save(player);
    }

    private Player createNonCompetitivePlayer(String riotPuuid, String displayName) {
        Player player = createPlayer(riotPuuid, displayName);
        player.setStatus(PlayerStatus.INACTIVE);

        return playerRepository.save(player);
    }

    private WeeklyChallenge createWeeklyChallenge(String code, ChallengeDifficulty difficulty) {
        Challenge challenge = challengeRepository.save(createChallenge(code, ChallengeCadence.WEEKLY, difficulty));

        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setResolvedConditionsJson(challenge.getConditionsJson());
        weeklyChallenge.setSelectedAt(CALCULATION_TIME.minusSeconds(3_600));

        return weeklyChallengeRepository.save(weeklyChallenge);
    }

    private WeeklyChallenge createDailyChallenge(String code, LocalDate day) {
        Challenge challenge = challengeRepository.save(createChallenge(code, ChallengeCadence.DAILY, null));

        WeeklyChallenge selection = new WeeklyChallenge();
        selection.setWeekStart(WEEK_START);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(day);
        selection.setChallenge(challenge);
        selection.setResolvedConditionsJson(challenge.getConditionsJson());
        selection.setSelectedAt(CALCULATION_TIME.minusSeconds(3_600));

        return weeklyChallengeRepository.save(selection);
    }

    private Challenge createChallenge(String code, ChallengeCadence cadence, ChallengeDifficulty difficulty) {
        Challenge challenge = new Challenge();
        challenge.setCode(code);
        challenge.setName(code);
        challenge.setDescription("Ranking integration challenge " + code);
        challenge.setCadence(cadence);
        challenge.setDifficulty(difficulty);
        challenge.setCategory(ChallengeCategory.OTHER);
        challenge.setProgressMode(ProgressMode.SUM);
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

    private PlayerChallengeProgress createProgress(Player player, WeeklyChallenge weeklyChallenge, boolean completed) {
        PlayerChallengeProgress progress = new PlayerChallengeProgress();
        progress.setPlayer(player);
        progress.setWeeklyChallenge(weeklyChallenge);
        progress.setCurrentValue(completed ? BigDecimal.ONE : BigDecimal.ZERO);
        progress.setTargetValue(BigDecimal.ONE);
        progress.setCompleted(completed);
        progress.setCompletedAt(completed ? CALCULATION_TIME.minusSeconds(60) : null);
        progress.setCalculatedAt(CALCULATION_TIME.minusSeconds(60));

        return progressRepository.save(progress);
    }

    private List<WeeklyPlayerScore> loadScores() {
        flushAndClear();

        return scoreRepository.findAllByWeekStartOrderByPositionAsc(WEEK_START);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Verifies one weekly row built without any match.
     *
     * @param score                    persisted row
     * @param expectedPlayer           expected player
     * @param expectedChallengePoints  expected challenge points, also the total since no match exists
     * @param expectedCompletedWeekly  expected weekly validations
     * @param expectedCompletedDaily   expected daily validations
     * @param expectedPosition         expected current position
     * @param expectedPreviousPosition expected former position
     */
    private void assertScore(
        WeeklyPlayerScore score,
        Player expectedPlayer,
        int expectedChallengePoints,
        int expectedCompletedWeekly,
        int expectedCompletedDaily,
        int expectedPosition,
        Integer expectedPreviousPosition
    ) {
        assertThat(score.getPlayer().getId()).isEqualTo(expectedPlayer.getId());
        assertThat(score.getWeekStart()).isEqualTo(WEEK_START);
        assertThat(score.getGuardianDamage()).isZero();
        assertThat(score.getChallengePoints()).isEqualTo(expectedChallengePoints);
        assertThat(score.getTotalPoints()).isEqualTo(expectedChallengePoints);
        assertThat(score.getCompletedChallenges()).isEqualTo(expectedCompletedWeekly);
        assertThat(score.getCompletedDailyChallenges()).isEqualTo(expectedCompletedDaily);
        assertThat(score.getPosition()).isEqualTo(expectedPosition);
        assertThat(score.getPreviousPosition()).isEqualTo(expectedPreviousPosition);
        assertThat(score.getCalculatedAt()).isEqualTo(CALCULATION_TIME);
        assertThat(score.getFinalizedAt()).isNull();
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
            return Clock.fixed(CALCULATION_TIME, ZoneOffset.UTC);
        }
    }
}
