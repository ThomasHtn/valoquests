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
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
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
 * Tests deterministic weekly challenge pack draws and validation rules.
 */
class DefaultWeeklyChallengeDrawServiceTest {

    /**
     * Monday used as the selected week.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 20);

    /**
     * Fixed selection timestamp.
     */
    private static final Instant SELECTION_TIME = Instant.parse("2026-07-20T08:00:00Z");

    /**
     * Number of interchangeable candidates offered for each tier.
     */
    private static final int CATALOGUE_CHALLENGES_PER_TIER = 10;

    /**
     * Number of consecutive weeks observed by the rotation test.
     */
    private static final int OBSERVED_WEEKS = 8;

    /**
     * JSON the mocked parser writes for every resolved definition.
     */
    private static final String RESOLVED_JSON = "[{\"resolved\":true}]";

    /**
     * Challenge catalogue repository dependency.
     */
    private ChallengeRepository challengeRepository;

    /**
     * Weekly selection repository dependency.
     */
    private ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Challenge progress repository dependency, cleared by the redraw.
     */
    private PlayerChallengeProgressRepository progressRepository;

    /**
     * Service under test.
     */
    private DefaultWeeklyChallengeDrawService service;

    /**
     * Creates the service and common mock behavior before each test.
     */
    @BeforeEach
    void setUp() {
        challengeRepository = mock(ChallengeRepository.class);
        challengeSelectionRepository = mock(ChallengeSelectionRepository.class);
        progressRepository = mock(PlayerChallengeProgressRepository.class);

        when(challengeSelectionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChallengeDefinitionParser definitionParser = mock(ChallengeDefinitionParser.class);
        when(definitionParser.parse(any(Challenge.class), any())).thenReturn(sumDefinition());
        when(definitionParser.toJson(anyList())).thenReturn(RESOLVED_JSON);

        Clock clock = Clock.fixed(SELECTION_TIME, ZoneOffset.UTC);

        service = new DefaultWeeklyChallengeDrawService(
            challengeRepository,
            challengeSelectionRepository,
            progressRepository,
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
     * Verifies that every drawn selection stores the definition it was resolved to.
     */
    @Test
    void shouldStoreResolvedConditionsOnEverySelection() {
        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(createCatalogue(1));

        assertThat(service.selectWeekChallenges(WEEK_START))
            .allSatisfy(selection -> {
                assertThat(selection.getCadence()).isEqualTo(ChallengeCadence.WEEKLY);
                assertThat(selection.getDay()).isNull();
                assertThat(selection.getResolvedConditionsJson()).isEqualTo(RESOLVED_JSON);
            });
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
     * Verifies that an existing complete pack is returned without catalogue access or writes.
     */
    @Test
    void shouldReturnExistingCompletePack() {
        List<ChallengeSelection> existingSelections = Arrays.stream(ChallengeTier.values())
            .map(tier -> createSelection(createChallenge(tier, categoryFor(tier))))
            .toList();

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(WEEK_START, ChallengeCadence.WEEKLY))
            .thenReturn(existingSelections.reversed());

        List<ChallengeSelection> result = service.selectWeekChallenges(WEEK_START);

        assertThat(result)
            .extracting(selection -> selection.getChallenge().getTier())
            .containsExactly(ChallengeTier.values());

        verify(challengeRepository, never()).findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY);
        verify(challengeSelectionRepository, never()).saveAll(anyList());
    }

    /**
     * Verifies that one challenge is selected for every tier with distinct categories.
     */
    @Test
    void shouldCreateCategoryDiversePack() {
        List<Challenge> candidates = Arrays.stream(ChallengeTier.values())
            .map(tier -> createChallenge(tier, categoryFor(tier)))
            .toList();

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(WEEK_START, ChallengeCadence.WEEKLY))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

        List<ChallengeSelection> result = service.selectWeekChallenges(WEEK_START);

        assertThat(result).hasSize(ChallengeTier.values().length);
        assertThat(result)
            .extracting(selection -> selection.getChallenge().getTier())
            .containsExactly(ChallengeTier.values());
        assertThat(result)
            .extracting(selection -> selection.getChallenge().getCategory())
            .doesNotHaveDuplicates();
        assertThat(result)
            .allSatisfy(selection -> {
                assertThat(selection.getWeekStart()).isEqualTo(WEEK_START);
                assertThat(selection.getSelectedAt()).isEqualTo(SELECTION_TIME);
            });

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChallengeSelection>> captor = ArgumentCaptor.forClass(List.class);

        verify(challengeSelectionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(ChallengeTier.values().length);
    }

    /**
     * Verifies that duplicate categories are accepted only when a diverse pack is impossible.
     */
    @Test
    void shouldFallbackToDuplicateCategories() {
        List<Challenge> candidates = Arrays.stream(ChallengeTier.values())
            .map(tier -> createChallenge(tier, ChallengeCategory.PERFORMANCE))
            .toList();

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(WEEK_START, ChallengeCadence.WEEKLY))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

        List<ChallengeSelection> result = service.selectWeekChallenges(WEEK_START);

        assertThat(result).hasSize(ChallengeTier.values().length);
        assertThat(result)
            .extracting(selection -> selection.getChallenge().getCategory())
            .containsOnly(ChallengeCategory.PERFORMANCE);
    }

    /**
     * Verifies that exclusion groups remain mandatory during the fallback selection.
     */
    @Test
    void shouldRejectPackWithConflictingExclusionGroups() {
        List<Challenge> candidates = Arrays.stream(ChallengeTier.values())
            .map(tier -> {
                Challenge challenge = createChallenge(tier, ChallengeCategory.PERFORMANCE);
                challenge.setExclusionGroup("shared-group");
                return challenge;
            })
            .toList();

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(WEEK_START, ChallengeCadence.WEEKLY))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

        assertThatThrownBy(() -> service.selectWeekChallenges(WEEK_START))
            .isInstanceOf(ChallengeDrawException.class)
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
        List<Challenge> candidates = createCatalogue(CATALOGUE_CHALLENGES_PER_TIER);

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(any(LocalDate.class), eq(ChallengeCadence.WEEKLY)))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

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
        List<Challenge> candidates = createCatalogue(CATALOGUE_CHALLENGES_PER_TIER);

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(any(LocalDate.class), eq(ChallengeCadence.WEEKLY)))
            .thenReturn(List.of());
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

        assertThat(selectCodes(WEEK_START)).isEqualTo(selectCodes(WEEK_START));
    }

    /**
     * Verifies that a challenge already drawn in its tier's current cycle is not drawn again
     * while another candidate of that tier is still untouched.
     */
    @Test
    void shouldNotRepeatAChallengeUntilItsTierHasCycled() {
        List<Challenge> candidates = createCatalogue(2);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

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
    void shouldAllowRepetitionOnceTheTierCycleCompletes() {
        List<Challenge> candidates = createCatalogue(2);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

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
     * challenge per tier, every week.
     */
    @Test
    void shouldReuseAChallengeRatherThanLeaveTheTierEmpty() {
        List<Challenge> candidates = createCatalogue(1);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);
        givenPastSelections(candidates);

        assertThat(selectCodes(WEEK_START)).hasSize(ChallengeTier.values().length);
    }

    /**
     * Verifies that a redraw clears the week's progress and its pack, then draws a different one.
     *
     * <p>The whole point of the operation: the draw is deterministic per week, so discarding the
     * pack and re-selecting would otherwise hand back the exact same five challenges.
     */
    @Test
    void shouldRedrawAPackDifferentFromTheOneItDiscards() {
        List<Challenge> candidates = createCatalogue(CATALOGUE_CHALLENGES_PER_TIER);

        givenNoExistingPack();
        when(challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY))
            .thenReturn(candidates);

        List<ChallengeSelection> discarded = service.selectWeekChallenges(WEEK_START);
        List<String> discardedCodes = codesOf(discarded);

        // The redraw reads the week twice: the pack it throws away, then the empty week it fills.
        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(any(LocalDate.class), eq(ChallengeCadence.WEEKLY)))
            .thenReturn(discarded, List.of());

        List<String> redrawnCodes = codesOf(service.redrawCurrentWeekChallenges());

        verify(progressRepository).deleteAll(anyList());
        verify(challengeSelectionRepository).deleteAll(discarded);

        assertThat(redrawnCodes).hasSize(ChallengeTier.values().length);
        assertThat(redrawnCodes).isNotEqualTo(discardedCodes);
    }

    /**
     * Verifies that a finalized pack is refused rather than rewritten.
     */
    @Test
    void shouldRefuseToRedrawAFinalizedPack() {
        ChallengeSelection finalized = createSelection(
            createChallenge(ChallengeTier.EASY, ChallengeCategory.AIM)
        );
        finalized.setFinalizedAt(SELECTION_TIME);

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(WEEK_START, ChallengeCadence.WEEKLY))
            .thenReturn(List.of(finalized));

        assertThatThrownBy(() -> service.redrawCurrentWeekChallenges())
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("finalized challenge pack");

        verify(progressRepository, never()).deleteAll(anyList());
        verify(challengeSelectionRepository, never()).deleteAll(anyList());
    }

    /**
     * Extracts one pack's challenge codes, ordered by tier.
     *
     * @param selections weekly selections
     * @return challenge codes
     */
    private List<String> codesOf(List<ChallengeSelection> selections) {
        return selections.stream()
            .map(selection -> selection.getChallenge().getCode())
            .toList();
    }

    /**
     * Declares that the week being drawn owns no selection yet.
     */
    private void givenNoExistingPack() {
        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(any(LocalDate.class), eq(ChallengeCadence.WEEKLY)))
            .thenReturn(List.of());
    }

    /**
     * Declares the challenges drawn during the weeks preceding the one being selected.
     *
     * @param challenges catalogue challenges already drawn, oldest first
     */
    private void givenPastSelections(List<Challenge> challenges) {
        when(challengeSelectionRepository
            .findAllByCadenceAndWeekStartLessThanOrderByWeekStartAsc(
                eq(ChallengeCadence.WEEKLY),
                any(LocalDate.class)
            ))
            .thenReturn(challenges.stream().map(this::createSelection).toList());
    }

    /**
     * Verifies that persisted duplicate tiers are rejected before catalogue access.
     */
    @Test
    void shouldRejectExistingDuplicateTier() {
        ChallengeSelection first = createSelection(
            createChallenge(ChallengeTier.EASY, ChallengeCategory.AIM)
        );
        ChallengeSelection second = createSelection(
            createChallenge(ChallengeTier.EASY, ChallengeCategory.SUPPORT)
        );

        when(challengeSelectionRepository
            .findAllByWeekStartAndCadenceOrderByIdAsc(WEEK_START, ChallengeCadence.WEEKLY))
            .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.selectWeekChallenges(WEEK_START))
            .isInstanceOf(ChallengeDrawException.class)
            .hasMessageContaining("multiple challenges for tier EASY");

        verify(challengeRepository, never()).findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY);
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
     * Creates a catalogue holding several interchangeable candidates per tier.
     *
     * @param challengesPerTier number of candidates offered for each tier
     * @return catalogue fixture
     */
    private List<Challenge> createCatalogue(int challengesPerTier) {
        return Arrays.stream(ChallengeTier.values())
            .flatMap(tier -> IntStream.range(0, challengesPerTier)
                .mapToObj(index -> createCandidate(tier, index)))
            .toList();
    }

    /**
     * Creates one interchangeable catalogue candidate.
     *
     * <p>Candidates of the same tier share a category, so category diversity never constrains
     * which one is drawn: only the weekly ordering does.</p>
     *
     * @param tier challenge tier
     * @param index      candidate index within its tier
     * @return challenge fixture
     */
    private Challenge createCandidate(ChallengeTier tier, int index) {
        Challenge challenge = createChallenge(tier, categoryFor(tier));
        challenge.setId((long) tier.ordinal() * CATALOGUE_CHALLENGES_PER_TIER + index + 1);
        challenge.setCode("CHALLENGE_" + tier.name() + "_" + index);
        return challenge;
    }

    /**
     * Selects one week's pack and returns its challenge codes.
     *
     * @param weekStart Monday identifying the week
     * @return selected challenge codes, ordered by tier
     */
    private List<String> selectCodes(LocalDate weekStart) {
        return service.selectWeekChallenges(weekStart).stream()
            .map(selection -> selection.getChallenge().getCode())
            .toList();
    }

    /**
     * Creates one enabled challenge with a supported progress mode.
     *
     * @param tier challenge tier
     * @param category   challenge category
     * @return challenge fixture
     */
    private Challenge createChallenge(
        ChallengeTier tier,
        ChallengeCategory category
    ) {
        Challenge challenge = new Challenge();
        challenge.setId((long) tier.ordinal() + 1);
        challenge.setCode("CHALLENGE_" + tier.name());
        challenge.setTier(tier);
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
    private ChallengeSelection createSelection(Challenge challenge) {
        ChallengeSelection selection = new ChallengeSelection();
        selection.setWeekStart(WEEK_START);
        selection.setChallenge(challenge);
        selection.setSelectedAt(SELECTION_TIME);
        return selection;
    }

    /**
     * Returns one distinct category for every supported tier.
     *
     * @param tier challenge tier
     * @return deterministic category
     */
    private ChallengeCategory categoryFor(ChallengeTier tier) {
        return switch (tier) {
            case EASY -> ChallengeCategory.TRAINING;
            case NORMAL -> ChallengeCategory.SUPPORT;
            case MEDIUM -> ChallengeCategory.AIM;
            case HARD -> ChallengeCategory.CONSISTENCY;
            case VERY_HARD -> ChallengeCategory.VICTORY;
        };
    }
}
