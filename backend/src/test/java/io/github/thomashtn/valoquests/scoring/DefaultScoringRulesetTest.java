package io.github.thomashtn.valoquests.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the barème in force, and the balance properties it exists to produce.
 */
@DisplayName("Scoring ruleset")
class DefaultScoringRulesetTest {

    /** Reference the gameplay document works its examples on. */
    private static final int DOCUMENT_REFERENCE = 5_300;

    /** Ruleset under test. */
    private final DefaultScoringRuleset ruleset = new DefaultScoringRuleset();

    @Test
    void shouldPriceEveryValuedModeAndLeaveTheRestAtZero() {
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.LOSS)).isEqualTo(350);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.DRAW)).isEqualTo(425);
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.WIN)).isEqualTo(500);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.LOSS)).isEqualTo(320);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.DRAW)).isEqualTo(390);
        assertThat(ruleset.matchDamage(GameMode.UNRATED, MatchOutcome.WIN)).isEqualTo(460);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.LOSS)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.DRAW)).isEqualTo(135);
        assertThat(ruleset.matchDamage(GameMode.TEAM_DEATHMATCH, MatchOutcome.WIN)).isEqualTo(160);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.LOSS)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.WIN)).isEqualTo(150);
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.LOSS)).isEqualTo(100);
        assertThat(ruleset.matchDamage(GameMode.DEATHMATCH, MatchOutcome.WIN)).isEqualTo(150);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.LOSS)).isEqualTo(90);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.DRAW)).isEqualTo(110);
        assertThat(ruleset.matchDamage(GameMode.SKIRMISH, MatchOutcome.WIN)).isEqualTo(130);

        // Imported but not part of the competition, so worth nothing.
        assertThat(ruleset.matchDamage(GameMode.SWIFTPLAY, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.OTHER, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(null, MatchOutcome.WIN)).isZero();
        assertThat(ruleset.matchDamage(GameMode.COMPETITIVE, null)).isZero();
    }

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
        assertThat(ruleset.matchDamage(GameMode.SPIKE_RUSH, MatchOutcome.DRAW)).isEqualTo(110);
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
        assertThat(ruleset.foodSharePercent(GameMode.SWIFTPLAY)).isZero();
        assertThat(ruleset.foodSharePercent(null)).isZero();
    }

    @Test
    void shouldWeighTheDailyChallengeBetweenEasyAndNormal() {
        assertThat(ruleset.challengeWeight(ChallengeCadence.DAILY, null)).isEqualTo(1.2);
        assertThat(ruleset.challengeWeight(ChallengeCadence.DAILY, ChallengeDifficulty.HARD)).isEqualTo(1.2);
        assertThat(ruleset.challengeWeight(ChallengeCadence.WEEKLY, ChallengeDifficulty.EASY)).isEqualTo(1.0);
        assertThat(ruleset.challengeWeight(ChallengeCadence.WEEKLY, ChallengeDifficulty.NORMAL)).isEqualTo(1.7);
        assertThat(ruleset.challengeWeight(ChallengeCadence.WEEKLY, ChallengeDifficulty.MEDIUM)).isEqualTo(2.7);
        assertThat(ruleset.challengeWeight(ChallengeCadence.WEEKLY, ChallengeDifficulty.HARD)).isEqualTo(3.9);
        assertThat(ruleset.challengeWeight(ChallengeCadence.WEEKLY, ChallengeDifficulty.VERY_HARD))
            .isEqualTo(5.4);
        assertThat(ruleset.challengeWeight(ChallengeCadence.WEEKLY, null)).isZero();
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
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.EASY)).isEqualTo(5);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.NORMAL)).isEqualTo(9);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.MEDIUM)).isEqualTo(14);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.HARD)).isEqualTo(21);
        assertThat(survivorsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.VERY_HARD)).isEqualTo(29);
    }

    @Test
    void shouldGrowSurvivorsWithTheCampaignWeek() {
        double weight = ruleset.challengeWeight(ChallengeCadence.WEEKLY, ChallengeDifficulty.VERY_HARD);

        assertThat(ruleset.challengeSurvivors(DOCUMENT_REFERENCE, weight, 10)).isEqualTo(39);
    }

    /**
     * The ranking points table of the gameplay document, at its reference of 5 300.
     */
    @Test
    void shouldPriceTheDocumentsRankingPointsPerChallenge() {
        assertThat(pointsOf(ChallengeCadence.DAILY, null)).isEqualTo(64);
        assertThat(pointsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.EASY)).isEqualTo(53);
        assertThat(pointsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.NORMAL)).isEqualTo(90);
        assertThat(pointsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.MEDIUM)).isEqualTo(143);
        assertThat(pointsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.HARD)).isEqualTo(207);
        assertThat(pointsOf(ChallengeCadence.WEEKLY, ChallengeDifficulty.VERY_HARD)).isEqualTo(286);
    }

    /**
     * A perfect week of challenges must weigh about a fifth of a median week of guardian damage, so
     * they stay a substantial bonus without ever becoming the way the ranking is won.
     */
    @Test
    void shouldKeepAPerfectWeekOfChallengesAroundAFifthOfTheRanking() {
        int perfectWeek = 7 * pointsOf(ChallengeCadence.DAILY, null);
        for (ChallengeDifficulty difficulty : ChallengeDifficulty.values()) {
            perfectWeek += pointsOf(ChallengeCadence.WEEKLY, difficulty);
        }

        assertThat(perfectWeek).isBetween(DOCUMENT_REFERENCE / 6, DOCUMENT_REFERENCE / 4);
    }

    @Test
    void shouldFloorTheReferenceAtTwoThousand() {
        assertThat(ruleset.referenceFloor()).isEqualTo(2_000);
    }

    private int survivorsOf(ChallengeCadence cadence, ChallengeDifficulty difficulty) {
        return ruleset.challengeSurvivors(DOCUMENT_REFERENCE, ruleset.challengeWeight(cadence, difficulty), 1);
    }

    private int pointsOf(ChallengeCadence cadence, ChallengeDifficulty difficulty) {
        return ruleset.challengeRankingPoints(DOCUMENT_REFERENCE, ruleset.challengeWeight(cadence, difficulty));
    }
}
