package io.github.thomashtn.valoquests.colony;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyTier;
import io.github.thomashtn.valoquests.colony.model.ColonyTierName;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import org.junit.jupiter.api.Test;

/**
 * Tests the colony's calibration against the numbers the design document states.
 *
 * <p>Every figure asserted here is printed in that document's table of constants. The point is not to
 * restate the implementation but to make a rebalancing visible: moving one of these has to move a test
 * with it.
 */
class DefaultColonyRulesetTest {

    /** Tolerance for the double arithmetic. */
    private static final double TOLERANCE = 1e-9;

    /** Calibration under test. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /**
     * Verifies one average competitive game is worth five food, which is the sentence the whole
     * production side of the model is calibrated around.
     */
    @Test
    void shouldPriceAnAverageCompetitiveGameAtFiveFood() {
        assertThat(ruleset.foodDamageDivisor()).isEqualTo(85);
        assertThat(ruleset.referenceMatchDamage() / (double) ruleset.foodDamageDivisor())
            .isEqualTo(5.0, within(0.01));
    }

    /**
     * Verifies a run opens on the base efficiency and that materials raise it, without a ceiling.
     *
     * <p>The absence of a ceiling is the point of the whole mechanism: a challenge validated on the last
     * Monday of a run is worth exactly what one validated on the first was. The housing ceiling this
     * replaced was worth 0.2% of a run's score because it never bound on the day the score was read.
     */
    @Test
    void shouldOpenOnTheBaseEfficiencyAndRaiseItWithMaterials() {
        assertThat(ruleset.efficiencyFor(0, 7)).isEqualTo(8.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(1_050, 7)).isEqualTo(9.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(2_100, 7)).isEqualTo(10.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(100_000, 7)).isGreaterThan(90.0);
    }

    /**
     * Verifies the same materials per player buy the same efficiency at any roster size.
     *
     * <p>This is what keeps the balance identical from two players to twenty. Challenges and bosses
     * already pay per player, so dividing by the roster before converting is what stops a large squad
     * from climbing the ladder faster than a small one for the same effort each.
     */
    @Test
    void shouldBuyTheSameEfficiencyAtEveryRosterSize() {
        assertThat(ruleset.efficiencyFor(300, 2)).isEqualTo(9.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(450, 3)).isEqualTo(9.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(1_050, 7)).isEqualTo(9.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(3_000, 20)).isEqualTo(9.0, within(TOLERANCE));
    }

    /**
     * Verifies an empty roster is priced at the base efficiency rather than dividing by zero.
     */
    @Test
    void shouldPriceAnEmptyRosterAtTheBaseEfficiency() {
        assertThat(ruleset.efficiencyFor(1_000, 0)).isEqualTo(8.0, within(TOLERANCE));
        assertThat(ruleset.efficiencyFor(-50, 7)).isEqualTo(8.0, within(TOLERANCE));
    }

    /**
     * Verifies a challenge is worth what the design document's table says, per player who completed it.
     *
     * <p>Derived from the scoring ruleset rather than restated, so the colony cannot drift from the
     * ranking on what a {@code HARD} is worth.
     */
    @Test
    void shouldPriceChallengesFromTheScoringBareme() {
        assertThat(ruleset.materialsForChallenge(ChallengeDifficulty.EASY)).isEqualTo(8);
        assertThat(ruleset.materialsForChallenge(ChallengeDifficulty.NORMAL)).isEqualTo(14);
        assertThat(ruleset.materialsForChallenge(ChallengeDifficulty.MEDIUM)).isEqualTo(22);
        assertThat(ruleset.materialsForChallenge(ChallengeDifficulty.HARD)).isEqualTo(32);
        assertThat(ruleset.materialsForChallenge(ChallengeDifficulty.VERY_HARD)).isEqualTo(45);
    }

