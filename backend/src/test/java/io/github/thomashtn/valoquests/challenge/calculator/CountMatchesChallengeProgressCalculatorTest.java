package io.github.thomashtn.valoquests.challenge.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScope;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests per-match occurrence challenge calculations.
 */
class CountMatchesChallengeProgressCalculatorTest {

    /**
     * Calculator under test.
     */
    private final CountMatchesChallengeProgressCalculator calculator =

        new CountMatchesChallengeProgressCalculator(
            new ChallengeMetricEvaluator(),
            new ChallengeMatchFilter()
        );

    /**
     * Verifies that only matching Deathmatch performances are counted.
     */
    @Test
    void shouldCountMatchesReachingKillTarget() {
        ChallengeCondition condition = new ChallengeCondition(
            ChallengeMetric.KILLS,
            ChallengeOperator.GTE,
            BigDecimal.valueOf(30),
            ChallengeGameMode.DEATHMATCH,
            null,
            ChallengeScope.PER_MATCH,
            3,
            null,
            null
        );

        ChallengeDefinition definition = new ChallengeDefinition(
            3,
            ChallengeRuleType.OCCURRENCE,
            ProgressMode.COUNT_MATCHES,
            List.of(condition)
        );

        PlayerChallengeContext context = createContext(
            createMatch(GameMode.DEATHMATCH, 30, 10),
            createMatch(GameMode.DEATHMATCH, 35, 15),
            createMatch(GameMode.DEATHMATCH, 29, 5),
            createMatch(GameMode.COMPETITIVE, 40, 20)
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
     * Verifies that a K/D challenge handles deathless matches safely.
     */
    @Test
    void shouldCountDeathlessMatchForKdCondition() {
        ChallengeCondition condition = new ChallengeCondition(
            ChallengeMetric.KD,
            ChallengeOperator.GTE,
            BigDecimal.ONE,
            ChallengeGameMode.DEATHMATCH,
            null,
            ChallengeScope.PER_MATCH,
            1,
            null,
            null
        );

        ChallengeDefinition definition = new ChallengeDefinition(
            3,
            ChallengeRuleType.OCCURRENCE,
            ProgressMode.COUNT_MATCHES,
            List.of(condition)
        );

        PlayerChallengeContext context = createContext(
            createMatch(GameMode.DEATHMATCH, 5, 0)
        );

        ChallengeProgressResult result =
            calculator.calculate(definition, context);

        assertThat(result.currentValue())
            .isEqualByComparingTo("1");
        assertThat(result.completed()).isTrue();
    }

    /**
     * Creates a weekly context from supplied matches.
     *
     * @param playerMatches player matches
     * @return calculation context
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
     * @param gameMode game mode
     * @param kills    number of kills
     * @param deaths   number of deaths
     * @return configured player match
     */
    private PlayerMatch createMatch(
        GameMode gameMode,
        int kills,
        int deaths
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setKills(kills);
        playerMatch.setDeaths(deaths);

        return playerMatch;
    }
}
