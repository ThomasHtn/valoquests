package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCondition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeOperator;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeScope;
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
 * Tests maximum consecutive-match streak calculations.
 */
class MaxStreakChallengeProgressCalculatorTest {

    /**
     * Calculator under test.
     */
    private final MaxStreakChallengeProgressCalculator calculator =

        new MaxStreakChallengeProgressCalculator(
            new ChallengeMetricEvaluator(),
            new ChallengeMatchFilter()
        );

    /**
     * Verifies that one uninterrupted sequence is calculated completely.
     */
    @Test
    void shouldCalculateUninterruptedStreak() {
        ChallengeDefinition definition = createDefinition(3);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(12, 10, 0),
            createCompetitiveMatch(15, 10, 1),
            createCompetitiveMatch(10, 10, 2)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("3");
        assertThat(result.targetValue())
            .isEqualByComparingTo("3");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that a failing match resets the current sequence.
     */
    @Test
    void shouldResetStreakWhenMatchFailsCondition() {
        ChallengeDefinition definition = createDefinition(4);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(15, 10, 0),
            createCompetitiveMatch(12, 10, 1),
            createCompetitiveMatch(9, 10, 2),
            createCompetitiveMatch(14, 10, 3)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("2");
        assertThat(result.targetValue())
            .isEqualByComparingTo("4");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("50.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that the longest sequence is retained when several streaks are
     * present.
     */
    @Test
    void shouldKeepLongestStreak() {
        ChallengeDefinition definition = createDefinition(5);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(12, 10, 0),
            createCompetitiveMatch(13, 10, 1),
            createCompetitiveMatch(8, 10, 2),
            createCompetitiveMatch(11, 10, 3),
            createCompetitiveMatch(12, 10, 4),
            createCompetitiveMatch(13, 10, 5),
            createCompetitiveMatch(14, 10, 6),
            createCompetitiveMatch(9, 10, 7)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("4");
        assertThat(result.targetValue())
            .isEqualByComparingTo("5");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("80.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that matches are reordered chronologically before calculation.
     */
    @Test
    void shouldSortMatchesChronologically() {
        ChallengeDefinition definition = createDefinition(3);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(12, 10, 3),
            createCompetitiveMatch(8, 10, 2),
            createCompetitiveMatch(11, 10, 0),
            createCompetitiveMatch(13, 10, 1)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("2");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that unrelated modes are ignored without interrupting the
     * competitive sequence.
     */
    @Test
    void shouldIgnoreOtherGameModesWithoutBreakingStreak() {
        ChallengeDefinition definition = createDefinition(3);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(12, 10, 0),
            createMatch(
                GameMode.DEATHMATCH,
                0,
                20,
                1
            ),
            createCompetitiveMatch(11, 10, 2),
            createCompetitiveMatch(14, 10, 3)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("3");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that a deathless match satisfies a positive K/D threshold.
     */
    @Test
    void shouldIncludeDeathlessMatchInStreak() {
        ChallengeDefinition definition = createDefinition(2);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(8, 0, 0),
            createCompetitiveMatch(12, 10, 1)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("2");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that an empty context produces no streak progress.
     */
    @Test
    void shouldReturnZeroWhenNoMatchIsAvailable() {
        ChallengeDefinition definition = createDefinition(6);

        ChallengeProgressResult result = calculator.calculate(
            definition,
            createContext()
        );

        assertThat(result.currentValue())
            .isEqualByComparingTo("0");
        assertThat(result.targetValue())
            .isEqualByComparingTo("6");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("0.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that only the requested target is displayed when the maximum
     * streak exceeds it.
     */
    @Test
    void shouldCapPercentageWhenStreakExceedsTarget() {
        ChallengeDefinition definition = createDefinition(2);

        PlayerChallengeContext context = createContext(
            createCompetitiveMatch(12, 10, 0),
            createCompetitiveMatch(13, 10, 1),
            createCompetitiveMatch(14, 10, 2)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("3");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that streak challenges require the per-match scope.
     */
    @Test
    void shouldRejectDefinitionWithoutPerMatchScope() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            ChallengeScope.WEEKLY,
            3
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PER_MATCH");
    }

    /**
     * Verifies that the streak target must be present and positive.
     */
    @Test
    void shouldRejectDefinitionWithoutPositiveStreak() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            ChallengeScope.PER_MATCH,
            0
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive streak");
    }

    /**
     * Verifies that grouped calendar-day metrics cannot be evaluated as a
     * per-match streak.
     */
    @Test
    void shouldRejectPlayDayMetric() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.PLAY_DAY,
            ChallengeScope.PER_MATCH,
            3
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PLAY_DAY");
    }

    /**
     * Creates the production-like competitive K/D streak definition.
     *
     * @param streak required consecutive-match count
     * @return configured challenge definition
     */
    private ChallengeDefinition createDefinition(
        int streak
    ) {
        return createDefinition(
            ChallengeMetric.KD,
            ChallengeScope.PER_MATCH,
            streak
        );
    }

    /**
     * Creates a configurable streak challenge definition.
     *
     * @param metric metric evaluated for each match
     * @param scope  condition evaluation scope
     * @param streak required consecutive-match count
     * @return configured challenge definition
     */
    private ChallengeDefinition createDefinition(
        ChallengeMetric metric,
        ChallengeScope scope,
        int streak
    ) {
        ChallengeCondition condition = new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.ONE,
            ChallengeGameMode.COMPETITIVE,
            null,
            scope,
            null,
            streak,
            null
        );

        return new ChallengeDefinition(
            3,
            ChallengeRuleType.STREAK,
            ProgressMode.MAX_STREAK,
            List.of(condition)
        );
    }

    /**
     * Creates a weekly context containing the supplied matches.
     *
     * @param playerMatches matches included in the context
     * @return weekly player context
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
     * Creates one competitive player match.
     *
     * @param kills   recorded kills
     * @param deaths  recorded deaths
     * @param dayHour chronological offset in hours
     * @return configured player match
     */
    private PlayerMatch createCompetitiveMatch(
        int kills,
        int deaths,
        int dayHour
    ) {
        return createMatch(
            GameMode.COMPETITIVE,
            kills,
            deaths,
            dayHour
        );
    }

    /**
     * Creates one player match.
     *
     * @param gameMode game mode
     * @param kills    recorded kills
     * @param deaths   recorded deaths
     * @param dayHour  chronological offset in hours
     * @return configured player match
     */
    private PlayerMatch createMatch(
        GameMode gameMode,
        int kills,
        int deaths,
        int dayHour
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);
        match.setStartedAt(
            Instant.parse("2026-07-20T08:00:00Z")
                .plusSeconds(dayHour * 3_600L)
        );

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setAgentName("Jett");
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(deaths);

        return playerMatch;
    }
}
