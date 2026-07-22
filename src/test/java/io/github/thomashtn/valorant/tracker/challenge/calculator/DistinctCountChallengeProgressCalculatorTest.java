package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCondition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeOperator;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests distinct-value challenge-progress calculations.
 */
class DistinctCountChallengeProgressCalculatorTest {

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
    private final DistinctCountChallengeProgressCalculator calculator =

        new DistinctCountChallengeProgressCalculator(
            metricEvaluator,
            matchFilter
        );

    /**
     * Verifies that duplicate agents and ineligible game modes are ignored.
     */
    @Test
    void shouldCountDistinctAgentsFromEligibleMatches() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            3
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T18:00:00Z"
            ),
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-21T18:00:00Z"
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-22T18:00:00Z"
            ),
            createMatch(
                "agent-neon",
                "Neon",
                GameMode.DEATHMATCH,
                MatchResult.WIN,
                "2026-07-23T18:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("2");
        assertThat(result.targetValue())
            .isEqualByComparingTo("3");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("66.67");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that only winning matches contribute to agent-win variety.
     */
    @Test
    void shouldCountOnlyAgentsUsedInWinningMatches() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_WON,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            2
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-20T18:00:00Z"
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-21T18:00:00Z"
            ),
            createMatch(
                "agent-neon",
                "Neon",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-22T18:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("2");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that several matches played on the same UTC day count once.
     */
    @Test
    void shouldCountDistinctUtcPlayDays() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.PLAY_DAY,
            ChallengeGameMode.ANY,
            ChallengeGroupBy.PLAY_DAY,
            3
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T08:00:00Z"
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.UNRATED,
                MatchResult.LOSS,
                "2026-07-20T22:00:00Z"
            ),
            createMatch(
                "agent-neon",
                "Neon",
                GameMode.SWIFTPLAY,
                MatchResult.WIN,
                "2026-07-21T00:30:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("2");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("66.67");
    }

    /**
     * Verifies that distinct normalized game modes are counted once each.
     */
    @Test
    void shouldCountDistinctGameModes() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.ANY,
            ChallengeGroupBy.GAME_MODE,
            3
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T08:00:00Z"
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-21T08:00:00Z"
            ),
            createMatch(
                "agent-neon",
                "Neon",
                GameMode.UNRATED,
                MatchResult.WIN,
                "2026-07-22T08:00:00Z"
            ),
            createMatch(
                "agent-omen",
                "Omen",
                GameMode.SWIFTPLAY,
                MatchResult.WIN,
                "2026-07-23T08:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("3");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that an agent name is used when its identifier is unavailable.
     */
    @Test
    void shouldFallbackToAgentNameWhenIdentifierIsMissing() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            1
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                null,
                "Killjoy",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T08:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that a grouping dimension is mandatory for this mode.
     */
    @Test
    void shouldRejectDefinitionWithoutGroupingDimension() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.ANY,
            null,
            2
        );

        PlayerChallengeContext context = createContext();

        assertThatThrownBy(
            () -> calculator.calculate(definition, context)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("grouping dimension");
    }

    /**
     * Creates a single-condition distinct-count definition.
     *
     * @param metric   evaluated metric
     * @param gameMode eligible game mode
     * @param groupBy  grouping dimension
     * @param target   required number of distinct values
     * @return configured challenge definition
     */
    private ChallengeDefinition createDefinition(
        ChallengeMetric metric,
        ChallengeGameMode gameMode,
        ChallengeGroupBy groupBy,
        int target
    ) {
        ChallengeCondition condition = new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            gameMode,
            groupBy,
            null,
            null,
            null,
            null
        );

        return new ChallengeDefinition(
            3,
            ChallengeRuleType.SINGLE,
            ProgressMode.DISTINCT_COUNT,
            List.of(condition)
        );
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
     * @param agentId   stable agent identifier
     * @param agentName human-readable agent name
     * @param gameMode  normalized game mode
     * @param result    match result
     * @param startedAt match start timestamp
     * @return configured player match
     */
    private PlayerMatch createMatch(
        String agentId,
        String agentName,
        GameMode gameMode,
        MatchResult result,
        String startedAt
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);
        match.setStartedAt(Instant.parse(startedAt));

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setAgentId(agentId);
        playerMatch.setAgentName(agentName);
        playerMatch.setResult(result);

        return playerMatch;
    }
}
