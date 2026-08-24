package io.github.thomashtn.valoquests.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import org.junit.jupiter.api.Test;

/**
 * Pins the barèmes in force, and the balance properties they exist to produce.
 */
class DefaultScoringRulesetTest {

    /** Ruleset under test. */
    private final DefaultScoringRuleset ruleset = new DefaultScoringRuleset();

    /** Average value of a competitive match over an even win/loss split. */
    private static final int AVERAGE_COMPETITIVE_DAMAGE = 425;

    @Test
    void shouldPriceEveryValuedModeAndLeaveTheRestAtZero() {
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.WIN)).isEqualTo(500);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.DRAW)).isEqualTo(425);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.LOSS)).isEqualTo(350);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.WIN)).isEqualTo(400);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.WIN)).isEqualTo(180);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.WIN)).isEqualTo(170);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.WIN)).isEqualTo(160);
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.WIN)).isEqualTo(150);

        // Imported but not part of the competition, so worth nothing.
        assertThat(ruleset.matchDamage(GameMode.OTHER, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(null, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, null)).isZero();
    }

    /**
     * Premier is a five-stack competitive queue synchronization imports, so leaving it out of the
     * barème made it count as an active day while dealing nothing.
     */
    @Test
    void shouldPricePremierLikeCompetitive() {
        for (MatchOutcome outcome : MatchOutcome.values()) {
            assertThat(ruleset.matchDamage(GameMode.PREMIER, outcome))
                .as("%s", outcome)
                .isEqualTo(ruleset.matchDamage(GameMode.COMPETITIVE, outcome));
        }
    }

    /**
     * A draw is folded into the defeat tier for the two modes that cannot end on one, so an upstream
     * surprise degrades quietly instead of breaking the weekly calculation.
     */
    @Test
    void shouldFoldDrawsIntoDefeatsForModesThatCannotDraw() {
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.DRAW)).isEqualTo(100);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.DRAW)).isEqualTo(130);
    }

    @Test
    void shouldApplyDiminishingReturnsPastTheFifthMatchOfADay() {
        assertThat(ruleset.matchDamageCoefficientPercent(1)).isEqualTo(100);
        assertThat(ruleset.matchDamageCoefficientPercent(5)).isEqualTo(100);
        assertThat(ruleset.matchDamageCoefficientPercent(6)).isEqualTo(50);
        assertThat(ruleset.matchDamageCoefficientPercent(9)).isEqualTo(50);
        assertThat(ruleset.matchDamageCoefficientPercent(10)).isEqualTo(25);
        assertThat(ruleset.matchDamageCoefficientPercent(30)).isEqualTo(25);
    }

    @Test
    void shouldPriceChallengesByDifficulty() {
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.EASY)).isEqualTo(800);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.NORMAL)).isEqualTo(1_400);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.MEDIUM)).isEqualTo(2_200);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.HARD)).isEqualTo(3_200);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.VERY_HARD)).isEqualTo(4_500);
    }

    @Test
    void shouldPayNothingBelowTwoActiveDaysThenClimb() {
        assertThat(ruleset.regularityBonus(0)).isZero();
        assertThat(ruleset.regularityBonus(1)).isZero();
        assertThat(ruleset.regularityBonus(2)).isEqualTo(600);
        assertThat(ruleset.regularityBonus(3)).isEqualTo(1_400);
        assertThat(ruleset.regularityBonus(4)).isEqualTo(2_400);
        assertThat(ruleset.regularityBonus(5)).isEqualTo(3_600);
        assertThat(ruleset.regularityBonus(6)).isEqualTo(4_800);
    }

    @Test
    void shouldClampRegularityBonusBeyondSevenDays() {
        assertThat(ruleset.regularityBonus(7)).isEqualTo(6_000);
        assertThat(ruleset.regularityBonus(9)).isEqualTo(ruleset.regularityBonus(7));
    }

    @Test
    void shouldPriceTeamBonusAsTenPercentPerJoiningPlayer() {
        ChallengeDifficulty difficulty = ChallengeDifficulty.MEDIUM;

        assertThat(ruleset.challengeTeamBonus(difficulty, 0)).isZero();
        assertThat(ruleset.challengeTeamBonus(difficulty, 1)).isZero();
        assertThat(ruleset.challengeTeamBonus(difficulty, 2)).isEqualTo(220);
        assertThat(ruleset.challengeTeamBonus(difficulty, 3)).isEqualTo(440);
        assertThat(ruleset.challengeTeamBonus(difficulty, 4)).isEqualTo(660);
        assertThat(ruleset.challengeTeamBonus(difficulty, 5)).isEqualTo(880);
        assertThat(ruleset.challengeTeamBonus(difficulty, 6)).isEqualTo(1_100);
    }

    @Test
    void shouldCapTeamBonusAtHalfTheChallengeDamage() {
        assertThat(ruleset.challengeTeamBonus(ChallengeDifficulty.VERY_HARD, 7))
            .isEqualTo(2_250)
            .isEqualTo(ruleset.challengeTeamBonus(ChallengeDifficulty.VERY_HARD, 6));
    }

    @Test
    void shouldScaleTeamBonusWithTheChallengeStake() {
        assertThat(ruleset.challengeTeamBonus(ChallengeDifficulty.EASY, 6)).isEqualTo(400);
        assertThat(ruleset.challengeTeamBonus(ChallengeDifficulty.VERY_HARD, 6)).isEqualTo(2_250);
    }

    @Test
    void shouldSizeBossHitPointsOnTheActiveRosterAndTheMeasuredReference() {
        int reference = 10_000;

        assertThat(ruleset.bossHitPoints(BossCategory.MINOR, 7, reference)).isEqualTo(56_000);
        assertThat(ruleset.bossHitPoints(BossCategory.STANDARD, 7, reference)).isEqualTo(70_000);
        assertThat(ruleset.bossHitPoints(BossCategory.ELITE, 7, reference)).isEqualTo(87_500);
        assertThat(ruleset.bossHitPoints(BossCategory.STANDARD, 4, reference)).isEqualTo(40_000);
    }

    /**
     * The point of the categories once hit points are calibrated: a standard boss asks the roster to
     * repeat its own recent week, a minor one to fall short of it, an elite one to beat it.
     */
    @Test
    void shouldAskAStandardBossForExactlyTheMeasuredReference() {
        int reference = 12_345;

        assertThat(ruleset.bossHitPoints(BossCategory.STANDARD, 1, reference)).isEqualTo(reference);
        assertThat(ruleset.bossHitPoints(BossCategory.MINOR, 1, reference)).isLessThan(reference);
        assertThat(ruleset.bossHitPoints(BossCategory.ELITE, 1, reference)).isGreaterThan(reference);
    }

    /**
     * A fight must follow the roster's measured output, not a constant written months earlier.
     */
    @Test
    void shouldFollowTheReferenceItIsGiven() {
        int quiet = ruleset.bossHitPoints(BossCategory.STANDARD, 6, 4_000);
        int busy = ruleset.bossHitPoints(BossCategory.STANDARD, 6, 16_000);

        assertThat(busy).isEqualTo(quiet * 4);
    }

    @Test
    void shouldNeverSizeABossBelowASinglePlayer() {
        assertThat(ruleset.bossHitPoints(BossCategory.MINOR, 0, 10_000))
            .isEqualTo(ruleset.bossHitPoints(BossCategory.MINOR, 1, 10_000));
    }

    @Test
    void shouldExposeACalibrationBandAroundItsSeed() {
        assertThat(ruleset.seedReferenceDamagePerPlayer()).isEqualTo(10_000);
        assertThat(ruleset.calibrationWindowWeeks()).isEqualTo(4);
        assertThat(ruleset.calibrationFloorPercent()).isLessThan(100);
        assertThat(ruleset.calibrationCeilingPercent()).isGreaterThan(100);
    }

    /**
     * The property the whole rebalance exists for: a player spread over seven days must outscore one
     * who plays far more matches over three, once regularity is counted.
     */
    @Test
    void shouldRankTheRegularPlayerAboveTheGrinder() {
        int regular = weeklyMatchDamage(7, 2) + ruleset.regularityBonus(7);
        int grinder = weeklyMatchDamage(3, 12) + ruleset.regularityBonus(3);

        assertThat(regular).isGreaterThan(grinder);
    }

    /**
     * At equal volume, spreading matches over more days must pay more.
     */
    @Test
    void shouldRewardSpreadingTheSameVolumeOverMoreDays() {
        int spread = weeklyMatchDamage(7, 2) + ruleset.regularityBonus(7);
        int binged = weeklyMatchDamage(2, 7) + ruleset.regularityBonus(2);

        assertThat(spread).isGreaterThan(binged);
    }

    /**
     * Sums a week of competitive matches, applying the daily coefficient ladder.
     *
     * @param days           days played
     * @param matchesPerDay  matches played each of those days
     * @return total match damage for the week
     */
    private int weeklyMatchDamage(int days, int matchesPerDay) {
        int dailyDamage = 0;

        for (int rank = 1; rank <= matchesPerDay; rank++) {
            dailyDamage += Math.round(
                AVERAGE_COMPETITIVE_DAMAGE * ruleset.matchDamageCoefficientPercent(rank) / 100.0f
            );
        }

        return dailyDamage * days;
    }
}
