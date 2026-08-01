package io.github.thomashtn.valorant.tracker.challenge.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.exception.InvalidChallengeDefinitionException;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeRuleType;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests challenge JSON parsing and rule validation.
 */
class JacksonChallengeDefinitionParserTest {

    /**
     * Parser under test.
     */
    private JacksonChallengeDefinitionParser parser;

    /**
     * Creates a parser with a Jackson 3 JSON mapper.
     */
    @BeforeEach
    void setUp() {
        parser = new JacksonChallengeDefinitionParser(
            JsonMapper.builder().build()
        );
    }

    /**
     * Verifies that a summed challenge is parsed into a typed definition.
     */
    @Test
    void shouldParseSumChallenge() {
        Challenge challenge = createChallenge(
            ChallengeRuleType.SINGLE,
            ProgressMode.SUM,
            """
                [
                  {
                    "metric": "KILLS",
                    "operator": "GTE",
                    "target": 100,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );

        var definition = parser.parse(challenge);
        var condition = definition.singleCondition();

        assertThat(definition.schemaVersion()).isEqualTo(3);
        assertThat(definition.progressMode()).isEqualTo(ProgressMode.SUM);
        assertThat(condition.metric()).isEqualTo(ChallengeMetric.KILLS);
        assertThat(condition.target())
            .isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(condition.gameMode())
            .isEqualTo(ChallengeGameMode.COMPETITIVE);
    }

    /**
     * Verifies that decimal ratio targets remain precise.
     */
    @Test
    void shouldParseDecimalRatioTarget() {
        Challenge challenge = createChallenge(
            ChallengeRuleType.RATIO,
            ProgressMode.RATIO,
            """
                [
                  {
                    "metric": "KD",
                    "operator": "GTE",
                    "target": 1.2,
                    "gameMode": "COMPETITIVE",
                    "minimumMatches": 15
                  }
                ]
                """
        );

        var definition = parser.parse(challenge);
        var condition = definition.singleCondition();

        assertThat(condition.target())
            .isEqualByComparingTo(new BigDecimal("1.2"));
        assertThat(condition.minimumMatches()).isEqualTo(15);
    }

    /**
     * Verifies that malformed JSON produces a contextual exception.
     */
    @Test
    void shouldRejectMalformedJson() {
        Challenge challenge = createChallenge(
            ChallengeRuleType.SINGLE,
            ProgressMode.SUM,
            "[invalid-json]"
        );

        assertThatThrownBy(() -> parser.parse(challenge))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("TEST_CHALLENGE")
            .hasMessageContaining("cannot be parsed");
    }

    /**
     * Verifies that grouped modes require a grouping dimension.
     */
    @Test
    void shouldRejectGroupedChallengeWithoutGroupBy() {
        Challenge challenge = createChallenge(
            ChallengeRuleType.DISTINCT,
            ProgressMode.DISTINCT_COUNT,
            """
                [
                  {
                    "metric": "MATCHES_PLAYED",
                    "operator": "GTE",
                    "target": 5,
                    "gameMode": "COMPETITIVE"
                  }
                ]
                """
        );

        assertThatThrownBy(() -> parser.parse(challenge))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("requires a groupBy value");
    }

    /**
     * Verifies that occurrence challenges require a positive occurrence target.
     */
    @Test
    void shouldRejectOccurrenceChallengeWithoutOccurrences() {
        Challenge challenge = createChallenge(
            ChallengeRuleType.OCCURRENCE,
            ProgressMode.COUNT_MATCHES,
            """
                [
                  {
                    "metric": "KILLS",
                    "operator": "GTE",
                    "target": 30,
                    "gameMode": "DEATHMATCH",
                    "scope": "PER_MATCH"
                  }
                ]
                """
        );

        assertThatThrownBy(() -> parser.parse(challenge))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("positive occurrences value");
    }

    /**
     * Creates a valid challenge entity for parser tests.
     *
     * @param ruleType       challenge rule type
     * @param progressMode   challenge progress mode
     * @param conditionsJson serialized conditions
     * @return configured challenge
     */
    private Challenge createChallenge(
        ChallengeRuleType ruleType,
        ProgressMode progressMode,
        String conditionsJson
    ) {
        Challenge challenge = new Challenge();

        challenge.setCode("TEST_CHALLENGE");
        challenge.setRuleType(ruleType);
        challenge.setProgressMode(progressMode);
        challenge.setConditionsJson(conditionsJson);
        challenge.setSchemaVersion(3);

        return challenge;
    }
}
