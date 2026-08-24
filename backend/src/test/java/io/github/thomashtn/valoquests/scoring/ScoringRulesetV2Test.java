package io.github.thomashtn.valoquests.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.scoring.model.MatchOutcome;
import org.junit.jupiter.api.Test;

/**
 * Pins the version 2 barèmes, and the balance properties they exist to produce.
 */
class ScoringRulesetV2Test {

    /** Ruleset under test. */
    private final ScoringRulesetV2 ruleset = new ScoringRulesetV2();

    /** Average value of a competitive match over an even win/loss split. */
    private static final int AVERAGE_COMPETITIVE_DAMAGE = 425;

    @Test
    void shouldResolveAsVersionTwo() {
        assertThat(ruleset.version()).isEqualTo(2);
    }

    @Test
    void shouldKeepVersionOneMatchDamageValues() {
        ScoringRulesetV1 previous = new ScoringRulesetV1();

        for (GameMode gameMode : GameMode.values()) {
            for (MatchOutcome outcome : MatchOutcome.values()) {
                assertThat(ruleset.matchDamage(gameMode, outcome))
                    .as("%s / %s", gameMode, outcome)
                    .isEqualTo(previous.matchDamage(gameMode, outcome));
            }
        }
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
    void shouldKeepVersionOneChallengeDamageValues() {
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.EASY)).isEqualTo(1_500);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.NORMAL)).isEqualTo(2_500);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.MEDIUM)).isEqualTo(4_000);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.HARD)).isEqualTo(6_000);
        assertThat(ruleset.challengeDamage(ChallengeDifficulty.VERY_HARD)).isEqualTo(9_000);
    }

    @Test
    void shouldDoubleTheRegularityLadder() {
        ScoringRulesetV1 previous = new ScoringRulesetV1();

        for (int activeDays = 0; activeDays <= 7; activeDays++) {
            assertThat(ruleset.regularityBonus(activeDays))
                .as("%d active day(s)", activeDays)
                .isEqualTo(previous.regularityBonus(activeDays) * 2);
        }
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
        assertThat(ruleset.challengeTeamBonus(difficulty, 2)).isEqualTo(400);
        assertThat(ruleset.challengeTeamBonus(difficulty, 3)).isEqualTo(800);
        assertThat(ruleset.challengeTeamBonus(difficulty, 4)).isEqualTo(1_200);
        assertThat(ruleset.challengeTeamBonus(difficulty, 5)).isEqualTo(1_600);
        assertThat(ruleset.challengeTeamBonus(difficulty, 6)).isEqualTo(2_000);
    }

    @Test
    void shouldCapTeamBonusAtHalfTheChallengeDamage() {
        assertThat(ruleset.challengeTeamBonus(ChallengeDifficulty.VERY_HARD, 7))
            .isEqualTo(4_500)
            .isEqualTo(ruleset.challengeTeamBonus(ChallengeDifficulty.VERY_HARD, 6));
    }

    @Test
    void shouldScaleTeamBonusWithTheChallengeStake() {
        assertThat(ruleset.challengeTeamBonus(ChallengeDifficulty.EASY, 6)).isEqualTo(750);
        assertThat(ruleset.challengeTeamBonus(ChallengeDifficulty.VERY_HARD, 6)).isEqualTo(4_500);
    }

    @Test
    void shouldSizeBossHitPointsOnTheActiveRoster() {
        assertThat(ruleset.bossBaseHp(BossCategory.MINOR, 7)).isEqualTo(91_000);
        assertThat(ruleset.bossBaseHp(BossCategory.STANDARD, 7)).isEqualTo(112_000);
        assertThat(ruleset.bossBaseHp(BossCategory.ELITE, 7)).isEqualTo(140_000);
        assertThat(ruleset.bossBaseHp(BossCategory.STANDARD, 4)).isEqualTo(64_000);
    }

    @Test
    void shouldNeverSizeABossBelowASinglePlayer() {
        assertThat(ruleset.bossBaseHp(BossCategory.MINOR, 0))
            .isEqualTo(ruleset.bossBaseHp(BossCategory.MINOR, 1));
    }

    @Test
    void shouldMoveTheDifficultyModifierSymmetrically() {
        assertThat(ruleset.initialDifficultyModifierPercent()).isEqualTo(100);
        assertThat(ruleset.nextDifficultyModifierPercent(100, true)).isEqualTo(107);
        assertThat(ruleset.nextDifficultyModifierPercent(100, false)).isEqualTo(93);
        assertThat(ruleset.nextDifficultyModifierPercent(128, true)).isEqualTo(130);
        assertThat(ruleset.nextDifficultyModifierPercent(72, false)).isEqualTo(70);
    }

    @Test
    void shouldReturnToNeutralAfterAlternatingOutcomes() {
        int modifier = ruleset.initialDifficultyModifierPercent();
        modifier = ruleset.nextDifficultyModifierPercent(modifier, false);
        modifier = ruleset.nextDifficultyModifierPercent(modifier, true);

        assertThat(modifier).isEqualTo(100);
    }

    @Test
    void shouldCarryASurvivingBossRemainderUpToAQuarterOfTheNextFight() {
        assertThat(ruleset.carriedOverHpCapPercent()).isEqualTo(25);
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
