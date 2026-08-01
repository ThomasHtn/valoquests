package io.github.thomashtn.valorant.tracker.challenge.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests maximum-group challenge-progress calculations.
 */
class MaxGroupChallengeProgressCalculatorTest {

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
    private final MaxGroupChallengeProgressCalculator calculator =

        new MaxGroupChallengeProgressCalculator(
            metricEvaluator,
            matchFilter,
            new WeekCalendar(Clock.systemUTC(), ZoneOffset.UTC)
        );

    /**
     * Verifies that the calculator returns the largest number of matches
     * played with the same agent.
     */
    @Test
    void shouldReturnLargestMatchCountForOneAgent() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            4
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
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-20T09:00:00Z"
            ),
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T10:00:00Z"
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T11:00:00Z"
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-20T12:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("3");
        assertThat(result.targetValue())
            .isEqualByComparingTo("4");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("75.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that metric values are accumulated within each group.
     */
    @Test
    void shouldAccumulateMetricValuesWithinSameGroup() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KILLS,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            20
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T08:00:00Z",
                8
            ),
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.COMPETITIVE,
                MatchResult.LOSS,
                "2026-07-20T09:00:00Z",
                7
            ),
            createMatch(
                "agent-sova",
                "Sova",
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T10:00:00Z",
                12
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("15");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("75.00");
    }

    /**
     * Verifies that matches outside the configured game mode are ignored.
     */
    @Test
    void shouldIgnoreMatchesFromOtherGameModes() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            2
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
                "agent-jett",
                "Jett",
                GameMode.DEATHMATCH,
                MatchResult.WIN,
                "2026-07-20T09:00:00Z"
            ),
            createMatch(
                "agent-jett",
                "Jett",
                GameMode.DEATHMATCH,
                MatchResult.WIN,
                "2026-07-20T10:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that an empty context produces zero progress.
     */
    @Test
    void shouldReturnZeroWhenNoMatchIsAvailable() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            12
        );

        ChallengeProgressResult result = calculator.calculate(
            definition,
            createContext()
        );

        assertThat(result.currentValue())
            .isEqualByComparingTo("0");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("0.00");
        assertThat(result.completed()).isFalse();
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
     * Verifies that matches without an available grouping value are ignored.
     */
    @Test
    void shouldIgnoreMatchWithoutGroupingValue() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            ChallengeGroupBy.AGENT,
            1
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                null,
                null,
                GameMode.COMPETITIVE,
                MatchResult.WIN,
                "2026-07-20T08:00:00Z"
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("0");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that a grouping dimension is mandatory for this mode.
     */
    @Test
    void shouldRejectDefinitionWithoutGroupingDimension() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.MATCHES_PLAYED,
            ChallengeGameMode.COMPETITIVE,
            null,
            12
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("grouping dimension");
    }

    /**
     * Creates a single-condition maximum-group definition.
     *
     * @param metric   evaluated metric
     * @param gameMode eligible game mode
     * @param groupBy  grouping dimension
     * @param target   required maximum group value
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
            ProgressMode.MAX_GROUP,
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
     * Creates one player match without custom kill statistics.
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
        return createMatch(
            agentId,
            agentName,
            gameMode,
            result,
            startedAt,
            0
        );
    }

    /**
     * Creates one player match with custom kill statistics.
     *
     * @param agentId   stable agent identifier
     * @param agentName human-readable agent name
     * @param gameMode  normalized game mode
     * @param result    match result
     * @param startedAt match start timestamp
     * @param kills     number of kills
     * @return configured player match
     */
    private PlayerMatch createMatch(
        String agentId,
        String agentName,
        GameMode gameMode,
        MatchResult result,
        String startedAt,
        int kills
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);
        match.setStartedAt(Instant.parse(startedAt));

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setAgentId(agentId);
        playerMatch.setAgentName(agentName);
        playerMatch.setResult(result);
        playerMatch.setKills(kills);

        return playerMatch;
    }
}
