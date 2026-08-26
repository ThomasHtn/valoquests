package io.github.thomashtn.valoquests.colony.service;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Tests the two properties the whole feature rests on.
 *
 * <p>The model self-regulates and its fixed point is one line:
 * {@code equilibrium population = capacity x min(Food gain, Energy gain) / 14}. The weakest gauge alone
 * sets the population, which is what gives the colony its anti-farming guarantee without a single
 * dedicated rule — and what makes the hard clamp on both gauges regret-free.
 */
class ColonyEquilibriumTest {

    /** First day of every fixture run. */
    private static final LocalDate DAY_ONE = LocalDate.of(2026, 6, 1);

    /** Roster the whole squad amounts to. */
    private static final int ROSTER_SIZE = 7;

    /** Days each fixture is held at constant inputs, long enough to settle. */
    private static final int DAYS_TO_SETTLE = 400;

    /**
     * Damage a competitive game is worth on an ordinary day, wins and losses averaged out.
     *
     * <p>The draw value of {@code DefaultScoringRuleset}, which is what a roughly even win rate comes
     * out at over a week.
     */
    private static final int AVERAGE_COMPETITIVE_DAMAGE = 425;

    /** Calibration under test. */
    private final ColonyRuleset ruleset = new DefaultColonyRuleset(new DefaultScoringRuleset());

    /** Engine under test. */
    private final ColonyReplayEngine engine = new ColonyReplayEngine(ruleset);

    /**
     * Verifies that, at constant inputs, the population settles on the fixed point.
     *
     * <p>Checked on three regimes so the property is not an accident of one set of numbers: Energy
     * limiting, Food limiting, and both saturated.
     */
    @Test
    void shouldConvergeOnTheFixedPoint() {
        assertConvergesToFixedPoint(7, 2);
        assertConvergesToFixedPoint(4, 3);
        assertConvergesToFixedPoint(2, 10);
    }

    /**
     * Verifies the three regimes the design was calibrated against.
     *
     * <p>Seven players playing twice each fill the colony outright, which is the sentence the whole
     * calibration is tuned around and is now exact rather than approximate. Four reasonable players
     * hold 57%. Two players grinding ten games each hold 29% — while producing <i>more</i> Food than
     * the four.
     */
    @Test
    void shouldReproduceTheCalibratedRegimes() {
        assertThat(settledSharePercent(7, 2)).isCloseTo(100.0, org.assertj.core.data.Offset.offset(1.5));
        assertThat(settledSharePercent(4, 3)).isCloseTo(57.0, org.assertj.core.data.Offset.offset(1.5));
        assertThat(settledSharePercent(2, 10)).isCloseTo(29.0, org.assertj.core.data.Offset.offset(1.5));
    }

    /**
     * Verifies that grinding on two accounts cannot beat regularity on seven, even while producing more
     * Food.
     *
     * <p>The feature's central guarantee. Two players playing ten games each feed the Food gauge harder
     * than four players playing three, and still plateau at less than half their population, because
     * Energy is what limits them.
     */
    @Test
    void shouldNeverLetGrindingBeatTurnout() {
        double grinders = settledSharePercent(2, 10);
        double reasonable = settledSharePercent(4, 3);
        double wholeSquad = settledSharePercent(7, 2);

        assertThat(dailyFood(2, 10)).isGreaterThan(dailyFood(4, 3));
        assertThat(grinders).isLessThan(reasonable);
        assertThat(reasonable).isLessThan(wholeSquad);
    }

