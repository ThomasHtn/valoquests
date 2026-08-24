package io.github.thomashtn.valoquests.challenge.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ChallengeProgressCalculator#findFirstCompletionIndex}, the default method used to
 * attribute a weekly-boss finishing blow to the exact match that unlocked a challenge.
 *
 * <p>Exercises both a monotonic calculator (Sum) and the one non-monotonic calculator in this
 * catalogue (Ratio), since the two require different reasoning about what "the triggering match" means.
 */
class ChallengeProgressCalculatorFirstCompletionTest {

    /** Metric evaluator shared by the calculators under test. */
    private final ChallengeMetricEvaluator metricEvaluator = new ChallengeMetricEvaluator(new MatchOutcomeResolver());

    /** Match filter shared by the calculators under test. */
    private final ChallengeMatchFilter matchFilter = new ChallengeMatchFilter(new MatchEligibility());

    /**
     * Verifies that a monotonic calculator reports the first match at which the target is reached.
     */
    @Test
    void shouldReportFirstCrossingForAMonotonicCalculator() {
        SumChallengeProgressCalculator calculator =
            new SumChallengeProgressCalculator(metricEvaluator, matchFilter);

        ChallengeDefinition definition = createSumKillsDefinition(30);

        PlayerChallengeContext context = createContext(
            createMatch(10),
            createMatch(15),
            createMatch(20)
        );

        // Cumulative kills: 10, 25, 45 — the target of 30 is first reached on the third match.
        OptionalInt completionIndex =
            calculator.findFirstCompletionIndex(definition, context);

        assertThat(completionIndex).hasValue(2);
    }

    /**
     * Verifies that a challenge never completed reports no completion index.
     */
    @Test
    void shouldReportNoIndexWhenTheTargetIsNeverReached() {
        SumChallengeProgressCalculator calculator =
            new SumChallengeProgressCalculator(metricEvaluator, matchFilter);

        ChallengeDefinition definition = createSumKillsDefinition(1_000);

        PlayerChallengeContext context = createContext(
            createMatch(10),
            createMatch(15)
        );

        assertThat(calculator.findFirstCompletionIndex(definition, context)).isEmpty();
    }

    /**
     * Verifies that an empty weekly context reports no completion index.
     */
    @Test
    void shouldReportNoIndexWhenNoMatchIsAvailable() {
        SumChallengeProgressCalculator calculator =
            new SumChallengeProgressCalculator(metricEvaluator, matchFilter);

        ChallengeDefinition definition = createSumKillsDefinition(30);

        assertThat(
            calculator.findFirstCompletionIndex(definition, createContext())
        ).isEmpty();
    }

    /**
     * Verifies that the non-monotonic Ratio calculator reports its first crossing even when the running
     * ratio later dips back below target.
     *
     * <p>This is the behaviour that makes completion latching consistent: the persisted progress keeps a
     * challenge completed once it has been reached, so the boss chronology has to credit the same match
     * rather than waiting for a crossing that holds to the end of the week.
     */
    @Test
    void shouldReportTheFirstCrossingForTheNonMonotonicRatioCalculator() {
        RatioChallengeProgressCalculator calculator =
            new RatioChallengeProgressCalculator(matchFilter, new AggregateRateCalculator());

        ChallengeCondition condition = new ChallengeCondition(
            ChallengeMetric.KD,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(1.5),
            ChallengeGameMode.COMPETITIVE,
            null,
            null,
            null,
            null,
            1
        );

        ChallengeDefinition definition = new ChallengeDefinition(
            3,
            ProgressMode.RATIO,
            List.of(condition)
        );

        PlayerChallengeContext context = createContext(
            // Match 0 alone: 3/1 = 3.0 — above target, and this is what latches the challenge.
            createMatchWithDeaths(3, 1),
            // Match 0+1: 3/11 = 0.27 — falls back below target, which no longer takes anything away.
            createMatchWithDeaths(0, 10),
            // Match 0+1+2: 23/12 = 1.92 — rises back above target.
            createMatchWithDeaths(20, 1)
        );

        OptionalInt completionIndex =
            calculator.findFirstCompletionIndex(definition, context);

        assertThat(completionIndex).hasValue(0);
    }

    /**
     * Creates a single-condition Sum challenge definition on kills.
     *
     * @param target kills required
     * @return configured challenge definition
     */
    private ChallengeDefinition createSumKillsDefinition(int target) {
        ChallengeCondition condition = new ChallengeCondition(
            ChallengeMetric.KILLS,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            ChallengeGameMode.COMPETITIVE,
            null,
            null,
            null,
            null,
            null
        );

        return new ChallengeDefinition(
            3,
            ProgressMode.SUM,
            List.of(condition)
        );
    }

    /**
     * Creates a weekly context from individual matches, in chronological order.
     *
     * @param playerMatches matches included in the context
     * @return weekly player context
     */
    private PlayerChallengeContext createContext(PlayerMatch... playerMatches) {
        return new PlayerChallengeContext(
            1L,
            LocalDate.of(2026, 7, 20),
            Instant.parse("2026-07-20T00:00:00Z"),
            Instant.parse("2026-07-27T00:00:00Z"),
            List.of(playerMatches)
        );
    }

    /**
     * Creates one competitive match with the given kill count.
     *
     * @param kills kills recorded in the match
     * @return configured player match
     */
    private PlayerMatch createMatch(int kills) {
        return createMatchWithDeaths(kills, 1);
    }

    /**
     * Creates one competitive match with the given kills and deaths.
     *
     * @param kills  kills recorded in the match
     * @param deaths deaths recorded in the match
     * @return configured player match
     */
    private PlayerMatch createMatchWithDeaths(int kills, int deaths) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(GameMode.COMPETITIVE);
        match.setStartedAt(
            Instant.parse("2026-07-20T08:00:00Z").plusSeconds(matchSequence++ * 60L)
        );

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setRoundsPlayed(20);
        playerMatch.setScore(4_000);
        playerMatch.setMatch(match);
        playerMatch.setAgentName("Jett");
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(deaths);

        return playerMatch;
    }

    /** Monotonically increasing offset used to keep fixture matches chronologically ordered. */
    private int matchSequence;
}
