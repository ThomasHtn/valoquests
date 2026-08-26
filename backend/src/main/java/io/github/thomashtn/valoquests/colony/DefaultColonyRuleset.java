package io.github.thomashtn.valoquests.colony;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyBuilding;
import io.github.thomashtn.valoquests.colony.model.ColonyBuildingTier;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The colony's calibration in force.
 *
 * <p>Tuned around one sentence: <b>a full colony is the seven players present every day, roughly two
 * competitive games each.</b> The same number, fourteen, governs both gauges' daily loss and the
 * maximum daily Energy gain, which is what makes that sentence exact rather than approximate.
 *
 * <p>The model self-regulates, and its fixed point is one line:
 * {@code equilibrium population = capacity x min(Food gain, Energy gain) / 14}. The weakest gauge alone
 * sets the population and the other one saturates, losing its surplus — which is what makes the hard
 * clamp on both gauges regret-free, and what gives the feature its anti-farming guarantee without a
 * single dedicated rule. Two players grinding ten games each produce more Food than four reasonable
 * ones and still cap at 29% of capacity, because Energy is what limits them.
 *
 * <p>Past roughly two games a day per player, Energy is what limits the colony in every regime the
 * squad actually plays. That is deliberate: turnout is the thing worth rewarding, and it is the one
 * number no amount of grinding on a single account can move.
 *
 * <p>A run opens on nothing at all — no population, both gauges at zero. An earlier calibration opened
 * on 300 inhabitants and both gauges at 50, which handed out a free half-health: the daily loss is
 * proportional to the population, so a small colony paid almost nothing while that inherited health
 * kept pulling it upwards. A run where <i>nobody ever played</i> grew from 300 to 870 inhabitants over
 * its first eight days before starting to fall. Opening at zero is what makes the first day of a run
 * obey the same rule as its fortieth.
 *
 * <p>Roughly fifteen of these numbers are calibrated a priori and should be read as an experimental
 * first pass; {@code colony_daily_snapshot} is deliberately rich enough to recalibrate them afterwards
 * without asking Henrik for anything again.
 */
@Component
public final class DefaultColonyRuleset implements ColonyRuleset {

    /**
     * Weekly rollovers a run spans.
     */
    private static final int RUN_LENGTH_WEEKS = 10;

    /**
     * Match damage worth one point of Food.
     *
     * <p>Below the 500 a competitive win is priced at, so two games a day already produce slightly more
     * Food than the turnout they came with produces Energy. That one-sided margin is what puts Energy in
     * charge of the population in every ordinary regime.
     */
    private static final int FOOD_DAMAGE_DIVISOR = 400;

    /**
     * Daily loss coefficient of both gauges, and the maximum daily Energy gain.
     */
    private static final double LOSS_AND_MAXIMUM_ENERGY_GAIN = 14.0;

    /**
     * Value both gauges are hard-clamped at.
     */
    private static final double GAUGE_MAXIMUM = 100.0;

    /**
     * Divisor turning a challenge's scoring damage into the materials it is worth per player.
     */
    private static final int CHALLENGE_DAMAGE_TO_MATERIALS_DIVISOR = 100;

    /**
     * Materials a defeated boss brings in, about a third of a perfect run's total.
     */
    private static final int MATERIALS_PER_DEFEATED_BOSS = 400;

    /**
     * Share of capacity the population may gain in one day.
     */
    private static final double GROWTH_RATE_PERCENT = 2.5;

    /**
     * Share of capacity the population may lose in one day.
     */
    private static final double DECLINE_RATE_PERCENT = 5.0;

    /**
     * Value both gauges open a run at.
     *
     * <p>Zero, so the first day of a run is earned like any other. See this class's own documentation
     * for what a non-zero opening did to a run nobody played.
     */
    private static final double INITIAL_GAUGE = 0.0;

    /**
     * Materials a run opens with.
     */
    private static final int INITIAL_MATERIALS = 0;

    /**
     * Population a run opens with.
     *
     * <p>Empty ground. With both gauges also at zero, an unplayed day leaves the colony exactly where it
     * was, and the first inhabitants only arrive once somebody has fed it.
     */
    private static final double INITIAL_POPULATION = 0.0;

    /**
     * Health below which the colony is flagged as in distress.
     */
    private static final double ALERT_HEALTH_THRESHOLD = 0.25;

    /**
     * Building tiers, cheapest first.
     *
     * <p>The Citadel asks for roughly 85% challenge completion and eight bosses out of ten. It is a
     * prestige reward, calibrated to fall in week nine at the earliest, which leaves just enough time to
     * populate the capacity it opens.
     */
    private static final List<ColonyBuildingTier> BUILDINGS = List.of(
        new ColonyBuildingTier(ColonyBuilding.CAMP, 0, 3_000),
        new ColonyBuildingTier(ColonyBuilding.BARRACKS, 2_500, 4_200),
        new ColonyBuildingTier(ColonyBuilding.RESIDENTIAL_QUARTER, 6_200, 5_500),
        new ColonyBuildingTier(ColonyBuilding.CITADEL, 10_200, 7_000)
    );

    /**
     * Scoring barèmes the challenge materials are derived from.
     */
    private final ScoringRuleset scoringRuleset;

    /**
     * Creates the colony ruleset.
     *
     * @param scoringRuleset scoring ruleset supplying each difficulty's challenge damage
     */
    public DefaultColonyRuleset(ScoringRuleset scoringRuleset) {
        this.scoringRuleset = scoringRuleset;
    }

    @Override
    public int runLengthWeeks() {
        return RUN_LENGTH_WEEKS;
    }

    @Override
    public int foodDamageDivisor() {
        return FOOD_DAMAGE_DIVISOR;
    }

    @Override
    public int referenceMatchDamage() {
        return scoringRuleset.matchDamage(GameMode.COMPETITIVE, MatchOutcome.DRAW);
    }

    @Override
    public double dailyLossCoefficient() {
        return LOSS_AND_MAXIMUM_ENERGY_GAIN;
    }

    @Override
    public double maximumEnergyGain() {
        return LOSS_AND_MAXIMUM_ENERGY_GAIN;
    }

    @Override
    public double gaugeMaximum() {
        return GAUGE_MAXIMUM;
    }

    @Override
    public int materialsForChallenge(ChallengeDifficulty difficulty) {
        return scoringRuleset.challengeDamage(difficulty) / CHALLENGE_DAMAGE_TO_MATERIALS_DIVISOR;
    }

    @Override
    public int materialsPerDefeatedBoss() {
        return MATERIALS_PER_DEFEATED_BOSS;
    }

    @Override
    public List<ColonyBuildingTier> buildings() {
        return BUILDINGS;
    }

    @Override
    public int capacityFor(int materials) {
        int capacity = BUILDINGS.getFirst().capacity();

        for (ColonyBuildingTier tier : BUILDINGS) {
            if (materials >= tier.materialsThreshold()) {
                capacity = tier.capacity();
            }
        }

        return capacity;
    }

    @Override
    public int maximumCapacity() {
        return BUILDINGS.getLast().capacity();
    }

    @Override
    public double growthRatePercent() {
        return GROWTH_RATE_PERCENT;
    }

    @Override
    public double declineRatePercent() {
        return DECLINE_RATE_PERCENT;
    }

    @Override
    public double initialGauge() {
        return INITIAL_GAUGE;
    }

    @Override
    public int initialMaterials() {
        return INITIAL_MATERIALS;
    }

    @Override
    public double initialPopulation() {
        return INITIAL_POPULATION;
    }

    @Override
    public double alertHealthThreshold() {
        return ALERT_HEALTH_THRESHOLD;
    }
}
