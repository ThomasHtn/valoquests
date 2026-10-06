package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Completes a weekly pack out of candidates grouped by tier.
 *
 * <p>Pure: no repository, no clock. Category diversity is preferred, exclusion groups are always
 * enforced, and the search is a bounded backtracking whose depth is the number of
 * tiers.</p>
 */
final class WeeklyPackSolver {

    /**
     * Not instantiable: static helpers only.
     */
    private WeeklyPackSolver() {
    }

    /**
     * Finds the tiers not already represented in a weekly pack.
     *
     * @param state current selection state
     * @return missing tiers in enum order
     */
    static List<ChallengeTier> missingTiers(WeeklyPackSelectionState state) {
        return EnumSet.allOf(ChallengeTier.class)
            .stream()
            .filter(tier -> !state.selectedTiers().contains(tier))
            .toList();
    }

    /**
     * Builds the best complete selection one candidate pool allows, preferring category diversity.
     *
     * @param candidatesByTier eligible candidates grouped by tier
     * @param tiers           missing tiers
     * @param initialState           state produced by existing selections
     * @return complete selection when the pool allows one
     */
    static Optional<List<Challenge>> solve(
        Map<ChallengeTier, List<Challenge>> candidatesByTier,
        List<ChallengeTier> tiers,
        WeeklyPackSelectionState initialState
    ) {
        return selectNextTier(candidatesByTier, tiers, 0, initialState, true, List.of())
            .or(() -> selectNextTier(candidatesByTier, tiers, 0, initialState, false, List.of()));
    }

    /**
     * Selects one compatible challenge for every remaining tier using bounded backtracking.
     *
     * <p>Immutable copies are used for each branch so failed attempts cannot leak state into later
     * attempts.</p>
     *
     * @param candidatesByTier  eligible challenges grouped by tier
     * @param tiers            missing tiers
     * @param tierIndex         current tier index
     * @param state                   current selection state
     * @param requireUniqueCategories whether categories must remain unique
     * @param selectedChallenges      challenges selected by the current branch
     * @return complete selection when one exists
     */
    private static Optional<List<Challenge>> selectNextTier(
        Map<ChallengeTier, List<Challenge>> candidatesByTier,
        List<ChallengeTier> tiers,
        int tierIndex,
        WeeklyPackSelectionState state,
        boolean requireUniqueCategories,
        List<Challenge> selectedChallenges
    ) {
        if (tierIndex == tiers.size()) {
            return Optional.of(List.copyOf(selectedChallenges));
        }

        ChallengeTier tier = tiers.get(tierIndex);

        for (Challenge candidate : candidatesByTier.getOrDefault(tier, List.of())) {
            if (!state.isCompatible(candidate, requireUniqueCategories)) {
                continue;
            }

            List<Challenge> nextSelection = new ArrayList<>(selectedChallenges.size() + 1);
            nextSelection.addAll(selectedChallenges);
            nextSelection.add(candidate);

            Optional<List<Challenge>> result = selectNextTier(
                candidatesByTier,
                tiers,
                tierIndex + 1,
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
