package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCategory;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable compatibility data of one weekly pack selection branch.
 *
 * @param selectedTiers selected tiers
 * @param categories           selected categories
 * @param exclusionGroups      selected exclusion groups
 */
record WeeklyPackSelectionState(
    Set<ChallengeTier> selectedTiers,
    Set<ChallengeCategory> categories,
    Set<String> exclusionGroups
) {

    /**
     * Creates a state from persisted weekly selections.
     *
     * @param selections existing selections
     * @return initialized selection state
     */
    static WeeklyPackSelectionState from(List<ChallengeSelection> selections) {
        Set<ChallengeTier> tiers = EnumSet.noneOf(ChallengeTier.class);
        Set<ChallengeCategory> categories = EnumSet.noneOf(ChallengeCategory.class);
        Set<String> exclusionGroups = new HashSet<>();

        for (ChallengeSelection selection : selections) {
            Challenge challenge = selection.getChallenge();
            tiers.add(challenge.getTier());
            categories.add(challenge.getCategory());

            if (challenge.getExclusionGroup() != null) {
                exclusionGroups.add(challenge.getExclusionGroup());
            }
        }

        return new WeeklyPackSelectionState(
            Set.copyOf(tiers),
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
        Set<ChallengeTier> nextTiers = new HashSet<>(selectedTiers);
        Set<ChallengeCategory> nextCategories = new HashSet<>(categories);
        Set<String> nextExclusionGroups = new HashSet<>(exclusionGroups);

        nextTiers.add(challenge.getTier());
        nextCategories.add(challenge.getCategory());

        if (challenge.getExclusionGroup() != null) {
            nextExclusionGroups.add(challenge.getExclusionGroup());
        }

        return new WeeklyPackSelectionState(
            Set.copyOf(nextTiers),
            Set.copyOf(nextCategories),
            Set.copyOf(nextExclusionGroups)
        );
    }
}
