package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * No-repeat cycle of the weekly draw, replayed from past selections.
 *
 * <p>Cycles run per tier rather than over the catalogue as a whole: a pack draws exactly one
 * challenge per tier, so tiers empty at their own pace and a shared cycle would let the largest
 * one hold the smallest hostage. A tier holding a single enabled challenge clears on every draw,
 * which is what keeps it drawable at all.</p>
 */
final class WeeklyChallengeCycle {

    /**
     * Not instantiable: static helpers only.
     */
    private WeeklyChallengeCycle() {
    }

    /**
     * Drops the candidates already drawn in the current cycle of their own tier.
     *
     * @param candidatesByTier eligible candidates grouped by tier
     * @param pastSelections         weekly selections strictly before the week being drawn, oldest first
     * @return the same grouping, keeping only challenges the cycle has not used yet
     */
    static Map<ChallengeTier, List<Challenge>> withoutCurrentCycle(
        Map<ChallengeTier, List<Challenge>> candidatesByTier,
        List<ChallengeSelection> pastSelections
    ) {
        Map<ChallengeTier, Set<Long>> usedByTier =
            usedInCurrentCycle(candidatesByTier, pastSelections);

        Map<ChallengeTier, List<Challenge>> remaining = new EnumMap<>(ChallengeTier.class);

        candidatesByTier.forEach((tier, candidates) -> {
            Set<Long> used = usedByTier.getOrDefault(tier, Set.of());

            remaining.put(
                tier,
                candidates.stream()
                    .filter(candidate -> !used.contains(candidate.getId()))
                    .toList()
            );
        });

        return remaining;
    }

    /**
     * Replays every past selection to determine which challenges were already used in the cycle
     * still in progress, per tier, resetting whenever a tier's cycle completes.
     *
     * @param candidatesByTier eligible candidates grouped by tier
     * @param pastSelections         weekly selections strictly before the week being drawn, oldest first
     * @return identifiers used since each tier's last completed cycle
     */
    private static Map<ChallengeTier, Set<Long>> usedInCurrentCycle(
        Map<ChallengeTier, List<Challenge>> candidatesByTier,
        List<ChallengeSelection> pastSelections
    ) {
        Map<ChallengeTier, Set<Long>> usedByTier = new EnumMap<>(ChallengeTier.class);

        for (ChallengeSelection selection : pastSelections) {
            Challenge challenge = selection.getChallenge();
            ChallengeTier tier = challenge.getTier();

            Set<Long> used = usedByTier.computeIfAbsent(tier, key -> new HashSet<>());
            used.add(challenge.getId());

            int tierSize = candidatesByTier.getOrDefault(tier, List.of()).size();

            if (used.size() >= tierSize) {
                used.clear();
            }
        }

        return usedByTier;
    }
}
