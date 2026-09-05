package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.dto.ChallengeCatalogueResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ChallengeScaling;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.model.SkillAnchor;
import io.github.thomashtn.valoquests.challenge.parser.JacksonChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.scoring.DefaultScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests the challenge catalogue read model.
 */
class DefaultChallengeCatalogueQueryServiceTest {

    /**
     * Current Monday.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Catalogue repository dependency.
     */
    private ChallengeRepository challengeRepository;

    /**
     * Calibration source dependency.
     */
    private ChallengeCalibrationSource calibrationSource;

    /**
     * Service under test.
     */
    private DefaultChallengeCatalogueQueryService service;

    /**
     * Creates the service over a mocked catalogue and real rules.
     */
    @BeforeEach
    void setUp() {
        challengeRepository = mock(ChallengeRepository.class);
        calibrationSource = mock(ChallengeCalibrationSource.class);

        service = new DefaultChallengeCatalogueQueryService(
            challengeRepository,
            new JacksonChallengeDefinitionParser(JsonMapper.builder().build()),
            new ChallengeTargetResolver(),
            new DefaultScoringRuleset(),
            calibrationSource,
            new WeekCalendar(Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that entries carry their cadence, competitive flag, resolved target and worth.
     */
    @Test
    void shouldResolveEveryEntryAgainstTheCalibrationInForce() {
        when(calibrationSource.forWeek(WEEK_START)).thenReturn(new ChallengeCalibration(
            10_600,
            3,
            new ChallengeScaling(BigDecimal.valueOf(2), Map.of(SkillAnchor.LONG_KILLS, BigDecimal.valueOf(20)))
        ));
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
            weekly(
                1L,
                ChallengeDifficulty.VERY_HARD,
                ProgressMode.SUM,
                "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":90,\"gameMode\":\"COMPETITIVE\"}]"
            ),
            daily(
                2L,
                "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":13,\"gameMode\":\"COMPETITIVE_OR_UNRATED\","
                    + "\"occurrences\":1,\"scope\":\"PER_MATCH\"}]"
            )
        ));

        ChallengeCatalogueResponse response = service.findCatalogue();

        assertThat(response.reference()).isEqualTo(10_600);
        assertThat(response.challenges()).hasSize(2);

        ChallengeCatalogueResponse.ChallengeCatalogueEntry veryHard = response.challenges().getFirst();
        assertThat(veryHard.cadence()).isEqualTo(ChallengeCadence.WEEKLY);
        assertThat(veryHard.difficulty()).isEqualTo(ChallengeDifficulty.VERY_HARD);
        assertThat(veryHard.competitiveOnly()).isTrue();
        assertThat(veryHard.targetValue()).isEqualByComparingTo(BigDecimal.valueOf(180));
        // 10 600 x 5.4 / 1 000 x 1.08 (third week) = 61.8.
        assertThat(veryHard.survivors()).isEqualTo(62);
        assertThat(veryHard.rankingPoints()).isEqualTo(62);

        ChallengeCatalogueResponse.ChallengeCatalogueEntry daily = response.challenges().getLast();
        assertThat(daily.cadence()).isEqualTo(ChallengeCadence.DAILY);
        assertThat(daily.difficulty()).isNull();
        assertThat(daily.competitiveOnly()).isFalse();
        // The progress target of a count-matches challenge is its number of occurrences, fixed on a
        // daily; the bar itself moved to 20 x 0.85 = 17 kills.
        assertThat(daily.targetValue()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(daily.survivors()).isEqualTo(14);
        assertThat(daily.rankingPoints()).isEqualTo(14);
    }

    /**
     * Verifies that base targets are exposed untouched outside any campaign.
     */
    @Test
    void shouldExposeBaseTargetsOutsideAnyCampaign() {
        when(calibrationSource.forWeek(WEEK_START))
            .thenReturn(new ChallengeCalibration(2_000, 1, ChallengeScaling.NONE));
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(List.of(
            weekly(
                1L,
                ChallengeDifficulty.EASY,
                ProgressMode.SUM,
                "[{\"metric\":\"KILLS\",\"operator\":\"GTE\",\"target\":40,\"gameMode\":\"COMPETITIVE_OR_UNRATED\"}]"
            )
        ));

        ChallengeCatalogueResponse.ChallengeCatalogueEntry entry = service.findCatalogue().challenges().getFirst();

        assertThat(entry.targetValue()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(entry.survivors()).isEqualTo(2);
        assertThat(entry.rankingPoints()).isEqualTo(2);
    }

    /**
     * Creates one weekly catalogue entry.
     *
     * @param id             challenge identifier
     * @param difficulty     difficulty tier
     * @param progressMode   progress mode
     * @param conditionsJson base conditions
     * @return challenge fixture
     */
    private Challenge weekly(
        long id,
        ChallengeDifficulty difficulty,
        ProgressMode progressMode,
        String conditionsJson
    ) {
        Challenge challenge = new Challenge();
        challenge.setId(id);
        challenge.setCode("CHALLENGE_" + id);
        challenge.setName("Challenge " + id);
        challenge.setDescription("Description " + id);
        challenge.setDifficulty(difficulty);
        challenge.setProgressMode(progressMode);
        challenge.setConditionsJson(conditionsJson);
        challenge.setSchemaVersion(3);
        return challenge;
    }

    /**
     * Creates one daily catalogue entry counting matches that cleared a bar.
     *
     * @param id             challenge identifier
     * @param conditionsJson base conditions
     * @return challenge fixture
     */
    private Challenge daily(long id, String conditionsJson) {
        Challenge challenge = weekly(id, null, ProgressMode.COUNT_MATCHES, conditionsJson);
        challenge.setCadence(ChallengeCadence.DAILY);
        return challenge;
    }
}
