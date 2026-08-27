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
 * <p><b>One ceiling, and nothing is ever wasted.</b> Food alone says how far the town can grow, and
 * {@link #efficiencyFor(int, int)} says how far one point of food carries. Materials raise that
 * efficiency, which is how challenges and bosses pay off; there is no maximum, so a challenge validated
 * on the last Monday is worth exactly what one validated on the first was.
 *
 * <p>The housing ceiling this replaced looked reasonable and did nothing. The seven-day food window
 * means settlement day always sees a full week of production, so the food ceiling peaked exactly when
 * it was measured and housing sat above it: taking challenge completion from 35% to 95% moved the final
 * score by 0.2%. Efficiency moves it by 28%, which is what materials were for in the first place.
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
     * Inhabitants one point of food feeds before any material has been gathered.
     */
    private static final double BASE_INHABITANTS_PER_FOOD = 8.0;

    /**
     * Materials per player buying one more inhabitant per point of food.
     *
     * <p>Calibrated for a threefold gap between a steady squad and one playing twice as much, chosen
     * against the 1.4 the housing ceiling used to allow. A regular run runs from efficiency 8.00 to
     * 16.15, linearly, which is also what sets the tier ladder's pace at one step a week.
     */
    private static final double MATERIALS_PER_EFFICIENCY_POINT = 150.0;

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
     * Morale floor, low enough to hurt but never an absorbing state.
     *
     * <p>Reached after five straight defeats rather than after two, since a surviving boss costs seven
     * and no longer twenty. A wiped-out run slides towards the floor over half a season instead of
     * hitting it in a fortnight.
     *
     * <p>At one, the town closed one percent of its gap a week: nothing a squad did could start it
     * again, which is waiting rather than losing. At twenty it closes nineteen percent a week, and the
     * punishment is almost untouched — on a squad that wasted its first three weeks, raising the floor
     * from one to twenty is worth two percent of the final score.
     */
    private static final double MINIMUM_MORALE = 20.0;

    /**
     * Morale ceiling, and the value at which the town closes its gap at full speed.
     */
    private static final double MAXIMUM_MORALE = 100.0;

    /**
     * Morale a surviving boss costs.
     *
     * <p>Exactly what an elite win pays, which is the invariant the whole table is built on: the
     * hardest fight of the run repairs one loss and no more.
     */
    private static final double MORALE_FOR_SURVIVING_BOSS = -7.0;

    /**
     * Divisor turning a challenge's scoring damage into the materials it is worth per player.
     */
    private static final int CHALLENGE_DAMAGE_TO_MATERIALS_DIVISOR = 100;

    /**
     * Efficiency between two consecutive tiers of the ladder.
     *
     * <p>Three quarters of a point, so a regular run's climb from 8.00 to 16.15 crosses ten steps: one
     * milestone a week, which is what the ladder is for. The ladder hangs on efficiency rather than on
     * population because efficiency never goes back down, so a name is never lost, and because it is
     * independent of roster size, where population is proportional to it.
     */
    private static final double EFFICIENCY_TIER_STEP = 0.75;

    /**
     * Names of the ladder, one per step from the opening efficiency up. The last one repeats, numbered.
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
    public double efficiencyFor(int materials, int rosterSize) {
        if (rosterSize <= 0) {
            return BASE_INHABITANTS_PER_FOOD;
        }

        double materialsPerPlayer = Math.max(0, materials) / (double) rosterSize;

        return BASE_INHABITANTS_PER_FOOD + materialsPerPlayer / MATERIALS_PER_EFFICIENCY_POINT;
    }

    @Override
    public int materialsForEfficiency(double efficiency, int rosterSize) {
        double climb = efficiency - BASE_INHABITANTS_PER_FOOD;

        if (rosterSize <= 0 || climb <= 0) {
            return 0;
        }

        return (int) Math.ceil(climb * rosterSize * MATERIALS_PER_EFFICIENCY_POINT);
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
     * <p><b>The table is sized against the room, not against the fight.</b> A run schedules two minor
     * fights, six standard ones and two elite ones, so winning every one of them offers
     * {@code 2x3 + 6x5 + 2x7 = 50} morale — exactly the distance from the fifty a run opens on to the
     * hundred it tops out at. A flawless run therefore lands on the ceiling with its <b>tenth</b> fight
     * and not before, which is the whole point: every one of the ten moves the gauge.
     *
     * <p>These numbers used to be 10, 15 and 20, and offered 150 morale into 50 points of room. The
     * ceiling was reached on week four of any decent run, after which six fights out of ten changed
     * nothing at all and the categories stopped meaning anything — the boss was an on/off switch rather
     * than a graded lever. Adding a weekly decay was considered and dropped: at five a week it bought
     * one week of headroom, and at the ten a week that would have worked a minor win nets zero, which
     * puts the dead fight back in a different place.
     *
     * <p>The loss stays worth exactly one elite win, so the break-even sits at a 58% win rate, within a
     * point of what the old table asked. Losing every fight now takes five weeks to reach the floor
     * rather than two: the punishment is spread over the run instead of bottoming out in a fortnight.
     */
    @Override
    public double moraleForDefeatedBoss(BossCategory category) {
        if (category == null) {
            return 0.0;
        }

        return switch (category) {
            case MINOR -> 3.0;
            case STANDARD -> 5.0;
            case ELITE -> 7.0;
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

    /**
     * {@inheritDoc}
     *
     * <p>Spread wide on purpose, and wider than the morale the same fight moves. The campaign schedules
     * its weight classes rather than drawing them, so a run always pays two minor fights, six standard
     * ones and two elite ones: the two elite weeks are the only ones that can move the town by a step
     * on their own, and they only read that way if they are worth several ordinary ones. Morale keeps
     * its narrower ladder because it is calibrated against what a surviving boss costs, not against
     * what the town can build.
     */
    @Override
    public int materialsForDefeatedBoss(BossCategory category, int rosterSize) {
        if (category == null) {
            return 0;
        }

        int materialsPerPlayer = switch (category) {
            case MINOR -> 40;
            case STANDARD -> 80;
            case ELITE -> 140;
        };

        return materialsPerPlayer * Math.max(0, rosterSize);
    }

    @Override
    public double efficiencyTierStep() {
        return EFFICIENCY_TIER_STEP;
    }

    @Override
    public ColonyTier tierFor(double efficiency) {
        return tierAt(stepOf(efficiency));
    }

    @Override
    public ColonyTier nextTierFor(double efficiency) {
        return tierAt(stepOf(efficiency) + 1);
    }

    @Override
    public ColonyTier tierAtStep(int step) {
        return tierAt(Math.max(0, step));
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
     * Returns the ladder step an efficiency sits on.
     *
     * <p>Never negative: a run opens exactly on the base efficiency, which is step zero, so the town
     * always has a name on day one whatever the roster size.
     *
     * @param efficiency efficiency reached
     * @return ladder step, counted from the opening efficiency
     */
    private int stepOf(double efficiency) {
        double climbed = efficiency - BASE_INHABITANTS_PER_FOOD;

        return climbed <= 0 ? 0 : (int) (climbed / EFFICIENCY_TIER_STEP);
    }

    /**
     * Names one step of the ladder.
     *
     * <p>Past the last name the ladder repeats, numbered, so it runs on without a maximum: a squad
     * validating nearly every challenge reaches efficiency 21 and enters the numbered citadels well
     * before its run ends.
     *
     * @param step ladder step, counted from the opening efficiency
     * @return the step, named
     */
    private ColonyTier tierAt(int step) {
        int nameIndex = Math.clamp(step, 0, TIER_NAMES.length - 1);
        ColonyTierName name = TIER_NAMES[nameIndex];
        int level = name == ColonyTierName.CITADEL ? step - TIER_NAMES.length + 2 : 0;

        return new ColonyTier(step, BASE_INHABITANTS_PER_FOOD + step * EFFICIENCY_TIER_STEP, name, level);
    }
}
