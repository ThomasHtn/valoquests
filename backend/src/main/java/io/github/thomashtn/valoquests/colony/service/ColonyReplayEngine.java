package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
     * <p>A rollover day settles the week that has just closed before its own night runs, in this order:
     *
     * <ol>
     *   <li>the week's completed challenges pay their materials;</li>
     *   <li>the fight pays its materials and its morale, or costs its morale if the boss held;</li>
     *   <li>efficiency is recomputed from the new materials total.</li>
     * </ol>
     *
     * <p>Then the night, on every day of the run without exception:
     *
     * <ol>
     *   <li>the day's harvest enters the stock, multiplied by the turnout;</li>
     *   <li>the harvest of seven days ago leaves it;</li>
     *   <li>the town moves a share of the way towards what its food can feed.</li>
     * </ol>
     *
     * <p>There is no fourth step, and the first day of a run is not a special case.
     *
     * @param days       the run's days in chronological order, must not be {@code null}
     * @param rosterSize roster size frozen on the run, which sets housing, turnout and boss rewards
     * @return one state per supplied day, in the same order
     */
    public List<ColonyDayState> replay(List<ColonyDailyInput> days, int rosterSize) {
        double population = ruleset.initialPopulation();
        double morale = ruleset.initialMorale();
        int materials = ruleset.initialMaterials();
        double efficiency = ruleset.efficiencyFor(materials, rosterSize);

        Deque<Double> window = new ArrayDeque<>();
        double foodStock = 0.0;

        List<ColonyDayState> states = new ArrayList<>(days.size());

        for (ColonyDailyInput day : days) {
            if (day.rollover()) {
                materials += day.creditedMaterials();
                morale = boundedMorale(morale + day.moraleDelta());
                efficiency = ruleset.efficiencyFor(materials, rosterSize);
            }

            double harvest = harvest(day.matchDamage(), day.presencePlayerCount(), rosterSize);
            window.addLast(harvest);
            foodStock += harvest;

            while (window.size() > ruleset.foodWindowDays()) {
                foodStock -= window.removeFirst();
            }

            double change = nightlyChange(population, foodStock, efficiency, morale);
            population = Math.max(0.0, population + change);

            states.add(new ColonyDayState(
                day.day(),
                foodStock,
                harvest,
                day.matchDamage(),
                day.presencePlayerCount(),
                morale,
                materials,
                efficiency,
                population,
                change
            ));
        }

        return states;
    }

    /**
     * Returns the food a day brings in, turnout included.
     *
     * <p>No barème of its own: the damage handed in has already been through the scoring ruleset's daily
     * diminishing returns, so the colony inherits the anti-farming for free and prices a given match
     * exactly as the weekly ranking does. It is also what makes a thirty-game evening worth far less
     * than six five-game ones without the colony writing a single anti-farming rule.
     *
     * @param matchDamage         total match damage of the day, after diminishing returns
     * @param presencePlayerCount players who cleared the turnout threshold
     * @param rosterSize          roster size frozen on the run
     * @return food harvested
     */
    public double harvest(int matchDamage, int presencePlayerCount, int rosterSize) {
        return matchDamage / (double) ruleset.foodDamageDivisor()
            * presenceMultiplier(presencePlayerCount, rosterSize);
    }

    /**
     * Returns the multiplier a day's turnout is worth.
     *
     * <p>The whole roster present doubles the day. Divided by the roster frozen on the run rather than
     * by a hard seven, so a five-player squad is not punished for having shrunk: five of five is still a
     * full house. Capped at one, since a player left out of the roster still plays and would otherwise
     * push the numerator past it.
     *
     * @param presencePlayerCount players who cleared the turnout threshold
     * @param rosterSize          roster size frozen on the run
     * @return multiplier applied to the day's harvest, between one and two
     */
    public double presenceMultiplier(int presencePlayerCount, int rosterSize) {
        if (rosterSize <= 0) {
            return 1.0;
        }

        return 1.0 + Math.min(1.0, Math.max(0, presencePlayerCount) / (double) rosterSize);
    }

    /**
     * Returns the inhabitants a food stock can feed.
     *
     * @param foodStock  food of the last seven days
     * @param efficiency inhabitants one point of food feeds
     * @return feedable population
     */
    public double feedablePopulation(double foodStock, double efficiency) {
        return foodStock * efficiency;
    }

    /**
     * Returns the food a population eats in a week.
     *
     * @param population current population
     * @param efficiency inhabitants one point of food feeds
     * @return weekly consumption
     */
    public double weeklyConsumption(double population, double efficiency) {
        return population / efficiency;
    }

    /**
     * Returns the population the town is heading towards.
     *
     * <p>One ceiling and one only, so nothing a squad does is ever wasted: every match raises it, and
     * every material raises what a match is worth. The housing ceiling this replaced never bound on the
     * day the score was read, which made the materials behind it worth 0.2% of a run.
     *
     * @param foodStock  food of the last seven days
     * @param efficiency inhabitants one point of food feeds
     * @return ceiling the town climbs towards
     */
    public double ceiling(double foodStock, double efficiency) {
        return feedablePopulation(foodStock, efficiency);
    }

    /**
     * Returns what one night moves the population by.
     *
     * <p>The one and only population rule. A share of the gap, and morale on the way up alone: a
     * demoralised town falls exactly as fast as any other. That asymmetry is deliberate — morale is a
     * reward for winning fights, never a shield against not playing.
     *
     * @param population current population
     * @param foodStock  food of the last seven days
     * @param efficiency inhabitants one point of food feeds
     * @param morale     current morale
     * @return signed change, negative when the town loses people
     */
    public double nightlyChange(double population, double foodStock, double efficiency, double morale) {
        double gap = ceiling(foodStock, efficiency) - population;
        double rate = ruleset.gapClosingRatePercent() / PERCENT_SCALE;

        if (gap < 0) {
            return gap * rate;
        }

        return gap * rate * (morale / ruleset.maximumMorale());
    }

    /**
     * Returns a morale value held inside the ruleset's bounds.
     *
     * @param morale morale before the bounds apply
     * @return morale, never outside the floor and the ceiling
     */
    public double boundedMorale(double morale) {
        return Math.clamp(morale, ruleset.minimumMorale(), ruleset.maximumMorale());
    }
}
