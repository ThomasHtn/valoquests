package io.github.thomashtn.valoquests.colony;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyTier;
import io.github.thomashtn.valoquests.colony.model.ColonyTierName;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
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
     * Verifies the one constant tying food to population works in both directions.
     */
    @Test
    void shouldTieFoodToPopulationWithASingleConstant() {
        assertThat(ruleset.inhabitantsPerFood()).isEqualTo(8);
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
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.MINOR, 7)).isEqualTo(420);
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.STANDARD, 7)).isEqualTo(560);
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.ELITE, 7)).isEqualTo(700);
        assertThat(ruleset.materialsForDefeatedBoss(BossCategory.STANDARD, 3)).isEqualTo(240);
    }

    /**
     * Verifies the morale a fight moves, and that a surviving boss costs more than a minor win pays.
     */
    @Test
    void shouldMoveMoraleOnlyByTheAmountsTheFightIsWorth() {
        assertThat(ruleset.moraleForDefeatedBoss(BossCategory.MINOR)).isEqualTo(10.0);
        assertThat(ruleset.moraleForDefeatedBoss(BossCategory.STANDARD)).isEqualTo(15.0);
        assertThat(ruleset.moraleForDefeatedBoss(BossCategory.ELITE)).isEqualTo(20.0);
        assertThat(ruleset.moraleForSurvivingBoss()).isEqualTo(-20.0);
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
     * Verifies the squad opens squarely inside its first named tier rather than on its edge.
     *
     * <p>Three hundred places per player is what puts a seven-player roster on 2 100, a hundred clear of
     * the 2 000 the first name sits on. At 285 it opened on 1 995 and its town had no name on day one.
     */
    @Test
    void shouldOpenTheSquadInsideItsFirstNamedTier() {
        assertThat(ruleset.capacityFor(7, 0)).isEqualTo(2_100);
        assertThat(ruleset.tierFor(ruleset.capacityFor(7, 0)).threshold()).isEqualTo(2_000);
        assertThat(ruleset.nextTierFor(ruleset.capacityFor(7, 0)).name())
            .isEqualTo(ColonyTierName.HAMLET);
    }

    /**
     * Verifies the opening housing stays strictly proportional to the roster, which is what keeps the
     * balance identical whatever size the squad is fielded at.
     */
    @Test
    void shouldOpenOnHousingProportionalToTheRoster() {
        assertThat(ruleset.capacityFor(3, 0)).isEqualTo(900);
        assertThat(ruleset.capacityFor(6, 0)).isEqualTo(1_800);
        assertThat(ruleset.capacityFor(20, 0)).isEqualTo(6_000);
    }

    /**
     * Verifies housing is continuous: two materials buy one place, with no threshold in between.
     *
     * <p>The design document's own worked state, 3 050 materials on a roster of seven, comes to 3 625.
     */
    @Test
    void shouldTurnTwoMaterialsIntoOnePlace() {
        assertThat(ruleset.housingForMaterials(3_050)).isEqualTo(1_525);
        assertThat(ruleset.capacityFor(7, 3_050)).isEqualTo(3_625);
    }

    /**
     * Verifies the surplus conversion: what the town cannot eat is turned into materials at a
     * deliberately bad rate.
     */
    @Test
    void shouldConvertOnlyTheFoodTheTownCannotEat() {
        // Housing for 4 000 needs 500 food; a stock of 600 leaves 100 over, worth 20 materials.
        assertThat(ruleset.materialsForSurplus(600.0, 4_000)).isEqualTo(20);
        assertThat(ruleset.materialsForSurplus(400.0, 4_000)).isZero();
    }

    /**
     * Verifies the ladder's names against the design document's table, including the open end where it
     * starts repeating.
     */
    @Test
    void shouldNameEveryStepOfTheLadder() {
        assertThat(ruleset.tierFor(2_000).name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(ruleset.tierFor(2_500).name()).isEqualTo(ColonyTierName.HAMLET);
        assertThat(ruleset.tierFor(3_000).name()).isEqualTo(ColonyTierName.VILLAGE);
        assertThat(ruleset.tierFor(3_625).name()).isEqualTo(ColonyTierName.BOROUGH);
        assertThat(ruleset.tierFor(4_000).name()).isEqualTo(ColonyTierName.TOWN);
        assertThat(ruleset.tierFor(4_500).name()).isEqualTo(ColonyTierName.CITY);
        assertThat(ruleset.tierFor(5_000).name()).isEqualTo(ColonyTierName.RESIDENTIAL_QUARTER);
        assertThat(ruleset.tierFor(5_500).name()).isEqualTo(ColonyTierName.GREAT_CITY);
        assertThat(ruleset.tierFor(6_000).name()).isEqualTo(ColonyTierName.METROPOLIS);
        assertThat(ruleset.tierFor(6_500).name()).isEqualTo(ColonyTierName.MEGALOPOLIS);
        assertThat(ruleset.tierFor(7_000).name()).isEqualTo(ColonyTierName.CAPITAL);
    }

    /**
     * Verifies the ladder never ends: past its last name it repeats, numbered, so a squad that keeps
     * building always has a next name to climb towards.
     */
    @Test
    void shouldRepeatTheLastNameNumberedForever() {
        assertThat(ruleset.tierFor(7_500)).isEqualTo(
            new ColonyTier(15, 7_500, ColonyTierName.CITADEL, 1)
        );
        assertThat(ruleset.tierFor(8_000)).isEqualTo(
            new ColonyTier(16, 8_000, ColonyTierName.CITADEL, 2)
        );
        assertThat(ruleset.tierFor(50_000).name()).isEqualTo(ColonyTierName.CITADEL);
    }

    /**
     * Verifies a town below the first named step still has a name. A three-player squad opens on 900
     * places, well under the 2 000 the document's table starts at, and a nameless town reads as a bug.
     */
    @Test
    void shouldNameATownBelowTheFirstNamedStep() {
        ColonyTier tier = ruleset.tierFor(ruleset.capacityFor(3, 0));

        assertThat(tier.name()).isEqualTo(ColonyTierName.CAMP);
        assertThat(tier.level()).isZero();
    }

    /**
     * Verifies the step the town is climbing towards is always the one immediately above it.
     */
    @Test
    void shouldPointAtTheStepImmediatelyAbove() {
        assertThat(ruleset.nextTierFor(3_625).threshold()).isEqualTo(4_000);
        assertThat(ruleset.nextTierFor(4_000).threshold()).isEqualTo(4_500);
    }

    /**
     * Verifies the bounds a run's morale lives between, and the speed the ceiling buys.
     */
    @Test
    void shouldBoundMoraleAndSpeed() {
        assertThat(ruleset.initialMorale()).isEqualTo(50.0);
        assertThat(ruleset.minimumMorale()).isEqualTo(1.0);
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
        assertThat(ruleset.tierStep()).isEqualTo(500);
    }

    /**
     * Verifies a run opens on nothing at all.
     */
    @Test
    void shouldOpenARunOnNothing() {
        assertThat(ruleset.initialMaterials()).isZero();
        assertThat(ruleset.initialPopulation()).isZero();
    }
}
