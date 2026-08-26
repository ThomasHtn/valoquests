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
 * Tests the colony's order of operations, its clamps and its migration limits.
 */
class ColonyReplayEngineTest {

    /** First day of every fixture run. */
    private static final LocalDate DAY_ONE = LocalDate.of(2026, 6, 1);

    /** Roster size every fixture is measured against. */
    private static final int ROSTER_SIZE = 7;

    /** Match damage a full squad of seven produces on a good day, two competitive wins each. */
    private static final int FULL_SQUAD_DAMAGE = 7 * 2 * 500;

    /** Tolerance for the double arithmetic the engine runs on. */
    private static final double TOLERANCE = 1e-6;

    /** Calibration under test. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /** Engine under test. */
    private final ColonyReplayEngine engine = new ColonyReplayEngine(ruleset);

    /**
     * Verifies the exact arithmetic of the first day, step by step.
     *
     * <p>The initial state is the state the colony is in <i>before</i> day one is played, so day one
     * runs the full order of operations against it rather than being a special case.
     */
    @Test
    void shouldRunTheWholeOrderOfOperationsOnTheFirstDay() {
        // Initial state: gauges at 0, population 0, capacity 3 000.
        // 1. loss = 14 x 0/3000 = 0, because an empty colony consumes nothing. That is the whole
        //    point of opening on zero: day one costs nothing and earns nothing on its own.
        // 2. Food gains 7 000/400 = 17.5, Energy gains 14 x 7/7 = 14. The squad's two games each
        //    out-produce its own turnout, which is what leaves Energy in charge.
        // 3. no materials on the first day. 4. capacity stays 3 000.
        // 5. health is the geometric mean of the two, and growth is capped at 2.5% of 3 000 = 75.
        ColonyDayState state = replayOne(new ColonyDailyInput(DAY_ONE, FULL_SQUAD_DAMAGE, 7, 0));

        double expectedHealth = Math.sqrt(0.175 * 0.14);

        assertThat(state.dailyLoss()).isZero();
        assertThat(state.foodGain()).isEqualTo(17.5, within(TOLERANCE));
        assertThat(state.energyGain()).isEqualTo(14.0, within(TOLERANCE));
        assertThat(state.food()).isEqualTo(17.5, within(TOLERANCE));
        assertThat(state.energy()).isEqualTo(14.0, within(TOLERANCE));
        assertThat(state.capacity()).isEqualTo(3_000);
        assertThat(state.health()).isEqualTo(expectedHealth, within(TOLERANCE));
        assertThat(state.target()).isEqualTo(3_000 * expectedHealth, within(TOLERANCE));
        assertThat(state.population()).isEqualTo(75.0, within(TOLERANCE));
    }

    /**
     * Verifies that a gauge's surplus above one hundred is discarded, never carried or converted.
     *
     * <p>Converting the surplay into materials was considered and rejected: worth about 3% of a run's
     * materials, for an extra rule, a branch, two tests and a paragraph on the page.
     */
    @Test
    void shouldClampGaugesAtOneHundredAndDiscardTheSurplus() {
        // Ten times what a full squad produces, every day for a fortnight.
        List<ColonyDayState> states = engine.replay(
            days(14, FULL_SQUAD_DAMAGE * 10, ROSTER_SIZE),
            ROSTER_SIZE
        );

        assertThat(states).allSatisfy(state -> {
            assertThat(state.food()).isLessThanOrEqualTo(100.0);
            assertThat(state.energy()).isLessThanOrEqualTo(100.0);
        });
        assertThat(states.getLast().food()).isEqualTo(100.0, within(TOLERANCE));
    }

    /**
     * Verifies that a gauge nobody feeds decays towards zero without ever going negative.
     *
     * <p>It approaches zero rather than landing on it: the loss is proportional to the population,
     * which is itself collapsing, so both shrink together. The floor is a guarantee, not a destination.
     */
    @Test
    void shouldDecayTowardsZeroWithoutEverGoingNegative() {
        List<ColonyDayState> states = engine.replay(days(30, 0, 0), ROSTER_SIZE);

        assertThat(states).allSatisfy(state -> {
            assertThat(state.food()).isGreaterThanOrEqualTo(0.0);
            assertThat(state.energy()).isGreaterThanOrEqualTo(0.0);
        });
        assertThat(states.getLast().food()).isLessThan(2.0);
        assertThat(states.getLast().energy()).isLessThan(2.0);
    }

