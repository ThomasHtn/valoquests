package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.DefaultColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayState;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the colony's one population rule, its order of operations and its food window.
 *
 * <p>Several of these replay the worked examples of the design document verbatim. That is deliberate:
 * the numbers there are what the whole feature was balanced against, so a rebalancing that moves them
 * has to move them here too, in the open, rather than drifting quietly.
 */
class ColonyReplayEngineTest {

    /** First day of every fixture run, a Monday. */
    private static final LocalDate DAY_ONE = LocalDate.of(2026, 6, 1);

    /** Roster size every fixture is measured against. */
    private static final int ROSTER_SIZE = 7;

    /** Housing a seven-player roster opens on, {@code 300 x 7}. */
    private static final int OPENING_CAPACITY = 2_100;

    /** Match damage a full squad of seven produces on a good day, two competitive wins each. */
    private static final int FULL_SQUAD_DAMAGE = 7 * 2 * 500;

    /** Tolerance for the double arithmetic the engine runs on. */
    private static final double TOLERANCE = 1e-6;

    /** Tolerance for figures the design document states rounded to the inhabitant. */
    private static final double INHABITANT = 1.0;

    /** Calibration under test. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /** Engine under test. */
    private final ColonyReplayEngine engine = new ColonyReplayEngine(ruleset);

    /**
     * Verifies the table of chapter two: a town of 3 000 whose food feeds 4 000 closes 15% of the gap
     * every night at full morale, so it moves fast and then slower and slower.
     *
     * <p>The document prints 150, 128, 108 and 92. Those are the same four numbers, rounded.
     */
    @Test
    void shouldCloseFifteenPercentOfTheGapEveryNightAtFullMorale() {
        double population = 3_000.0;
        double foodStock = 500.0;
        List<Double> arrivals = new ArrayList<>();

        for (int night = 0; night < 4; night++) {
            double change = engine.nightlyChange(population, foodStock, 10_000, 100.0);
            arrivals.add(change);
            population += change;
        }

        assertThat(engine.feedablePopulation(foodStock)).isEqualTo(4_000.0, within(TOLERANCE));
        assertThat(arrivals.get(0)).isEqualTo(150.0, within(INHABITANT));
        assertThat(arrivals.get(1)).isEqualTo(128.0, within(INHABITANT));
        assertThat(arrivals.get(2)).isEqualTo(108.0, within(INHABITANT));
        assertThat(arrivals.get(3)).isEqualTo(92.0, within(INHABITANT));
    }

    /**
     * Verifies the three days of chapter eleven, the document's most cited worked example: a good
     * evening, then one nobody played, then a full house.
     *
     * <p>It is the example that says a missed evening costs about seventy people and a good one brings
     * back twice that, which is the whole reason the model has neither a disaster nor a jackpot in it.
     */
    @Test
    void shouldReplayTheThreeWorkedDaysOfTheDesignDocument() {
        double population = 2_950.0;
        int capacity = 4_600;
        double morale = 70.0;

        // Thursday: five of seven played eleven competitive games, and the stock lands on 556.
        double thursday = engine.nightlyChange(population, 556.0, capacity, morale);
        population += thursday;
        assertThat(population).isEqualTo(3_107.0, within(INHABITANT));

        // Friday: nobody played, so the stock falls to 330 and feeds fewer people than the town holds.
        double friday = engine.nightlyChange(population, 330.0, capacity, morale);
        population += friday;
        assertThat(friday).isEqualTo(-70.0, within(INHABITANT));
        assertThat(population).isEqualTo(3_037.0, within(INHABITANT));

        // Saturday: the whole squad turned out, and the stock climbs back to 546.
        double saturday = engine.nightlyChange(population, 546.0, capacity, morale);
        population += saturday;
        assertThat(saturday).isEqualTo(140.0, within(INHABITANT));
        assertThat(population).isEqualTo(3_177.0, within(INHABITANT));
    }

    /**
     * Verifies that the harvest of that same Thursday matches the document: 4 600 damage brought home by
     * five of seven players is worth 92 food, not 54.
     */
    @Test
    void shouldMultiplyTheHarvestByTheDaysTurnout() {
        assertThat(engine.presenceMultiplier(5, ROSTER_SIZE)).isEqualTo(1.714, within(1e-3));
        assertThat(engine.harvest(4_600, 5, ROSTER_SIZE)).isEqualTo(92.0, within(INHABITANT));
    }

