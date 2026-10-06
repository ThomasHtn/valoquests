package io.github.thomashtn.valoquests.scoring.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.ScoredOutcome;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the scoring table in force, and the balance properties it exists to produce.
 */
@DisplayName("Scoring ruleset")
class DefaultScoringRulesetTest {

    /**
     * Reference the gameplay document works its examples on.
     */
    private static final int DOCUMENT_REFERENCE = 5_300;

    /**
     * Ruleset under test.
     */
    private final DefaultScoringRuleset ruleset = new DefaultScoringRuleset();

    @Test
    void shouldPriceEveryValuedModeAndLeaveTheRestAtZero() {
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, ScoredOutcome.LOSS)).isEqualTo(350);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, ScoredOutcome.DRAW)).isEqualTo(425);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, ScoredOutcome.WIN)).isEqualTo(500);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, ScoredOutcome.LOSS)).isEqualTo(320);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, ScoredOutcome.DRAW)).isEqualTo(390);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, ScoredOutcome.WIN)).isEqualTo(460);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, ScoredOutcome.LOSS)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, ScoredOutcome.DRAW)).isEqualTo(135);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, ScoredOutcome.WIN)).isEqualTo(160);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, ScoredOutcome.LOSS)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, ScoredOutcome.WIN)).isEqualTo(150);
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, ScoredOutcome.LOSS)).isEqualTo(100);
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, ScoredOutcome.WIN)).isEqualTo(150);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, ScoredOutcome.LOSS)).isEqualTo(90);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, ScoredOutcome.DRAW)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, ScoredOutcome.WIN)).isEqualTo(130);

        assertThat(ruleset.matchDamage(GameMode.SWIFTPLAY, ScoredOutcome.LOSS)).isEqualTo(160);
        assertThat(ruleset.matchDamage(GameMode.SWIFTPLAY, ScoredOutcome.WIN)).isEqualTo(230);
        assertThat(ruleset.matchDamage(GameMode.ESCALATION, ScoredOutcome.LOSS)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.ESCALATION, ScoredOutcome.WIN)).isEqualTo(150);

        // Imported but not part of the competition, so worth nothing.
        assertThat(ruleset.matchDamage(GameMode.NEW_MAP, ScoredOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.OTHER, ScoredOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(null, ScoredOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, null)).isZero();
    }

    @Test
    void shouldPricePremierLikeCompetitive() {
        for (ScoredOutcome outcome : ScoredOutcome.values()) {
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
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, ScoredOutcome.DRAW)).isEqualTo(100);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, ScoredOutcome.DRAW)).isEqualTo(110);
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
    void shouldGiveNothingOnTheFirstDayThenTwoPercentPerDayUpToTen() {
        assertThat(ruleset.streakBonusPercent(0)).isZero();
        assertThat(ruleset.streakBonusPercent(1)).isZero();
        assertThat(ruleset.streakBonusPercent(2)).isEqualTo(2);
        assertThat(ruleset.streakBonusPercent(3)).isEqualTo(4);
        assertThat(ruleset.streakBonusPercent(4)).isEqualTo(6);
        assertThat(ruleset.streakBonusPercent(5)).isEqualTo(8);
        assertThat(ruleset.streakBonusPercent(6)).isEqualTo(10);
        assertThat(ruleset.streakBonusPercent(12)).isEqualTo(10);
    }

    @Test
    void shouldLeanLongModesTowardsComponentsAndQuickModesTowardsFood() {
        assertThat(ruleset.foodSharePercent(GameMode.COMPETITIVE)).isEqualTo(30);
        assertThat(ruleset.foodSharePercent(GameMode.PREMIER)).isEqualTo(30);
        assertThat(ruleset.foodSharePercent(GameMode.UNRATED)).isEqualTo(30);
        assertThat(ruleset.foodSharePercent(GameMode.DEATHMATCH)).isEqualTo(70);
        assertThat(ruleset.foodSharePercent(GameMode.TEAM_DEATHMATCH)).isEqualTo(70);
        assertThat(ruleset.foodSharePercent(GameMode.SPIKE_RUSH)).isEqualTo(70);
        assertThat(ruleset.foodSharePercent(GameMode.SKIRMISH)).isEqualTo(70);
        assertThat(ruleset.foodSharePercent(GameMode.SWIFTPLAY)).isEqualTo(70);
        assertThat(ruleset.foodSharePercent(GameMode.ESCALATION)).isEqualTo(70);
        assertThat(ruleset.foodSharePercent(GameMode.NEW_MAP)).isZero();
        assertThat(ruleset.foodSharePercent(null)).isZero();
    }

    /**
     * Pins each weight through the reward, at a reference where one unit of weight is ten survivors.
     */
    @Test
    void shouldWeighTheDailyChallengeBetweenEasyAndNormal() {
        assertThat(tenTimesWeightOf(ChallengeCadence.DAILY, null)).isEqualTo(12);
        assertThat(tenTimesWeightOf(ChallengeCadence.DAILY, ChallengeTier.HARD)).isEqualTo(12);
        assertThat(tenTimesWeightOf(ChallengeCadence.WEEKLY, ChallengeTier.EASY)).isEqualTo(10);
        assertThat(tenTimesWeightOf(ChallengeCadence.WEEKLY, ChallengeTier.NORMAL)).isEqualTo(17);
        assertThat(tenTimesWeightOf(ChallengeCadence.WEEKLY, ChallengeTier.MEDIUM)).isEqualTo(27);
        assertThat(tenTimesWeightOf(ChallengeCadence.WEEKLY, ChallengeTier.HARD)).isEqualTo(39);
        assertThat(tenTimesWeightOf(ChallengeCadence.WEEKLY, ChallengeTier.VERY_HARD)).isEqualTo(54);
        assertThat(tenTimesWeightOf(ChallengeCadence.WEEKLY, null)).isZero();
    }

    @Test
    void shouldGrowRewardsByFourPercentAWeekLinearly() {
        assertThat(ruleset.rewardProgressionPercent(1)).isEqualTo(100);
        assertThat(ruleset.rewardProgressionPercent(2)).isEqualTo(104);
        assertThat(ruleset.rewardProgressionPercent(10)).isEqualTo(136);
        assertThat(ruleset.rewardProgressionPercent(0)).isEqualTo(100);
    }

    /**
     * The survivors table of the gameplay document, at its reference of 5 300 on the first week.
     */
    @Test
    void shouldRescueTheDocumentsSurvivorsPerChallenge() {
        assertThat(survivorsOf(ChallengeCadence.DAILY, null)).isEqualTo(6);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeTier.EASY)).isEqualTo(5);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeTier.NORMAL)).isEqualTo(9);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeTier.MEDIUM)).isEqualTo(14);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeTier.HARD)).isEqualTo(21);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeTier.VERY_HARD)).isEqualTo(29);
    }

    @Test
    void shouldGrowSurvivorsWithTheCampaignWeek() {
        assertThat(ruleset.challengeReward(ChallengeCadence.WEEKLY, ChallengeTier.VERY_HARD, calibrationAtWeek(10)))
            .isEqualTo(39);
    }

    private int tenTimesWeightOf(ChallengeCadence cadence, ChallengeTier tier) {
        return ruleset.challengeReward(
            cadence,
            tier,
            new ChallengeCalibration(10_000, 1, CampaignDifficulty.AMATEUR)
        );
    }

    private int survivorsOf(ChallengeCadence cadence, ChallengeTier tier) {
        return ruleset.challengeReward(cadence, tier, calibrationAtWeek(1));
    }

    private static ChallengeCalibration calibrationAtWeek(int weekIndex) {
        return new ChallengeCalibration(DOCUMENT_REFERENCE, weekIndex, CampaignDifficulty.AMATEUR);
    }
}