    /**
     * Verifies that spreading one day's output over more players is never worth less, and is strictly
     * worth more for as long as turnout is what limits the colony.
     *
     * <p>Stated at strictly equal total damage, so the only thing that changes is how many people
     * produced it. Eight competitive games' worth of damage feeds Food at 8.5, and every turnout short
     * of the full squad caps below it: two players at an Energy gain of 4, four at 8. Spreading the
     * same output is therefore worth strictly more every time, right up to the seven who finally push
     * Energy past Food and take the colony to the ceiling the damage itself sets.
     */
    @Test
    void shouldNeverPunishSpreadingTheSameOutputOverMorePlayers() {
        int totalDamage = 8 * AVERAGE_COMPETITIVE_DAMAGE;
        double foodGain = totalDamage / (double) ruleset.foodDamageDivisor();

        double onTwo = settledShareOf(totalDamage, 2);
        double onFour = settledShareOf(totalDamage, 4);
        double onSeven = settledShareOf(totalDamage, 7);

        assertThat(onTwo).isLessThan(onFour);
        assertThat(onFour).isLessThan(onSeven);

        // Everyone short of the full squad is held back by turnout, not by output: the Food gain is
        // the same 8.5 in all three regimes.
        assertThat(engine.energyGain(2, ROSTER_SIZE)).isLessThan(foodGain);
        assertThat(engine.energyGain(4, ROSTER_SIZE)).isLessThan(foodGain);
        assertThat(engine.energyGain(7, ROSTER_SIZE)).isGreaterThan(foodGain);
    }

    /**
     * Asserts that a regime settles on {@code capacity x min(gains) / 14}.
     *
     * @param players        players turning up each day
     * @param matchesEach    matches each of them plays
     */
    private void assertConvergesToFixedPoint(int players, int matchesEach) {
        double foodGain = dailyFood(players, matchesEach);
        double energyGain = engine.energyGain(players, ROSTER_SIZE);
        double expectedShare = Math.min(1.0, Math.min(foodGain, energyGain)
            / ruleset.dailyLossCoefficient());

        assertThat(settledSharePercent(players, matchesEach) / 100.0)
            .isCloseTo(expectedShare, org.assertj.core.data.Offset.offset(0.015));
    }

    /**
     * Runs a regime to its settling point and returns the share of capacity it holds.
     *
     * @param players     players turning up each day
     * @param matchesEach matches each of them plays
     * @return settled population as a percentage of capacity
     */
    private double settledSharePercent(int players, int matchesEach) {
        return settledShareOf((int) Math.round(dailyDamage(players, matchesEach)), players) * 100.0;
    }

    /**
     * Runs one constant regime to its settling point.
     *
     * @param matchDamage total daily match damage
     * @param players     players turning up each day
     * @return settled population as a share of capacity
     */
    private double settledShareOf(int matchDamage, int players) {
        List<ColonyDailyInput> days = new ArrayList<>(DAYS_TO_SETTLE);
        for (int index = 0; index < DAYS_TO_SETTLE; index++) {
            days.add(new ColonyDailyInput(DAY_ONE.plusDays(index), matchDamage, players, 0));
        }

        ColonyDayState settled = engine.replay(days, ROSTER_SIZE).getLast();

        return settled.population() / settled.capacity();
    }

    /**
     * Returns the Food a regime produces daily.
     *
     * @param players     players turning up each day
     * @param matchesEach matches each of them plays
     * @return daily Food gain
     */
    private double dailyFood(int players, int matchesEach) {
        return dailyDamage(players, matchesEach) / ruleset.foodDamageDivisor();
    }

    /**
     * Returns the damage a regime produces daily, after the scoring ruleset's diminishing returns.
     *
     * <p>Applied here rather than assumed away: a player's sixth to ninth game of a day is worth half,
     * and everything past the ninth a quarter. That reduction is precisely why grinding produces less
     * than its raw volume suggests.
     *
     * @param players     players turning up each day
     * @param matchesEach matches each of them plays
     * @return total daily match damage
     */
    private double dailyDamage(int players, int matchesEach) {
        double perPlayer = 0.0;
        for (int rank = 1; rank <= matchesEach; rank++) {
            perPlayer += AVERAGE_COMPETITIVE_DAMAGE
                * new DefaultScoringRuleset().matchDamageCoefficientPercent(rank) / 100.0;
        }

        return perPlayer * players;
    }
}