    /**
     * Verifies a full house doubles the day, and that a roster shrunk to five is not punished for it.
     */
    @Test
    void shouldDoubleTheDayWhenTheWholeRosterTurnedUp() {
        assertThat(engine.presenceMultiplier(7, 7)).isEqualTo(2.0, within(TOLERANCE));
        assertThat(engine.presenceMultiplier(5, 5)).isEqualTo(2.0, within(TOLERANCE));
    }

    /**
     * Verifies the multiplier is capped at two even when more players turn up than the frozen roster
     * holds. A player left out of the roster still plays, and would otherwise push it past a full house.
     */
    @Test
    void shouldCapTheTurnoutAtAFullHouse() {
        assertThat(engine.presenceMultiplier(9, ROSTER_SIZE)).isEqualTo(2.0, within(TOLERANCE));
    }

    /**
     * Verifies morale speeds the climb and does nothing at all to the fall.
     *
     * <p>The asymmetry is the point: morale rewards winning fights, it never shields a squad that has
     * stopped playing.
     */
    @Test
    void shouldApplyMoraleOnTheWayUpAndNeverOnTheWayDown() {
        double halfSpeedClimb = engine.nightlyChange(1_000.0, 250.0, 10_000, 50.0);
        double fullSpeedClimb = engine.nightlyChange(1_000.0, 250.0, 10_000, 100.0);

        assertThat(halfSpeedClimb).isEqualTo(fullSpeedClimb / 2, within(TOLERANCE));

        double demoralisedFall = engine.nightlyChange(3_000.0, 250.0, 10_000, 20.0);
        double confidentFall = engine.nightlyChange(3_000.0, 250.0, 10_000, 100.0);

        assertThat(demoralisedFall).isEqualTo(confidentFall, within(TOLERANCE));
        assertThat(demoralisedFall).isNegative();
    }

    /**
     * Verifies the lower of the two ceilings commands, whichever one it is.
     */
    @Test
    void shouldClimbTowardsTheLowerOfTheTwoCeilings() {
        assertThat(engine.ceiling(500.0, 10_000)).isEqualTo(4_000.0, within(TOLERANCE));
        assertThat(engine.ceiling(500.0, 3_000)).isEqualTo(3_000.0, within(TOLERANCE));
    }

    /**
     * Verifies the food window holds seven days and no more: an eighth day pushes the first one out, so
     * the stock is a moving average rather than a reserve that could be hoarded.
     */
    @Test
    void shouldHoldSevenDaysOfHarvestAndDropTheEighth() {
        List<ColonyDayState> states = engine.replay(playedDays(8, FULL_SQUAD_DAMAGE), ROSTER_SIZE);

        double oneDay = engine.harvest(FULL_SQUAD_DAMAGE, ROSTER_SIZE, ROSTER_SIZE);

        assertThat(states.get(6).foodStock()).isEqualTo(oneDay * 7, within(TOLERANCE));
        assertThat(states.get(7).foodStock()).isEqualTo(oneDay * 7, within(TOLERANCE));
    }

    /**
     * Verifies a run opens on empty ground and that a day nobody played leaves it exactly there.
     */
    @Test
    void shouldOpenOnEmptyGroundAndStayThereWhileNobodyPlays() {
        List<ColonyDayState> states = engine.replay(playedDays(3, 0), ROSTER_SIZE);

        assertThat(states).allSatisfy(state -> {
            assertThat(state.population()).isZero();
            assertThat(state.foodStock()).isZero();
            assertThat(state.materials()).isZero();
            assertThat(state.capacity()).isEqualTo(OPENING_CAPACITY);
            assertThat(state.morale()).isEqualTo(ruleset.initialMorale());
        });
    }

