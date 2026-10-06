package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.exception.ChallengeDrawException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ChallengeOperator;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the deterministic daily challenge draw and its no-repeat window.
 */
class DefaultDailyChallengeDrawServiceTest {

    /**
     * Monday used as the selected week.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Fixed selection timestamp.
     */
    private static final Instant SELECTION_TIME = Instant.parse("2026-07-20T08:00:00Z");

    /**
     * Size of the production daily pool since V45.
     */
    private static final int DAILY_POOL_SIZE = 16;

    /**
     * JSON the mocked parser writes for every resolved definition.
     */
    private static final String RESOLVED_JSON = "[{\"resolved\":true}]";

    /**
     * Challenge catalogue repository dependency.
     */
    private ChallengeRepository challengeRepository;

    /**
     * Daily selection repository dependency.
     */
    private ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Service under test.
     */
    private DefaultDailyChallengeDrawService service;

    /**
     * Creates the service and common mock behavior before each test.
     */
    @BeforeEach
    void setUp() {
        challengeRepository = mock(ChallengeRepository.class);
        challengeSelectionRepository = mock(ChallengeSelectionRepository.class);

        when(challengeSelectionRepository.save(any(ChallengeSelection.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChallengeDefinitionParser definitionParser = mock(ChallengeDefinitionParser.class);
        when(definitionParser.parse(any(Challenge.class), any())).thenReturn(sumDefinition());
        when(definitionParser.toJson(anyList())).thenReturn(RESOLVED_JSON);

        Clock clock = Clock.fixed(SELECTION_TIME, ZoneOffset.UTC);

        service = new DefaultDailyChallengeDrawService(
            challengeRepository,
            challengeSelectionRepository,
            new ChallengeSelectionFactory(
                definitionParser,
                weekStart -> new ChallengeCalibration(
                    CampaignDifficulty.AMATEUR.reference(),
                    1,
                    CampaignDifficulty.AMATEUR
                )
            ),
            clock,
            new WeekCalendar(clock, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that a day keeps the challenge it was given.
     */
    @Test
    void shouldReturnTheExistingDailyChallenge() {
        ChallengeSelection existing = createDailyChallenge(createDailyCandidate(0), WEEK_START);

        when(challengeSelectionRepository.findByCadenceAndDay(ChallengeCadence.DAILY, WEEK_START))
            .thenReturn(Optional.of(existing));

        assertThat(service.selectDailyChallenge(WEEK_START)).isSameAs(existing);

        verify(challengeRepository, never()).findAllByEnabledTrueAndCadenceOrderByIdAsc(any());
        verify(challengeSelectionRepository, never()).save(any(ChallengeSelection.class));
    }

    /**
     * Verifies that as many consecutive days as the pool holds draw as many different challenges,
     * and that the next day brings the first one back.
     */
    @Test
    void shouldNotRepeatADailyChallengeWithinThePoolSize() {
        List<ChallengeSelection> history = givenDailyHistory(DAILY_POOL_SIZE);

        List<String> codes = IntStream.range(0, DAILY_POOL_SIZE)
            .mapToObj(offset -> service.selectDailyChallenge(WEEK_START.plusDays(offset)))
            .map(selection -> selection.getChallenge().getCode())
            .toList();

        assertThat(codes).doesNotHaveDuplicates();
        assertThat(history).hasSize(DAILY_POOL_SIZE);
        assertThat(history)
            .allSatisfy(selection -> {
                assertThat(selection.getCadence()).isEqualTo(ChallengeCadence.DAILY);
                assertThat(selection.getDay()).isNotNull();
                assertThat(selection.getWeekStart()).isEqualTo(
                    selection.getDay().minusDays(selection.getDay().getDayOfWeek().getValue() - 1)
                );
                assertThat(selection.getResolvedConditionsJson()).isEqualTo(RESOLVED_JSON);
            });

        ChallengeSelection dayAfterPool = service.selectDailyChallenge(WEEK_START.plusDays(DAILY_POOL_SIZE));

        assertThat(dayAfterPool.getChallenge().getCode()).isEqualTo(codes.getFirst());
    }

    /**
     * Verifies that a pool smaller than the window brings back its least recently drawn challenge
     * rather than leaving a day without one.
     */
    @Test
    void shouldFallBackToTheLeastRecentlyDrawnDailyChallenge() {
        givenDailyHistory(3);

        List<String> codes = IntStream.range(0, 5)
            .mapToObj(offset -> service.selectDailyChallenge(WEEK_START.plusDays(offset)))
            .map(selection -> selection.getChallenge().getCode())
            .toList();

        assertThat(codes.subList(0, 3)).doesNotHaveDuplicates();
        assertThat(codes.get(3)).isEqualTo(codes.get(0));
        assertThat(codes.get(4)).isEqualTo(codes.get(1));
    }

    /**
     * Verifies that an empty daily pool is reported rather than silently skipped.
     */
    @Test
    void shouldRejectAnEmptyDailyPool() {
        givenDailyHistory(0);

        assertThatThrownBy(() -> service.selectDailyChallenge(WEEK_START))
            .isInstanceOf(ChallengeDrawException.class)
            .hasMessageContaining("daily pool is empty");
    }

    /**
     * Declares a daily pool and wires the repository mocks to an in-memory draw history.
     *
     * @param poolSize number of enabled daily challenges
     * @return the mutable history every draw is appended to
     */
    private List<ChallengeSelection> givenDailyHistory(int poolSize) {
        List<ChallengeSelection> history = new ArrayList<>();

        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.DAILY))
            .thenReturn(IntStream.range(0, poolSize).mapToObj(this::createDailyCandidate).toList());
        when(challengeSelectionRepository.findByCadenceAndDay(eq(ChallengeCadence.DAILY), any()))
            .thenAnswer(invocation -> history.stream()
                .filter(selection -> selection.getDay().equals(invocation.getArgument(1)))
                .findFirst());
        when(challengeSelectionRepository.findAllByCadenceAndDayBetweenOrderByDayAsc(
            eq(ChallengeCadence.DAILY),
            any(),
            any()
        ))
            .thenAnswer(invocation -> {
                LocalDate first = invocation.getArgument(1);
                LocalDate last = invocation.getArgument(2);

                return history.stream()
                    .filter(selection -> !selection.getDay().isBefore(first)
                        && !selection.getDay().isAfter(last))
                    .toList();
            });
        when(challengeSelectionRepository.save(any(ChallengeSelection.class)))
            .thenAnswer(invocation -> {
                ChallengeSelection saved = invocation.getArgument(0);
                history.add(saved);
                return saved;
            });

        return history;
    }

    /**
     * Creates one enabled daily challenge.
     *
     * @param index candidate index within the pool
     * @return challenge fixture
     */
    private Challenge createDailyCandidate(int index) {
        Challenge challenge = new Challenge();
        challenge.setId(1_000L + index);
        challenge.setCode("DAILY_" + index);
        challenge.setCadence(ChallengeCadence.DAILY);
        challenge.setCategory(ChallengeCategory.TRAINING);
        challenge.setProgressMode(ProgressMode.SUM);
        challenge.setEnabled(true);
        return challenge;
    }

    /**
     * Creates a persisted daily selection fixture.
     *
     * @param challenge daily catalogue challenge
     * @param day       covered day
     * @return daily selection fixture
     */
    private ChallengeSelection createDailyChallenge(Challenge challenge, LocalDate day) {
        ChallengeSelection selection = createSelection(challenge);
        selection.setCadence(ChallengeCadence.DAILY);
        selection.setDay(day);
        return selection;
    }

    /**
     * Creates the one-condition definition the mocked parser hands to the factory.
     *
     * @return summed definition
     */
    private ChallengeDefinition sumDefinition() {
        return new ChallengeDefinition(
            ProgressMode.SUM,
            List.of(new ChallengeCondition(
                ChallengeMetric.KILLS,
                ChallengeOperator.GTE,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                null
            ))
        );
    }

    /**
     * Creates a persisted weekly challenge fixture.
     *
     * @param challenge catalogue challenge
     * @return weekly challenge fixture
     */
    private ChallengeSelection createSelection(Challenge challenge) {
        ChallengeSelection selection = new ChallengeSelection();
        selection.setWeekStart(WEEK_START);
        selection.setChallenge(challenge);
        selection.setSelectedAt(SELECTION_TIME);
        return selection;
    }
}
