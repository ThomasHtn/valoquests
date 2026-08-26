package io.github.thomashtn.valoquests.colony;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.colony.model.ColonyTier;
import io.github.thomashtn.valoquests.colony.model.ColonyTierName;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import org.springframework.stereotype.Component;

/**
 * The colony's calibration in force.
 *
 * <p>The whole feature reduces to one sentence: <b>you play Valorant, that produces food, food says how
 * many inhabitants the town can feed, and every night the town moves a little closer to that number.</b>
 * Everything below only prices that sentence.
 *
 * <p>Two ceilings decide the score and the lower one commands. Food says what the town can feed,
 * housing what it can lodge; whichever is smaller wins and the other is wasted. {@link
 * #inhabitantsPerFood()} is the number tying them, calibrated so an ordinary run finishes with the two
 * neck and neck — without it, all the work done on one of the two would be invisible in the final
 * score. It is the most delicate figure here and the one most worth revisiting after a real run.
 *
 * <p>Morale is the only lever that is not a resource: it sets the speed the town closes its gap at, it
 * moves on bosses and on nothing else, and it is deliberately asymmetric — it makes the town climb
 * faster but never slows its fall. Everything else the squad does is already measured by the seven-day
 * food window; a second bar measuring the same thing would have added nothing. The boss was measured
 * nowhere.
 *
 * <p>These numbers are an experimental first pass; {@code colony_daily_snapshot} is deliberately rich
 * enough to recalibrate them afterwards without asking Henrik for anything again.
 */
@Component
public final class DefaultColonyRuleset implements ColonyRuleset {

    /**
     * Weekly rollovers a run spans.
     */
    private static final int RUN_LENGTH_WEEKS = 10;

    /**
     * Match damage worth one point of food.
     *
     * <p>Under the 425 an average competitive game is priced at, so one such game is worth five food.
     */
    private static final int FOOD_DAMAGE_DIVISOR = 85;

    /**
     * Inhabitants one point of food feeds, and the divisor turning a population into what it eats.
     */
    private static final int INHABITANTS_PER_FOOD = 8;

    /**
     * Raw daily damage one player must reach to count towards turnout.
     *
     * <p>One competitive game, or three deathmatches. Without a threshold everybody would fire up a
     * two-minute deathmatch to reach the maximum multiplier.
     */
    private static final int PRESENCE_DAMAGE_THRESHOLD = 300;

    /**
     * Days of harvest the food stock holds.
     */
    private static final int FOOD_WINDOW_DAYS = 7;

    /**
     * Share of the gap the town closes in one night, at full morale.
     */
    private static final double GAP_CLOSING_RATE_PERCENT = 15.0;

    /**
     * Morale a run opens on, halfway up the bar.
     */
    private static final double INITIAL_MORALE = 50.0;

    /**
     * Morale floor, kept just above zero rather than at a playable value: a squad that loses every boss
     * stops the town dead, and only winning one starts it again.
     */
    private static final double MINIMUM_MORALE = 1.0;

    /**
     * Morale ceiling, and the value at which the town closes its gap at full speed.
     */
    private static final double MAXIMUM_MORALE = 100.0;

    /**
     * Morale a surviving boss costs.
     */
    private static final double MORALE_FOR_SURVIVING_BOSS = -20.0;

    /**
     * Divisor turning a challenge's scoring damage into the materials it is worth per player.
     */
    private static final int CHALLENGE_DAMAGE_TO_MATERIALS_DIVISOR = 100;

    /**
     * Housing a run opens with, per player of the frozen roster.
     *
     * <p>Three hundred rather than a round share of some total, so a seven-player squad opens on 2 100
     * and lands <i>inside</i> its first named tier instead of on its edge. At 285 it opened at 1 995,
     * five places short, and its town simply had no name on day one.
     *
     * <p>A smaller roster does open below the first named step — six players on 1 800, three on 900 —
     * and that is left alone rather than papered over by inflating the constant: the ladder names those
     * steps {@code CAMP} too, so the town always has a name, and the housing a squad starts on stays
     * proportional to the squad, which is what keeps the balance identical at every size.
     */
    private static final int CAPACITY_PER_PLAYER = 300;

    /**
     * Materials one place of housing costs.
     */
    private static final int MATERIALS_PER_CAPACITY = 2;

    /**
     * Divisor turning a Monday's leftover food into materials.
     */
    private static final int SURPLUS_TO_MATERIALS_DIVISOR = 5;

    /**
     * Housing between two consecutive tiers of the ladder.
     */
    private static final int TIER_STEP = 500;

    /**
     * Ladder step the first name sits on, {@code 2 000 / 500}. Everything below wears that same name.
     */
    private static final int FIRST_NAMED_STEP = 4;

