package io.github.thomashtn.valoquests.colony;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyBuilding;
import io.github.thomashtn.valoquests.colony.model.ColonyBuildingTier;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the colony's calibration, barème by barème and tier by tier.
 */
class DefaultColonyRulesetTest {

    /** Scoring barèmes the challenge materials are derived from. */
    private static final ScoringRuleset SCORING = new DefaultScoringRuleset();

    /** Ruleset under test. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(SCORING);

    /**
     * Verifies that a run is ten weekly rollovers long.
     */
    @Test
    void shouldSpanTenWeeklyRollovers() {
        assertThat(ruleset.runLengthWeeks()).isEqualTo(10);
    }

    /**
     * Verifies that two games a day per player out-produce the Energy that turnout brings in.
     *
     * <p>The whole reason the divisor sits below the 500 a competitive win is priced at. It is what
     * puts Energy, and therefore turnout, in charge of the population in every ordinary regime: at two
     * games each the squad's Food gain clears its Energy gain, so the weak link is who showed up.
     */
    @Test
    void shouldLeaveTurnoutInChargeAtTwoGamesADay() {
        assertThat(ruleset.foodDamageDivisor()).isEqualTo(400);
        assertThat(ruleset.foodDamageDivisor()).isLessThan(500);
    }

    /**
     * Verifies that the same fourteen governs both losses and the maximum Energy gain.
     *
     * <p>What reduces the model to one sentence: a full colony is the seven players present every day,
     * roughly two competitive games each.
     */
    @Test
    void shouldGovernBothLossesAndMaximumEnergyGainWithTheSameNumber() {
        assertThat(ruleset.dailyLossCoefficient()).isEqualTo(14.0);
        assertThat(ruleset.maximumEnergyGain()).isEqualTo(ruleset.dailyLossCoefficient());
    }

    /**
     * Verifies each difficulty's materials, and that they are the scoring damage divided by a hundred.
     *
     * <p>A one-line derivation rather than a second barème to maintain: the colony cannot drift from
     * the ranking on what a {@code HARD} is worth.
     *
     * @param difficulty        challenge difficulty
     * @param expectedMaterials materials one player earns by completing it
     */
    @ParameterizedTest
    @CsvSource({
        "EASY, 8",
        "NORMAL, 14",
        "MEDIUM, 22",
        "HARD, 32",
        "VERY_HARD, 45"
    })
    void shouldDeriveChallengeMaterialsFromTheScoringBareme(
        ChallengeDifficulty difficulty,
        int expectedMaterials
    ) {
        assertThat(ruleset.materialsForChallenge(difficulty)).isEqualTo(expectedMaterials);
        assertThat(ruleset.materialsForChallenge(difficulty))
            .isEqualTo(SCORING.challengeDamage(difficulty) / 100);
    }

    /**
     * Verifies that a perfect week and a perfect run land on the figures the design was calibrated to.
     *
     * <p>Seven players, five challenges, one per difficulty: 847 materials a week, 8 470 over ten
     * weeks, plus 4 000 from ten defeated bosses, for a theoretical maximum of 12 470.
     */
    @Test
    void shouldAddUpToTheCalibratedRunMaximum() {
        int perfectWeek = 7 * (
            ruleset.materialsForChallenge(ChallengeDifficulty.EASY)
                + ruleset.materialsForChallenge(ChallengeDifficulty.NORMAL)
                + ruleset.materialsForChallenge(ChallengeDifficulty.MEDIUM)
                + ruleset.materialsForChallenge(ChallengeDifficulty.HARD)
                + ruleset.materialsForChallenge(ChallengeDifficulty.VERY_HARD)
        );

        assertThat(perfectWeek).isEqualTo(847);
        assertThat(perfectWeek * ruleset.runLengthWeeks()).isEqualTo(8_470);
        assertThat(ruleset.materialsPerDefeatedBoss() * ruleset.runLengthWeeks()).isEqualTo(4_000);
        assertThat(perfectWeek * ruleset.runLengthWeeks()
            + ruleset.materialsPerDefeatedBoss() * ruleset.runLengthWeeks())
            .isEqualTo(12_470);
    }

    /**
     * Verifies the four building tiers, their thresholds and the capacities they open.
     */
    @Test
    void shouldDeclareFourBuildingTiers() {
        assertThat(ruleset.buildings()).containsExactly(
            new ColonyBuildingTier(ColonyBuilding.CAMP, 0, 3_000),
            new ColonyBuildingTier(ColonyBuilding.BARRACKS, 2_500, 4_200),
            new ColonyBuildingTier(ColonyBuilding.RESIDENTIAL_QUARTER, 6_200, 5_500),
            new ColonyBuildingTier(ColonyBuilding.CITADEL, 10_200, 7_000)
        );
    }

    /**
     * Verifies that capacity is a pure function of materials, on both sides of every threshold.
     *
     * @param materials        cumulative materials
     * @param expectedCapacity capacity they unlock
     */
    @ParameterizedTest
    @CsvSource({
        "0, 3000",
        "2499, 3000",
        "2500, 4200",
        "6199, 4200",
        "6200, 5500",
        "10199, 5500",
        "10200, 7000",
        "12470, 7000"
    })
    void shouldDeriveCapacityFromMaterials(int materials, int expectedCapacity) {
        assertThat(ruleset.capacityFor(materials)).isEqualTo(expectedCapacity);
    }

    /**
     * Verifies that the last tier's capacity is a run's theoretical maximum score.
     */
    @Test
    void shouldCapARunAtTheCitadelsCapacity() {
        assertThat(ruleset.maximumCapacity()).isEqualTo(7_000);
        assertThat(ruleset.maximumCapacity()).isEqualTo(ruleset.buildings().getLast().capacity());
    }

    /**
     * Verifies that decline is exactly twice growth.
     *
     * <p>The asymmetry that makes a week of neglect cost twice what a week of effort brings in.
     */
    @Test
    void shouldMakeDeclineTwiceAsFastAsGrowth() {
        assertThat(ruleset.growthRatePercent()).isEqualTo(2.5);
        assertThat(ruleset.declineRatePercent()).isEqualTo(ruleset.growthRatePercent() * 2);
    }

    /**
     * Verifies that a run opens on empty ground.
     *
     * <p>Both gauges and the population at zero, so an unplayed first day leaves the colony exactly
     * where it was. A non-zero opening handed out a health nobody had earned, and since the daily loss
     * is proportional to the population, a colony nobody ever played grew for its first eight days.
     */
    @Test
    void shouldOpenARunOnEmptyGround() {
        assertThat(ruleset.initialGauge()).isZero();
        assertThat(ruleset.initialMaterials()).isZero();
        assertThat(ruleset.initialPopulation()).isZero();
        assertThat(ruleset.capacityFor(ruleset.initialMaterials())).isEqualTo(3_000);
    }

    /**
     * Verifies the health the distress flag is raised under.
     */
    @Test
    void shouldFlagDistressUnderAQuarterOfHealth() {
        assertThat(ruleset.alertHealthThreshold()).isEqualTo(0.25, within(1e-9));
    }
}
