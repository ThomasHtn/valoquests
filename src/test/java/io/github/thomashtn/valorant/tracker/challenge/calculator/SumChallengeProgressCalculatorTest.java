package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCondition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeOperator;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests summed challenge-progress calculations.
 */
class SumChallengeProgressCalculatorTest {

    /**
     * Metric evaluator used by the calculator.
     */
    private final ChallengeMetricEvaluator metricEvaluator =
        new ChallengeMetricEvaluator();

    /**
     * Match filter used by the calculator.
     */
    private final ChallengeMatchFilter matchFilter =
        new ChallengeMatchFilter();

    /**
     * Calculator under test.
     */
    private final SumChallengeProgressCalculator calculator =
        new SumChallengeProgressCalculator(
            metricEvaluator,
            matchFilter
        );

    /**
     * Verifies that kills are summed only from the configured game mode.
     */
    @Test
    void shouldSumKillsFromEligibleMatches() {
        ChallengeCondition condition = new ChallengeCondition(
            ChallengeMetric.KILLS,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(50),
            ChallengeGameMode.COMPETITIVE,
            null,
            null,
            null,
            null,
            null
        );

        ChallengeDefinition definition = new ChallengeDefinition(
            3,
            ChallengeRuleType.SINGLE,
            ProgressMode.SUM,
            List.of(condition)
        );

        PlayerChallengeContext context = createContext(
            createMatch(GameMode.COMPETITIVE, 20),
            createMatch(GameMode.COMPETITIVE, 18),
            createMatch(GameMode.DEATHMATCH, 35)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("38");
        assertThat(result.targetValue())
            .isEqualByComparingTo("50");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("76.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Creates a calculation context from the supplied matches.
     *
     * @param playerMatches matches included in the context
     * @return weekly context
     */
    private PlayerChallengeContext createContext(
        PlayerMatch... playerMatches
    ) {
        return new PlayerChallengeContext(
            1L,
            LocalDate.of(2026, 7, 20),
            Instant.parse("2026-07-20T00:00:00Z"),
            Instant.parse("2026-07-27T00:00:00Z"),
            List.of(playerMatches)
        );
    }

    /**
     * Creates one player match for testing.
     *
     * @param gameMode match mode
     * @param kills    number of kills
     * @return configured player match
     */
    private PlayerMatch createMatch(
        GameMode gameMode,
        int kills
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setKills(kills);

        return playerMatch;
    }
}