    /**
     * Verifies a fight pays per player of the frozen roster, which is what keeps the reward in step with
     * the squad it was sized against.
     */
    @Test
    void shouldPriceAFightPerPlayerOfTheFrozenRoster() {
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.MINOR, 7)).isEqualTo(280);
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.STANDARD, 7)).isEqualTo(560);
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.ELITE, 7)).isEqualTo(980);
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.STANDARD, 3)).isEqualTo(240);
    }

    /**
     * The reason the gap between the classes is wide: the campaign now schedules exactly two elite
     * fights per run, and those two weeks are meant to decide how far the town gets. A class worth
     * barely more than the one below it would have made them ordinary weeks with a scarier name.
     */
    @Test
    void shouldMakeAnEliteFightWorthSeveralMinorOnes() {
        int minor = ruleset.materialsForDefeatedBoss(BossCategory.MINOR, 7);
        int elite = ruleset.materialsForDefeatedBoss(BossCategory.ELITE, 7);

        assertThat(elite).isGreaterThanOrEqualTo(minor * 3);
    }

    /**
     * Verifies the morale a fight moves, and that a surviving boss costs more than a minor win pays.
     */
    @Test
    void shouldMoveMoraleOnlyByTheAmountsTheFightIsWorth() {
        assertThat(ruleset.moraleForDefeatedBoss(BossCategory.MINOR)).isEqualTo(3.0);
        assertThat(ruleset.moraleForDefeatedBoss(BossCategory.STANDARD)).isEqualTo(5.0);
        assertThat(ruleset.moraleForDefeatedBoss(BossCategory.ELITE)).isEqualTo(7.0);
        assertThat(ruleset.moraleForSurvivingBoss()).isEqualTo(-7.0);
    }

    /**
     * Verifies a flawless run reaches the morale ceiling on its <b>last</b> fight and never before.
     *
     * <p>The property the morale table exists for, and the one a rebalancing is most likely to break
     * silently. The ten scheduled fights pay, between them, exactly the distance from the opening
     * morale to the ceiling: offer more and the gauge tops out mid-run, after which the remaining
     * fights change nothing and the categories stop meaning anything. That is what the previous table
     * did — 150 morale poured into 50 points of room, ceiling reached on week four.
     */
    @Test
    void shouldReachTheMoraleCeilingOnTheLastFightOfAFlawlessRun() {
        ScoringRuleset scoringRuleset = new DefaultScoringRuleset();
        double morale = ruleset.initialMorale();

        for (int week = 1; week <= ruleset.runLengthWeeks(); week++) {
            assertThat(morale)
                .as("morale must still have room to move on week %d", week)
                .isLessThan(ruleset.maximumMorale());

            morale += ruleset.moraleForDefeatedBoss(scoringRuleset.bossCategoryForRunWeek(week));
        }

        assertThat(morale).isEqualTo(ruleset.maximumMorale(), within(TOLERANCE));
    }

    /**
     * Verifies an elite win repairs one loss exactly, which is what the loss is priced against.
     *
     * <p>Keeps the break-even win rate where the previous table put it: a squad taking the average
     * fight has to win about three out of five to hold its morale steady.
     */
    @Test
    void shouldPriceALossAtExactlyOneEliteWin() {
        assertThat(ruleset.moraleForSurvivingBoss())
            .isEqualTo(-ruleset.moraleForDefeatedBoss(BossCategory.ELITE), within(TOLERANCE));
    }

    /**
     * Verifies a null category is priced at nothing rather than blowing up, which is what a week whose
     * fight was never drawn hands in.
     */
    @Test
    void shouldPriceAnUndrawnFightAtNothing() {
        assertThat(ruleset.materialsForDefeatedBoss(null, 7)).isZero();
        assertThat(ruleset.moraleForDefeatedBoss(null)).isEqualTo(0.0);
    }

    /**
     * Verifies every squad opens its run on the ladder's first named step, whatever its size.
     *
     * <p>The ladder hangs on efficiency, which every run opens at 8 regardless of roster, so a
     * two-player squad and a twenty-player one both start on {@code CAMP}. Under the housing ladder they
     * did not: a squad of twenty opened on 6 000 places, already at {@code METROPOLIS}.
     */
    @Test
    void shouldOpenEverySquadOnTheFirstNamedStep() {
        assertThat(ruleset.tierFor(ruleset.efficiencyFor(0, 2)).name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(ruleset.tierFor(ruleset.efficiencyFor(0, 7)).name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(ruleset.tierFor(ruleset.efficiencyFor(0, 20)).name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(ruleset.tierFor(ruleset.efficiencyFor(0, 7)).level()).isZero();
    }

    /**
     * Verifies the ladder's names, one step every three quarters of a point of efficiency.
     *
     * <p>That step is what paces the ladder at one milestone a week: a regular run climbs from 8.00 to
     * 16.15, which crosses exactly ten of these.
     */
    @Test
    void shouldNameEveryStepOfTheLadder() {
        assertThat(ruleset.tierFor(8.00).name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(ruleset.tierFor(8.75).name()).isEqualTo(ColonyTierName.HAMLET);
        assertThat(ruleset.tierFor(9.50).name()).isEqualTo(ColonyTierName.VILLAGE);
        assertThat(ruleset.tierFor(10.25).name()).isEqualTo(ColonyTierName.BOROUGH);
        assertThat(ruleset.tierFor(11.00).name()).isEqualTo(ColonyTierName.TOWN);
        assertThat(ruleset.tierFor(11.75).name()).isEqualTo(ColonyTierName.CITY);
        assertThat(ruleset.tierFor(12.50).name()).isEqualTo(ColonyTierName.RESIDENTIAL_QUARTER);
        assertThat(ruleset.tierFor(13.25).name()).isEqualTo(ColonyTierName.GREAT_CITY);
        assertThat(ruleset.tierFor(14.00).name()).isEqualTo(ColonyTierName.METROPOLIS);
        assertThat(ruleset.tierFor(14.75).name()).isEqualTo(ColonyTierName.MEGALOPOLIS);
        assertThat(ruleset.tierFor(15.50).name()).isEqualTo(ColonyTierName.CAPITAL);
    }

    /**
     * Verifies where a regular run lands, which is the calibration the tier step was chosen against.
     */
    @Test
    void shouldLandARegularRunOnTheTenthStep() {
        ColonyTier tier = ruleset.tierFor(16.15);

        assertThat(tier.step()).isEqualTo(10);
        assertThat(tier.name()).isEqualTo(ColonyTierName.CAPITAL);
    }

    /**
     * Verifies the ladder never ends: past its last name it repeats, numbered, so a squad that keeps
     * building always has a next name to climb towards.
     */
    @Test
    void shouldRepeatTheLastNameNumberedForever() {
        assertThat(ruleset.tierFor(16.25)).isEqualTo(
            new ColonyTier(11, 16.25, ColonyTierName.CITADEL, 1)
        );
        assertThat(ruleset.tierFor(17.00)).isEqualTo(
            new ColonyTier(12, 17.00, ColonyTierName.CITADEL, 2)
        );
        assertThat(ruleset.tierFor(100.0).name()).isEqualTo(ColonyTierName.CITADEL);
    }

    /**
     * Verifies an efficiency at or below the opening one still has a name rather than none.
     */
    @Test
    void shouldNameATownAtTheOpeningEfficiency() {
        ColonyTier tier = ruleset.tierFor(7.5);

        assertThat(tier.name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(tier.step()).isZero();
    }

    /**
     * Verifies the step the town is climbing towards is always the one immediately above it.
     */
    @Test
    void shouldPointAtTheStepImmediatelyAbove() {
        assertThat(ruleset.nextTierFor(8.0).threshold()).isEqualTo(8.75, within(TOLERANCE));
        assertThat(ruleset.nextTierFor(9.0).threshold()).isEqualTo(9.50, within(TOLERANCE));
    }

    /**
     * Verifies the ladder can be walked by index, which is what the page does to draw the steps around
     * the town's own.
     */
    @Test
    void shouldWalkTheLadderByStep() {
        assertThat(ruleset.tierAtStep(0).name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(ruleset.tierAtStep(4).name()).isEqualTo(ColonyTierName.TOWN);
        assertThat(ruleset.tierAtStep(4).threshold()).isEqualTo(11.0, within(TOLERANCE));
        assertThat(ruleset.tierAtStep(-3).name()).isEqualTo(ColonyTierName.CAMP);
    }

    /**
     * Verifies the bounds a run's morale lives between, and the speed the ceiling buys.
     *
     * <p>The floor sits at twenty rather than at one so that a wrecked run is never an absorbing state:
     * at one the town closed one percent of its gap a week and nothing the squad did started it again,
     * which is waiting rather than losing. At twenty it closes nineteen percent, and the punishment is
     * barely softened — two percent of final score on a squad that wasted its first three weeks.
     */
    @Test
    void shouldBoundMoraleAndSpeed() {
        assertThat(ruleset.initialMorale()).isEqualTo(50.0);
        assertThat(ruleset.minimumMorale()).isEqualTo(20.0);
        assertThat(ruleset.maximumMorale()).isEqualTo(100.0);
        assertThat(ruleset.gapClosingRatePercent()).isEqualTo(15.0, within(TOLERANCE));
    }

    /**
     * Verifies the turnout threshold and the length of the food window.
     */
    @Test
    void shouldStateTheTurnoutThresholdAndTheWindow() {
        assertThat(ruleset.presenceDamageThreshold()).isEqualTo(300);
        assertThat(ruleset.foodWindowDays()).isEqualTo(7);
        assertThat(ruleset.runLengthWeeks()).isEqualTo(10);
        assertThat(ruleset.efficiencyTierStep()).isEqualTo(0.75, within(TOLERANCE));
    }

    /**
     * Verifies a run opens on nothing at all.
     */
    @Test
    void shouldOpenARunOnNothing() {
        assertThat(ruleset.initialMaterials()).isZero();
        assertThat(ruleset.initialPopulation()).isZero();
    }

    /**
     * Verifies pricing a step in materials always buys at least the efficiency it was asked for.
     *
     * <p>The rounding has to go up: quoted one material short, a step would read as affordable on the
     * page and then fail to open, which is the one way a figure meant to be actionable can lie.
     */
    @Test
    void shouldPriceAStepInEnoughMaterialsToActuallyOpenIt() {
        for (int rosterSize : new int[] {1, 2, 5, 7, 20}) {
            for (int step = 1; step <= 12; step++) {
                double threshold = ruleset.tierAtStep(step).threshold();
                int materials = ruleset.materialsForEfficiency(threshold, rosterSize);

                assertThat(ruleset.efficiencyFor(materials, rosterSize))
                    .isGreaterThanOrEqualTo(threshold);
            }
        }
    }

    /**
     * Verifies a step costs proportionally more to a larger squad, which is what keeps the ladder
     * independent of roster size: materials are earned per player, so a step has to be priced per
     * player too.
     */
    @Test
    void shouldPriceAStepPerPlayerOfTheRoster() {
        double threshold = ruleset.tierAtStep(4).threshold();

        assertThat(ruleset.materialsForEfficiency(threshold, 14))
            .isEqualTo(2 * ruleset.materialsForEfficiency(threshold, 7));
    }

    /**
     * Verifies the two ends of the pricing: the opening step is free, and an empty roster is priced at
     * nothing rather than dividing by it.
     */
    @Test
    void shouldPriceTheOpeningStepAndAnEmptyRosterAtNothing() {
        assertThat(ruleset.materialsForEfficiency(ruleset.tierAtStep(0).threshold(), 7)).isZero();
        assertThat(ruleset.materialsForEfficiency(4.0, 7)).isZero();
        assertThat(ruleset.materialsForEfficiency(12.0, 0)).isZero();
    }
}
