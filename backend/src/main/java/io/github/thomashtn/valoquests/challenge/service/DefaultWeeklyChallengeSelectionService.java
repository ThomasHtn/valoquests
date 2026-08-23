package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.exception.WeeklyChallengeSelectionException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and retrieves deterministic weekly challenge packs.
 *
 * <p>A complete pack contains exactly one challenge for every supported difficulty. Existing
 * selections are never replaced during the week. Category diversity is preferred, while
 * exclusion groups are always enforced.</p>
 */
@Service
public class DefaultWeeklyChallengeSelectionService implements WeeklyChallengeSelectionService {

    /**
     * Number of challenges expected in one complete weekly pack.
     */
    private static final int WEEKLY_CHALLENGE_COUNT = ChallengeDifficulty.values().length;

    /**
     * Orders persisted selections from the easiest to the hardest challenge.
     */
    private static final Comparator<WeeklyChallenge> WEEKLY_CHALLENGE_COMPARATOR =
        Comparator.comparingInt(selection -> selection.getChallenge().getDifficulty().ordinal());

    /**
     * Odd 64-bit constant separating consecutive weeks before diffusion (golden-ratio derived).
     */
    private static final long WEEK_SEED_MULTIPLIER = 0x9E3779B97F4A7C15L;

    /**
     * First SplitMix64 finalizer multiplier.
     */
    private static final long AVALANCHE_FIRST_MULTIPLIER = 0xBF58476D1CE4E5B9L;

    /**
     * Second SplitMix64 finalizer multiplier.
     */
    private static final long AVALANCHE_SECOND_MULTIPLIER = 0x94D049BB133111EBL;

    /**
     * First SplitMix64 finalizer shift.
     */
    private static final int AVALANCHE_FIRST_SHIFT = 30;

    /**
     * Second SplitMix64 finalizer shift.
     */
    private static final int AVALANCHE_SECOND_SHIFT = 27;

