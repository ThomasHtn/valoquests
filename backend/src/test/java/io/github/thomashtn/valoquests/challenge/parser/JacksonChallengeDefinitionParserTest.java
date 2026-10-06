package io.github.thomashtn.valoquests.challenge.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.exception.InvalidChallengeDefinitionException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGameMode;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
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

        var definition = parser.parse(challenge, CampaignDifficulty.AMATEUR);
        var condition = definition.singleCondition();

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

        var definition = parser.parse(challenge, CampaignDifficulty.AMATEUR);
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
            ProgressMode.SUM,
            "[invalid-json]"
        );

        assertThatThrownBy(() -> parser.parse(challenge, CampaignDifficulty.AMATEUR))
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

        assertThatThrownBy(() -> parser.parse(challenge, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("requires a groupBy value");
    }

    /**
     * Verifies that occurrence challenges require a positive occurrence target.
     */
    @Test
    void shouldRejectOccurrenceChallengeWithoutOccurrences() {
        Challenge challenge = createChallenge(
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

        assertThatThrownBy(() -> parser.parse(challenge, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("positive occurrences value");
    }

    @Test
    @DisplayName("Rejects a RATIO challenge on a total rather than a rate")
    void shouldRejectARatioChallengeOnATotal() {
        Challenge challenge = createChallenge(
            ProgressMode.RATIO,
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":100,\"gameMode\":\"COMPETITIVE\","
                + "\"minimumMatches\":15}]"
        );

        assertThatThrownBy(() -> parser.parse(challenge, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("RATIO requires a rate metric");
    }

    @Test
    @DisplayName("Rejects a RATIO challenge without a positive minimum sample")
    void shouldRejectARatioChallengeWithoutMinimumMatches() {
        Challenge withoutSample = createChallenge(
            ProgressMode.RATIO,
            "[{\"metric\":\"KD\",\"operator\":\"GTE\",\"target\":1.2,\"gameMode\":\"COMPETITIVE\"}]"
        );
        Challenge emptySample = createChallenge(
            ProgressMode.RATIO,
            "[{\"metric\":\"KD\",\"operator\":\"GTE\",\"target\":1.2,\"gameMode\":\"COMPETITIVE\","
                + "\"minimumMatches\":0}]"
        );

        assertThatThrownBy(() -> parser.parse(withoutSample, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("positive minimumMatches");
        assertThatThrownBy(() -> parser.parse(emptySample, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("positive minimumMatches");
    }

    @Test
    @DisplayName("Accepts the PLAY_DAY metric only in a DISTINCT_COUNT challenge")
    void shouldAcceptPlayDayOnlyInADistinctCount() {
        String playDays = "[{\"metric\":\"PLAY_DAY\",\"operator\":\"GTE\",\"target\":3,"
            + "\"gameMode\":\"ANY\",\"groupBy\":\"PLAY_DAY\"}]";
        Challenge distinctDays = createChallenge(ProgressMode.DISTINCT_COUNT, playDays);
        Challenge busiestDay = createChallenge(ProgressMode.MAX_GROUP, playDays);
        Challenge streak = createChallenge(
            ProgressMode.MAX_STREAK,
            "[{\"metric\":\"PLAY_DAY\",\"operator\":\"GTE\",\"target\":1,\"gameMode\":\"ANY\","
                + "\"scope\":\"PER_MATCH\",\"streak\":3}]"
        );

        assertThat(parser.parse(distinctDays, CampaignDifficulty.AMATEUR).singleCondition().metric())
            .isEqualTo(ChallengeMetric.PLAY_DAY);
        assertThatThrownBy(() -> parser.parse(busiestDay, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("PLAY_DAY metric is only valid with DISTINCT_COUNT");
        assertThatThrownBy(() -> parser.parse(streak, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("PLAY_DAY metric is only valid with DISTINCT_COUNT");
    }

    @Test
    @DisplayName("Rejects a MAX_STREAK challenge outside the PER_MATCH scope or without a positive streak")
    void shouldRejectAnIllFormedStreak() {
        Challenge noScope = createChallenge(
            ProgressMode.MAX_STREAK,
            "[{\"metric\":\"KD\",\"operator\":\"GTE\",\"target\":1,\"gameMode\":\"ANY\",\"streak\":3}]"
        );
        Challenge noStreak = createChallenge(
            ProgressMode.MAX_STREAK,
            "[{\"metric\":\"KD\",\"operator\":\"GTE\",\"target\":1,\"gameMode\":\"ANY\","
                + "\"scope\":\"PER_MATCH\",\"streak\":0}]"
        );

        assertThatThrownBy(() -> parser.parse(noScope, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("PER_MATCH");
        assertThatThrownBy(() -> parser.parse(noStreak, CampaignDifficulty.AMATEUR))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("positive streak");
    }

    /**
     * Verifies that a selection is parsed from its resolved conditions, not the catalogue's.
     */
    @Test
    void shouldParseTheResolvedConditionsOfASelection() {
        Challenge challenge = createChallenge(
            ProgressMode.SUM,
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":60,\"gameMode\":\"COMPETITIVE\"}]"
        );
        ChallengeSelection selection = new ChallengeSelection();
        selection.setChallenge(challenge);
        selection.setResolvedConditionsJson(
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":135,\"gameMode\":\"COMPETITIVE\"}]"
        );

        var definition = parser.parse(selection);

        assertThat(definition.progressMode()).isEqualTo(ProgressMode.SUM);
        assertThat(definition.singleCondition().target()).isEqualByComparingTo(BigDecimal.valueOf(135));
    }

    @Test
    @DisplayName("Reads a selection stored with the derived metric flags conditions no longer carry")
    void shouldReadAResolvedConditionStoredWithLegacyFlags() {
        JacksonChallengeDefinitionParser strictParser = new JacksonChallengeDefinitionParser(
            JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()
        );
        ChallengeSelection selection = new ChallengeSelection();
        selection.setChallenge(createChallenge(
            ProgressMode.SUM,
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":60}]"
        ));
        selection.setResolvedConditionsJson(
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":135,"
                + "\"rateMetric\":false,\"ratioMetric\":false,\"matchCountMetric\":false}]"
        );

        assertThat(strictParser.parse(selection).singleCondition().target())
            .isEqualByComparingTo(BigDecimal.valueOf(135));
    }

    /**
     * Verifies that a selection without resolved conditions is rejected like a blank rule.
     */
    @Test
    void shouldRejectASelectionWithoutResolvedConditions() {
        ChallengeSelection selection = new ChallengeSelection();
        selection.setChallenge(createChallenge(ProgressMode.SUM, "[]"));

        assertThatThrownBy(() -> parser.parse(selection))
            .isInstanceOf(InvalidChallengeDefinitionException.class)
            .hasMessageContaining("must not be blank");
    }

    /**
     * Verifies that written conditions read back identical, without null fields.
     */
    @Test
    void shouldWriteConditionsThatReadBackIdentical() {
        Challenge challenge = createChallenge(
            ProgressMode.COUNT_MATCHES,
            """
                [
                  {
                    "metric": "KD",
                    "operator": "GTE",
                    "target": 1.2,
                    "gameMode": "COMPETITIVE_OR_UNRATED",
                    "occurrences": 6,
                    "scope": "PER_MATCH"
                  }
                ]
                """
        );
        List<?> conditions = parser.parse(challenge, CampaignDifficulty.AMATEUR).conditions();

        String json = parser.toJson(parser.parse(challenge, CampaignDifficulty.AMATEUR).conditions());

        assertThat(json).doesNotContain("null");
        assertThat(json).contains("\"target\":1.2", "\"occurrences\":6", "\"scope\":\"PER_MATCH\"");

        challenge.setAmateurConditionsJson(json);
        challenge.setProConditionsJson(challenge.getAmateurConditionsJson());

        assertThat(parser.parse(challenge, CampaignDifficulty.AMATEUR).conditions()).isEqualTo(conditions);
    }

    /**
     * Creates a valid challenge entity for parser tests.
     *
     * @param progressMode   challenge progress mode
     * @param conditionsJson serialized conditions
     * @return configured challenge
     */
    private Challenge createChallenge(
        ProgressMode progressMode,
        String conditionsJson
    ) {
        Challenge challenge = new Challenge();

        challenge.setCode("TEST_CHALLENGE");
        challenge.setProgressMode(progressMode);
        challenge.setAmateurConditionsJson(conditionsJson);
        challenge.setProConditionsJson(challenge.getAmateurConditionsJson());
        challenge.setSchemaVersion(3);

        return challenge;
    }
}
