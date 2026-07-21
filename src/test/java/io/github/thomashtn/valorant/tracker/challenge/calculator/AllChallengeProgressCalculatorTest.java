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

/**
 * Tests composite all-condition challenge-progress calculations.
 */
class AllChallengeProgressCalculatorTest {

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
    private final AllChallengeProgressCalculator calculator =
        new AllChallengeProgressCalculator(
            metricEvaluator,
            matchFilter
        );

    /**
     * Verifies that a challenge is completed when every condition reaches its
     * target.
     */
    @Test
    void shouldCompleteChallengeWhenAllConditionsReachTheirTargets() {
        ChallengeDefinition definition = createDefinition(
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.DEATHMATCH,
                2
            ),
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.TEAM_DEATHMATCH,
                2
            )
        );

        PlayerChallengeContext context = createContext(
            createMatches(GameMode.DEATHMATCH, 2, 0),
            createMatches(GameMode.TEAM_DEATHMATCH, 2, 0)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("4");
        assertThat(result.targetValue())
            .isEqualByComparingTo("4");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("100.00");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Verifies that the challenge remains incomplete when one condition has
     * not reached its target.
     */
    @Test
    void shouldRemainIncompleteWhenOneConditionIsMissingProgress() {
        ChallengeDefinition definition = createDefinition(
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.DEATHMATCH,
                2
            ),
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.TEAM_DEATHMATCH,
                2
            )
        );

        PlayerChallengeContext context = createContext(
            createMatches(GameMode.DEATHMATCH, 2, 0),
            createMatches(GameMode.TEAM_DEATHMATCH, 1, 0)
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
     * Verifies that excessive progress on one condition does not compensate
     * for an incomplete second condition.
     */
    @Test
    void shouldNotCompensateMissingConditionWithExcessProgress() {
        ChallengeDefinition definition = createDefinition(
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.DEATHMATCH,
                2
            ),
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.TEAM_DEATHMATCH,
                2
            )
        );

        PlayerChallengeContext context = createContext(
            createMatches(GameMode.DEATHMATCH, 10, 0),
            createMatches(GameMode.TEAM_DEATHMATCH, 1, 0)
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
     * Verifies that kill totals are calculated independently for each game
     * mode.
     */
    @Test
    void shouldCalculateKillConditionsIndependently() {
        ChallengeDefinition definition = createDefinition(
            createCondition(
                ChallengeMetric.KILLS,
                ChallengeGameMode.DEATHMATCH,
                100
            ),
            createCondition(
                ChallengeMetric.KILLS,
                ChallengeGameMode.TEAM_DEATHMATCH,
                200
            )
        );

        PlayerChallengeContext context = createContext(
            createMatches(GameMode.DEATHMATCH, 2, 50),
            createMatches(GameMode.TEAM_DEATHMATCH, 2, 75)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("250");
        assertThat(result.targetValue())
            .isEqualByComparingTo("300");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("83.33");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Verifies that unrelated game modes do not contribute to any condition.
     */
    @Test
    void shouldIgnoreUnrelatedGameModes() {
        ChallengeDefinition definition = createDefinition(
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.DEATHMATCH,
                2
            ),
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.TEAM_DEATHMATCH,
                2
            )
        );

        PlayerChallengeContext context = createContext(
            createMatches(GameMode.DEATHMATCH, 1, 0),
            createMatches(GameMode.TEAM_DEATHMATCH, 1, 0),
            createMatches(GameMode.COMPETITIVE, 10, 0)
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
     * Verifies that an empty context produces zero progress.
     */
    @Test
    void shouldReturnZeroWhenNoMatchIsAvailable() {
        ChallengeDefinition definition = createDefinition(
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.DEATHMATCH,
                10
            ),
            createCondition(
                ChallengeMetric.MATCHES_PLAYED,
                ChallengeGameMode.TEAM_DEATHMATCH,
                10
            )
        );

        ChallengeProgressResult result = calculator.calculate(
            definition,
            createContext()
        );

        assertThat(result.currentValue())
            .isEqualByComparingTo("0");
        assertThat(result.targetValue())
            .isEqualByComparingTo("20");
        assertThat(result.progressPercentage())
            .isEqualByComparingTo("0.00");
        assertThat(result.completed()).isFalse();
    }

    /**
     * Creates a composite definition requiring every supplied condition.
     *
     * @param conditions challenge conditions
     * @return configured challenge definition
     */
    private ChallengeDefinition createDefinition(
        ChallengeCondition... conditions
    ) {
        return new ChallengeDefinition(
            3,
            ChallengeRuleType.COMPOSITE,
            ProgressMode.ALL,
            List.of(conditions)
        );
    }

    /**
     * Creates one challenge condition.
     *
     * @param metric   evaluated metric
     * @param gameMode eligible game mode
     * @param target   required value
     * @return configured condition
     */
    private ChallengeCondition createCondition(
        ChallengeMetric metric,
        ChallengeGameMode gameMode,
        int target
    ) {
        return new ChallengeCondition(
            metric,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(target),
            gameMode,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Creates a calculation context from several groups of matches.
     *
     * @param matchGroups groups of player matches
     * @return weekly player context
     */
    @SafeVarargs
    private PlayerChallengeContext createContext(
        List<PlayerMatch>... matchGroups
    ) {
        List<PlayerMatch> playerMatches = new ArrayList<>();

        for (List<PlayerMatch> matchGroup : matchGroups) {
            playerMatches.addAll(matchGroup);
        }

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
     * @param kills    kills recorded in every match
     * @return created player matches
     */
    private List<PlayerMatch> createMatches(
        GameMode gameMode,
        int count,
        int kills
    ) {
        List<PlayerMatch> playerMatches = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            playerMatches.add(
                createMatch(
                    gameMode,
                    kills,
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
     * @param index    timestamp offset
     * @return configured player match
     */
    private PlayerMatch createMatch(
        GameMode gameMode,
        int kills,
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
        playerMatch.setResult(MatchResult.WIN);
        playerMatch.setKills(kills);

        return playerMatch;
    }
}