    /**
     * Closing SplitMix64 finalizer shift.
     */
    private static final int AVALANCHE_FINAL_SHIFT = 31;

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultWeeklyChallengeSelectionService.class);

    /**
     * Challenge catalogue repository.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Weekly challenge repository.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Registry used to exclude challenges that cannot currently be calculated.
     */
    private final ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Application clock used for week resolution and selection timestamps.
     */
    private final Clock clock;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly challenge selection service.
     *
     * @param challengeRepository       challenge catalogue repository
     * @param weeklyChallengeRepository weekly challenge repository
     * @param calculatorRegistry        challenge calculator registry
     * @param clock                     application clock
     * @param weekCalendar              calendar resolving the current week
     */
    public DefaultWeeklyChallengeSelectionService(
        ChallengeRepository challengeRepository,
        WeeklyChallengeRepository weeklyChallengeRepository,
        ChallengeProgressCalculatorRegistry calculatorRegistry,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.challengeRepository = challengeRepository;
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.calculatorRegistry = calculatorRegistry;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Selects the challenge pack for the current UTC week.
     *
     * @return current weekly challenges
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> selectCurrentWeekChallenges() {
        return selectWeekChallenges(weekCalendar.currentWeekStart());
    }

    /**
     * Retrieves or creates the challenge pack assigned to one week.
     *
     * @param weekStart Monday identifying the week
     * @return complete weekly challenge pack
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> selectWeekChallenges(LocalDate weekStart) {
        validateWeekStart(weekStart);

        List<WeeklyChallenge> existingSelections =
            weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(weekStart);

        validateExistingSelections(weekStart, existingSelections);

        if (existingSelections.size() == WEEKLY_CHALLENGE_COUNT) {
            LOGGER.debug("Weekly challenge pack already exists for week {}.", weekStart);
            return sortSelections(existingSelections);
        }

        List<Challenge> missingChallenges = selectMissingChallenges(weekStart, existingSelections);
        List<WeeklyChallenge> newSelections = createWeeklyChallenges(
            weekStart,
            missingChallenges,
            clock.instant()
        );

        weeklyChallengeRepository.saveAll(newSelections);

        List<WeeklyChallenge> completedPack = new ArrayList<>(WEEKLY_CHALLENGE_COUNT);
        completedPack.addAll(existingSelections);
        completedPack.addAll(newSelections);
        completedPack.sort(WEEKLY_CHALLENGE_COMPARATOR);

        logSelection(weekStart, completedPack);
        return List.copyOf(completedPack);
    }

    /**
     * Retrieves the challenge pack a week already owns, creating nothing.
     *
     * @param weekStart Monday identifying the week
     * @return the week's challenges, empty when it never had a pack
     */
    @Override
    @Transactional(readOnly = true)
    public List<WeeklyChallenge> findExistingWeekChallenges(LocalDate weekStart) {
        validateWeekStart(weekStart);

        return weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(weekStart);
    }

    /**
     * Selects challenges for all difficulty tiers missing from an existing weekly pack.
     *
     * @param weekStart          selected week
     * @param existingSelections already persisted selections
     * @return challenges required to complete the pack
     */
    private List<Challenge> selectMissingChallenges(
        LocalDate weekStart,
        List<WeeklyChallenge> existingSelections
    ) {
        SelectionState initialState = SelectionState.from(existingSelections);
        List<ChallengeDifficulty> missingDifficulties = findMissingDifficulties(initialState);

        if (missingDifficulties.isEmpty()) {
            return List.of();
        }

        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty =
            loadCandidatesByDifficulty(weekStart);

        Optional<List<Challenge>> diverseSelection = findSelection(
            candidatesByDifficulty,
            missingDifficulties,
            initialState,
            true
        );

        if (diverseSelection.isPresent()) {
            return diverseSelection.orElseThrow();
        }

        return findSelection(
            candidatesByDifficulty,
            missingDifficulties,
            initialState,
            false
        ).orElseThrow(() -> createSelectionException(weekStart));
    }

    /**
     * Loads supported catalogue challenges and groups them by difficulty.
     *
     * <p>Each group uses a deterministic week-dependent order. The same week therefore produces
     * the same selection candidate order across application restarts.</p>
     *
     * @param weekStart selected week
     * @return eligible candidates grouped by difficulty
     */
    private Map<ChallengeDifficulty, List<Challenge>> loadCandidatesByDifficulty(LocalDate weekStart) {
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty =
            new EnumMap<>(ChallengeDifficulty.class);

        for (ChallengeDifficulty difficulty : ChallengeDifficulty.values()) {
            candidatesByDifficulty.put(difficulty, new ArrayList<>());
        }

        challengeRepository.findAllByEnabledTrueOrderByIdAsc()
            .stream()
            .filter(challenge -> calculatorRegistry.supports(challenge.getProgressMode()))
            .sorted(Comparator.comparingLong(challenge -> selectionOrder(weekStart, challenge)))
            .forEach(challenge -> candidatesByDifficulty.get(challenge.getDifficulty()).add(challenge));

        return candidatesByDifficulty;
    }

    /**
     * Finds the difficulty tiers not already represented in a weekly pack.
     *
     * @param state current selection state
     * @return missing difficulty tiers in enum order
     */
    private List<ChallengeDifficulty> findMissingDifficulties(SelectionState state) {
        return EnumSet.allOf(ChallengeDifficulty.class)
            .stream()
            .filter(difficulty -> !state.selectedDifficulties().contains(difficulty))
            .toList();
    }

    /**
     * Attempts to build a complete compatible selection.
     *
     * @param candidatesByDifficulty  eligible challenges grouped by difficulty
     * @param difficulties            missing difficulty tiers
     * @param initialState            state produced by existing selections
     * @param requireUniqueCategories whether categories must remain unique
     * @return complete selection when one exists
     */
    private Optional<List<Challenge>> findSelection(
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty,
        List<ChallengeDifficulty> difficulties,
        SelectionState initialState,
        boolean requireUniqueCategories
    ) {
        return selectNextDifficulty(
            candidatesByDifficulty,
            difficulties,
            0,
            initialState,
            requireUniqueCategories,
            List.of()
        );
    }

    /**
     * Selects one compatible challenge for every remaining difficulty using bounded backtracking.
     *
     * <p>The recursion depth is limited to the number of supported difficulty tiers. Immutable
     * copies are used for each branch so failed attempts cannot leak state into later attempts.</p>
     *
     * @param candidatesByDifficulty  eligible challenges grouped by difficulty
     * @param difficulties            missing difficulty tiers
     * @param difficultyIndex         current difficulty index
     * @param state                   current selection state
     * @param requireUniqueCategories whether categories must remain unique
     * @param selectedChallenges      challenges selected by the current branch
     * @return complete selection when one exists
     */
    private Optional<List<Challenge>> selectNextDifficulty(
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty,
        List<ChallengeDifficulty> difficulties,
        int difficultyIndex,
        SelectionState state,
        boolean requireUniqueCategories,
        List<Challenge> selectedChallenges
    ) {
        if (difficultyIndex == difficulties.size()) {
            return Optional.of(List.copyOf(selectedChallenges));
        }

        ChallengeDifficulty difficulty = difficulties.get(difficultyIndex);

        for (Challenge candidate : candidatesByDifficulty.getOrDefault(difficulty, List.of())) {
            if (!state.isCompatible(candidate, requireUniqueCategories)) {
                continue;
            }

            List<Challenge> nextSelection = new ArrayList<>(selectedChallenges.size() + 1);
            nextSelection.addAll(selectedChallenges);
            nextSelection.add(candidate);

            Optional<List<Challenge>> result = selectNextDifficulty(
                candidatesByDifficulty,
                difficulties,
                difficultyIndex + 1,
                state.with(candidate),
                requireUniqueCategories,
                nextSelection
            );

            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Creates all weekly challenge entities with one shared selection timestamp.
     *
     * @param weekStart     selected week
     * @param challenges    selected catalogue challenges
     * @param selectionTime selection timestamp
     * @return new weekly challenge entities
     */
    private List<WeeklyChallenge> createWeeklyChallenges(
        LocalDate weekStart,
        List<Challenge> challenges,
        Instant selectionTime
    ) {
        return challenges.stream()
            .map(challenge -> createWeeklyChallenge(weekStart, challenge, selectionTime))
            .toList();
    }

    /**
     * Creates one weekly challenge association.
     *
     * @param weekStart     selected week
     * @param challenge     selected catalogue challenge
     * @param selectionTime selection timestamp
     * @return new weekly challenge
     */
    private WeeklyChallenge createWeeklyChallenge(
        LocalDate weekStart,
        Challenge challenge,
        Instant selectionTime
    ) {
        WeeklyChallenge weeklyChallenge = new WeeklyChallenge();
        weeklyChallenge.setWeekStart(weekStart);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(selectionTime);
        return weeklyChallenge;
    }

    /**
     * Sorts a weekly pack by difficulty and returns an immutable copy.
     *
     * @param selections weekly challenge selections
     * @return sorted immutable selections
     */
    private List<WeeklyChallenge> sortSelections(List<WeeklyChallenge> selections) {
        return selections.stream()
            .sorted(WEEKLY_CHALLENGE_COMPARATOR)
            .toList();
    }

    /**
     * Logs the completed weekly challenge pack.
     *
     * @param weekStart selected week
     * @param selections completed challenge pack
     */
    private void logSelection(LocalDate weekStart, List<WeeklyChallenge> selections) {
        LOGGER.info(
            "Weekly challenge pack prepared for week {} with {} challenge(s).",
            weekStart,
            selections.size()
        );

        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        selections.forEach(selection -> {
            Challenge challenge = selection.getChallenge();
            LOGGER.debug(
                "Selected weekly challenge: difficulty={}, code={}, category={}, damage={}.",
                challenge.getDifficulty(),
                challenge.getCode(),
                challenge.getCategory(),
                challenge.getDamage()
            );
        });
    }

    /**
     * Produces a stable weekly order for one challenge.
     *
     * <p>The week has to be mixed into every candidate's value non-additively. {@code Objects.hash}
     * only adds a shared week term to each candidate, which shifts them all equally and leaves the
     * sorted order untouched: every week then drew the exact same pack.
     *
     * @param weekStart selected week
     * @param challenge challenge candidate
     * @return deterministic ordering value
     */
    private long selectionOrder(LocalDate weekStart, Challenge challenge) {
        long challengeSeed = Objects.hash(challenge.getId(), challenge.getCode());

        return avalanche(weekStart.toEpochDay() * WEEK_SEED_MULTIPLIER + challengeSeed);
    }

    /**
     * Spreads a seed over the whole {@code long} range so neighbouring seeds order unrelatedly.
     *
     * <p>SplitMix64 finalizer: a bijection, so two distinct seeds keep distinct ordering values.</p>
     *
     * @param seed ordering seed
     * @return diffused ordering value
     */
    private static long avalanche(long seed) {
        long mixed = seed;
        mixed = (mixed ^ (mixed >>> AVALANCHE_FIRST_SHIFT)) * AVALANCHE_FIRST_MULTIPLIER;
        mixed = (mixed ^ (mixed >>> AVALANCHE_SECOND_SHIFT)) * AVALANCHE_SECOND_MULTIPLIER;
        return mixed ^ (mixed >>> AVALANCHE_FINAL_SHIFT);
    }

    /**
     * Ensures that an existing weekly pack contains no duplicate difficulty or excess entry.
     *
     * @param weekStart          selected week
     * @param existingSelections persisted selections
     */
    private void validateExistingSelections(
        LocalDate weekStart,
        List<WeeklyChallenge> existingSelections
    ) {
        if (existingSelections.size() > WEEKLY_CHALLENGE_COUNT) {
            throw new WeeklyChallengeSelectionException(
                "Week " + weekStart + " contains more challenges than the supported difficulty count."
            );
        }

        Set<ChallengeDifficulty> difficulties = EnumSet.noneOf(ChallengeDifficulty.class);

        for (WeeklyChallenge selection : existingSelections) {
            ChallengeDifficulty difficulty = selection.getChallenge().getDifficulty();

            if (!difficulties.add(difficulty)) {
                throw new WeeklyChallengeSelectionException(
                    "Week " + weekStart + " contains multiple challenges for difficulty " + difficulty + "."
                );
            }
        }
    }

    /**
     * Validates the requested week identifier.
     *
     * @param weekStart requested week start
     */
    private void validateWeekStart(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "Week start must not be null.");

        if (!weekCalendar.isWeekStart(weekStart)) {
            throw new IllegalArgumentException(
                "Weekly challenge selection must use a Monday as week start."
            );
        }
    }

    /**
     * Creates the exception raised when no compatible complete pack can be built.
     *
     * @param weekStart selected week
     * @return descriptive selection exception
     */
    private WeeklyChallengeSelectionException createSelectionException(LocalDate weekStart) {
        return new WeeklyChallengeSelectionException(
            "A complete weekly challenge pack cannot be selected for week "
                + weekStart
                + ". Verify that every difficulty has at least one enabled challenge with an "
                + "implemented progress calculator and compatible exclusion group."
        );
    }

    /**
     * Holds immutable compatibility data for one selection branch.
     *
     * @param selectedDifficulties selected difficulty tiers
     * @param categories           selected categories
     * @param exclusionGroups      selected exclusion groups
     */
    private record SelectionState(
        Set<ChallengeDifficulty> selectedDifficulties,
        Set<ChallengeCategory> categories,
        Set<String> exclusionGroups
    ) {

        /**
         * Creates a state from persisted weekly selections.
         *
         * @param selections existing selections
         * @return initialized selection state
         */
        private static SelectionState from(List<WeeklyChallenge> selections) {
            Set<ChallengeDifficulty> difficulties = EnumSet.noneOf(ChallengeDifficulty.class);
            Set<ChallengeCategory> categories = EnumSet.noneOf(ChallengeCategory.class);
            Set<String> exclusionGroups = new HashSet<>();

            for (WeeklyChallenge selection : selections) {
                Challenge challenge = selection.getChallenge();
                difficulties.add(challenge.getDifficulty());
                categories.add(challenge.getCategory());

                if (challenge.getExclusionGroup() != null) {
                    exclusionGroups.add(challenge.getExclusionGroup());
                }
            }

            return new SelectionState(
                Set.copyOf(difficulties),
                Set.copyOf(categories),
                Set.copyOf(exclusionGroups)
            );
        }

        /**
         * Checks whether a challenge can be added to the current branch.
         *
         * @param candidate               challenge candidate
         * @param requireUniqueCategories whether categories must remain unique
         * @return whether the candidate is compatible
         */
        private boolean isCompatible(Challenge candidate, boolean requireUniqueCategories) {
            String exclusionGroup = candidate.getExclusionGroup();

            if (exclusionGroup != null && exclusionGroups.contains(exclusionGroup)) {
                return false;
            }

            return !requireUniqueCategories || !categories.contains(candidate.getCategory());
        }

        /**
         * Creates a new state containing one additional challenge.
         *
         * @param challenge selected challenge
         * @return extended immutable state
         */
        private SelectionState with(Challenge challenge) {
            Set<ChallengeDifficulty> nextDifficulties = copyDifficulties();
            Set<ChallengeCategory> nextCategories = copyCategories();
            Set<String> nextExclusionGroups = new HashSet<>(exclusionGroups);

            nextDifficulties.add(challenge.getDifficulty());
            nextCategories.add(challenge.getCategory());

            if (challenge.getExclusionGroup() != null) {
                nextExclusionGroups.add(challenge.getExclusionGroup());
            }

            return new SelectionState(
                Set.copyOf(nextDifficulties),
                Set.copyOf(nextCategories),
                Set.copyOf(nextExclusionGroups)
            );
        }

        /**
         * Creates a mutable difficulty set preserving the enum implementation.
         *
         * @return mutable difficulty copy
         */
        private Set<ChallengeDifficulty> copyDifficulties() {
            return selectedDifficulties.isEmpty()
                ? EnumSet.noneOf(ChallengeDifficulty.class)
                : EnumSet.copyOf(selectedDifficulties);
        }

        /**
         * Creates a mutable category set preserving the enum implementation.
         *
         * @return mutable category copy
         */
        private Set<ChallengeCategory> copyCategories() {
            return categories.isEmpty()
                ? EnumSet.noneOf(ChallengeCategory.class)
                : EnumSet.copyOf(categories);
        }
    }
}
