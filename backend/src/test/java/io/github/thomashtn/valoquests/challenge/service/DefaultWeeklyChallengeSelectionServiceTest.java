package io.github.thomashtn.valoquests.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.exception.WeeklyChallengeSelectionException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests deterministic weekly challenge selection and validation rules.
 */
class DefaultWeeklyChallengeSelectionServiceTest {

    /**
     * Monday used as the selected week.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Fixed selection timestamp.
     */
    private static final Instant SELECTION_TIME = Instant.parse("2026-07-20T08:00:00Z");

    /**
     * Number of interchangeable candidates offered for each difficulty.
     */
    private static final int CATALOGUE_CHALLENGES_PER_DIFFICULTY = 10;

    /**
     * Number of consecutive weeks observed by the rotation test.
     */
    private static final int OBSERVED_WEEKS = 8;

    /**
     * Challenge catalogue repository dependency.
     */
    private ChallengeRepository challengeRepository;

    /**
     * Weekly selection repository dependency.
     */
    private WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Calculator registry dependency.
     */
    private ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Service under test.
     */
    private DefaultWeeklyChallengeSelectionService service;

    /**
     * Creates the service and common mock behavior before each test.
     */
    @BeforeEach
    void setUp() {
        challengeRepository = mock(ChallengeRepository.class);
        weeklyChallengeRepository = mock(WeeklyChallengeRepository.class);
        calculatorRegistry = mock(ChallengeProgressCalculatorRegistry.class);

        when(calculatorRegistry.supports(ProgressMode.SUM)).thenReturn(true);
        when(weeklyChallengeRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service = new DefaultWeeklyChallengeSelectionService(
            challengeRepository,
            weeklyChallengeRepository,
            calculatorRegistry,
            Clock.fixed(SELECTION_TIME, ZoneOffset.UTC),
            new WeekCalendar(Clock.fixed(SELECTION_TIME, ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that an existing complete pack is returned without catalogue access or writes.
     */
    @Test
    void shouldReturnExistingCompletePack() {
        List<WeeklyChallenge> existingSelections = Arrays.stream(ChallengeDifficulty.values())
            .map(difficulty -> createWeeklyChallenge(createChallenge(difficulty, categoryFor(difficulty))))
            .toList();

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(existingSelections.reversed());

        List<WeeklyChallenge> result = service.selectWeekChallenges(WEEK_START);

        assertThat(result)
            .extracting(selection -> selection.getChallenge().getDifficulty())
            .containsExactly(ChallengeDifficulty.values());

        verify(challengeRepository, never()).findAllByEnabledTrueOrderByIdAsc();
        verify(weeklyChallengeRepository, never()).saveAll(anyList());
    }

    /**
     * Verifies that one challenge is selected for every difficulty with distinct categories.
     */
    @Test
    void shouldCreateCategoryDiversePack() {
        List<Challenge> candidates = Arrays.stream(ChallengeDifficulty.values())
            .map(difficulty -> createChallenge(difficulty, categoryFor(difficulty)))
            .toList();

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        List<WeeklyChallenge> result = service.selectWeekChallenges(WEEK_START);

        assertThat(result).hasSize(ChallengeDifficulty.values().length);
        assertThat(result)
            .extracting(selection -> selection.getChallenge().getDifficulty())
            .containsExactly(ChallengeDifficulty.values());
        assertThat(result)
            .extracting(selection -> selection.getChallenge().getCategory())
            .doesNotHaveDuplicates();
        assertThat(result)
            .allSatisfy(selection -> {
                assertThat(selection.getWeekStart()).isEqualTo(WEEK_START);
                assertThat(selection.getSelectedAt()).isEqualTo(SELECTION_TIME);
            });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WeeklyChallenge>> captor = ArgumentCaptor.forClass(List.class);

        verify(weeklyChallengeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(ChallengeDifficulty.values().length);
    }

    /**
     * Verifies that duplicate categories are accepted only when a diverse pack is impossible.
     */
    @Test
    void shouldFallbackToDuplicateCategories() {
        List<Challenge> candidates = Arrays.stream(ChallengeDifficulty.values())
            .map(difficulty -> createChallenge(difficulty, ChallengeCategory.PERFORMANCE))
            .toList();

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        List<WeeklyChallenge> result = service.selectWeekChallenges(WEEK_START);

        assertThat(result).hasSize(ChallengeDifficulty.values().length);
        assertThat(result)
            .extracting(selection -> selection.getChallenge().getCategory())
            .containsOnly(ChallengeCategory.PERFORMANCE);
    }

    /**
     * Verifies that exclusion groups remain mandatory during the fallback selection.
     */
    @Test
    void shouldRejectPackWithConflictingExclusionGroups() {
        List<Challenge> candidates = Arrays.stream(ChallengeDifficulty.values())
            .map(difficulty -> {
                Challenge challenge = createChallenge(difficulty, ChallengeCategory.PERFORMANCE);
                challenge.setExclusionGroup("shared-group");
                return challenge;
            })
            .toList();

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        assertThatThrownBy(() -> service.selectWeekChallenges(WEEK_START))
            .isInstanceOf(WeeklyChallengeSelectionException.class)
            .hasMessageContaining("complete weekly challenge pack cannot be selected");
    }

    /**
     * Verifies that consecutive weeks draw different packs from the same catalogue.
     *
     * <p>Regression test: the week used to be mixed into the candidate order as a shared additive
     * offset, which left the sorted order identical and drew the same pack every single week.</p>
     */
    @Test
    void shouldDrawDifferentPacksOnConsecutiveWeeks() {
        List<Challenge> candidates = createCatalogue(CATALOGUE_CHALLENGES_PER_DIFFICULTY);

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(any(LocalDate.class)))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        Set<List<String>> distinctPacks = IntStream.range(0, OBSERVED_WEEKS)
            .mapToObj(weekIndex -> selectCodes(WEEK_START.plusWeeks(weekIndex)))
            .collect(Collectors.toSet());

        assertThat(distinctPacks).hasSizeGreaterThan(1);
    }

    /**
     * Verifies that re-selecting the same week keeps drawing the same pack.
     */
    @Test
    void shouldDrawTheSamePackForTheSameWeek() {
        List<Challenge> candidates = createCatalogue(CATALOGUE_CHALLENGES_PER_DIFFICULTY);

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(any(LocalDate.class)))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        assertThat(selectCodes(WEEK_START)).isEqualTo(selectCodes(WEEK_START));
    }

    /**
     * Verifies that a challenge already drawn in its difficulty's current cycle is not drawn again
     * while another candidate of that tier is still untouched.
     */
    @Test
    void shouldNotRepeatAChallengeUntilItsDifficultyHasCycled() {
        List<Challenge> candidates = createCatalogue(2);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        // Every tier's first candidate was drawn last week, so only the second remains in the cycle.
        List<Challenge> alreadyDrawn = candidates.stream()
            .filter(candidate -> candidate.getCode().endsWith("_0"))
            .toList();

        givenPastSelections(alreadyDrawn);

        assertThat(selectCodes(WEEK_START))
            .allSatisfy(code -> assertThat(code).endsWith("_1"));
    }

    /**
     * Verifies that a completed tier cycle resets and lets any of its challenges be drawn again.
     */
    @Test
    void shouldAllowRepetitionOnceTheDifficultyCycleCompletes() {
        List<Challenge> candidates = createCatalogue(2);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);

        // Both candidates of every tier were drawn: the cycle is complete, so the next draw picks
        // from the full catalogue again and lands on whatever the weekly ordering ranks first.
        givenPastSelections(candidates);

        List<String> withCompletedCycle = selectCodes(WEEK_START);

        givenPastSelections(List.of());

        assertThat(withCompletedCycle).isEqualTo(selectCodes(WEEK_START));
    }

    /**
     * Verifies that a tier holding a single challenge keeps drawing it, rather than the week being
     * left without a pack.
     *
     * <p>No-repeat is a preference. A tier with one enabled challenge has nothing to alternate
     * with, and refusing to repeat it there would break the one guarantee the pack does make: one
     * challenge per difficulty, every week.
     */
    @Test
    void shouldReuseAChallengeRatherThanLeaveTheTierEmpty() {
        List<Challenge> candidates = createCatalogue(1);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueOrderByIdAsc()).thenReturn(candidates);
        givenPastSelections(candidates);

        assertThat(selectCodes(WEEK_START)).hasSize(ChallengeDifficulty.values().length);
    }

    /**
     * Declares that the week being drawn owns no selection yet.
     */
    private void givenNoExistingPack() {
        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(any(LocalDate.class)))
            .thenReturn(List.of());
    }

    /**
     * Declares the challenges drawn during the weeks preceding the one being selected.
     *
     * @param challenges catalogue challenges already drawn, oldest first
     */
    private void givenPastSelections(List<Challenge> challenges) {
        when(weeklyChallengeRepository
            .findAllByWeekStartLessThanOrderByWeekStartAsc(any(LocalDate.class)))
            .thenReturn(challenges.stream().map(this::createWeeklyChallenge).toList());
    }

    /**
     * Verifies that persisted duplicate difficulty tiers are rejected before catalogue access.
     */
    @Test
    void shouldRejectExistingDuplicateDifficulty() {
        WeeklyChallenge first = createWeeklyChallenge(
            createChallenge(ChallengeDifficulty.EASY, ChallengeCategory.AIM)
        );
        WeeklyChallenge second = createWeeklyChallenge(
            createChallenge(ChallengeDifficulty.EASY, ChallengeCategory.SUPPORT)
        );

        when(weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(WEEK_START))
            .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.selectWeekChallenges(WEEK_START))
            .isInstanceOf(WeeklyChallengeSelectionException.class)
            .hasMessageContaining("multiple challenges for difficulty EASY");

        verify(challengeRepository, never()).findAllByEnabledTrueOrderByIdAsc();
    }