    /**
     * Names of the ladder, from the first named step up. The last one repeats, numbered.
     */
    private static final ColonyTierName[] TIER_NAMES = {
        ColonyTierName.CAMP,
        ColonyTierName.HAMLET,
        ColonyTierName.VILLAGE,
        ColonyTierName.BOROUGH,
        ColonyTierName.TOWN,
        ColonyTierName.CITY,
        ColonyTierName.RESIDENTIAL_QUARTER,
        ColonyTierName.GREAT_CITY,
        ColonyTierName.METROPOLIS,
        ColonyTierName.MEGALOPOLIS,
        ColonyTierName.CAPITAL,
        ColonyTierName.CITADEL
    };

    /**
     * Materials a run opens with.
     */
    private static final int INITIAL_MATERIALS = 0;

    /**
     * Population a run opens with. Empty ground: the first inhabitants only arrive once somebody has fed
     * the place.
     */
    private static final double INITIAL_POPULATION = 0.0;

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
    public int inhabitantsPerFood() {
        return INHABITANTS_PER_FOOD;
    }

    @Override
    public int presenceDamageThreshold() {
        return PRESENCE_DAMAGE_THRESHOLD;
    }

    @Override
    public int foodWindowDays() {
        return FOOD_WINDOW_DAYS;
    }

    @Override
    public double gapClosingRatePercent() {
        return GAP_CLOSING_RATE_PERCENT;
    }

    @Override
    public double initialMorale() {
        return INITIAL_MORALE;
    }

    @Override
    public double minimumMorale() {
        return MINIMUM_MORALE;
    }

    @Override
    public double maximumMorale() {
        return MAXIMUM_MORALE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calibrated against the twenty points a surviving boss costs: a minor win only half repairs a
     * loss, an elite win exactly repairs it. Ten fights are what a run's morale is made of, and nothing
     * else touches it.
     */
    @Override
    public double moraleForDefeatedBoss(BossCategory category) {
        if (category == null) {
            return 0.0;
        }

        return switch (category) {
            case MINOR -> 10.0;
            case STANDARD -> 15.0;
            case ELITE -> 20.0;
        };
    }

    @Override
    public double moraleForSurvivingBoss() {
        return MORALE_FOR_SURVIVING_BOSS;
    }

    @Override
    public int materialsForChallenge(ChallengeDifficulty difficulty) {
        return scoringRuleset.challengeDamage(difficulty) / CHALLENGE_DAMAGE_TO_MATERIALS_DIVISOR;
    }

    @Override
    public int materialsForDefeatedBoss(BossCategory category, int rosterSize) {
        if (category == null) {
            return 0;
        }

        int materialsPerPlayer = switch (category) {
            case MINOR -> 60;
            case STANDARD -> 80;
            case ELITE -> 100;
        };

        return materialsPerPlayer * Math.max(0, rosterSize);
    }

    @Override
    public int capacityFor(int rosterSize, int materials) {
        return CAPACITY_PER_PLAYER * Math.max(0, rosterSize) + housingForMaterials(materials);
    }

    @Override
    public int housingForMaterials(int materials) {
        return Math.max(0, materials) / MATERIALS_PER_CAPACITY;
    }

    @Override
    public int materialsForSurplus(double foodStock, int capacity) {
        double needed = capacity / (double) INHABITANTS_PER_FOOD;
        double surplus = Math.max(0.0, foodStock - needed);

        return (int) (surplus / SURPLUS_TO_MATERIALS_DIVISOR);
    }

    @Override
    public int tierStep() {
        return TIER_STEP;
    }

    @Override
    public ColonyTier tierFor(int capacity) {
        return tierAt(Math.max(0, capacity) / TIER_STEP);
    }

    @Override
    public ColonyTier nextTierFor(int capacity) {
        return tierAt(Math.max(0, capacity) / TIER_STEP + 1);
    }

    @Override
    public int initialMaterials() {
        return INITIAL_MATERIALS;
    }

    @Override
    public double initialPopulation() {
        return INITIAL_POPULATION;
    }

    /**
     * Names one step of the ladder.
     *
     * <p>Steps under the first named one all wear the opening name rather than none: a three-player
     * squad opens its run at 900 housing, well under the 2 000 the spec's table starts at, and a town
     * with no name at all on day one reads as a bug. Past the last name the ladder repeats, numbered, so
     * it runs on without a maximum.
     *
     * @param step ladder step, {@code capacity / 500}
     * @return the step, named
     */
    private ColonyTier tierAt(int step) {
        int nameIndex = Math.clamp(step - FIRST_NAMED_STEP, 0, TIER_NAMES.length - 1);
        ColonyTierName name = TIER_NAMES[nameIndex];
        int level = name == ColonyTierName.CITADEL
            ? step - FIRST_NAMED_STEP - TIER_NAMES.length + 2
            : 0;

        return new ColonyTier(step, step * TIER_STEP, name, level);
    }
}