    /**
     * Verifies a rollover credits its materials and its morale before the night runs, and that the
     * housing is recomputed from the new total straight away.
     *
     * <p>There is no threshold to save up for: a challenge validated over the week widens the town on
     * the Monday that settles it.
     */
    @Test
    void shouldCreditARolloverBeforeTheNightRuns() {
        List<ColonyDailyInput> days = new ArrayList<>();
        days.add(new ColonyDailyInput(DAY_ONE, 0, 0, false, 0, 0.0));
        days.add(new ColonyDailyInput(DAY_ONE.plusDays(7), 0, 0, true, 1_000, 15.0));

        List<ColonyDayState> states = engine.replay(days, ROSTER_SIZE);
        ColonyDayState rollover = states.getLast();

        assertThat(rollover.materials()).isEqualTo(1_000);
        assertThat(rollover.capacity()).isEqualTo(OPENING_CAPACITY + 500);
        assertThat(rollover.morale()).isEqualTo(65.0, within(TOLERANCE));
    }

    /**
     * Verifies morale never leaves its bounds, so a run that lost every fight stays playable and a run
     * that won them all cannot bank speed it will never use.
     */
    @Test
    void shouldHoldMoraleInsideItsBounds() {
        assertThat(engine.boundedMorale(-40.0)).isEqualTo(ruleset.minimumMorale());
        assertThat(engine.boundedMorale(180.0)).isEqualTo(ruleset.maximumMorale());
        assertThat(engine.boundedMorale(55.0)).isEqualTo(55.0);
    }

    /**
     * Verifies a Monday's surplus is converted against the housing of the week that has just closed,
     * before that Monday's own materials are counted.
     *
     * <p>Settle it the other way round and a Monday heavy with validated challenges would erase its own
     * surplus, which is the one ordering subtlety of the whole model.
     */
    @Test
    void shouldConvertTheSurplusAgainstTheClosingWeeksHousing() {
        List<ColonyDailyInput> days = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            days.add(new ColonyDailyInput(DAY_ONE.plusDays(day), FULL_SQUAD_DAMAGE, 7, false, 0, 0.0));
        }
        days.add(new ColonyDailyInput(DAY_ONE.plusDays(7), 0, 0, true, 0, 0.0));

        List<ColonyDayState> states = engine.replay(days, ROSTER_SIZE);

        // The stock on Sunday evening, converted against Sunday's housing, never against the Monday's.
        double sundayStock = states.get(6).foodStock();
        int expected = ruleset.materialsForSurplus(sundayStock, OPENING_CAPACITY);

        assertThat(expected).isPositive();
        assertThat(states.getLast().materials()).isEqualTo(expected);
    }

    /**
     * Verifies the population never goes below zero, however far the food falls short.
     */
    @Test
    void shouldNeverDriveThePopulationBelowZero() {
        assertThat(engine.replay(playedDays(30, 0), ROSTER_SIZE))
            .allSatisfy(state -> assertThat(state.population()).isGreaterThanOrEqualTo(0.0));
    }

    /**
     * Verifies a roster of zero cannot make the turnout multiplier blow up, which is the only division
     * in the model whose denominator comes from outside it.
     */
    @Test
    void shouldSurviveAnEmptyRoster() {
        assertThat(engine.presenceMultiplier(3, 0)).isEqualTo(1.0, within(TOLERANCE));
    }

    /**
     * Verifies what the town eats is the exact inverse of what a point of food feeds.
     */
    @Test
    void shouldEatTheInverseOfWhatFoodFeeds() {
        assertThat(engine.weeklyConsumption(2_400.0)).isEqualTo(300.0, within(TOLERANCE));
        assertThat(engine.feedablePopulation(300.0)).isEqualTo(2_400.0, within(TOLERANCE));
    }

    /**
     * Builds a run of consecutive days all played the same way, none of them a rollover.
     *
     * @param count       days to build
     * @param matchDamage damage each of them brought home
     * @return the days, chronologically
     */
    private List<ColonyDailyInput> playedDays(int count, int matchDamage) {
        List<ColonyDailyInput> days = new ArrayList<>();

        for (int day = 0; day < count; day++) {
            days.add(new ColonyDailyInput(
                DAY_ONE.plusDays(day),
                matchDamage,
                matchDamage > 0 ? ROSTER_SIZE : 0,
                false,
                0,
                0.0
            ));
        }

        return days;
    }
}
