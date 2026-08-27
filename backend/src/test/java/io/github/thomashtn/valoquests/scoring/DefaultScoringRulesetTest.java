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

    /** Weeks a run spans, the span the difficulty ladder is written over. */
    private static final int RUN_LENGTH_WEEKS = 10;

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

        assertThat(ruleset.bossHitPoints(BossCategory.MINOR, 7, reference)).isEqualTo(45_500);
        assertThat(ruleset.bossHitPoints(BossCategory.STANDARD, 7, reference)).isEqualTo(59_500);
        assertThat(ruleset.bossHitPoints(BossCategory.ELITE, 7, reference)).isEqualTo(73_500);
        assertThat(ruleset.bossHitPoints(BossCategory.STANDARD, 4, reference)).isEqualTo(34_000);
    }

    /**
     * Encodes why every weight sits at or below the measured reference.
     *
     * <p>The reference is the squad's own recent median, so asking for exactly it is a win by zero
     * margin that week-to-week noise decides rather than effort — and an elite boss well above it is
     * unwinnable by construction for a squad that is merely regular. A standard boss must therefore
     * leave room for one slow week, and an elite one must ask for a push rather than a miracle.
     */
    @Test
    void shouldLeaveAStandardBossRoomForOneSlowWeek() {
        int reference = 12_345;

        int minor = ruleset.bossHitPoints(BossCategory.MINOR, 1, reference);
        int standard = ruleset.bossHitPoints(BossCategory.STANDARD, 1, reference);
        int elite = ruleset.bossHitPoints(BossCategory.ELITE, 1, reference);

        assertThat(standard).isLessThan(reference);
        assertThat(minor).isLessThan(standard);
        assertThat(elite).isGreaterThan(reference);

        // An elite boss asks for a push, not a miracle: a fifth above the median would be out of a
        // regular squad's reach whatever it did, which is what made the category a guaranteed loss.
        assertThat(elite).isLessThan((int) (reference * 1.1));
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
    void shouldWalkTheRunThroughItsDifficultyLadder() {
        assertThat(ruleset.bossCategoryForRunWeek(1)).isEqualTo(BossCategory.MINOR);
        assertThat(ruleset.bossCategoryForRunWeek(2)).isEqualTo(BossCategory.STANDARD);
        assertThat(ruleset.bossCategoryForRunWeek(3)).isEqualTo(BossCategory.STANDARD);
        assertThat(ruleset.bossCategoryForRunWeek(4)).isEqualTo(BossCategory.STANDARD);
        assertThat(ruleset.bossCategoryForRunWeek(5)).isEqualTo(BossCategory.ELITE);
        assertThat(ruleset.bossCategoryForRunWeek(6)).isEqualTo(BossCategory.MINOR);
        assertThat(ruleset.bossCategoryForRunWeek(7)).isEqualTo(BossCategory.STANDARD);
        assertThat(ruleset.bossCategoryForRunWeek(8)).isEqualTo(BossCategory.STANDARD);
        assertThat(ruleset.bossCategoryForRunWeek(9)).isEqualTo(BossCategory.STANDARD);
        assertThat(ruleset.bossCategoryForRunWeek(10)).isEqualTo(BossCategory.ELITE);
    }

    /**
     * The shape of the ladder, stated as the rules state it: a peak is always followed by a breather,
     * and a run always opens on one. Week one counts as following the previous run's closing elite,
     * which is why the ladder wraps rather than starting mid-slope.
     */
    @Test
    void shouldFollowEveryEliteWithAMinor() {
        assertThat(ruleset.bossCategoryForRunWeek(1)).isEqualTo(BossCategory.MINOR);

        for (int week = 1; week < RUN_LENGTH_WEEKS; week++) {
            if (ruleset.bossCategoryForRunWeek(week) == BossCategory.ELITE) {
                assertThat(ruleset.bossCategoryForRunWeek(week + 1)).isEqualTo(BossCategory.MINOR);
            }
        }
    }

    /**
     * The ladder is read by whoever holds a week index, and nothing guarantees that index stays inside
     * the run: a week drawn before its run was opened, or a run length shortened under a campaign in
     * progress, would both land outside. Clamping keeps that a duller boss rather than a crash.
     */
    @Test
    void shouldClampWeekIndexesOutsideTheRun() {
        assertThat(ruleset.bossCategoryForRunWeek(0)).isEqualTo(ruleset.bossCategoryForRunWeek(1));
        assertThat(ruleset.bossCategoryForRunWeek(-3)).isEqualTo(ruleset.bossCategoryForRunWeek(1));
        assertThat(ruleset.bossCategoryForRunWeek(99))
            .isEqualTo(ruleset.bossCategoryForRunWeek(RUN_LENGTH_WEEKS));
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
