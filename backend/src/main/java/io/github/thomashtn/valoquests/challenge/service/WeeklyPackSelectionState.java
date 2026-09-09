package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable compatibility data of one weekly pack selection branch.
 *
 * @param selectedDifficulties selected difficulty tiers
 * @param categories           selected categories
 * @param exclusionGroups      selected exclusion groups
 */
record WeeklyPackSelectionState(
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
    static WeeklyPackSelectionState from(List<WeeklyChallenge> selections) {
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

        return new WeeklyPackSelectionState(
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
    boolean isCompatible(Challenge candidate, boolean requireUniqueCategories) {
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
    WeeklyPackSelectionState with(Challenge challenge) {
        Set<ChallengeDifficulty> nextDifficulties = copyDifficulties();
        Set<ChallengeCategory> nextCategories = copyCategories();
        Set<String> nextExclusionGroups = new HashSet<>(exclusionGroups);

        nextDifficulties.add(challenge.getDifficulty());
        nextCategories.add(challenge.getCategory());

        if (challenge.getExclusionGroup() != null) {
            nextExclusionGroups.add(challenge.getExclusionGroup());
        }

        return new WeeklyPackSelectionState(
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
