package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.JacksonChallengeDefinitionParser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link ChallengeSelectionFactory}: the draw resolves, the row stores.
 */
class ChallengeSelectionFactoryTest {

    /**
     * Monday of the drawn week.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Draw timestamp.
     */
    private static final Instant DRAWN_AT = Instant.parse("2026-07-20T00:10:00Z");

    /**
     * Parser shared by the factory and the assertions.
     */
    private final JacksonChallengeDefinitionParser parser =
        new JacksonChallengeDefinitionParser(JsonMapper.builder().build());

    /**
     * Verifies that a weekly selection stores its conditions scaled to the week's calibration and
     * reads back through the parser.
     */
    @Test
    void shouldStoreConditionsResolvedToTheWeeksCalibration() {
        ChallengeSelectionFactory factory = factory(new ChallengeScaling(BigDecimal.valueOf(2), Map.of()));

        WeeklyChallenge selection = factory.weekly(WEEK_START, challenge(), DRAWN_AT);

        assertThat(selection.getWeekStart()).isEqualTo(WEEK_START);
        assertThat(selection.getCadence()).isEqualTo(ChallengeCadence.WEEKLY);
        assertThat(selection.getDay()).isNull();
        assertThat(selection.getSelectedAt()).isEqualTo(DRAWN_AT);
        assertThat(selection.getResolvedConditionsJson()).doesNotContain("null");
        assertThat(parser.parse(selection).singleCondition().target())
            .isEqualByComparingTo(BigDecimal.valueOf(120));
        // The catalogue's own definition is untouched by the draw.
        assertThat(parser.parse(selection.getChallenge()).singleCondition().target())
            .isEqualByComparingTo(BigDecimal.valueOf(60));
    }

    /**
     * Verifies that a daily selection carries its day and the base targets outside any campaign.
     */
    @Test
    void shouldCreateADailySelectionOnItsDay() {
        ChallengeSelectionFactory factory = factory(ChallengeScaling.NONE);
        Challenge challenge = challenge();
        challenge.setCadence(ChallengeCadence.DAILY);
        challenge.setDifficulty(null);

        WeeklyChallenge selection = factory.daily(WEEK_START, WEEK_START.plusDays(2), challenge, DRAWN_AT);

        assertThat(selection.getCadence()).isEqualTo(ChallengeCadence.DAILY);
        assertThat(selection.getDay()).isEqualTo(WEEK_START.plusDays(2));
        assertThat(selection.getWeekStart()).isEqualTo(WEEK_START);
        assertThat(parser.parse(selection).singleCondition().target())
            .isEqualByComparingTo(BigDecimal.valueOf(60));
    }

    /**
     * Creates a factory whose calibration source hands back one scaling for every week.
     *
     * @param scaling scaling in force
     * @return factory under test
     */
    private ChallengeSelectionFactory factory(ChallengeScaling scaling) {
        return new ChallengeSelectionFactory(
            parser,
            new ChallengeTargetResolver(),
            weekStart -> new ChallengeCalibration(5_300, 1, scaling)
        );
    }

    /**
     * Creates a summed kill challenge with a base of sixty.
     *
     * @return challenge fixture
     */
    private Challenge challenge() {
        Challenge challenge = new Challenge();
        challenge.setId(1L);
        challenge.setCode("NORMAL_LONG_KILLS");
        challenge.setDifficulty(ChallengeDifficulty.NORMAL);
        challenge.setProgressMode(ProgressMode.SUM);
        challenge.setSchemaVersion(3);
        challenge.setConditionsJson(
            "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":60,\"gameMode\":\"COMPETITIVE_OR_UNRATED\"}]"
        );
        return challenge;
    }
}
