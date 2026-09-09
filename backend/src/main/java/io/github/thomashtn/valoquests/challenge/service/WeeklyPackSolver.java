package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Completes a weekly pack out of candidates grouped by difficulty.
 *
 * <p>Pure: no repository, no clock. Category diversity is preferred, exclusion groups are always
 * enforced, and the search is a bounded backtracking whose depth is the number of difficulty
 * tiers.</p>
 */
final class WeeklyPackSolver {

    /**
     * Not instantiable: static helpers only.
     */
    private WeeklyPackSolver() {
    }

    /**
     * Finds the difficulty tiers not already represented in a weekly pack.
     *
     * @param state current selection state
     * @return missing difficulty tiers in enum order
     */
    static List<ChallengeDifficulty> missingDifficulties(WeeklyPackSelectionState state) {
        return EnumSet.allOf(ChallengeDifficulty.class)
            .stream()
            .filter(difficulty -> !state.selectedDifficulties().contains(difficulty))
            .toList();
    }

    /**
     * Builds the best complete selection one candidate pool allows, preferring category diversity.
     *
     * @param candidatesByDifficulty eligible candidates grouped by difficulty
     * @param difficulties           missing difficulty tiers
     * @param initialState           state produced by existing selections
     * @return complete selection when the pool allows one
     */
    static Optional<List<Challenge>> solve(
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty,
        List<ChallengeDifficulty> difficulties,
        WeeklyPackSelectionState initialState
    ) {
        return selectNextDifficulty(candidatesByDifficulty, difficulties, 0, initialState, true, List.of())
            .or(() -> selectNextDifficulty(candidatesByDifficulty, difficulties, 0, initialState, false, List.of()));
    }

    /**
     * Selects one compatible challenge for every remaining difficulty using bounded backtracking.
     *
     * <p>Immutable copies are used for each branch so failed attempts cannot leak state into later
     * attempts.</p>
     *
     * @param candidatesByDifficulty  eligible challenges grouped by difficulty
     * @param difficulties            missing difficulty tiers
     * @param difficultyIndex         current difficulty index
     * @param state                   current selection state
     * @param requireUniqueCategories whether categories must remain unique
     * @param selectedChallenges      challenges selected by the current branch
     * @return complete selection when one exists
     */
    private static Optional<List<Challenge>> selectNextDifficulty(
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty,
        List<ChallengeDifficulty> difficulties,
        int difficultyIndex,
        WeeklyPackSelectionState state,
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
}
