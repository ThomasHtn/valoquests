package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayState;
import io.github.thomashtn.valoquests.colony.model.ColonyEquilibrium;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Turns a run's days into the colony they produce.
 *
 * <p>Pure arithmetic over its arguments: no repository, no clock, no persistence. That is what lets the
 * whole model be unit-tested, and it is the reason the colony is <b>never mutated incrementally</b>. The
 * state is a function of four already-persisted inputs — imported matches, completed challenges, boss
 * outcomes and the run's frozen roster size — so recomputing it is always cheaper and safer than
 * carrying it forward:
 *
 * <ul>
 *   <li>a match played at 23:50 and imported the next morning is picked up rather than lost;</li>
 *   <li>three days of downtime are rebuilt on the next run rather than caught up by hand;</li>
 *   <li>an admin-triggered recompute cannot double-apply anything.</li>
 * </ul>
 *
 * <p>The order of operations is normative. Changing it changes the results.
 */
@Component
public class ColonyReplayEngine {

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * Days {@link #settle} runs before reading the plateau off.
     *
     * <p>Comfortably past it: the population moves by at most 2.5% of capacity a day, so it needs
     * forty days to cross a full capacity from empty, and the gauges converge faster than that.
     */
    private static final int SETTLING_DAYS = 400;

    /**
     * Calibration every step reads its numbers from.
     */
    private final ColonyRuleset ruleset;

    /**
     * Creates the replay engine.
     *
     * @param ruleset colony ruleset
     */
    public ColonyReplayEngine(ColonyRuleset ruleset) {
        this.ruleset = ruleset;
    }

    /**
     * Replays a run's days from its initial state, one state per day.
     *
     * <p>For each day, in this order:
     *
     * <ol>
     *   <li>both gauges lose {@code 14 x (population / capacity)}, floored at zero;</li>
     *   <li>both gauges take the day's gains and are hard-clamped at one hundred, surplus discarded;</li>
     *   <li>the rollover's materials are credited, on the one day a week that carries them;</li>
     *   <li>capacity is recomputed from the cumulative materials;</li>
     *   <li>health and target are recomputed, and the population migrates towards the target within
     *       its daily limits.</li>
     * </ol>
     *
     * <p>The first day is not a special case: the initial state is the state the colony is in before
     * that day is played, so the day's loss applies to it like any other.
     *
     * @param days       the run's days in chronological order, must not be {@code null}
     * @param rosterSize roster size frozen on the run, the Energy gauge's denominator
     * @return one state per supplied day, in the same order
     */
    public List<ColonyDayState> replay(List<ColonyDailyInput> days, int rosterSize) {
        double food = ruleset.initialGauge();
        double energy = ruleset.initialGauge();
        double population = ruleset.initialPopulation();
        int materials = ruleset.initialMaterials();
        int capacity = ruleset.capacityFor(materials);

        List<ColonyDayState> states = new ArrayList<>(days.size());

        for (ColonyDailyInput day : days) {
            double loss = dailyLoss(population, capacity);
            food = Math.max(0.0, food - loss);
            energy = Math.max(0.0, energy - loss);

            double foodGain = foodGain(day.matchDamage());
            double energyGain = energyGain(day.activePlayerCount(), rosterSize);
            food = capped(food + foodGain);
            energy = capped(energy + energyGain);

            materials += day.creditedMaterials();
            capacity = ruleset.capacityFor(materials);

            double health = health(food, energy);
            double target = targetPopulation(capacity, health);
            population = migrate(population, target, capacity);

            states.add(new ColonyDayState(
                day.day(),
                food,
                energy,
                materials,
                population,
                capacity,
                day.activePlayerCount(),
                foodGain,
                energyGain,
                loss,
                health,
                target
            ));
        }

        return states;
    }

    /**
     * Returns the state the colony settles on if a day's gains repeat indefinitely.
     *
     * <p>Simulated rather than solved. The closed form has a discontinuity where the two gains meet:
     * approach it with Food a hair below Energy and Food settles at {@code 100 x health²} while Energy
     * saturates, but make them exactly equal and both settle at {@code 100 x health}. Running the same
     * loop the replay runs sidesteps that entirely, and guarantees the figure the page shows is the one
     * the colony would actually reach rather than a second formula that has to be kept in step.
     *
     * @param foodGain   Food a day brings in
     * @param energyGain Energy a day brings in
     * @param capacity   capacity the colony settles inside
     * @return the settled gauges and population
     */
    public ColonyEquilibrium settle(double foodGain, double energyGain, int capacity) {
        double food = 0.0;
        double energy = 0.0;
        double population = 0.0;

        for (int day = 0; day < SETTLING_DAYS; day++) {
            double loss = dailyLoss(population, capacity);
            food = capped(Math.max(0.0, food - loss) + foodGain);
            energy = capped(Math.max(0.0, energy - loss) + energyGain);
            population = migrate(
                population,
                targetPopulation(capacity, health(food, energy)),
                capacity
            );
        }

        return new ColonyEquilibrium(food, energy, population);
    }

    /**
     * Returns a gauge clamped at its ceiling, surplus discarded.
     *
     * @param gauge gauge value before the ceiling applies
     * @return the gauge, never above the maximum
     */
    private double capped(double gauge) {
        return Math.min(ruleset.gaugeMaximum(), gauge);
    }

    /**
     * Returns the Food a day's match damage is worth.
     *
     * <p>No barème of its own: the damage handed in has already been through the scoring ruleset's
     * daily diminishing returns, so the colony inherits the anti-farming for free and prices a given
     * match exactly as the weekly ranking does.
     *
     * @param matchDamage total match damage of the day
     * @return Food gained
     */
    public double foodGain(int matchDamage) {
        return matchDamage / (double) ruleset.foodDamageDivisor();
    }

    /**
     * Returns the Energy a day's turnout is worth.
     *
     * <p>Proportional to the roster rather than a fixed amount per player, because the backoffice can
     * activate, deactivate or archive one: with an absolute value, a roster down to five would make a
     * full colony impossible to sustain and collapse the run's ceiling for no visible reason. The ratio
     * is capped at one, since a player left inactive still plays and would otherwise push the numerator
     * past the frozen roster.
     *
     * @param activePlayerCount distinct players who played at least one eligible match
     * @param rosterSize        roster size frozen on the run
     * @return Energy gained
     */
    public double energyGain(int activePlayerCount, int rosterSize) {
        if (rosterSize <= 0) {
            return 0.0;
        }

        return ruleset.maximumEnergyGain()
            * Math.min(1.0, activePlayerCount / (double) rosterSize);
    }

    /**
     * Returns the amount each gauge loses in a day.
     *
     * @param population population at the end of the previous day
     * @param capacity   capacity at the end of the previous day
     * @return loss applied to both gauges
     */
    public double dailyLoss(double population, int capacity) {
        if (capacity <= 0) {
            return 0.0;
        }

        return ruleset.dailyLossCoefficient() * (population / capacity);
    }

    /**
     * Returns the colony's health, the geometric mean of both gauges.
     *
     * <p>Geometric rather than arithmetic so neglecting one gauge costs more than an average would
     * suggest, and so a gauge at zero collapses the colony outright: 100 and 20 give 45%, not 60%, and
     * 100 and 0 give nothing at all.
     *
     * @param food   Food gauge
     * @param energy Energy gauge
     * @return health as a ratio in {@code [0, 1]}
     */
    public double health(double food, double energy) {
        double maximum = ruleset.gaugeMaximum();

        return Math.sqrt((food / maximum) * (energy / maximum));
    }

    /**
     * Returns the population the colony is heading towards.
     *
     * @param capacity current capacity
     * @param health   current health
     * @return target population
     */
    public double targetPopulation(int capacity, double health) {
        return capacity * health;
    }

    /**
     * Moves the population towards its target, within the day's asymmetric limits.
     *
     * <p>The most important mechanism in the system. No amount of grinding on a single day can produce
     * more than 2.5% of capacity in inhabitants, so there is <b>no</b> way to accelerate growth other
     * than holding both gauges high day after day. The 1-to-2 asymmetry makes a week of neglect cost
     * twice what a week of effort brings in.
     *
     * @param population current population
     * @param target     population being headed towards
     * @param capacity   current capacity
     * @return population after the day's migration
     */
    private double migrate(double population, double target, int capacity) {
        double growthLimit = capacity * ruleset.growthRatePercent() / PERCENT_SCALE;
        double declineLimit = capacity * ruleset.declineRatePercent() / PERCENT_SCALE;

        double delta = target - population;
        double moved = delta >= 0
            ? Math.min(delta, growthLimit)
            : Math.max(delta, -declineLimit);

        return Math.clamp(population + moved, 0.0, capacity);
    }
}
