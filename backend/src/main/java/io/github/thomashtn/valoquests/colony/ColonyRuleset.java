package io.github.thomashtn.valoquests.colony;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyTier;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;

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
     * Returns the divisor turning a day's match damage into food.
     *
     * <p>No barème is created here: the damage read is the one produced <i>after</i> the daily
     * diminishing returns, so the colony and the weekly ranking price a given match identically.
     *
     * @return match damage worth one point of food
     */
    int foodDamageDivisor();

    /**
     * Returns the damage one ordinary competitive game is counted as being worth.
     *
     * <p>Used for one thing only: turning an amount of food into a number of games, so a page can say
     * "about six games" rather than "2 700 damage". Taken at its draw value, which is what a roughly
     * even win rate averages out at over a week.
     *
     * @return damage of a reference competitive game
     */
    int referenceMatchDamage();

    /**
     * Returns how many inhabitants one point of food feeds.
     *
     * <p>The single constant tying food to population, and it works both ways: {@code food x 8} is what
     * the town can feed, {@code population / 8} is what it eats in a week. It is also the most delicate
     * number of the whole system — it is calibrated so an ordinary run ends with its two ceilings, food
     * and housing, neck and neck, and the moment it moves the two stop weighing the same.
     *
     * @return inhabitants fed by one point of food
     */
    int inhabitantsPerFood();

    /**
     * Returns the raw daily damage a player must reach to count towards turnout.
     *
     * <p>Read on <b>raw</b> damage, before the daily diminishing returns: those exist to stop farming,
     * not to decide whether somebody logged in tonight. Without that precision a player stringing
     * fifteen games together could watch their own turnout drop by playing more.
     *
     * @return raw damage threshold for one player's day to count
     */
    int presenceDamageThreshold();

    /**
     * Returns how many days of harvest the food stock holds.
     *
     * <p>A moving average, never a reserve. Each night today enters the count and the day seven days
     * back leaves it, so a quiet Tuesday dents the stock instead of emptying it, and three intense weeks
     * cannot be banked against a month of silence.
     *
     * @return length of the food window, in days
     */
    int foodWindowDays();

    /**
     * Returns the share of the gap between the town and its ceiling that is closed in one night.
     *
     * <p>The only population rule there is. The town moves fast at first and slower and slower after,
     * and never quite arrives, which is what leaves something to gain on every single evening of a run.
     *
     * @return share of the gap closed per night, as a percentage
     */
    double gapClosingRatePercent();

    /**
     * Returns the morale a run opens on.
     *
     * @return initial morale
     */
    double initialMorale();

    /**
     * Returns the morale floor.
     *
     * <p>Sits just above zero rather than at a comfortable value: the floor only keeps the speed
     * multiplier positive, so a town that has lost every boss is frozen until it wins one back.
     *
     * @return lowest morale reachable
     */
    double minimumMorale();

    /**
     * Returns the morale ceiling, which is also the value at which the town moves at full speed.
     *
     * @return highest morale reachable
     */
    double maximumMorale();

    /**
     * Returns the morale a defeated boss of a category is worth.
     *
     * @param category category of the defeated boss
     * @return morale gained
     */
    double moraleForDefeatedBoss(BossCategory category);

    /**
     * Returns the morale a surviving boss costs, as a negative number.
     *
     * @return morale lost when the week's boss holds
     */
    double moraleForSurvivingBoss();

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
     * <p>Priced per player of the frozen roster, so a fight is worth what the squad it was sized against
     * is worth. A surviving boss brings nothing; that is its entire cost.
     *
     * <p>The boss deliberately hands out no inhabitants at all. A gift of inhabitants fades at fifteen
     * percent a night and is gone three weeks later, which made the first six fights of a run pointless;
     * materials buy housing, and housing is still standing on settlement day.
     *
     * @param category   category of the defeated boss
     * @param rosterSize roster size frozen on the run
     * @return materials credited for the defeated boss
     */
    int materialsForDefeatedBoss(BossCategory category, int rosterSize);

    /**
     * Returns the housing a roster and a materials total open.
     *
     * <p>Continuous and unbounded: there is no threshold to cross, so a challenge validated on a Monday
     * widens the town that same Monday. A pure function of its two arguments, which is what keeps the
     * whole replay reproducible.
     *
     * @param rosterSize roster size frozen on the run
     * @param materials  cumulative materials
     * @return housing available
     */
    int capacityFor(int rosterSize, int materials);

    /**
     * Returns the housing an amount of materials buys, on its own.
     *
     * <p>What lets a reward be quoted in housing rather than in materials. Materials are an intermediate
     * currency the player never handles; housing is the axis every readout of the page already uses.
     *
     * @param materials materials to convert
     * @return housing they are worth
     */
    int housingForMaterials(int materials);

    /**
     * Returns the materials a week's leftover food is turned into.
     *
     * <p>What guarantees no evening is ever wasted and the population has no maximum. A deliberately bad
     * exchange rate that puts itself out: the bigger the town, the more food it takes to fill it, so the
     * surplus melts on its own and housing catches food up rather than running away from it.
     *
     * @param foodStock food stock the Monday opens on
     * @param capacity  housing of the week that has just closed
     * @return materials the surplus is worth, never negative
     */
    int materialsForSurplus(double foodStock, int capacity);

    /**
     * Returns the housing between two consecutive tiers of the ladder.
     *
     * @return width of one tier
     */
    int tierStep();

    /**
     * Returns the tier a housing figure currently sits in.
     *
     * @param capacity housing available
     * @return tier reached
     */
    ColonyTier tierFor(int capacity);

    /**
     * Returns the tier following the one a housing figure sits in.
     *
     * <p>Never {@code null}: the ladder has no end, so there is always a next name to climb towards.
     *
     * @param capacity housing available
     * @return next tier
     */
    ColonyTier nextTierFor(int capacity);

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
}
