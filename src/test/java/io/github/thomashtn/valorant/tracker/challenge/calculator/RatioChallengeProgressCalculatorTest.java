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
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests ratio-based challenge-progress calculations.
 */
class RatioChallengeProgressCalculatorTest {

    /**
     * Match filter used by the calculator.
     */
    private final ChallengeMatchFilter matchFilter =

        new ChallengeMatchFilter();

    /**
     * Calculator under test.
     */
    private final RatioChallengeProgressCalculator calculator =

        new RatioChallengeProgressCalculator(matchFilter);

    /**
     * Verifies that the weekly K/D is calculated from total kills and deaths.
     */
    @Test
    void shouldCalculateGlobalKillDeathRatio() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.20,
            2
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                GameMode.COMPETITIVE,
                20,
                10,
                0
            ),
            createMatch(
                GameMode.COMPETITIVE,
                5,
                10,
                1
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1.2500");
        assertThat(result.targetValue())
            .isEqualByComparingTo("1.2");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that per-match ratios are not averaged.
     */
    @Test
    void shouldNotAveragePerMatchRatios() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.50,
            2
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                GameMode.COMPETITIVE,
                20,
                10,
                0
            ),
            createMatch(
                GameMode.COMPETITIVE,
                5,
                10,
                1
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1.2500");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("83.33");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that a sufficient ratio does not complete the challenge before
     * the minimum number of matches is reached.
     */
    @Test
    void shouldRemainIncompleteBeforeMinimumMatchCount() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.20,
            15
        );

        PlayerChallengeContext context = createContext(
            createMatches(
                GameMode.COMPETITIVE,
                14,
                15,
                10
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1.5000");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that the challenge completes once both the ratio and minimum
     * match requirements are satisfied.
     */
    @Test
    void shouldCompleteWhenRatioAndMinimumMatchCountAreReached() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.20,
            15
        );

        PlayerChallengeContext context = createContext(
            createMatches(
                GameMode.COMPETITIVE,
                15,
                12,
                10
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1.2000");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that matches outside the configured game mode are ignored.
     */
    @Test
    void shouldIgnoreMatchesFromOtherGameModes() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.20,
            2
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                GameMode.COMPETITIVE,
                12,
                10,
                0
            ),
            createMatch(
                GameMode.COMPETITIVE,
                12,
                10,
                1
            ),
            createMatch(
                GameMode.DEATHMATCH,
                100,
                0,
                2
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1.2000");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that a deathless week uses the total number of kills as its
     * ratio value.
     */
    @Test
    void shouldUseTotalKillsWhenNoDeathIsRecorded() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.20,
            2
        );

        PlayerChallengeContext context = createContext(
            createMatch(
                GameMode.COMPETITIVE,
                8,
                0,
                0
            ),
            createMatch(
                GameMode.COMPETITIVE,
                6,
                0,
                1
            )
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("14");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that an empty weekly context produces zero progress.
     */
    @Test
    void shouldReturnZeroWhenNoMatchIsAvailable() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            1.20,
            15
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
     * Verifies that unsupported metrics are rejected clearly.
     */
    @Test
    void shouldRejectUnsupportedRatioMetric() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KILLS,
            100,
            15
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported ratio metric")
            .hasMessageContaining("KILLS");
    }

    /**
     * Verifies that a minimum match requirement is mandatory.
     */
    @Test
    void shouldRejectDefinitionWithoutMinimumMatches() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            BigDecimal.valueOf(1.20),
            null
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minimum number of matches");
    }

    /**
     * Verifies that the minimum match requirement must be positive.
     */
    @Test
    void shouldRejectNonPositiveMinimumMatches() {
        ChallengeDefinition definition = createDefinition(
            ChallengeMetric.KD,
            BigDecimal.valueOf(1.20),
            0
        );

        assertThatThrownBy(
            () -> calculator.calculate(
                definition,
                createContext()
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than zero");
    }

    /**
     * Creates a ratio challenge definition.
     *
     * @param metric         ratio metric
     * @param target         required ratio
     * @param minimumMatches minimum eligible match count
     * @return configured challenge definition
     */
    private ChallengeDefinition createDefinition(
        ChallengeMetric metric,
        double target,
        Integer minimumMatches
    ) {
        return createDefinition(
            metric,
            BigDecimal.valueOf(target),
            minimumMatches
        );
    }

    /**
     * Creates a ratio challenge definition.
     *
     * @param metric         ratio metric
     * @param target         required ratio
     * @param minimumMatches minimum eligible match count
     * @return configured challenge definition
     */
    private ChallengeDefinition createDefinition(
        ChallengeMetric metric,
        BigDecimal target,
        Integer minimumMatches
    ) {
        ChallengeCondition condition = new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            target,
            ChallengeGameMode.COMPETITIVE,
            null,
            null,
            null,
            null,
            minimumMatches
        );

        return new ChallengeDefinition(
            3,
            ChallengeRuleType.RATIO,
            ProgressMode.RATIO,
            List.of(condition)
        );
    }

    /**
     * Creates a weekly context from individual matches.
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
     * Creates a weekly context from a match list.
     *
     * @param playerMatches matches included in the context
     * @return weekly player context
     */
    private PlayerChallengeContext createContext(
        List<PlayerMatch> playerMatches
    ) {
        return new PlayerChallengeContext(
            1L,
            LocalDate.of(2026, 7, 20),
            Instant.parse("2026-07-20T00:00:00Z"),
            Instant.parse("2026-07-27T00:00:00Z"),
            playerMatches
        );
    }

    /**
     * Creates several matches sharing the same mode and statistics.
     *
     * @param gameMode game mode
     * @param count    number of matches
     * @param kills    kills recorded in each match
     * @param deaths   deaths recorded in each match
     * @return created player matches
     */
    private List<PlayerMatch> createMatches(
        GameMode gameMode,
        int count,
        int kills,
        int deaths
    ) {
        List<PlayerMatch> playerMatches = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            playerMatches.add(
                createMatch(
                    gameMode,
                    kills,
                    deaths,
                    index
                )
            );
        }

        return playerMatches;
    }

    /**
     * Creates one player match.
     *
     * @param gameMode game mode
     * @param kills    number of kills
     * @param deaths   number of deaths
     * @param index    timestamp offset
     * @return configured player match
     */
    private PlayerMatch createMatch(
        GameMode gameMode,
        int kills,
        int deaths,
        int index
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);
        match.setStartedAt(
            Instant.parse("2026-07-20T08:00:00Z")
                .plusSeconds(index * 60L)
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
