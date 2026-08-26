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
 *
 * <p>That inheritance is now load-bearing. Since the housing ceiling was removed, the scoring ruleset's
 * diminishing returns are the <b>only</b> brake on sheer volume of play: they barely engage below six
 * games a day per player and only bite on a grinder, which is deliberate, but retuning them upstream
 * moves the top of the colony's scale with them.
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
     * Returns how many inhabitants one point of food feeds, given how developed the colony is.
     *
     * <p>The single number tying food to population, and it works both ways: {@code food x efficiency}
     * is what the town can feed, {@code population / efficiency} is what it eats in a week. It is no
     * longer a constant: materials raise it, which is how challenges and bosses pay off. There is no
     * maximum, so a challenge validated in week ten is worth exactly what one validated in week one was.
     *
     * <p>Materials are divided by the roster <b>before</b> being turned into efficiency, which is what
     * keeps the balance identical from two players to twenty: challenges and bosses already pay per
     * player, so without that division a large squad would climb faster than a small one.
     *
     * @param materials  cumulative materials of the run
     * @param rosterSize roster size frozen on the run
     * @return inhabitants fed by one point of food, never below the base value
     */
    double efficiencyFor(int materials, int rosterSize);

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
     * materials raise efficiency, and efficiency never comes back down.
     *
     * @param category   category of the defeated boss
     * @param rosterSize roster size frozen on the run
     * @return materials credited for the defeated boss
     */
    int materialsForDefeatedBoss(BossCategory category, int rosterSize);

    /**
     * Returns the efficiency between two consecutive tiers of the ladder.
     *
     * @return width of one tier, in efficiency
     */
    double efficiencyTierStep();

    /**
     * Returns the tier an efficiency currently sits in.
     *
     * @param efficiency efficiency reached
     * @return tier reached
     */
    ColonyTier tierFor(double efficiency);

    /**
     * Returns the tier following the one an efficiency sits in.
     *
     * <p>Never {@code null}: the ladder has no end, so there is always a next name to climb towards.
     *
     * @param efficiency efficiency reached
     * @return next tier
     */
    ColonyTier nextTierFor(double efficiency);

    /**
     * Returns the tier sitting on one step of the ladder.
     *
     * <p>What lets a page walk the ladder by index without knowing the efficiency each step opens at.
     * A negative step is read as the first one, so the ladder always starts on a named tier.
     *
     * @param step ladder step, counted from the opening efficiency
     * @return the tier that step carries
     */
    ColonyTier tierAtStep(int step);

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