    /**
     * Verifies that weekly selection only accepts Mondays.
     */
    @Test
    void shouldRejectNonMondayWeekStart() {
        assertThatThrownBy(() -> service.selectWeekChallenges(WEEK_START.plusDays(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Monday");
    }

    /**
     * Creates a catalogue holding several interchangeable candidates per difficulty.
     *
     * @param challengesPerDifficulty number of candidates offered for each difficulty
     * @return catalogue fixture
     */
    private List<Challenge> createCatalogue(int challengesPerDifficulty) {
        return Arrays.stream(ChallengeDifficulty.values())
            .flatMap(difficulty -> IntStream.range(0, challengesPerDifficulty)
                .mapToObj(index -> createCandidate(difficulty, index)))
            .toList();
    }

    /**
     * Creates one interchangeable catalogue candidate.
     *
     * <p>Candidates of the same difficulty share a category, so category diversity never constrains
     * which one is drawn: only the weekly ordering does.</p>
     *
     * @param difficulty challenge difficulty
     * @param index      candidate index within its difficulty
     * @return challenge fixture
     */
    private Challenge createCandidate(ChallengeDifficulty difficulty, int index) {
        Challenge challenge = createChallenge(difficulty, categoryFor(difficulty));
        challenge.setId((long) difficulty.ordinal() * CATALOGUE_CHALLENGES_PER_DIFFICULTY + index + 1);
        challenge.setCode("CHALLENGE_" + difficulty.name() + "_" + index);
        return challenge;
    }

    /**
     * Selects one week's pack and returns its challenge codes.
     *
     * @param weekStart Monday identifying the week
     * @return selected challenge codes, ordered by difficulty
     */
    private List<String> selectCodes(LocalDate weekStart) {
        return service.selectWeekChallenges(weekStart).stream()
            .map(selection -> selection.getChallenge().getCode())
            .toList();
    }

    /**
     * Creates one enabled challenge with a supported progress mode.
     *
     * @param difficulty challenge difficulty
     * @param category   challenge category
     * @return challenge fixture
     */
    private Challenge createChallenge(
        ChallengeDifficulty difficulty,
        ChallengeCategory category
    ) {
        Challenge challenge = new Challenge();
        challenge.setId((long) difficulty.ordinal() + 1);
        challenge.setCode("CHALLENGE_" + difficulty.name());
        challenge.setDifficulty(difficulty);
        challenge.setCategory(category);
        challenge.setProgressMode(ProgressMode.SUM);
        challenge.setEnabled(true);
        return challenge;
    }

    /**
     * Creates a persisted weekly challenge fixture.
     *
     * @param challenge catalogue challenge
     * @return weekly challenge fixture
     */
    private WeeklyChallenge createWeeklyChallenge(Challenge challenge) {
        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(WEEK_START);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(SELECTION_TIME);
        return weeklyChallenge;
    }

    /**
     * Returns one distinct category for every supported difficulty.
     *
     * @param difficulty challenge difficulty
     * @return deterministic category
     */
    private ChallengeCategory categoryFor(ChallengeDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> ChallengeCategory.TRAINING;
            case NORMAL -> ChallengeCategory.SUPPORT;
            case MEDIUM -> ChallengeCategory.AIM;
            case HARD -> ChallengeCategory.CONSISTENCY;
            case VERY_HARD -> ChallengeCategory.VICTORY;
        };
    }
}
