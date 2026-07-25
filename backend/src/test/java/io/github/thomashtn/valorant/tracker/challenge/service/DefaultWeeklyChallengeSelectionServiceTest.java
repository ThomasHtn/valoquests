package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.exception.WeeklyChallengeSelectionException;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCategory;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import io.github.thomashtn.valorant.tracker.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valorant.tracker.challenge.repository.WeeklyChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            Clock.fixed(SELECTION_TIME, ZoneOffset.UTC)
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
        challenge.setPoints((difficulty.ordinal() + 1) * 100);
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