    /**
     * Verifies that abandoning the colony collapses it, and that a single day of play lifts it again.
     *
     * <p>There is no game over: a month away costs dearly but never makes the rest of the run
     * pointless.
     */
    @Test
    void shouldCollapseOnAbandonmentAndRecoverAfterOneDayOfPlay() {
        List<ColonyDailyInput> inputs = new ArrayList<>(days(40, 0, 0));
        inputs.add(new ColonyDailyInput(DAY_ONE.plusDays(40), FULL_SQUAD_DAMAGE, ROSTER_SIZE, 0));

        List<ColonyDayState> states = engine.replay(inputs, ROSTER_SIZE);

        ColonyDayState collapsed = states.get(39);
        assertThat(collapsed.health()).isLessThan(0.01);
        assertThat(collapsed.population()).isLessThan(collapsed.capacity() * 0.01);

        ColonyDayState recovered = states.getLast();
        assertThat(recovered.population()).isGreaterThan(collapsed.population());
        assertThat(recovered.health()).isGreaterThan(0.1);
    }

    /**
     * Verifies that a day nobody played can never leave the colony better off than it found it.
     *
     * <p>The regression this guards against was real and invisible. The daily loss is proportional to
     * the population, so a small colony pays almost nothing, and the run used to open on a population
     * of 300 with both gauges at 50 — a health of 0.5 nobody had earned, pulling towards a target of
     * 1 458. A run <i>nobody ever played</i> therefore grew from 300 to 870 inhabitants over its first
     * eight days before starting to fall.
     *
     * <p>A run nobody ever played must therefore stay flat on the floor, and no idle day may ever lift
     * a gauge. The population is deliberately <i>not</i> asserted to fall on the first idle day: the
     * gauges are the stock that buffers a single bad evening, so a colony still climbing towards a
     * target its accumulated gauges justify keeps climbing for a few days after play stops. That
     * buffer is the feature. What must not exist is growth out of nothing.
     */
    @Test
    void shouldNeverRewardADayNobodyPlayed() {
        for (ColonyDayState state : engine.replay(days(30, 0, 0), ROSTER_SIZE)) {
            assertThat(state.population()).isZero();
            assertThat(state.food()).isZero();
            assertThat(state.energy()).isZero();
        }

        List<ColonyDailyInput> playedThenIdle =
            new ArrayList<>(days(20, FULL_SQUAD_DAMAGE, ROSTER_SIZE));
        double populationWhenPlayStopped =
            engine.replay(playedThenIdle, ROSTER_SIZE).getLast().population();

        for (int index = 0; index < 30; index++) {
            playedThenIdle.add(new ColonyDailyInput(DAY_ONE.plusDays(20L + index), 0, 0, 0));
        }

        List<ColonyDayState> idleDays =
            engine.replay(playedThenIdle, ROSTER_SIZE).subList(20, playedThenIdle.size());
        ColonyDayState previous = idleDays.getFirst();

        for (ColonyDayState state : idleDays) {
            assertThat(state.food()).isLessThanOrEqualTo(previous.food() + TOLERANCE);
            assertThat(state.energy()).isLessThanOrEqualTo(previous.energy() + TOLERANCE);
            previous = state;
        }

        assertThat(idleDays.getLast().population()).isLessThan(populationWhenPlayStopped);
        assertThat(idleDays.getLast().health()).isLessThan(0.01);
    }

    /**
     * Verifies that growth never exceeds 2.5% of capacity in a day, however hard the squad plays.
     *
     * <p>The most important mechanism in the system: there is no way to accelerate a colony other than
     * holding both gauges high day after day.
     */
    @Test
    void shouldNeverGrowFasterThanTheDailyLimit() {
        List<ColonyDayState> states = engine.replay(
            days(60, FULL_SQUAD_DAMAGE * 5, ROSTER_SIZE),
            ROSTER_SIZE
        );

        double previous = ruleset.initialPopulation();
        for (ColonyDayState state : states) {
            double limit = state.capacity() * ruleset.growthRatePercent() / 100.0;
            assertThat(state.population() - previous).isLessThanOrEqualTo(limit + TOLERANCE);
            previous = state.population();
        }
    }

    /**
     * Verifies that decline never exceeds 5% of capacity in a day, and is exactly twice the growth cap.
     */
    @Test
    void shouldNeverDeclineFasterThanTwiceTheGrowthLimit() {
        // Build a populated colony, then stop playing entirely.
        List<ColonyDailyInput> inputs = new ArrayList<>(days(60, FULL_SQUAD_DAMAGE, ROSTER_SIZE));
        for (int index = 0; index < 20; index++) {
            inputs.add(new ColonyDailyInput(DAY_ONE.plusDays(60L + index), 0, 0, 0));
        }

        List<ColonyDayState> states = engine.replay(inputs, ROSTER_SIZE);

        double declineLimit = 3_000 * ruleset.declineRatePercent() / 100.0;
        double biggestDrop = 0.0;
        for (int index = 60; index < states.size(); index++) {
            double drop = states.get(index - 1).population() - states.get(index).population();
            biggestDrop = Math.max(biggestDrop, drop);
        }

        assertThat(biggestDrop).isLessThanOrEqualTo(declineLimit + TOLERANCE);
        assertThat(biggestDrop).isEqualTo(declineLimit, within(TOLERANCE));
    }

