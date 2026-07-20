package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valorant.tracker.challenge.entity.Challenge;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.exception.WeeklyChallengeSelectionException;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCategory;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valorant.tracker.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valorant.tracker.challenge.repository.WeeklyChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Creates and retrieves weekly challenge packs.
 *
 * <p>A complete pack contains exactly one challenge for every supported
 * difficulty. Existing selections are never replaced during the week.</p>
 */
@Service
public class DefaultWeeklyChallengeSelectionService
    implements WeeklyChallengeSelectionService {

    /**
     * Number of challenges expected in one complete weekly pack.
     */
    private static final int WEEKLY_CHALLENGE_COUNT =
        ChallengeDifficulty.values().length;

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            DefaultWeeklyChallengeSelectionService.class
        );

    /**
     * Challenge catalogue repository.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Weekly challenge repository.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Registry used to exclude challenges that cannot yet be calculated.
     */
    private final ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Application clock.
     */
    private final Clock clock;

    /**
     * Creates the weekly challenge selection service.
     *
     * @param challengeRepository       challenge catalogue repository
     * @param weeklyChallengeRepository weekly challenge repository
     * @param calculatorRegistry        challenge calculator registry
     * @param clock                     application clock
     */
    public DefaultWeeklyChallengeSelectionService(
        ChallengeRepository challengeRepository,
        WeeklyChallengeRepository weeklyChallengeRepository,
        ChallengeProgressCalculatorRegistry calculatorRegistry,
        Clock clock
    ) {
        this.challengeRepository = challengeRepository;
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.calculatorRegistry = calculatorRegistry;
        this.clock = clock;
    }

    /**
     * Selects the challenge pack for the current UTC week.
     *
     * @return current weekly challenges
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> selectCurrentWeekChallenges() {
        return selectWeekChallenges(
            resolveCurrentWeekStart()
        );
    }

    /**
     * Retrieves or creates the challenge pack assigned to one week.
     *
     * @param weekStart Monday identifying the week
     * @return complete weekly challenge pack
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> selectWeekChallenges(
        LocalDate weekStart
    ) {
        validateWeekStart(weekStart);

        List<WeeklyChallenge> existingSelections =
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(weekStart);

        validateExistingSelections(
            weekStart,
            existingSelections
        );

        if (existingSelections.size() == WEEKLY_CHALLENGE_COUNT) {
            LOGGER.debug(
                "Weekly challenge pack already exists for week {}.",
                weekStart
            );

            return List.copyOf(existingSelections);
        }

        List<Challenge> missingChallenges = selectMissingChallenges(
            weekStart,
            existingSelections
        );

        Instant selectionTime = clock.instant();

        List<WeeklyChallenge> newSelections =
            missingChallenges.stream()
                .map(
                    challenge -> createWeeklyChallenge(
                        weekStart,
                        challenge,
                        selectionTime
                    )
                )
                .toList();

        weeklyChallengeRepository.saveAll(newSelections);

        List<WeeklyChallenge> completedPack =
            new ArrayList<>(existingSelections);

        completedPack.addAll(newSelections);
        completedPack.sort(
            Comparator.comparing(
                weeklyChallenge ->
                    weeklyChallenge
                        .getChallenge()
                        .getDifficulty()
                        .ordinal()
            )
        );

        LOGGER.info(
            "Weekly challenge pack prepared for week {} with {} challenge(s).",
            weekStart,
            completedPack.size()
        );

        for (WeeklyChallenge weeklyChallenge : completedPack) {
            Challenge challenge = weeklyChallenge.getChallenge();

            LOGGER.info(
                "Selected weekly challenge: difficulty={}, code={}, "
                    + "category={}, points={}.",
                challenge.getDifficulty(),
                challenge.getCode(),
                challenge.getCategory(),
                challenge.getPoints()
            );
        }

        return List.copyOf(completedPack);
    }

    /**
     * Selects challenges for the difficulty tiers missing from the current
     * weekly pack.
     *
     * <p>Category diversity is preferred. When a fully category-diverse pack
     * cannot be created, duplicate categories are allowed while exclusion
     * groups remain enforced.</p>
     *
     * @param weekStart          selected week
     * @param existingSelections already persisted selections
     * @return challenges required to complete the pack
     */
    private List<Challenge> selectMissingChallenges(
        LocalDate weekStart,
        List<WeeklyChallenge> existingSelections
    ) {
        List<Challenge> candidates = challengeRepository
            .findAllByEnabledTrueOrderByIdAsc()
            .stream()
            .filter(
                challenge -> calculatorRegistry.supports(
                    challenge.getProgressMode()
                )
            )
            .sorted(
                Comparator.comparingLong(
                    challenge -> selectionOrder(
                        weekStart,
                        challenge
                    )
                )
            )
            .toList();

        SelectionState initialState =
            SelectionState.from(existingSelections);

        List<ChallengeDifficulty> missingDifficulties =
            EnumSet.allOf(ChallengeDifficulty.class)
                .stream()
                .filter(
                    difficulty ->
                        !initialState
                            .selectedDifficulties()
                            .contains(difficulty)
                )
                .toList();

        List<Challenge> diverseSelection = findSelection(
            candidates,
            missingDifficulties,
            initialState,
            true
        );

        if (!diverseSelection.isEmpty()
            || missingDifficulties.isEmpty()) {
            return diverseSelection;
        }

        List<Challenge> fallbackSelection = findSelection(
            candidates,
            missingDifficulties,
            initialState,
            false
        );

        if (fallbackSelection.isEmpty()) {
            throw new WeeklyChallengeSelectionException(
                "A complete weekly challenge pack cannot be selected for week "
                    + weekStart
                    + ". Verify that every difficulty has at least one enabled "
                    + "challenge with an implemented progress calculator and "
                    + "compatible exclusion group."
            );
        }

        return fallbackSelection;
    }

    /**
     * Tries to build a compatible selection using recursive backtracking.
     *
     * @param candidates              eligible catalogue challenges
     * @param difficulties            missing difficulty tiers
     * @param initialState            state produced by existing selections
     * @param requireUniqueCategories whether categories must remain unique
     * @return selected challenges, or an empty list when impossible
     */
    private List<Challenge> findSelection(
        List<Challenge> candidates,
        List<ChallengeDifficulty> difficulties,
        SelectionState initialState,
        boolean requireUniqueCategories
    ) {
        List<Challenge> result = new ArrayList<>();

        boolean selected = selectNextDifficulty(
            candidates,
            difficulties,
            0,
            initialState.copy(),
            requireUniqueCategories,
            result
        );

        if (!selected) {
            return List.of();
        }

        return List.copyOf(result);
    }

    /**
     * Selects one compatible challenge for every remaining difficulty.
     *
     * @param candidates              eligible challenges
     * @param difficulties            missing difficulty tiers
     * @param difficultyIndex         current difficulty index
     * @param state                   current selection state
     * @param requireUniqueCategories whether categories must be unique
     * @param result                  current challenge selection
     * @return whether a complete selection was found
     */
    private boolean selectNextDifficulty(
        List<Challenge> candidates,
        List<ChallengeDifficulty> difficulties,
        int difficultyIndex,
        SelectionState state,
        boolean requireUniqueCategories,
        List<Challenge> result
    ) {
        if (difficultyIndex >= difficulties.size()) {
            return true;
        }

        ChallengeDifficulty difficulty =
            difficulties.get(difficultyIndex);

        for (Challenge candidate : candidates) {
            if (candidate.getDifficulty() != difficulty) {
                continue;
            }

            if (!isCompatible(
                candidate,
                state,
                requireUniqueCategories
            )) {
                continue;
            }

            state.add(candidate);
            result.add(candidate);

            if (selectNextDifficulty(
                candidates,
                difficulties,
                difficultyIndex + 1,
                state,
                requireUniqueCategories,
                result
            )) {
                return true;
            }

            result.removeLast();
            state.remove(candidate);
        }

        return false;
    }

    /**
     * Checks whether a challenge can be added to the current pack.
     *
     * @param candidate               challenge candidate
     * @param state                   current selection state
     * @param requireUniqueCategories whether categories must remain unique
     * @return whether the candidate is compatible
     */
    private boolean isCompatible(
        Challenge candidate,
        SelectionState state,
        boolean requireUniqueCategories
    ) {
        String exclusionGroup = candidate.getExclusionGroup();

        if (exclusionGroup != null
            && state.exclusionGroups().contains(exclusionGroup)) {
            return false;
        }

        return !requireUniqueCategories
            || !state.categories().contains(candidate.getCategory());
    }

    /**
     * Produces a stable weekly order for one challenge.
     *
     * <p>The order changes with the week but remains deterministic across
     * repeated application restarts.</p>
     *
     * @param weekStart selected week
     * @param challenge challenge candidate
     * @return deterministic ordering value
     */
    private long selectionOrder(
        LocalDate weekStart,
        Challenge challenge
    ) {
        return Objects.hash(
            weekStart,
            challenge.getId(),
            challenge.getCode()
        );
    }

    /**
     * Creates a weekly challenge association.
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
        WeeklyChallenge weeklyChallenge =
            new WeeklyChallenge();

        weeklyChallenge.setWeekStart(weekStart);
        weeklyChallenge.setChallenge(challenge);
        weeklyChallenge.setSelectedAt(selectionTime);

        return weeklyChallenge;
    }

    /**
     * Ensures that an existing weekly pack contains no duplicate difficulty.
     *
     * @param weekStart          selected week
     * @param existingSelections persisted selections
     */
    private void validateExistingSelections(
        LocalDate weekStart,
        List<WeeklyChallenge> existingSelections
    ) {
        Set<ChallengeDifficulty> difficulties =
            EnumSet.noneOf(ChallengeDifficulty.class);

        for (WeeklyChallenge selection : existingSelections) {
            ChallengeDifficulty difficulty =
                selection.getChallenge().getDifficulty();

            if (!difficulties.add(difficulty)) {
                throw new WeeklyChallengeSelectionException(
                    "Week "
                        + weekStart
                        + " contains multiple challenges for difficulty "
                        + difficulty
                        + "."
                );
            }
        }
    }

    /**
     * Validates the requested week identifier.
     *
     * @param weekStart requested week start
     */
    private void validateWeekStart(
        LocalDate weekStart
    ) {
        Objects.requireNonNull(
            weekStart,
            "Week start must not be null."
        );

        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException(
                "Weekly challenge selection must use a Monday as week start."
            );
        }
    }

    /**
     * Resolves the Monday beginning the current UTC week.
     *
     * @return current UTC week start
     */
    private LocalDate resolveCurrentWeekStart() {
        return LocalDate
            .now(clock.withZone(ZoneOffset.UTC))
            .with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            );
    }

    /**
     * Holds mutable compatibility data while selecting a weekly pack.
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
        private static SelectionState from(
            List<WeeklyChallenge> selections
        ) {
            SelectionState state = new SelectionState(
                EnumSet.noneOf(ChallengeDifficulty.class),
                EnumSet.noneOf(ChallengeCategory.class),
                new HashSet<>()
            );

            selections.stream()
                .map(WeeklyChallenge::getChallenge)
                .forEach(state::add);

            return state;
        }

        /**
         * Creates an independent copy for one selection attempt.
         *
         * @return copied selection state
         */
        private SelectionState copy() {
            Set<ChallengeDifficulty> difficultyCopy =
                selectedDifficulties.isEmpty()
                    ? EnumSet.noneOf(ChallengeDifficulty.class)
                    : EnumSet.copyOf(selectedDifficulties);

            Set<ChallengeCategory> categoryCopy =
                categories.isEmpty()
                    ? EnumSet.noneOf(ChallengeCategory.class)
                    : EnumSet.copyOf(categories);

            return new SelectionState(
                difficultyCopy,
                categoryCopy,
                new HashSet<>(exclusionGroups)
            );
        }

        /**
         * Adds a challenge to the selection state.
         *
         * @param challenge selected challenge
         */
        private void add(
            Challenge challenge
        ) {
            selectedDifficulties.add(challenge.getDifficulty());
            categories.add(challenge.getCategory());

            if (challenge.getExclusionGroup() != null) {
                exclusionGroups.add(challenge.getExclusionGroup());
            }
        }

        /**
         * Removes a challenge after an unsuccessful selection attempt.
         *
         * @param challenge challenge to remove
         */
        private void remove(
            Challenge challenge
        ) {
            selectedDifficulties.remove(challenge.getDifficulty());
            categories.remove(challenge.getCategory());

            if (challenge.getExclusionGroup() != null) {
                exclusionGroups.remove(challenge.getExclusionGroup());
            }
        }
    }
}
