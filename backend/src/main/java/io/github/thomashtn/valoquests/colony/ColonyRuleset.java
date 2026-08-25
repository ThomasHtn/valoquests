package io.github.thomashtn.valoquests.colony;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyBuildingTier;
import java.util.List;

/**
 * Every number the colony is calibrated with.
 *
 * <p>On the exact model of {@link io.github.thomashtn.valoquests.scoring.ScoringRuleset}, and like it
 * <b>without versioning and without a registry</b>: {@code V27__drop_ruleset_version.sql} removed that
 * mechanism from the project after it made a week's barème depend on a side effect. A rebalancing edits
 * the implementation and propagates to the replay of the run in progress; closed runs keep their
 * snapshots as they are.
 *
 * <p>There is deliberately no anti-farming rule here. The daily diminishing returns and the regularity
 * bonus already exist upstream in the scoring ruleset and apply before the colony reads anything. The
 * colony consumes their output, it never recomputes it.
 */
public interface ColonyRuleset {

    /**
     * Returns how many weekly rollovers a run spans.
     *
     * @return number of weeks in a run
     */
    int runLengthWeeks();

    /**
     * Returns the divisor turning a day's match damage into Food.
     *
     * <p>A competitive win is worth 500 damage in the scoring ruleset, so it yields exactly one point
     * of Food. No barème is created here: the damage read is the one produced <i>after</i> the daily
     * diminishing returns, so the colony and the weekly ranking price a given match identically.
     *
     * @return match damage worth one point of Food
     */
    int foodDamageDivisor();

    /**
     * Returns the coefficient governing both gauges' daily loss, applied to {@code population /
     * capacity}.
     *
     * <p>The same number as {@link #maximumEnergyGain()}, which is what reduces the whole model to one
     * sentence: a full colony is the seven players present every day, roughly two competitive games
     * each.
     *
     * @return daily loss coefficient
     */
    double dailyLossCoefficient();

    /**
     * Returns the Energy gained on a day when the whole roster played.
     *
     * @return maximum daily Energy gain
     */
    double maximumEnergyGain();

    /**
     * Returns the value both gauges are hard-clamped at, surplus discarded.
     *
     * @return gauge ceiling
     */
    double gaugeMaximum();

    /**
     * Returns the materials one player earns by completing a challenge of a difficulty.
     *
     * <p>Derived from the scoring ruleset's challenge damage rather than restated, so the colony cannot
     * drift from the ranking on what a {@code HARD} is worth.
     *
     * @param difficulty challenge difficulty
     * @return materials credited per player who completed it
     */
    int materialsForChallenge(ChallengeDifficulty difficulty);

    /**
     * Returns the materials a defeated boss brings in.
     *
     * <p>A surviving boss brings nothing; that is its entire cost. Materials are permanent and raise
     * capacity, hence the ceiling of the final score, which is what makes all ten bosses count equally.
     *
     * @return materials credited for a defeated boss
     */
    int materialsPerDefeatedBoss();

    /**
     * Returns the building tiers, ordered from the cheapest to the most expensive.
     *
     * @return ordered building tiers, the first one costing nothing
     */
    List<ColonyBuildingTier> buildings();

    /**
     * Returns the capacity a cumulative materials total unlocks.
     *
     * <p>A pure function of materials: no erection event to schedule, no list of buildings to persist,
     * no ordering to guarantee.
     *
     * @param materials cumulative materials
     * @return capacity of the highest tier reached
     */
    int capacityFor(int materials);

    /**
     * Returns the capacity of the last building tier, which is a run's theoretical maximum score.
     *
     * @return highest reachable capacity
     */
    int maximumCapacity();

    /**
     * Returns the share of capacity the population may gain in one day.
     *
     * @return daily growth limit, as a percentage of capacity
     */
    double growthRatePercent();

    /**
     * Returns the share of capacity the population may lose in one day.
     *
     * <p>Twice {@link #growthRatePercent()}: a week of neglect costs twice what a week of effort brings
     * in. This asymmetry, together with the growth limit, is what makes it impossible to accelerate a
     * colony any way other than holding both gauges high day after day.
     *
     * @return daily decline limit, as a percentage of capacity
     */
    double declineRatePercent();

    /**
     * Returns the value both gauges open a run at.
     *
     * @return initial gauge value
     */
    double initialGauge();

    /**
     * Returns the materials a run opens with.
     *
     * @return initial materials
     */
    int initialMaterials();

    /**
     * Returns the population a run opens with.
     *
     * @return initial population
     */
    double initialPopulation();

    /**
     * Returns the health below which the colony is flagged as in distress.
     *
     * <p>Strictly a derived display flag, with no mechanical effect whatsoever. An earlier design made
     * it a persisted state with hysteresis and a third decay rate, which was a column, a branch in the
     * order of operations and a scheduling subtlety for an effect nobody measures.
     *
     * @return alert threshold, as a ratio in {@code [0, 1]}
     */
    double alertHealthThreshold();
}