    /**
     * Verifies that the population never leaves {@code [0, capacity]}.
     */
    @Test
    void shouldKeepThePopulationWithinItsCapacity() {
        List<ColonyDailyInput> inputs = new ArrayList<>();
        for (int index = 0; index < 71; index++) {
            inputs.add(new ColonyDailyInput(
                DAY_ONE.plusDays(index),
                FULL_SQUAD_DAMAGE * 3,
                ROSTER_SIZE,
                index % 7 == 0 ? 900 : 0
            ));
        }

        List<ColonyDayState> states = engine.replay(inputs, ROSTER_SIZE);

        assertThat(states).allSatisfy(state -> {
            assertThat(state.population()).isBetween(0.0, (double) state.capacity());
        });
    }

    /**
     * Verifies that materials only ever go up, and that crossing two thresholds on one day lifts the
     * capacity straight to the higher tier.
     */
    @Test
    void shouldCrossTwoBuildingThresholdsOnASingleDay() {
        List<ColonyDayState> states = engine.replay(List.of(
            new ColonyDailyInput(DAY_ONE, FULL_SQUAD_DAMAGE, ROSTER_SIZE, 0),
            new ColonyDailyInput(DAY_ONE.plusDays(1), FULL_SQUAD_DAMAGE, ROSTER_SIZE, 6_300)
        ), ROSTER_SIZE);

        assertThat(states.getFirst().capacity()).isEqualTo(3_000);
        assertThat(states.getLast().materials()).isEqualTo(6_300);
        assertThat(states.getLast().capacity()).isEqualTo(5_500);
    }

    /**
     * Verifies that health is the geometric mean, so neglecting one gauge costs more than an average
     * would suggest.
     */
    @Test
    void shouldMeasureHealthAsTheGeometricMeanOfBothGauges() {
        assertThat(engine.health(100, 100)).isEqualTo(1.0, within(TOLERANCE));
        assertThat(engine.health(80, 80)).isEqualTo(0.8, within(TOLERANCE));
        assertThat(engine.health(100, 20)).isEqualTo(0.4472136, within(1e-6));
        assertThat(engine.health(100, 0)).isZero();
    }

    /**
     * Verifies that the Energy gain is proportional to the frozen roster and capped at its maximum.
     *
     * <p>Capped because a player left inactive still plays: without it, an eighth participant would
     * push the numerator past a roster of seven.
     */
    @Test
    void shouldScaleEnergyOnTheFrozenRosterAndCapItThere() {
        assertThat(engine.energyGain(0, 7)).isZero();
        assertThat(engine.energyGain(4, 7)).isEqualTo(8.0, within(TOLERANCE));
        assertThat(engine.energyGain(7, 7)).isEqualTo(14.0, within(TOLERANCE));
        assertThat(engine.energyGain(9, 7)).isEqualTo(14.0, within(TOLERANCE));
    }

    /**
     * Verifies that an empty roster produces no Energy rather than dividing by zero.
     */
    @Test
    void shouldProduceNoEnergyOnAnEmptyRoster() {
        assertThat(engine.energyGain(3, 0)).isZero();
        assertThat(engine.replay(days(3, FULL_SQUAD_DAMAGE, 3), 0))
            .allSatisfy(state -> assertThat(state.energyGain()).isZero());
    }

    /**
     * Verifies that a run with no days at all produces no state.
     */
    @Test
    void shouldProduceNothingForARunWithNoDays() {
        assertThat(engine.replay(List.of(), ROSTER_SIZE)).isEmpty();
    }

    /**
     * Verifies that the number of matches has no effect whatsoever on the Energy gauge.
     *
     * <p>Two strictly independent behaviours: a gauge fed by the same thing as another one would be
     * pointless.
     */
    @Test
    void shouldKeepTheTwoGaugesIndependent() {
        ColonyDayState few = replayOne(new ColonyDailyInput(DAY_ONE, 1_000, 4, 0));
        ColonyDayState many = replayOne(new ColonyDailyInput(DAY_ONE, 90_000, 4, 0));

        assertThat(few.energyGain()).isEqualTo(many.energyGain());
        assertThat(few.foodGain()).isLessThan(many.foodGain());
    }

    /**
     * Replays a single day and returns its state.
     *
     * @param input the day
     * @return the state it closes on
     */
    private ColonyDayState replayOne(ColonyDailyInput input) {
        return engine.replay(List.of(input), ROSTER_SIZE).getFirst();
    }

    /**
     * Builds a stretch of identical days.
     *
     * @param count             number of days
     * @param matchDamage       damage each day brings
     * @param activePlayerCount players active each day
     * @return the days, starting on {@link #DAY_ONE}
     */
    private static List<ColonyDailyInput> days(int count, int matchDamage, int activePlayerCount) {
        List<ColonyDailyInput> inputs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            inputs.add(new ColonyDailyInput(
                DAY_ONE.plusDays(index),
                matchDamage,
                activePlayerCount,
                0
            ));
        }

        return inputs;
    }
}
